package com.funcam.app.gl

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import com.funcam.app.model.Accessories
import com.funcam.app.model.Border
import com.funcam.app.model.InstantFrame
import com.funcam.app.model.Ratio
import com.funcam.app.model.Recipe
import com.funcam.app.model.StampStyle
import com.funcam.app.model.Tweak
import com.funcam.app.model.V3
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Everything needed to render a camera's look. */
data class RenderParams(
    val recipe: Recipe,
    val ratio: Ratio,
    val acc: Accessories = Accessories(),
    val borderOn: Boolean = true,
    val stampOn: Boolean = true,
    val exposure: Float = 0f,
    val temp: Float = 0f,
    val tint: Float = 0f,
    val flashFired: Boolean = false,
    val instantFrame: InstantFrame = InstantFrame.WHITE,
    val tweak: Tweak = Tweak(),
) {
    val stampStyle: StampStyle get() = if (stampOn) recipe.stamp else StampStyle.NONE
}

/** Output layout: overall aspect and where the picture sits inside its frame (GL coords, y up). */
data class FrameSpec(val outAspect: Float, val rect: FloatArray, val imgAspect: Float, val border: Border) {
    companion object {
        fun compute(border: Border, imgAspect: Float, borderOn: Boolean): FrameSpec {
            val b = if (borderOn) border else when (border) {
                // gates and exposed sprockets are part of the image itself, keep them
                Border.GATE8, Border.GATE16, Border.SPROCKET, Border.TOY -> border
                else -> Border.NONE
            }
            if (b == Border.SLIDE) {
                val winW = 1f
                val winH = 1f / imgAspect
                val size = maxOf(winW, winH) * 1.36f
                val x0 = (size - winW) / 2f / size
                val y0 = (size - winH) / 2f / size
                return FrameSpec(1f, floatArrayOf(x0, y0, 1f - x0, 1f - y0), imgAspect, b)
            }
            // margins in units of picture width: side, top, bottom
            val m = when (b) {
                Border.INSTANT -> when {
                    imgAspect < 0.85f -> floatArrayOf(0.087f, 0.15f, 0.37f)   // mini
                    imgAspect < 1.2f -> floatArrayOf(0.08f, 0.11f, 0.27f)     // square
                    else -> floatArrayOf(0.045f, 0.07f, 0.17f)                // wide
                }
                Border.BLACK -> floatArrayOf(0.05f, 0.05f, 0.05f)
                Border.NEGATIVE -> floatArrayOf(0.03f, 0.15f, 0.15f)
                Border.GATE8 -> floatArrayOf(0.035f, 0.035f, 0.035f)
                Border.GATE16 -> floatArrayOf(0.02f, 0.02f, 0.02f)
                Border.TOY -> floatArrayOf(0.03f, 0.03f, 0.03f)
                Border.SPROCKET -> floatArrayOf(0.012f, 0.012f, 0.012f)
                else -> floatArrayOf(0f, 0f, 0f)
            }
            val (side, top, bottom) = Triple(m[0], m[1], m[2])
            val w = 1f + 2 * side
            val h = 1f / imgAspect + top + bottom
            return FrameSpec(w / h, floatArrayOf(side / w, bottom / h, (side + 1f) / w, (bottom + 1f / imgAspect) / h), imgAspect, b)
        }

        fun frameColor(b: Border, instant: InstantFrame): V3 = when (b) {
            Border.INSTANT -> argb(instant.color)
            Border.SLIDE -> V3(0.93f, 0.91f, 0.86f)
            Border.NONE -> V3.ZERO
            else -> V3(0.035f, 0.032f, 0.03f)
        }

        private fun argb(c: Long) = V3(((c shr 16) and 0xFF) / 255f, ((c shr 8) and 0xFF) / 255f, (c and 0xFF) / 255f)

        /** Centre crop of a source with aspect [src] to [dst], then digital [zoom]: offset.xy, scale.zw. */
        fun crop(src: Float, dst: Float, zoom: Float = 1f): FloatArray {
            val c = if (dst < src) floatArrayOf((1f - dst / src) / 2f, 0f, dst / src, 1f)
            else floatArrayOf(0f, (1f - src / dst) / 2f, 1f, src / dst)
            if (zoom <= 1f) return c
            val sw = c[2] / zoom
            val sh = c[3] / zoom
            return floatArrayOf(c[0] + (c[2] - sw) / 2f, c[1] + (c[3] - sh) / 2f, sw, sh)
        }
    }
}

