package com.funcam.app.gl

import android.opengl.GLES30
import com.funcam.app.model.Accessories
import com.funcam.app.model.Border
import com.funcam.app.model.Ratio
import com.funcam.app.model.Recipe
import com.funcam.app.model.StampStyle
import com.funcam.app.model.V3

/** Everything needed to render one frame of a camera's look. */
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
    val grainScale: Float = 1f,
) {
    val stampStyle: StampStyle get() = if (stampOn) recipe.stamp else StampStyle.NONE
}

/** Output frame layout: overall aspect and where the picture sits inside its border (GL coords). */
data class FrameSpec(val outAspect: Float, val rect: FloatArray, val imgAspect: Float, val border: Border) {
    companion object {
        fun compute(border: Border, imgAspect: Float, borderOn: Boolean): FrameSpec {
            val b = if (borderOn) border else Border.NONE
            // margins in units of image width
            val (side, top, bottom) = when (b) {
                Border.NONE -> Triple(0f, 0f, 0f)
                Border.INSTANT -> Triple(0.06f, 0.07f, 0.26f)
                Border.FILM -> Triple(0.035f, 0.11f, 0.11f)
                Border.SLIDE -> Triple(0.16f, 0f, 0f)
            }
            if (b == Border.SLIDE) {
                // square card mount with a centred window
                val winW = 1f
                val winH = 1f / imgAspect
                val size = maxOf(winW, winH) * 1.36f
                val x0 = (size - winW) / 2f / size
                val y0 = (size - winH) / 2f / size
                return FrameSpec(1f, floatArrayOf(x0, y0, 1f - x0, 1f - y0), imgAspect, b)
            }
            val w = 1f + 2 * side
            val h = 1f / imgAspect + top + bottom
            val rect = floatArrayOf(side / w, bottom / h, (side + 1f) / w, (bottom + 1f / imgAspect) / h)
            return FrameSpec(w / h, rect, imgAspect, b)
        }

        fun borderColor(b: Border): V3 = when (b) {
            Border.INSTANT -> V3(0.95f, 0.94f, 0.91f)
            Border.FILM -> V3(0.07f, 0.06f, 0.055f)
            Border.SLIDE -> V3(0.93f, 0.91f, 0.86f)
            Border.NONE -> V3.ZERO
        }

        /** Centre-crop of a source with aspect [src] to [dst]: offset.xy, scale.zw. */
        fun crop(src: Float, dst: Float): FloatArray =
            if (dst < src) {
                val s = dst / src
                floatArrayOf((1f - s) / 2f, 0f, s, 1f)
            } else {
                val s = src / dst
                floatArrayOf(0f, (1f - s) / 2f, 1f, s)
            }
    }
}

class FilmProgram {
    private val prog = GlUtil.program(Shaders.VERTEX, Shaders.FILM)
    private val locs = HashMap<String, Int>()
    private fun u(n: String) = locs.getOrPut(n) { GLES30.glGetUniformLocation(prog, n) }
    private fun f(n: String, v: Float) = GLES30.glUniform1f(u(n), v)
    private fun v3(n: String, v: V3) = GLES30.glUniform3f(u(n), v.x, v.y, v.z)

    /**
     * Draws into the currently bound framebuffer/viewport of size [w]x[h].
     * [srcCrop] maps image-local uv into the source texture.
     */
    fun draw(
        src: Int, srcCrop: FloatArray, p: RenderParams, spec: FrameSpec, w: Int, h: Int,
        time: Float, seed: Float, flipY: Boolean,
        tex2: Int, tex2Crop: FloatArray?, stampTex: Int, blank: Int,
    ) {
        val l = p.recipe.look
        val a = p.acc
        GLES30.glUseProgram(prog)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, src)
        GLES30.glUniform1i(u("uTex"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (tex2 != 0) tex2 else blank)
        GLES30.glUniform1i(u("uTex2"), 1)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (stampTex != 0) stampTex else blank)
        GLES30.glUniform1i(u("uStamp"), 2)

        f("uDouble", if (tex2 != 0) 1f else 0f)
        val c2 = tex2Crop ?: floatArrayOf(0f, 0f, 1f, 1f)
        GLES30.glUniform4f(u("uSrc2Crop"), c2[0], c2[1], c2[2], c2[3])
        f("uStampOn", if (stampTex != 0) 1f else 0f)
        GLES30.glUniform2f(u("uRes"), w.toFloat(), h.toFloat())
        f("uTime", time)
        f("uSeed", seed)
        f("uFlipY", if (flipY) 1f else 0f)
        GLES30.glUniform4f(u("uImgRect"), spec.rect[0], spec.rect[1], spec.rect[2], spec.rect[3])
        f("uAspect", spec.imgAspect)
        v3("uBorderColor", FrameSpec.borderColor(spec.border))
        GLES30.glUniform1i(u("uBorder"), spec.border.id)
        GLES30.glUniform4f(u("uSrcCrop"), srcCrop[0], srcCrop[1], srcCrop[2], srcCrop[3])

        f("uExposure", l.exposure + p.exposure - if (a.nd) 1.3f else 0f)
        f("uContrast", l.contrast)
        f("uSaturation", l.saturation)
        f("uTemp", l.temp + p.temp)
        f("uTint", l.tint + p.tint)
        f("uFade", l.fade)
        f("uGamma", l.gamma)
        v3("uLift", l.lift)
        v3("uGain", l.gain)
        v3("uShadowTint", l.shadowTint)
        v3("uHighTint", l.highTint)
        f("uMono", l.mono)
        f("uCross", l.cross)
        f("uHalation", l.halation)
        v3("uHalationColor", l.halationColor)
        f("uBloom", l.bloom)
        f("uGrain", l.grain * p.grainScale)
        f("uGrainSize", l.grainSize)
        f("uVignette", l.vignette)
        f("uLeak", if (a.lightLeaks) l.leak else 0f)
        v3("uLeakColor", l.leakColor)
        f("uChroma", l.chroma)
        f("uSoft", l.soft)
        f("uSharpen", l.sharpen)
        f("uPixel", l.pixel)
        f("uScan", l.scan)
        f("uVhs", l.vhs)
        f("uWeave", l.weave)
        f("uFlicker", l.flicker)
        f("uDust", l.dust)
        f("uFisheye", if (a.fisheye) 1f else 0f)
        f("uPrism", if (a.prism) 1f else 0f)
        f("uStar", if (a.star) 1f else 0f)
        f("uFlash", if (p.flashFired) l.flash.coerceAtLeast(0.6f) else 0f)
        v3("uFlashColor", a.gel.color)
        GlUtil.drawQuad()
    }

    fun release() = GLES30.glDeleteProgram(prog)
}
