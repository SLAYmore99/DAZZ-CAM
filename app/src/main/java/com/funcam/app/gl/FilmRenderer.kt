package com.funcam.app.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor
import kotlin.random.Random

/**
 * Owns the GL thread for the live camera:
 * camera (OES) -> accumulation buffer (slow shutter) -> film look -> viewfinder + video encoder.
 */
class FilmRenderer(private val context: Context) {
    private val thread = HandlerThread("FunCamGL").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }

    /** Runs on the GL thread. In headless mode (JVM screenshot tests) there is no GL, so nothing runs. */
    private fun post(block: () -> Unit) { if (!headless) handler.post(block) }

    companion object {
        /** Set by the JVM screenshot tests: no camera and no GL. */
        @Volatile var headless = false
    }


    private lateinit var egl: EglCore
    private lateinit var pbuffer: EGLSurface
    private lateinit var film: FilmPipeline
    private var outFbo: Fbo? = null
    private var marksTex = 0
    private var marksKey = ""
    private var frameCount = 0
    private var inputProg = 0
    private var copyProg = 0
    private var blank = 0
    private var halfFloat = false

    private var camTex = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var bufW = 0
    private var bufH = 0
    /** Total clockwise rotation the sensor buffer needs to be upright (from CameraX). */
    @Volatile private var rotation = 0
    val rotationDegrees: Int get() = rotation
    /** Rotation our input shader still has to apply (the SurfaceTexture matrix may already hold the camera transform). */
    @Volatile private var shaderRotation = 0
    @Volatile private var mirror = false
    /** User calibration in 90° steps, a safety net for unusual devices. */
    @Volatile var rotationFix = 0

    private var accA: Fbo? = null
    private var accB: Fbo? = null
    private var accCurrent: Fbo? = null
    private var accFresh = true
    private var grabFbo: Fbo? = null

    private var display: EGLSurface? = null
    private var displayW = 0
    private var displayH = 0

    private var encoder: EGLSurface? = null
    private var encW = 0
    private var encH = 0
    private var encFps = 30
    private var encStartNs = 0L
    private var lastEncNs = 0L

    private var doubleTex = 0
    private var doubleAspect = 1f
    private var stampTex = 0
    private var stampKey = ""

    private val stMatrix = FloatArray(16)
    private val startNs = System.nanoTime()
    private var lastFrameNs = 0L
    private var frameDt = 1f / 30f
    private val grabs = ArrayList<(Bitmap) -> Unit>()

    /** Light meter: called on the GL thread a few times a second with mean RGB (0..1) and a 24-bin brightness histogram. */
    @Volatile var onMeter: ((FloatArray, FloatArray) -> Unit)? = null
    private var meterFbo: Fbo? = null
    private val meterBuf = java.nio.ByteBuffer.allocateDirect(32 * 24 * 4).order(java.nio.ByteOrder.nativeOrder())

    /** Current look; written from the UI thread. */
    @Volatile var params: RenderParams? = null
    /** Simulated shutter time in seconds. Longer than a frame => motion trails. */
    @Volatile var shutterSeconds: Float = 0f
    /** Image aspect shown in the viewfinder (may differ from params.ratio for multi-frame bodies). */
    @Volatile var previewImgAspect: Float = 0.75f
    /** Seed of the next shot: decides light-leak shape and grain, so the preview matches the photo. */
    @Volatile var seed: Int = Random.nextInt(1, Int.MAX_VALUE)
    @Volatile var zoom: Float = 1f
    @Volatile var frozen = false

    init {
        post {
            egl = EglCore()
            pbuffer = egl.createPbuffer()
            egl.makeCurrent(pbuffer)
            film = FilmPipeline(context)
            inputProg = GlUtil.program(Shaders.VERTEX, Shaders.INPUT)
            copyProg = GlUtil.program(Shaders.VERTEX, Shaders.COPY)
            blank = GlUtil.blankTexture()
            halfFloat = GlUtil.supportsHalfFloatTargets()
        }
    }

    // ---------------------------------------------------------------- camera input

    fun provideSurface(request: SurfaceRequest, isFront: Boolean) {
        post {
            egl.makeCurrent(pbuffer)
            val res = request.resolution
            val tex = GlUtil.oesTexture()
            val st = SurfaceTexture(tex)
            st.setDefaultBufferSize(res.width, res.height)
            st.setOnFrameAvailableListener({ onFrame(it) }, handler)
            val surface = Surface(st)
            mirror = isFront
            bufW = res.width
            bufH = res.height
            request.setTransformationInfoListener(executor) { info ->
                rotation = info.rotationDegrees
                // When the camera writes its own transform into the SurfaceTexture (the usual case), the
                // transform matrix already rotates (and, for the front camera, mirrors) the image upright.
                if (info.hasCameraTransform()) {
                    shaderRotation = 0
                    mirror = false
                } else {
                    shaderRotation = info.rotationDegrees
                    mirror = info.isMirroring
                }
                accFresh = true
            }
            val old = surfaceTexture
            surfaceTexture = st
            camTex = tex
            accFresh = true
            request.provideSurface(surface, executor) {
                if (surfaceTexture === st) surfaceTexture = null
                surface.release()
                st.release()
                try {
                    egl.makeCurrent(pbuffer)
                    GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
                } catch (_: Exception) {
                }
            }
            if (old != null && old !== st) old.setOnFrameAvailableListener(null)
        }
    }

    /** Upright camera frame size. */
    private fun uprightSize(): Pair<Int, Int> {
        val r = (rotation + rotationFix * 90) % 360
        return if (r == 90 || r == 270) bufH to bufW else bufW to bufH
    }

    val cameraAspect: Float
        get() {
            val (w, h) = uprightSize()
            return if (h == 0) 0.75f else w.toFloat() / h
        }

    private fun ensureAcc(w: Int, h: Int) {
        if (accA?.width == w && accA?.height == h) return
        accA?.release(); accB?.release(); grabFbo?.release()
        accA = try { Fbo(w, h, halfFloat) } catch (e: Exception) { halfFloat = false; Fbo(w, h) }
        accB = Fbo(w, h, halfFloat)
        grabFbo = Fbo(w, h)
        accCurrent = null
        accFresh = true
    }

    private fun onFrame(st: SurfaceTexture) {
        if (st !== surfaceTexture) return
        try {
            egl.makeCurrent(pbuffer)
            st.updateTexImage()
            if (frozen) return
            st.getTransformMatrix(stMatrix)
            val now = System.nanoTime()
            if (lastFrameNs != 0L) {
                val dt = ((now - lastFrameNs) / 1e9f).coerceIn(0.005f, 0.5f)
                frameDt = frameDt * 0.9f + dt * 0.1f
            }
            lastFrameNs = now
            val (w, h) = uprightSize()
            if (w == 0 || h == 0) return
            ensureAcc(w, h)

            // 1. input + accumulation
            val dst = if (accCurrent === accA) accB!! else accA!!
            val prev = accCurrent ?: dst
            dst.bind()
            GLES30.glUseProgram(inputProg)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camTex)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(inputProg, "uCam"), 0)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (prev === dst) blank else prev.tex)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(inputProg, "uPrev"), 1)
            GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(inputProg, "uSt"), 1, false, stMatrix, 0)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(inputProg, "uRot"), (shaderRotation + rotationFix * 90) % 360)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(inputProg, "uMirror"), if (mirror) 1f else 0f)
            val shutter = shutterSeconds
            val alpha = if (accFresh || prev === dst || shutter <= frameDt) 1f
            else (frameDt / shutter).coerceIn(0.02f, 1f)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(inputProg, "uAlpha"), alpha)
            GlUtil.drawQuad()
            accCurrent = dst
            accFresh = false

            // 2. pending raw grabs (for long exposures / multi-lens bodies)
            if (grabs.isNotEmpty()) {
                val g = grabFbo!!
                g.bind()
                copy(dst.tex, flipY = true)
                val bmp = GlUtil.readPixels(g.width, g.height)
                val list = ArrayList(grabs); grabs.clear()
                list.forEach { it(bmp) }
            }

            // 2b. light meter + histogram from a tiny copy of the frame
            val meterCb = onMeter
            if (meterCb != null && frameCount % 8 == 0) {
                val m = meterFbo ?: Fbo(32, 24).also { meterFbo = it }
                m.bind()
                copy(dst.tex, flipY = false)
                meterBuf.rewind()
                GLES30.glReadPixels(0, 0, 32, 24, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, meterBuf)
                val rgb = FloatArray(3)
                val hist = FloatArray(24)
                for (i in 0 until 32 * 24) {
                    val r = (meterBuf.get(i * 4).toInt() and 0xFF) / 255f
                    val g = (meterBuf.get(i * 4 + 1).toInt() and 0xFF) / 255f
                    val b = (meterBuf.get(i * 4 + 2).toInt() and 0xFF) / 255f
                    rgb[0] += r; rgb[1] += g; rgb[2] += b
                    hist[minOf(23, ((0.2126f * r + 0.7152f * g + 0.0722f * b) * 24).toInt())]++
                }
                for (k in 0..2) rgb[k] /= 32f * 24f
                val mx = hist.max().coerceAtLeast(1f)
                for (k in hist.indices) hist[k] /= mx
                meterCb(rgb, hist)
            }

            val p = params ?: return
            val time = (now - startNs) / 1e9f
            frameCount++
            val enc = encoder
            val encodeNow = enc != null && (lastEncNs == 0L || now - lastEncNs >= 1_000_000_000L / encFps - 4_000_000L)

            if (enc != null) {
                // render once at encoder size, then show the same pixels in the viewfinder
                if (outFbo?.width != encW || outFbo?.height != encH) { outFbo?.release(); outFbo = Fbo(encW, encH) }
                val o = outFbo!!
                drawFilm(dst.tex, p, encW, encH, time, { o.bind() })
                if (encodeNow) {
                    lastEncNs = if (lastEncNs == 0L) now else lastEncNs + (1_000_000_000L / encFps).coerceAtMost(now - lastEncNs)
                    egl.makeCurrent(enc)
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    GLES30.glViewport(0, 0, encW, encH)
                    copy(o.tex, flipY = false)
                    egl.setPresentationTime(enc, now - encStartNs)
                    egl.swap(enc)
                }
                display?.let { surf ->
                    egl.makeCurrent(surf)
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    GLES30.glViewport(0, 0, displayW, displayH)
                    copy(o.tex, flipY = false)
                    egl.swap(surf)
                }
            } else {
                display?.let { surf ->
                    egl.makeCurrent(surf)
                    drawFilm(dst.tex, p, displayW, displayH, time, {
                        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                        GLES30.glViewport(0, 0, displayW, displayH)
                    })
                    egl.swap(surf)
                }
            }
        } catch (e: Exception) {
            Log.e("FunCam", "render error", e)
        }
    }

    private fun drawFilm(src: Int, p: RenderParams, w: Int, h: Int, time: Float, bindTarget: () -> Unit) {
        val imgAspect = previewImgAspect
        val spec = FrameSpec.compute(p.recipe.border, imgAspect, p.borderOn)
        val crop = FrameSpec.crop(cameraAspect, imgAspect, zoom)
        updateStamp(p, imgAspect)
        updateMarks(p, spec)
        val c2 = if (doubleTex != 0) FrameSpec.crop(doubleAspect, imgAspect) else null
        val (sw, sh) = uprightSize()
        // grain dances live (like a real viewfinder of film); the leak shape stays put for the next shot
        film.render(src = src, srcW = sw, srcH = sh, srcCrop = crop, p = p, spec = spec, outW = w, outH = h,
            bindTarget = bindTarget, time = time, seed = seed + frameCount, leakSeed = seed, flipY = false,
            stampTex = stampTex, marksTex = marksTex, tex2 = doubleTex, tex2Crop = c2)
    }

    private fun updateMarks(p: RenderParams, spec: FrameSpec) {
        val k = "${spec.border}|${p.recipe.edgeText}|${spec.outAspect}"
        if (k == marksKey) return
        marksKey = k
        val w = 1080
        val h = (1080 / spec.outAspect).toInt()
        val bmp = StampRenderer.marks(spec.border, p.recipe.edgeText, w, h, spec.rect, 12)
        if (bmp == null) { GlUtil.deleteTexture(marksTex); marksTex = 0 } else { marksTex = GlUtil.uploadBitmap(bmp, marksTex); bmp.recycle() }
    }

    private fun updateStamp(p: RenderParams, imgAspect: Float) {
        val now = System.currentTimeMillis()
        val k = StampRenderer.key(p.stampStyle, now) + imgAspect
        if (k == stampKey) return
        stampKey = k
        val w = 1080
        val h = (1080 / imgAspect).toInt()
        val bmp = StampRenderer.render(p.stampStyle, w, h, now)
        if (bmp == null) {
            GlUtil.deleteTexture(stampTex); stampTex = 0
        } else {
            stampTex = GlUtil.uploadBitmap(bmp, stampTex)
            bmp.recycle()
        }
    }

    private fun copy(tex: Int, flipY: Boolean) {
        GLES30.glUseProgram(copyProg)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(copyProg, "uTex"), 0)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(copyProg, "uFlipY"), if (flipY) 1f else 0f)
        GlUtil.drawQuad()
    }

    // ---------------------------------------------------------------- outputs

    fun setDisplay(surface: Surface?, w: Int, h: Int) {
        post {
            display?.let { egl.makeCurrent(pbuffer); egl.releaseSurface(it) }
            display = null
            if (surface != null && surface.isValid) {
                display = egl.createWindowSurface(surface)
                displayW = w; displayH = h
            }
        }
    }

    fun startEncoding(surface: Surface, w: Int, h: Int, fps: Int, startNs: Long) {
        post {
            encoder = egl.createWindowSurface(surface)
            encW = w; encH = h; encFps = fps; encStartNs = startNs; lastEncNs = 0L
        }
    }

    fun stopEncoding(done: () -> Unit) {
        post {
            encoder?.let { egl.makeCurrent(pbuffer); egl.releaseSurface(it) }
            encoder = null
            done()
        }
    }

    /** Returns the next raw (pre-film, accumulated) upright camera frame. */
    fun grabRaw(callback: (Bitmap) -> Unit) {
        post { grabs.add(callback) }
    }

    /** Ghost image for double exposure, or null to clear. */
    fun setDoubleExposure(bmp: Bitmap?) {
        post {
            egl.makeCurrent(pbuffer)
            if (bmp == null) {
                GlUtil.deleteTexture(doubleTex); doubleTex = 0
            } else {
                doubleTex = GlUtil.uploadBitmap(bmp, doubleTex)
                doubleAspect = bmp.width.toFloat() / bmp.height
            }
        }
    }

    /** Clear the accumulation buffer, e.g. when the shutter speed changes. */
    fun resetAccumulation() = post { accFresh = true }

    fun release() {
        post {
            try {
                egl.makeCurrent(pbuffer)
                accA?.release(); accB?.release(); grabFbo?.release(); outFbo?.release(); meterFbo?.release()
                film.release()
                display?.let { egl.releaseSurface(it) }
                encoder?.let { egl.releaseSurface(it) }
                surfaceTexture?.release()
                egl.releaseSurface(pbuffer)
                egl.release()
            } catch (_: Exception) {
            }
            thread.quitSafely()
        }
    }
}