/** Loads 33³ film LUTs from assets into GL 3D textures (one cache per GL context). */
class LutCache(private val context: Context) {
    private val cache = HashMap<String, Int>()
    val identity: Int by lazy { upload(null) }

    fun get(name: String?): Int {
        if (name == null) return identity
        return cache.getOrPut(name) {
            try {
                val bmp = context.assets.open("luts/$name.png").use { BitmapFactory.decodeStream(it) }
                upload(bmp).also { bmp.recycle() }
            } catch (e: Exception) {
                identity
            }
        }
    }

    private fun upload(bmp: android.graphics.Bitmap?): Int {
        val n = 33
        val buf = ByteBuffer.allocateDirect(n * n * n * 3).order(ByteOrder.nativeOrder())
        if (bmp != null) {
            val px = IntArray(n * n * n)
            bmp.getPixels(px, 0, n, 0, 0, n, n * n)
            for (c in px) { buf.put(((c shr 16) and 0xFF).toByte()); buf.put(((c shr 8) and 0xFF).toByte()); buf.put((c and 0xFF).toByte()) }
        } else {
            for (b in 0 until n) for (g in 0 until n) for (r in 0 until n) {
                buf.put((r * 255 / (n - 1)).toByte()); buf.put((g * 255 / (n - 1)).toByte()); buf.put((b * 255 / (n - 1)).toByte())
            }
        }
        buf.position(0)
        val t = IntArray(1)
        GLES30.glGenTextures(1, t, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, t[0])
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage3D(GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB8, n, n, n, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buf)
        for (p in intArrayOf(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_TEXTURE_WRAP_R))
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, p, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)
        return t[0]
    }

    fun release() {
        val ids = (cache.values + identity).distinct().toIntArray()
        GLES30.glDeleteTextures(ids.size, ids, 0)
        cache.clear()
    }
}

private class Program(vs: String, fs: String) {
    val id = GlUtil.program(vs, fs)
    private val locs = HashMap<String, Int>()
    fun u(n: String) = locs.getOrPut(n) { GLES30.glGetUniformLocation(id, n) }
    fun use() = GLES30.glUseProgram(id)
    fun f(n: String, v: Float) = GLES30.glUniform1f(u(n), v)
    fun i(n: String, v: Int) = GLES30.glUniform1i(u(n), v)
    fun ui(n: String, v: Int) = GLES30.glUniform1ui(u(n), v)
    fun v2(n: String, a: Float, b: Float) = GLES30.glUniform2f(u(n), a, b)
    fun v3(n: String, v: V3) = GLES30.glUniform3f(u(n), v.x, v.y, v.z)
    fun v4(n: String, v: FloatArray) = GLES30.glUniform4f(u(n), v[0], v[1], v[2], v[3])
    fun tex(n: String, unit: Int, target: Int, t: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, t)
        i(n, unit)
    }
}

/**
 * The multi-pass film look: GRADE -> DOWN -> BLUR(H,V) -> COMPOSITE. Must be used on one GL thread.
 */
class FilmPipeline(context: Context) {
    val luts = LutCache(context)
    private val grade = Program(Shaders.VERTEX, Shaders.GRADE)
    private val down = Program(Shaders.VERTEX, Shaders.DOWN)
    private val blur = Program(Shaders.VERTEX, Shaders.BLUR)
    private val comp = Program(Shaders.VERTEX, Shaders.COMPOSITE)
    private val blank = GlUtil.blankTexture()
    private var gradeFbo: Fbo? = null
    private var qa: Fbo? = null
    private var qb: Fbo? = null
    private var wa: Fbo? = null
    private var wb: Fbo? = null

    private fun ensure(w: Int, h: Int) {
        if (gradeFbo?.width != w || gradeFbo?.height != h) {
            gradeFbo?.release(); gradeFbo = Fbo(w, h)
        }
        val qw = maxOf(1, w / 4)
        val qh = maxOf(1, h / 4)
        if (qa?.width != qw || qa?.height != qh) {
            qa?.release(); qb?.release(); qa = Fbo(qw, qh); qb = Fbo(qw, qh)
            wa?.release(); wb?.release()
            wa = Fbo(maxOf(1, qw / 4), maxOf(1, qh / 4)); wb = Fbo(maxOf(1, qw / 4), maxOf(1, qh / 4))
        }
    }

