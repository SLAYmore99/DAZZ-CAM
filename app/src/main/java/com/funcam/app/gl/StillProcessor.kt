package com.funcam.app.gl

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

/** Develops full-resolution stills (and wiggle clips) off the UI thread with the same film shader. */
class StillProcessor {
    private val thread = HandlerThread("FunCamDevelop").apply { start() }
    private val handler = Handler(thread.looper)
    private lateinit var egl: EglCore
    private lateinit var pbuffer: EGLSurface
    private lateinit var film: FilmProgram
    private var blank = 0
    private var maxTex = 4096

    init {
        handler.post {
            egl = EglCore()
            pbuffer = egl.createPbuffer()
            egl.makeCurrent(pbuffer)
            film = FilmProgram()
            blank = GlUtil.blankTexture()
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
     * Applies [params] to [src]. [imgAspect] is the picture's aspect (border excluded).
     * [stampTime] is the date burned into the frame.
     */
    suspend fun develop(
        src: Bitmap, params: RenderParams, imgAspect: Float, seed: Float,
        stampTime: Long, double: Bitmap? = null, maxLong: Int = 4096,
    ): Bitmap = onGl {
        val srcTex = GlUtil.uploadBitmap(src)
        val dTex = double?.let { GlUtil.uploadBitmap(it) } ?: 0
        val spec = FrameSpec.compute(params.recipe.border, imgAspect, params.borderOn)
        val srcAspect = src.width.toFloat() / src.height
        // picture pixel size after centre crop
        val imgW = if (imgAspect < srcAspect) src.height * imgAspect else src.width.toFloat()
        var outW = imgW / (spec.rect[2] - spec.rect[0])
        var outH = outW / spec.outAspect
        val limit = minOf(maxLong, maxTex).toFloat()
        val scale = minOf(1f, limit / maxOf(outW, outH))
        outW *= scale; outH *= scale
        val w = outW.toInt().coerceAtLeast(16)
        val h = outH.toInt().coerceAtLeast(16)

        val imgPxW = (w * (spec.rect[2] - spec.rect[0])).toInt().coerceIn(64, 2048)
        val stampBmp = StampRenderer.render(params.stampStyle, imgPxW, (imgPxW / imgAspect).toInt(), stampTime)
        val stampTex = stampBmp?.let { GlUtil.uploadBitmap(it) } ?: 0
        stampBmp?.recycle()

        val fbo = Fbo(w, h)
        fbo.bind()
        val crop = FrameSpec.crop(srcAspect, imgAspect)
        val c2 = double?.let { FrameSpec.crop(it.width.toFloat() / it.height, imgAspect) }
        film.draw(srcTex, crop, params, spec, w, h, seed * 3.7f, seed, true, dTex, c2, stampTex, blank)
        val out = GlUtil.readPixels(w, h)
        fbo.release()
        GlUtil.deleteTexture(srcTex); GlUtil.deleteTexture(dTex); GlUtil.deleteTexture(stampTex)
        out
    }

    /** Renders [frames] as a looping "wiggle" clip (0,1,2,1,0,...) into [out]. */
    suspend fun encodeWiggle(frames: List<Bitmap>, params: RenderParams, imgAspect: Float, seed: Float, out: File) =
        onGl {
            val spec = FrameSpec.compute(params.recipe.border, imgAspect, params.borderOn)
            var w = 1080
            var h = (w / spec.outAspect).toInt()
            w = w / 16 * 16; h = h / 16 * 16
            val enc = SimpleVideoEncoder(out, w, h, 10)
            val surf = egl.createWindowSurface(enc.inputSurface)
            egl.makeCurrent(surf)
            val texes = frames.map { GlUtil.uploadBitmap(it) }
            val order = listOf(0, 1, 2, 1).map { it.coerceAtMost(frames.size - 1) }
            val base = FrameSpec.crop(frames[0].width.toFloat() / frames[0].height, imgAspect)
            // zoom in slightly so each "lens" can be offset sideways for a parallax wobble
            val crops = frames.indices.map { i ->
                val sw = base[2] * 0.92f
                val sh = base[3] * 0.92f
                val cx = base[0] + base[2] / 2f + (i - 1) * base[2] * 0.025f
                val cy = base[1] + base[3] / 2f
                floatArrayOf((cx - sw / 2f).coerceIn(0f, 1f - sw), (cy - sh / 2f).coerceIn(0f, 1f - sh), sw, sh)
            }
            var pts = 0L
            repeat(10) {
                for (i in order) {
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    GLES30.glViewport(0, 0, w, h)
                    film.draw(texes[i], crops[i], params, spec, w, h, pts / 1e9f, seed, false, 0, null, 0, blank)
                    egl.setPresentationTime(surf, pts)
                    egl.swap(surf)
                    enc.drain(false)
                    pts += 100_000_000L
                }
            }
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
