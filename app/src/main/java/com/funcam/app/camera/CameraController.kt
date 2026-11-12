package com.funcam.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.funcam.app.gl.FilmRenderer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CameraCaps(
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val manualSensor: Boolean = false,
    val isoRange: Range<Int>? = null,
    val exposureRange: Range<Long>? = null,
    val hasFlash: Boolean = false,
    val stabilization: Boolean = false,
)

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalCamera2Interop::class)
class CameraController(private val context: Context, private val renderer: FilmRenderer) {
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    var isFront = false
        private set
    var caps = CameraCaps()
        private set

    suspend fun bind(owner: LifecycleOwner, front: Boolean, stabilize: Boolean): CameraCaps {
        val p = provider ?: awaitProvider().also { provider = it }
        isFront = front
        p.unbindAll()
        val selector = if (front && p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        isFront = selector == CameraSelector.DEFAULT_FRONT_CAMERA

        val previewRes = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1440), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
            .build()
        val pv = Preview.Builder().setResolutionSelector(previewRes).build()
        val front2 = isFront
        pv.setSurfaceProvider(ContextCompat.getMainExecutor(context)) { req -> renderer.provideSurface(req, front2) }

        val captureRes = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()
        val ic = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(captureRes)
            .build()

        val cam = p.bindToLifecycle(owner, selector, pv, ic)
        camera = cam; preview = pv; imageCapture = ic

        val stabSupported = try {
            Preview.getPreviewCapabilities(cam.cameraInfo).isStabilizationSupported
        } catch (_: Exception) { false }
        if (stabSupported && stabilize) {
            // rebind with stabilization enabled
            p.unbindAll()
            val pv2 = Preview.Builder().setResolutionSelector(previewRes).setPreviewStabilizationEnabled(true).build()
            pv2.setSurfaceProvider(ContextCompat.getMainExecutor(context)) { req -> renderer.provideSurface(req, front2) }
            camera = p.bindToLifecycle(owner, selector, pv2, ic)
            preview = pv2
        }

        val info = Camera2CameraInfo.from(camera!!.cameraInfo)
        val capabilities = info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val manual = capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
        val zoom = camera!!.cameraInfo.zoomState.value
        caps = CameraCaps(
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            manualSensor = manual,
            isoRange = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE),
            exposureRange = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE),
            hasFlash = camera!!.cameraInfo.hasFlashUnit(),
            stabilization = stabSupported,
        )
        return caps
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try { cont.resume(future.get()) } catch (e: Exception) { cont.resumeWithException(e) }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(caps.minZoom, caps.maxZoom))
    }

    fun zoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    fun setTorch(on: Boolean) {
        if (caps.hasFlash) camera?.cameraControl?.enableTorch(on)
    }

    /**
     * Tap to focus. (x, y) are 0..1 in the upright camera frame, y down.
     */
    fun focus(x: Float, y: Float, rotation: Int, lock: Boolean = false) {
        val cam = camera ?: return
        val ux = if (isFront) 1f - x else x
        val (bx, by) = when (rotation) {
            90 -> y to 1f - ux
            180 -> 1f - ux to 1f - y
            270 -> 1f - y to ux
            else -> ux to y
        }
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(bx, by)
        val builder = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        // locked focus and exposure stay until unlockFocus(); a plain tap returns to automatic after a few seconds
        if (lock) builder.disableAutoCancel() else builder.setAutoCancelDuration(4, TimeUnit.SECONDS)
        cam.cameraControl.startFocusAndMetering(builder.build())
    }

    fun unlockFocus() {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    /**
     * Manual sensor control. iso = 0 means auto. Exposures longer than one frame are
     * completed by the renderer's frame accumulation.
     */
    fun setManual(iso: Int, shutterSeconds: Float) {
        val cam = camera ?: return
        val c2 = Camera2CameraControl.from(cam.cameraControl)
        val b = CaptureRequestOptions.Builder()
        if (iso > 0 && caps.manualSensor) {
            val isoR = caps.isoRange
            val expR = caps.exposureRange
            val isoV = isoR?.let { iso.coerceIn(it.lower, it.upper) } ?: iso
            var expNs = (minOf(shutterSeconds, 1f / 15f) * 1e9).toLong()
            if (expR != null) expNs = expNs.coerceIn(expR.lower, expR.upper)
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoV)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expNs)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(expNs, 33_333_333L))
        }
        c2.setCaptureRequestOptions(b.build())
    }

    /** Full-resolution still, upright and mirrored for the front camera. */
    suspend fun takePicture(flash: Boolean): Bitmap = suspendCancellableCoroutine { cont ->
        val ic = imageCapture ?: run { cont.resumeWithException(IllegalStateException("not bound")); return@suspendCancellableCoroutine }
        ic.flashMode = if (flash && caps.hasFlash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        ic.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val raw = image.toBitmap()
                    val m = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        if (isFront) postScale(-1f, 1f)
                    }
                    val out = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                    if (out !== raw) raw.recycle()
                    cont.resume(out)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resumeWithException(exception)
            }
        })
    }

    fun unbind() {
        provider?.unbindAll()
    }
}
