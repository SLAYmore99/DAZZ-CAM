package com.funcam.app.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaActionSound
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.funcam.app.camera.CameraCaps
import com.funcam.app.camera.CameraController
import com.funcam.app.gl.FilmRenderer
import com.funcam.app.gl.FrameSpec
import com.funcam.app.gl.RenderParams
import com.funcam.app.gl.StillProcessor
import com.funcam.app.media.MediaSaver
import com.funcam.app.media.Negative
import com.funcam.app.media.Negatives
import com.funcam.app.model.Accessories
import com.funcam.app.model.CaptureMode
import com.funcam.app.model.Cameras
import com.funcam.app.model.InstantFrame
import com.funcam.app.model.Tweak
import com.funcam.app.model.Ratio
import com.funcam.app.model.Recipe
import com.funcam.app.video.LiveRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.random.Random

class CamViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("funcam", Context.MODE_PRIVATE)

    val renderer = FilmRenderer(app)
    val processor = StillProcessor(app)
    val controller = CameraController(app, renderer)
    private val sound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    // ---------------------------------------------------------------- state
    var recipe by mutableStateOf(Cameras.byId(prefs.getString("recipe", null)))
        private set
    var ratioIndex by mutableIntStateOf(0)
        private set
    var acc by mutableStateOf(Accessories(lightLeaks = prefs.getBoolean("leaks", true)))
        private set
    var flashOn by mutableStateOf(recipe.flashDefault)
    var timer by mutableIntStateOf(0)
    var grid by mutableStateOf(prefs.getBoolean("grid", false))
    var exposure by mutableFloatStateOf(0f)
    var temp by mutableFloatStateOf(0f)
    var tint by mutableFloatStateOf(0f)
    var iso by mutableIntStateOf(0)
    var shutter by mutableFloatStateOf(0f)
    var fps by mutableIntStateOf(30)
    var front by mutableStateOf(false)
        private set
    var zoom by mutableFloatStateOf(1f)
        private set
    var caps by mutableStateOf(CameraCaps())
        private set

    // settings
    var stampOn by mutableStateOf(prefs.getBoolean("stamp", true))
    var borderOn by mutableStateOf(prefs.getBoolean("border", true))
    var saveNegatives by mutableStateOf(prefs.getBoolean("negatives", true))
    var shutterSound by mutableStateOf(prefs.getBoolean("sound", true))
    var stabilize by mutableStateOf(prefs.getBoolean("stabilize", true))
    var recordAudio by mutableStateOf(prefs.getBoolean("audio", true))
    /** Per-camera customisation for the current camera. */
    var tweak by mutableStateOf(Tweak.decode(prefs.getString("tw_" + recipe.id, null)))
        private set

    fun updateTweak(t: Tweak) {
        tweak = t
        prefs.edit().putString("tw_" + recipe.id, t.encode()).apply()
        sync()
    }

    /** True when the shutter is slower than a normal frame: motion trails / long exposure. */
    val slowShutter: Boolean get() = shutter > 1 / 30f

    var instantFrame by mutableStateOf(runCatching { InstantFrame.valueOf(prefs.getString("iframe", "WHITE")!!) }.getOrDefault(InstantFrame.WHITE))
    /** Extra preview rotation in 90° steps — a fallback for devices that report orientation oddly. */
    var previewRotationFix by mutableIntStateOf(prefs.getInt("rotfix", 0))
        private set

    fun cyclePreviewRotation() {
        previewRotationFix = (previewRotationFix + 1) % 4
        prefs.edit().putInt("rotfix", previewRotationFix).apply()
        renderer.rotationFix = previewRotationFix
        renderer.resetAccumulation()
    }

    // capture status
    var busy by mutableStateOf(false)
        private set
    var countdown by mutableIntStateOf(0)
        private set
    var flashBlink by mutableIntStateOf(0)
        private set
    var multiProgress by mutableStateOf<String?>(null)
        private set
    var recording by mutableStateOf(false)
        private set
    var recordStart by mutableLongStateOf(0L)
        private set
    var lastThumb by mutableStateOf<Bitmap?>(null)
        private set
    var lastUri by mutableStateOf<Uri?>(null)
        private set
    var lastIsVideo by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    private var owner: LifecycleOwner? = null
    private val pendingFrames = ArrayList<Bitmap>()
    private var doubleFirst: Bitmap? = null
    private var recorder: LiveRecorder? = null

    val ratio: Ratio get() = recipe.ratios[ratioIndex.coerceIn(0, recipe.ratios.lastIndex)]

    /** Aspect of the picture area shown in the viewfinder. */
    val previewImgAspect: Float get() = ratio.value

    val frameSpec: FrameSpec get() = FrameSpec.compute(recipe.border, previewImgAspect, borderOn)

    init {
        ratioIndex = prefs.getInt("ratio_" + recipe.id, 0)
        applyRecipeDefaults(recipe)
        renderer.rotationFix = previewRotationFix
        sync()
    }

    // ---------------------------------------------------------------- camera lifecycle

    fun bindCamera(owner: LifecycleOwner) {
        this.owner = owner
        if (FilmRenderer.headless) return
        viewModelScope.launch {
            try {
                caps = controller.bind(owner, front, stabilize)
                zoom = 1f
                controller.setZoom(1f)
                applyManual()
            } catch (e: Exception) {
                message = "Camera unavailable: ${e.message}"
            }
        }
    }

    fun flipCamera() {
        if (recording) return
        front = !front
        owner?.let { bindCamera(it) }
    }

    fun setZoomRatio(r: Float) {
        zoom = r.coerceIn(caps.minZoom, caps.maxZoom)
        controller.setZoom(zoom)
    }

    fun tapFocus(x: Float, y: Float) {
        // map viewfinder (picture-area) coords to the full upright camera frame
        val crop = FrameSpec.crop(renderer.cameraAspect, previewImgAspect)
        controller.focus(crop[0] + x * crop[2], crop[1] + y * crop[3], renderer.rotationDegrees)
    }

    // ---------------------------------------------------------------- selections

    fun selectRecipe(r: Recipe) {
        if (recording) stopRecording()
        if (r.id == recipe.id) return
        resetMulti()
        recipe = r
        tweak = Tweak.decode(prefs.getString("tw_" + r.id, null))
        ratioIndex = prefs.getInt("ratio_" + r.id, 0).coerceIn(0, r.ratios.lastIndex)
        prefs.edit().putString("recipe", r.id).apply()
        applyRecipeDefaults(r)
        flashOn = r.flashDefault
        controller.setTorch(r.isVideo && flashOn)
        sync()
    }

    /** Swipe on the viewfinder: next / previous camera. */
    fun stepCamera(dir: Int) {
        val list = Cameras.all
        val i = list.indexOfFirst { it.id == recipe.id }
        selectRecipe(list[(i + dir + list.size) % list.size])
    }

    private fun applyRecipeDefaults(r: Recipe) {
        when (r.id) {
            "dreamblur" -> { shutter = 1 / 4f; fps = 12; iso = 160 }
            // shutter / ISO stay as you set them across cameras (the SLOW badge shows when it's on)
            else -> { fps = r.videoFps }
        }
        applyManual()
    }

    fun cycleRatio() {
        ratioIndex = (ratioIndex + 1) % recipe.ratios.size
        prefs.edit().putInt("ratio_" + recipe.id, ratioIndex).apply()
        resetMulti()
        sync()
    }

    fun updateAcc(a: Accessories) {
        if (a.doubleExposure != acc.doubleExposure) resetMulti()
        acc = a
        prefs.edit().putBoolean("leaks", a.lightLeaks).apply()
        sync()
    }

    fun toggleFlash() {
        flashOn = !flashOn
        if (recipe.isVideo) controller.setTorch(flashOn)
        sync()
    }

    fun cycleTimer() {
        timer = when (timer) { 0 -> 3; 3 -> 10; else -> 0 }
    }

    fun setManualValues(iso: Int = this.iso, shutter: Float = this.shutter, fps: Int = this.fps) {
        this.iso = iso; this.shutter = shutter; this.fps = fps
        applyManual()
        renderer.resetAccumulation()
        sync()
    }

    private fun applyManual() {
        controller.setManual(iso, shutter)
    }

    fun resetAdjustments() {
        exposure = 0f; temp = 0f; tint = 0f; sync()
    }

    fun saveSettings() {
        prefs.edit()
            .putBoolean("stamp", stampOn).putBoolean("border", borderOn)
            .putBoolean("negatives", saveNegatives).putBoolean("sound", shutterSound)
            .putBoolean("stabilize", stabilize).putBoolean("audio", recordAudio)
            .putBoolean("grid", grid).putString("iframe", instantFrame.name).apply()
        sync()
    }

    fun rebindForStabilization() {
        saveSettings()
        owner?.let { bindCamera(it) }
    }

    fun params(flashFired: Boolean = flashOn, r: Recipe = recipe, rt: Ratio = ratio) = RenderParams(
        recipe = r, ratio = rt, acc = acc, borderOn = borderOn, stampOn = stampOn,
        exposure = exposure, temp = temp, tint = tint, flashFired = flashFired, instantFrame = instantFrame,
        tweak = if (r.id == recipe.id) tweak else Tweak.decode(prefs.getString("tw_" + r.id, null)),
    )

    /** Push the current look to the live renderer. */
    fun sync() {
        renderer.params = params(flashFired = flashOn && !recipe.isVideo)
        renderer.previewImgAspect = previewImgAspect
        renderer.shutterSeconds = shutter
    }

    private fun resetMulti() {
        pendingFrames.forEach { it.recycle() }
        pendingFrames.clear()
        doubleFirst = null
        renderer.setDoubleExposure(null)
        multiProgress = null
    }

    // ---------------------------------------------------------------- shutter

    fun onShutter() {
        if (busy && !recording) return
        if (recipe.isVideo) {
            if (recording) stopRecording() else viewModelScope.launch { countdownThen { startRecording() } }
            return
        }
        viewModelScope.launch { countdownThen { shootPhoto() } }
    }

    private suspend fun countdownThen(block: suspend () -> Unit) {
        if (timer > 0) {
            busy = true
            for (i in timer downTo 1) { countdown = i; delay(1000) }
            countdown = 0
            busy = false
        }
        block()
    }

    private fun click() {
        if (shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
        flashBlink++
    }

    private suspend fun grabRaw(): Bitmap = suspendCancellableCoroutine { c -> renderer.grabRaw { c.resume(it) } }

    private suspend fun shootPhoto() {
        busy = true
        val r = recipe
        val fired = flashOn
        val seed = renderer.seed
        try {
            when (r.capture) {
                CaptureMode.SINGLE -> {
                    val longExposure = shutter > 1 / 30f
                    click()
                    val raw = if (longExposure) {
                        if (fired) controller.setTorch(true)
                        renderer.resetAccumulation()
                        delay((shutter * 1000).toLong() + 120)
                        grabRaw().also { if (fired) controller.setTorch(false) }
                    } else {
                        try { controller.takePicture(fired) } catch (e: Exception) { grabRaw() }
                    }
                    if (acc.doubleExposure) {
                        val first = doubleFirst
                        if (first == null) {
                            doubleFirst = raw
                            val ghostScale = minOf(1f, 1440f / maxOf(raw.width, raw.height))
                            renderer.setDoubleExposure(
                                Bitmap.createScaledBitmap(raw, (raw.width * ghostScale).toInt(), (raw.height * ghostScale).toInt(), true)
                            )
                            multiProgress = "1/2"
                            message = "Double exposure: take the second shot"
                            return
                        }
                        doubleFirst = null
                        renderer.setDoubleExposure(null)
                        multiProgress = null
                        developAndSave(raw, params(fired, r), ratio.value, seed, System.currentTimeMillis(), first)
                        first.recycle()
                    } else {
                        developAndSave(raw, params(fired, r), ratio.value, seed, System.currentTimeMillis(), null)
                    }
                }
                CaptureMode.HALF, CaptureMode.COLLAGE -> {
                    val need = if (r.capture == CaptureMode.HALF) 2 else 4
                    click()
                    val raw = try { controller.takePicture(fired) } catch (e: Exception) { grabRaw() }
                    pendingFrames.add(raw)
                    if (pendingFrames.size < need) { multiProgress = "${pendingFrames.size}/$need"; return }
                    val composite = if (need == 2) compose(pendingFrames, 2, 1, ratio.value)
                    else compose(pendingFrames, 2, 2, ratio.value, background = Color.rgb(246, 244, 238), gapFrac = 0.04f, padFrac = 0.06f)
                    resetMulti()
                    developAndSave(composite, params(fired, r), composite.width.toFloat() / composite.height, seed,
                        System.currentTimeMillis(), null)
                }
                CaptureMode.GRID16 -> {
                    val frames = ArrayList<Bitmap>()
                    if (fired) controller.setTorch(true)
                    click()
                    repeat(16) { i ->
                        multiProgress = "${i + 1}/16"
                        frames.add(grabRaw())
                        delay(110)
                    }
                    if (fired) controller.setTorch(false)
                    multiProgress = null
                    val composite = compose(frames, 4, 4, ratio.value, gapFrac = 0.02f)
                    frames.forEach { it.recycle() }
                    developAndSave(composite, params(fired, r), composite.width.toFloat() / composite.height, seed,
                        System.currentTimeMillis(), null)
                }
                CaptureMode.WIGGLE -> {
                    val frames = ArrayList<Bitmap>()
                    click()
                    repeat(4) { frames.add(grabRaw()); delay(90) }
                    val file = File(ctx.cacheDir, "wiggle_${System.currentTimeMillis()}.mp4")
                    processor.encodeWiggle(frames, params(fired, r), ratio.value, seed, file)
                    val thumb = withContext(Dispatchers.IO) { videoThumb(file) }
                    val uri = withContext(Dispatchers.IO) { MediaSaver.saveVideo(ctx, file) }
                    frames.forEach { it.recycle() }
                    onSaved(uri, thumb, true)
                }
            }
        } catch (e: Exception) {
            message = "Capture failed: ${e.message}"
        } finally {
            busy = false
            renderer.seed = Random.nextInt(1, Int.MAX_VALUE)
        }
    }

    /** Lays frames out on black with thin gaps, each centre-cropped to [cell] aspect. */
    private suspend fun compose(
        frames: List<Bitmap>, cols: Int, rows: Int, cell: Float,
        background: Int = Color.rgb(12, 11, 10), gapFrac: Float = 0.03f, padFrac: Float = 0f,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val cellH = minOf(frames.minOf { it.height }, (frames.minOf { it.width } / cell).toInt(), 2400 / rows)
            val cellW = (cellH * cell).toInt()
            val gap = (cellH * gapFrac).toInt()
            val pad = (cellH * padFrac).toInt()
            val out = Bitmap.createBitmap(cols * cellW + (cols - 1) * gap + 2 * pad, rows * cellH + (rows - 1) * gap + 2 * pad, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawColor(background)
            c.translate(pad.toFloat(), pad.toFloat())
            frames.forEachIndexed { i, f ->
                val fa = f.width.toFloat() / f.height
                val srcRect = if (fa > cell) {
                    val w = (f.height * cell).toInt(); val x = (f.width - w) / 2
                    Rect(x, 0, x + w, f.height)
                } else {
                    val h = (f.width / cell).toInt(); val y = (f.height - h) / 2
                    Rect(0, y, f.width, y + h)
                }
                val col = i % cols; val row = i / cols
                val dst = Rect(col * (cellW + gap), row * (cellH + gap), col * (cellW + gap) + cellW, row * (cellH + gap) + cellH)
                c.drawBitmap(f, srcRect, dst, null)
            }
            out
        }

    private suspend fun developAndSave(
        raw: Bitmap, p: RenderParams, imgAspect: Float, seed: Int, time: Long, double: Bitmap?,
    ) {
        val out = processor.develop(raw, p, imgAspect, seed, time, double)
        val uri = withContext(Dispatchers.IO) {
            if (saveNegatives && double == null) {
                Negatives.save(ctx, raw, Negative(
                    id = time.toString(), image = File(""), recipeId = p.recipe.id, ratioW = p.ratio.w, ratioH = p.ratio.h,
                    imgAspect = imgAspect, exposure = p.exposure, temp = p.temp, tint = p.tint, flash = p.flashFired,
                    seed = seed.toFloat(), time = time,
                ))
            }
            MediaSaver.saveJpeg(ctx, out, p.recipe.name)
        }
        raw.recycle()
        val thumb = Bitmap.createScaledBitmap(out, 240, (240 * out.height / out.width.toFloat()).toInt(), true)
        out.recycle()
        onSaved(uri, thumb, false)
    }

    private fun onSaved(uri: Uri?, thumb: Bitmap?, video: Boolean) {
        if (uri == null) { message = "Couldn't save to gallery"; return }
        lastUri = uri; lastThumb = thumb; lastIsVideo = video
    }

    private fun videoThumb(file: File): Bitmap? = try {
        MediaMetadataRetriever().run {
            setDataSource(file.absolutePath)
            val f = getFrameAtTime(0)
            release()
            f?.let { Bitmap.createScaledBitmap(it, 240, (240 * it.height / it.width.toFloat()).toInt(), true) }
        }
    } catch (_: Exception) { null }

    // ---------------------------------------------------------------- video

    private fun startRecording() {
        val r = recipe
        val spec = frameSpec
        val long = 1280
        var w: Int; var h: Int
        if (spec.outAspect < 1f) { h = long; w = (long * spec.outAspect).toInt() } else { w = long; h = (long / spec.outAspect).toInt() }
        w = (w / 16) * 16; h = (h / 16) * 16
        val outFps = fps
        val file = File(ctx.cacheDir, "rec_${System.currentTimeMillis()}.mp4")
        val audioOk = recordAudio && ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        try {
            val rec = LiveRecorder(file, w, h, outFps, audioOk)
            recorder = rec
            renderer.startEncoding(rec.inputSurface, w, h, outFps, rec.startNs)
            if (flashOn) controller.setTorch(true)
            if (shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
            recording = true
            recordStart = System.currentTimeMillis()
        } catch (e: Exception) {
            message = "Recording failed: ${e.message}"
            recorder = null
        }
    }

    fun stopRecording() {
        val rec = recorder ?: return
        recorder = null
        recording = false
        busy = true
        if (shutterSound) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        renderer.stopEncoding {
            viewModelScope.launch(Dispatchers.IO) {
                val ok = rec.finish()
                if (!ok) {
                    withContext(Dispatchers.Main) { message = "Recording was too short"; busy = false }
                    return@launch
                }
                val thumb = videoThumb(rec.out)
                val uri = MediaSaver.saveVideo(ctx, rec.out)
                withContext(Dispatchers.Main) {
                    onSaved(uri, thumb, true)
                    busy = false
                }
            }
        }
        if (!flashOn || !recipe.isVideo) controller.setTorch(false)
    }

    // ---------------------------------------------------------------- import / darkroom

    fun importPhoto(uri: Uri) {
        val r = recipe
        if (r.isVideo || !r.canImport) { message = "${r.name} can't develop imported photos — pick a photo camera"; return }
        viewModelScope.launch {
            busy = true
            try {
                val loaded = withContext(Dispatchers.IO) { MediaSaver.loadBitmap(ctx, uri) }
                if (loaded == null) { message = "Couldn't open that photo"; return@launch }
                val (bmp, taken) = loaded
                developAndSave(bmp, params(false, r), ratio.value, Random.nextInt(1, Int.MAX_VALUE),
                    taken ?: System.currentTimeMillis(), null)
                message = "Developed with ${r.name}"
            } catch (e: Exception) {
                message = "Import failed: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    fun negatives(): List<Negative> = Negatives.list(ctx)

    fun deleteNegative(n: Negative) = Negatives.delete(ctx, n)

    /** Re-develop a stored negative with another camera and adjustments. */
    suspend fun preview(n: Negative, r: Recipe, exp: Float, tmp: Float, tnt: Float, maxLong: Int = 1200): Bitmap? {
        val src = withContext(Dispatchers.IO) { Negatives.load(n) } ?: return null
        val original = Ratio(n.ratioW, n.ratioH)
        val composite = Cameras.byId(n.recipeId).capture != CaptureMode.SINGLE
        val rt = if (composite || original in r.ratios) original else r.ratios.first()
        val p = RenderParams(r, rt, acc, borderOn, stampOn, exp, tmp, tnt, n.flash)
        val aspect = if (composite) n.imgAspect else rt.value
        return processor.develop(src, p, aspect, n.seed.toInt(), n.time, null, maxLong).also { src.recycle() }
    }

    suspend fun redevelop(n: Negative, r: Recipe, exp: Float, tmp: Float, tnt: Float): Boolean {
        val full = preview(n, r, exp, tmp, tnt, 4096) ?: return false
        val uri = withContext(Dispatchers.IO) { MediaSaver.saveJpeg(ctx, full, r.name) }
        val thumb = Bitmap.createScaledBitmap(full, 240, (240 * full.height / full.width.toFloat()).toInt(), true)
        full.recycle()
        onSaved(uri, thumb, false)
        return uri != null
    }

    override fun onCleared() {
        recorder?.let { rec -> renderer.stopEncoding { rec.finish() } }
        controller.unbind()
        renderer.release()
        processor.release()
        sound.release()
    }
}
