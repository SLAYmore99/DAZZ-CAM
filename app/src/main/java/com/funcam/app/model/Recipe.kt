package com.funcam.app.model

/** Menu groups, same as the retro-camera app genre: Digital · Video · Vintage 135 · Vintage 120 · Inst. */
enum class Category(val label: String) {
    DIGITAL("Digital"), VIDEO("Video"), V135("Vintage 135"), V120("Vintage 120"), INST("Inst")
}

/** Drawn body shape for the camera icon (original artwork drawn in code). */
enum class Body {
    COMPACT, DIGICAM, SLR, RANGEFINDER, DISPOSABLE, HALF, TOY, TLR, MEDIUM, QUAD, MULTI16, STEREO, INSTANT, INSTANT_SQ,
    SUPER8, CINE, VHS, HANDYCAM, PROJECTOR, PHONE, PANORAMA
}

/** Frame / gate around the picture. Ids match the COMPOSITE shader's uBorder. */
enum class Border(val id: Int) {
    NONE(0), INSTANT(1), BLACK(2), SLIDE(3), SPROCKET(4), GATE8(5), GATE16(6), NEGATIVE(7), TOY(8)
}

/** What one shutter press produces. */
enum class CaptureMode { SINGLE, HALF, WIGGLE, GRID16, COLLAGE }

enum class StampStyle { FILM_ORANGE, DIGI_ORANGE, CAMCORDER, VHS, NONE }

data class Ratio(val w: Int, val h: Int) {
    val value: Float get() = w.toFloat() / h
    val label: String get() = if (w == 239) "2.39" else "$w:$h"
}

data class V3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val ONE = V3(1f, 1f, 1f)
    }
}

/**
 * A camera's look. Colour comes from a film-stock LUT ([lut], see tools/make_luts.py) plus tone tweaks;
 * everything else is optics, film and video character applied by the shader.
 */
data class Look(
    val lut: String? = null,
    val lutMix: Float = 1f,
    val exposure: Float = 0f,
    val temp: Float = 0f,
    val tint: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val fade: Float = 0f,
    val lift: V3 = V3.ZERO,
    val gain: V3 = V3.ONE,
    val shadowTint: V3 = V3.ZERO,
    val highTint: V3 = V3.ZERO,
    val mono: Float = 0f,
    val halation: Float = 0f,
    val halationColor: V3 = V3(1f, 0.3f, 0.12f),
    val bloom: Float = 0f,
    /** Corner softness of cheap lenses. */
    val soft: Float = 0f,
    /** Overall diffusion (dreamy / soft focus). */
    val softAll: Float = 0f,
    val grain: Float = 0f,
    /** 1 = ISO 400-ish; bigger = chunkier. */
    val grainSize: Float = 1f,
    /** Digital sensor noise (CCD bodies). */
    val noise: Float = 0f,
    val vignette: Float = 0f,
    val leak: Float = 0f,
    val leakColor: V3 = V3(1f, 0.42f, 0.12f),
    val chroma: Float = 0f,
    val sharpen: Float = 0f,
    val pixel: Float = 0f,
    val scan: Float = 0f,
    val vhs: Float = 0f,
    val weave: Float = 0f,
    val flicker: Float = 0f,
    val dust: Float = 0f,
    val interlace: Float = 0f,
    /** Strength of the on-camera flash look when the flash fires. */
    val flash: Float = 0.85f,
)

data class Recipe(
    val id: String,
    val name: String,
    val category: Category,
    val body: Body,
    val bodyColor: Long,
    val accentColor: Long,
    val description: String,
    val look: Look,
    val ratios: List<Ratio> = listOf(Ratio(3, 4)),
    val border: Border = Border.NONE,
    val capture: CaptureMode = CaptureMode.SINGLE,
    val stamp: StampStyle = StampStyle.FILM_ORANGE,
    val isVideo: Boolean = false,
    val videoFps: Int = 30,
    val manual: Boolean = false,
    val canImport: Boolean = true,
    /** Flash on by default (disposables). */
    val flashDefault: Boolean = false,
    /** Text printed on film edges (negative / sprocket frames). */
    val edgeText: String = "",
)

/** Optional add-ons that stack on top of any camera. */
enum class FlashGel(val label: String, val color: V3) {
    WHITE("White", V3.ONE),
    RED("Red", V3(1f, 0.25f, 0.3f)),
    BLUE("Blue", V3(0.3f, 0.5f, 1f)),
    GREEN("Green", V3(0.35f, 1f, 0.45f)),
    YELLOW("Yellow", V3(1f, 0.85f, 0.3f)),
    PURPLE("Purple", V3(0.75f, 0.35f, 1f)),
}

/** Instant-print border colours. */
enum class InstantFrame(val label: String, val color: Long) {
    WHITE("White", 0xFFF4F2EC), CREAM("Cream", 0xFFEFE6D2), BLACK("Black", 0xFF141414),
    PINK("Pink", 0xFFF2CDD3), BLUE("Blue", 0xFFC9DCEB), MINT("Mint", 0xFFCFE8D8),
}

data class Accessories(
    val fisheyeW: Boolean = false,
    val fisheyeC: Boolean = false,
    val prism: Boolean = false,
    val star: Boolean = false,
    val nd: Boolean = false,
    val gel: FlashGel = FlashGel.WHITE,
    val doubleExposure: Boolean = false,
    val lightLeaks: Boolean = true,
) {
    val fisheyeMode: Int get() = if (fisheyeC) 2 else if (fisheyeW) 1 else 0
    val anyLens: Boolean get() = fisheyeW || fisheyeC || prism || star || nd || doubleExposure
}

/** Per-camera user customisation (1 = the camera's own look). Saved per camera. */
data class Tweak(
    val intensity: Float = 1f,
    val grain: Float = 1f,
    val leaks: Float = 1f,
    val glow: Float = 1f,
    val vignette: Float = 1f,
    val soft: Float = 1f,
) {
    fun encode() = listOf(intensity, grain, leaks, glow, vignette, soft).joinToString(",")
    val isDefault: Boolean get() = this == Tweak()

    companion object {
        fun decode(s: String?): Tweak = try {
            val v = s!!.split(",").map { it.toFloat() }
            Tweak(v[0], v[1], v[2], v[3], v[4], v[5])
        } catch (_: Exception) { Tweak() }
    }
}