    /**
     * Renders into the framebuffer that [bindTarget] binds (it must also set the viewport to outW x outH).
     * [seed] varies grain per frame/shot; [leakSeed] picks the light-leak shape.
     */
    fun render(
        src: Int, srcW: Int, srcH: Int, srcCrop: FloatArray, p: RenderParams, spec: FrameSpec, outW: Int, outH: Int,
        bindTarget: () -> Unit, time: Float, seed: Int, leakSeed: Int, flipY: Boolean,
        stampTex: Int, marksTex: Int, tex2: Int = 0, tex2Crop: FloatArray? = null,
    ) {
        val l = p.recipe.look
        val a = p.acc
        val t = p.tweak
        val k = t.intensity
        fun dev(v: Float, neutral: Float) = neutral + (v - neutral) * k
        fun dev3(v: V3, neutral: Float) = V3(dev(v.x, neutral), dev(v.y, neutral), dev(v.z, neutral))
        val imgW = maxOf(16, Math.round(outW * (spec.rect[2] - spec.rect[0])))
        val imgH = maxOf(16, Math.round(outH * (spec.rect[3] - spec.rect[1])))
        ensure(imgW, imgH)
        val g = gradeFbo!!
        val qa = qa!!
        val qb = qb!!

        // ---- 1. grade (picture size)
        g.bind()
        grade.use()
        grade.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, src)
        grade.v4("uSrcCrop", srcCrop)
        grade.v2("uSrcTexel", 1f / srcW, 1f / srcH)
        grade.tex("uSrc2", 1, GLES30.GL_TEXTURE_2D, if (tex2 != 0) tex2 else blank)
        grade.v4("uSrc2Crop", tex2Crop ?: floatArrayOf(0f, 0f, 1f, 1f))
        grade.f("uDouble", if (tex2 != 0) 1f else 0f)
        grade.tex("uLut", 2, GLES30.GL_TEXTURE_3D, luts.get(l.lut))
        grade.f("uLutMix", if (l.lut == null) 0f else (l.lutMix * k).coerceIn(0f, 1f))
        grade.f("uAspect", spec.imgAspect)
        grade.f("uTime", time)
        grade.ui("uSeed", seed)
        grade.f("uExposure", dev(l.exposure, 0f) + p.exposure - if (a.nd) 1.3f else 0f)
        grade.f("uTemp", dev(l.temp, 0f) + p.temp)
        grade.f("uTint", dev(l.tint, 0f) + p.tint)
        grade.f("uContrast", dev(l.contrast, 1f))
        grade.f("uSaturation", dev(l.saturation, 1f).coerceAtLeast(0f))
        grade.f("uFade", dev(l.fade, 0f).coerceAtLeast(0f))
        grade.f("uMono", l.mono)
        grade.v3("uLift", dev3(l.lift, 0f))
        grade.v3("uGain", dev3(l.gain, 1f))
        grade.v3("uShadowTint", dev3(l.shadowTint, 0f))
        grade.v3("uHighTint", dev3(l.highTint, 0f))
        grade.f("uFlash", if (p.flashFired) l.flash else 0f)
        grade.v3("uFlashColor", a.gel.color)
        grade.i("uFisheye", a.fisheyeMode)
        grade.f("uChroma", l.chroma * k)
        grade.f("uPixel", l.pixel)
        grade.f("uSharpen", l.sharpen)
        grade.f("uPrism", if (a.prism) 1f else 0f)
        grade.f("uVhs", l.vhs)
        grade.f("uWeave", l.weave)
        grade.f("uDistort", if (com.funcam.app.model.Engine.v4) l.distort * k else 0f)
        grade.f("uRolloff", if (com.funcam.app.model.Engine.v4) l.rolloff else 0f)
        GlUtil.drawQuad()

