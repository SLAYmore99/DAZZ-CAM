package com.funcam.app.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import com.funcam.app.video.SimpleVideoEncoder
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Develops full-resolution stills (and wiggle clips) off the UI thread with the same pipeline as the preview. */
class StillProcessor(private val context: Context) {
    private val thread = HandlerThread("FunCamDevelop").apply { start() }
    private val handler = Handler(thread.looper)
    private lateinit var egl: EglCore
    private lateinit var pbuffer: EGLSurface
    private lateinit var film: FilmPipeline
    private var maxTex = 4096

    init {
        if (!FilmRenderer.headless) handler.post {
            egl = EglCore()
            pbuffer = egl.createPbuffer()
            egl.makeCurrent(pbuffer)
            film = FilmPipeline(context)
            maxTex = GlUtil.maxTextureSize()
        }
    }

    private suspend fun <T> onGl(block: () -> T): T = suspendCancellableCoroutine { cont ->
        handler.post {
            try {
                egl.makeCurrent(pbuffer)
                cont.resume(block())
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }
    }

    /**
     * Applies [params] to [src]. [imgAspect] is the picture's aspect (frame excluded), [zoom] the digital crop,
     * [seed] the shot seed used by the preview (same leaks and grain), [stampTime] the burned-in date.
     */
    suspend fun develop(
        src: Bitmap, params: RenderParams, imgAspect: Float, seed: Int, stampTime: Long,
        double: Bitmap? = null, maxLong: Int = 4096, zoom: Float = 1f, frameNo: Int = 12,
    ): Bitmap = onGl {
        val srcTex = GlUtil.uploadBitmap(src)
        val dTex = double?.let { GlUtil.uploadBitmap(it) } ?: 0
        val spec = FrameSpec.compute(params.recipe.border, imgAspect, params.borderOn)
        val srcAspect = src.width.toFloat() / src.height
        val imgW = (if (imgAspect < srcAspect) src.height * imgAspect else src.width.toFloat()) / zoom
        var outW = imgW / (spec.rect[2] - spec.rect[0])
        var outH = outW / spec.outAspect
        val k = minOf(1f, minOf(maxLong, maxTex).toFloat() / maxOf(outW, outH))
        outW *= k; outH *= k
        val w = outW.toInt().coerceAtLeast(16)
        val h = outH.toInt().coerceAtLeast(16)

        val imgPxW = (w * (spec.rect[2] - spec.rect[0])).toInt().coerceIn(64, 2400)
        val stampBmp = StampRenderer.render(params.stampStyle, imgPxW, (imgPxW / imgAspect).toInt(), stampTime)
        val stampTex = stampBmp?.let { GlUtil.uploadBitmap(it) } ?: 0
        stampBmp?.recycle()
        val marksBmp = StampRenderer.marks(spec.border, params.recipe.edgeText, minOf(w, 2400), (minOf(w, 2400) / spec.outAspect).toInt(), spec.rect, frameNo)
        val marksTex = marksBmp?.let { GlUtil.uploadBitmap(it) } ?: 0
        marksBmp?.recycle()

        val fbo = Fbo(w, h)
        val crop = FrameSpec.crop(srcAspect, imgAspect, zoom)
        val c2 = double?.let { FrameSpec.crop(it.width.toFloat() / it.height, imgAspect) }
        film.render(srcTex, src.width, src.height, crop, params, spec, w, h, { fbo.bind() },
            time = 0f, seed = seed, leakSeed = seed, flipY = true, stampTex = stampTex, marksTex = marksTex, tex2 = dTex, tex2Crop = c2)
        fbo.bind()
        val out = GlUtil.readPixels(w, h)
        fbo.release()
        GlUtil.deleteTexture(srcTex); GlUtil.deleteTexture(dTex); GlUtil.deleteTexture(stampTex); GlUtil.deleteTexture(marksTex)
        out
    }

    /** Renders [frames] as a looping wobble clip into [out]. */
    suspend fun encodeWiggle(frames: List<Bitmap>, params: RenderParams, imgAspect: Float, seed: Int, out: File) = onGl {
        val spec = FrameSpec.compute(params.recipe.border, imgAspect, params.borderOn)
        var w = 1080
        var h = (w / spec.outAspect).toInt()
        w = w / 16 * 16; h = h / 16 * 16
        val enc = SimpleVideoEncoder(out, w, h, 12)
        val surf = egl.createWindowSurface(enc.inputSurface)
        val texes = frames.map { GlUtil.uploadBitmap(it) }
        val n = frames.size
        val order = (0 until n) + (n - 2 downTo 1)
        val base = FrameSpec.crop(frames[0].width.toFloat() / frames[0].height, imgAspect)
        // zoom in a touch so each "lens" can sit at a different sideways offset: the parallax wobble
        val crops = frames.indices.map { i ->
            val sw = base[2] * 0.9f
            val sh = base[3] * 0.9f
            val cx = base[0] + base[2] / 2f + (i - (n - 1) / 2f) * base[2] * 0.022f
            val cy = base[1] + base[3] / 2f
            floatArrayOf((cx - sw / 2f).coerceIn(0f, 1f - sw), (cy - sh / 2f).coerceIn(0f, 1f - sh), sw, sh)
        }
        var pts = 0L
        repeat(8) {
            for (i in order) {
                film.render(texes[i], frames[i].width, frames[i].height, crops[i], params, spec, w, h, {
                    egl.makeCurrent(surf)
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    GLES30.glViewport(0, 0, w, h)
                }, time = 0f, seed = seed, leakSeed = seed, flipY = false, stampTex = 0, marksTex = 0)
                egl.setPresentationTime(surf, pts)
                egl.swap(surf)
                enc.drain(false)
                egl.makeCurrent(pbuffer)
                pts += 83_333_333L
            }
        }
        egl.makeCurrent(surf)
        enc.drain(true)
        egl.makeCurrent(pbuffer)
        egl.releaseSurface(surf)
        enc.release()
        texes.forEach { GlUtil.deleteTexture(it) }
    }

    fun release() = handler.post {
        try { film.release(); egl.releaseSurface(pbuffer); egl.release() } catch (_: Exception) {}
        thread.quitSafely()
    }
}
