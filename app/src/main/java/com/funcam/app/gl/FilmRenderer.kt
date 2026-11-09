package com.funcam.app.gl

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
class FilmRenderer {
    private val thread = HandlerThread("FunCamGL").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }

    private lateinit var egl: EglCore
    private lateinit var pbuffer: EGLSurface
    private lateinit var film: FilmProgram
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

    /** Current look; written from the UI thread. */
    @Volatile var params: RenderParams? = null
    /** Simulated shutter time in seconds. Longer than a frame => motion trails. */
    @Volatile var shutterSeconds: Float = 0f
    /** Image aspect shown in the viewfinder (may differ from params.ratio for multi-frame bodies). */
    @Volatile var previewImgAspect: Float = 0.75f
    @Volatile var seed: Float = Random.nextFloat() * 100f
    @Volatile var frozen = false

    init {
        handler.post {
            egl = EglCore()
            pbuffer = egl.createPbuffer()
            egl.makeCurrent(pbuffer)
            film = FilmProgram()
            inputProg = GlUtil.program(Shaders.VERTEX, Shaders.INPUT)
            copyProg = GlUtil.program(Shaders.VERTEX, Shaders.COPY)
            blank = GlUtil.blankTexture()
            halfFloat = GlUtil.supportsHalfFloatTargets()
        }
    }

    // ---------------------------------------------------------------- camera input

    fun provideSurface(request: SurfaceRequest, isFront: Boolean) {
        handler.post {
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

            val p = params ?: return
            val time = (now - startNs) / 1e9f

            // 3. viewfinder
            display?.let { surf ->
                egl.makeCurrent(surf)
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                GLES30.glViewport(0, 0, displayW, displayH)
                drawFilm(dst.tex, p, displayW, displayH, time, liveSeed(time, p))
                egl.swap(surf)
            }

            // 4. video encoder at the body's frame rate
            encoder?.let { surf ->
                val interval = 1_000_000_000L / encFps
                if (lastEncNs == 0L || now - lastEncNs >= interval - 4_000_000L) {
                    lastEncNs = if (lastEncNs == 0L) now else lastEncNs + interval.coerceAtMost(now - lastEncNs)
                    egl.makeCurrent(surf)
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    GLES30.glViewport(0, 0, encW, encH)
                    drawFilm(dst.tex, p, encW, encH, time, liveSeed(time, p))
                    egl.setPresentationTime(surf, now - encStartNs)
                    egl.swap(surf)
                }
            }
        } catch (e: Exception) {
            Log.e("FunCam", "render error", e)
        }
    }

    /** Video bodies get slowly wandering light leaks; photo bodies keep a fixed seed per shot. */
    private fun liveSeed(time: Float, p: RenderParams): Float =
        if (p.recipe.isVideo) seed + kotlin.math.floor(time / 3.5f) else seed

    private fun drawFilm(src: Int, p: RenderParams, w: Int, h: Int, time: Float, seed: Float) {
        val imgAspect = previewImgAspect
        val spec = FrameSpec.compute(p.recipe.border, imgAspect, p.borderOn)
        val crop = FrameSpec.crop(cameraAspect, imgAspect)
        updateStamp(p, imgAspect)
        val c2 = if (doubleTex != 0) FrameSpec.crop(doubleAspect, imgAspect) else null
        film.draw(src, crop, p, spec, w, h, time, seed, false, doubleTex, c2, stampTex, blank)
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
        handler.post {
            display?.let { egl.makeCurrent(pbuffer); egl.releaseSurface(it) }
            display = null
            if (surface != null && surface.isValid) {
                display = egl.createWindowSurface(surface)
                displayW = w; displayH = h
            }
        }
    }

    fun startEncoding(surface: Surface, w: Int, h: Int, fps: Int, startNs: Long) {
        handler.post {
            encoder = egl.createWindowSurface(surface)
            encW = w; encH = h; encFps = fps; encStartNs = startNs; lastEncNs = 0L
        }
    }

    fun stopEncoding(done: () -> Unit) {
        handler.post {
            encoder?.let { egl.makeCurrent(pbuffer); egl.releaseSurface(it) }
            encoder = null
            done()
        }
    }

    /** Returns the next raw (pre-film, accumulated) upright camera frame. */
    fun grabRaw(callback: (Bitmap) -> Unit) {
        handler.post { grabs.add(callback) }
    }

    /** Ghost image for double exposure, or null to clear. */
    fun setDoubleExposure(bmp: Bitmap?) {
        handler.post {
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
    fun resetAccumulation() = handler.post { accFresh = true }

    fun release() {
        handler.post {
            try {
                egl.makeCurrent(pbuffer)
                accA?.release(); accB?.release(); grabFbo?.release()
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
