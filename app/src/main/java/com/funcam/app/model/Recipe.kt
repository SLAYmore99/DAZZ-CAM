package com.funcam.app.model

/** Menu groups, mirroring how retro-camera apps organise their bodies. */
enum class Category(val label: String) {
    DIGITAL("Digital"), VIDEO("Video"), V135("Vintage 135"), V120("Vintage 120"), INST("Inst")
}

/** Drawn body shape for the camera icon (original artwork drawn in code). */
enum class Body {
    COMPACT, DIGICAM, SLR, DISPOSABLE, HALF, TOY, MEDIUM, QUAD, STEREO, INSTANT,
    SUPER8, CINE, VHS, HANDYCAM, PROJECTOR, MANUAL
}

/** Frame drawn around the image. Ids match the shader's uBorder. */
enum class Border(val id: Int) { NONE(0), INSTANT(1), FILM(2), SLIDE(3), SPROCKET(4) }

/** How many frames a single shutter press produces and how they are combined. */
enum class CaptureMode { SINGLE, HALF, QUAD, WIGGLE }

/** Overlay text drawn on top of the image. */
enum class StampStyle { FILM_ORANGE, DIGI_ORANGE, CAMCORDER, VHS, PROJECTOR, NONE }

data class Ratio(val w: Int, val h: Int) {
    val value: Float get() = w.toFloat() / h
    val label: String get() = "$w:$h"
}

data class V3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val ONE = V3(1f, 1f, 1f)
        fun rgb(hex: Long) = V3(
            ((hex shr 16) and 0xFF) / 255f, ((hex shr 8) and 0xFF) / 255f, (hex and 0xFF) / 255f
        )
    }
}

/** Every look is a set of shader parameters — no LUT files from anywhere else. */
data class Look(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temp: Float = 0f,
    val tint: Float = 0f,
    val fade: Float = 0f,
    val gamma: Float = 1f,
    val lift: V3 = V3.ZERO,
    val gain: V3 = V3.ONE,
    val shadowTint: V3 = V3.ZERO,
    val highTint: V3 = V3.ZERO,
    val mono: Float = 0f,
    val cross: Float = 0f,
    val halation: Float = 0f,
    val halationColor: V3 = V3(1f, 0.25f, 0.1f),
    val bloom: Float = 0f,
    val grain: Float = 0f,
    val grainSize: Float = 1f,
    val vignette: Float = 0f,
    val leak: Float = 0f,
    val leakColor: V3 = V3(1f, 0.35f, 0.1f),
    val chroma: Float = 0f,
    val soft: Float = 0f,
    val sharpen: Float = 0f,
    val pixel: Float = 0f,
    val scan: Float = 0f,
    val vhs: Float = 0f,
    val weave: Float = 0f,
    val flicker: Float = 0f,
    val dust: Float = 0f,
    /** How strongly the flash look is applied when flash fires. */
    val flash: Float = 0.7f,
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
    /** Output frame rate for video bodies. */
    val videoFps: Int = 30,
    /** Exposes ISO / shutter / FPS controls. */
    val manual: Boolean = false,
    val canImport: Boolean = true,
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

data class Accessories(
    val fisheye: Boolean = false,
    val prism: Boolean = false,
    val star: Boolean = false,
    val nd: Boolean = false,
    val gel: FlashGel = FlashGel.WHITE,
    val doubleExposure: Boolean = false,
    val lightLeaks: Boolean = true,
)