        // ---- 2. downsample + 3. gaussian blur at quarter size
        qa.bind()
        down.use()
        down.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, g.tex)
        down.v2("uTexel", 1f / qa.width, 1f / qa.height)
        GlUtil.drawQuad()
        val sigma = maxOf(1.5f, 0.018f * minOf(qa.width, qa.height))
        qb.bind()
        blur.use()
        blur.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, qa.tex)
        blur.v2("uStep", 1f / qa.width, 0f)
        blur.f("uSigma", sigma)
        GlUtil.drawQuad()
        qa.bind()
        blur.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, qb.tex)
        blur.v2("uStep", 0f, 1f / qa.height)
        GlUtil.drawQuad()

        // ---- 3b. engine v4: a very wide blur at 1/16 size, only for looks that use it
        val v4 = com.funcam.app.model.Engine.v4
        val glowWide = if (v4) l.glowWide * t.glow else 0f
        val halWide = if (v4) l.halWide * t.glow else 0f
        val mist = if (v4) (l.mist * t.soft + if (a.mist) 0.85f else 0f) else 0f
        val wide = glowWide + halWide + mist > 0f
        val wa = wa!!
        if (wide) {
            val wb = wb!!
            wb.bind()
            down.use()
            down.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, qa.tex)
            down.v2("uTexel", 1f / wb.width, 1f / wb.height)
            GlUtil.drawQuad()
            val ws = maxOf(1.5f, 0.07f * minOf(wb.width, wb.height))
            wa.bind()
            blur.use()
            blur.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, wb.tex)
            blur.v2("uStep", 1f / wb.width, 0f)
            blur.f("uSigma", ws)
            GlUtil.drawQuad()
            wb.bind()
            blur.tex("uSrc", 0, GLES30.GL_TEXTURE_2D, wa.tex)
            blur.v2("uStep", 0f, 1f / wb.height)
            GlUtil.drawQuad()
        }

        // ---- 4. composite into the target
        bindTarget()
        comp.use()
        comp.tex("uGraded", 0, GLES30.GL_TEXTURE_2D, g.tex)
        comp.tex("uBlur", 1, GLES30.GL_TEXTURE_2D, qa.tex)
        comp.tex("uStamp", 2, GLES30.GL_TEXTURE_2D, if (stampTex != 0) stampTex else blank)
        comp.f("uStampOn", if (stampTex != 0) 1f else 0f)
        comp.tex("uFrameTex", 3, GLES30.GL_TEXTURE_2D, if (marksTex != 0) marksTex else blank)
        comp.f("uFrameTexOn", if (marksTex != 0) 1f else 0f)
        comp.v2("uRes", outW.toFloat(), outH.toFloat())
        comp.f("uFlipY", if (flipY) 1f else 0f)
        comp.v4("uImgRect", spec.rect)
        comp.f("uAspect", spec.imgAspect)
        comp.i("uBorder", spec.border.id)
        comp.v3("uFrameColor", FrameSpec.frameColor(spec.border, p.instantFrame))
        comp.f("uTime", time)
        comp.ui("uSeed", seed)
        comp.ui("uLeakSeed", leakSeed)
        comp.f("uBloom", l.bloom * t.glow)
        comp.f("uHalation", l.halation * t.glow)
        comp.v3("uHalationColor", l.halationColor)
        comp.f("uSoft", l.soft * t.soft)
        comp.f("uSoftAll", l.softAll * t.soft)
        comp.f("uStar", if (a.star) 1f else 0f)
        comp.f("uVignette", (l.vignette * t.vignette).coerceAtMost(1.2f))
        comp.f("uLeak", if (a.lightLeaks) l.leak * t.leaks else 0f)
        comp.f("uLeakAnim", if (p.recipe.isVideo) 1f else 0f)
        comp.v3("uLeakColor", l.leakColor)
        comp.f("uGrain", l.grain * t.grain)
        comp.f("uGrainSize", l.grainSize)
        comp.f("uNoise", l.noise)
        comp.f("uScan", l.scan)
        comp.f("uVhs", l.vhs)
        comp.f("uFlicker", l.flicker)
        comp.f("uDust", l.dust)
        comp.f("uInterlace", l.interlace)
        comp.tex("uWide", 4, GLES30.GL_TEXTURE_2D, if (wide) wb!!.tex else blank)
        comp.f("uGlowWide", glowWide)
        comp.f("uHalWide", halWide)
        comp.f("uMist", mist.coerceAtMost(1.6f))
        comp.f("uStreak", if (v4) l.streak + (if (a.streak) 1f else 0f) else 0f)
        comp.f("uSpeck", if (v4 && !p.recipe.isVideo) l.speck * t.grain.coerceAtMost(1.5f) else 0f)
        comp.f("uPaper", if (v4) l.paper else 0f)
        GlUtil.drawQuad()
    }

    fun release() {
        gradeFbo?.release(); qa?.release(); qb?.release(); wa?.release(); wb?.release()
        luts.release()
        GlUtil.deleteTexture(blank)
    }
}
