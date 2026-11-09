package com.funcam.app.model

import com.funcam.app.model.V3.Companion.rgb

/**
 * The FUN CAM roster. All names, looks and icons are original. Every camera is free.
 */
object Cameras {
    private val r34 = Ratio(3, 4)
    private val r11 = Ratio(1, 1)
    private val r23 = Ratio(2, 3)
    private val r916 = Ratio(9, 16)
    private val r43 = Ratio(4, 3)
    private val r169 = Ratio(16, 9)
    private val r32 = Ratio(3, 2)

    val all: List<Recipe> = listOf(
        // ---------------- Digital ----------------
        Recipe(
            "pixel02", "Pixel 02", Category.DIGITAL, Body.DIGICAM, 0xFFB9BEC6, 0xFF2E3440,
            "2000s CCD point-and-shoot. Cool cyan cast, crisp detail, harsh flash.",
            Look(contrast = 1.12f, saturation = 1.1f, temp = -0.35f, tint = -0.1f, sharpen = 0.6f,
                bloom = 0.25f, grain = 0.12f, grainSize = 0.8f, vignette = 0.15f, flash = 1f,
                highTint = V3(-0.02f, 0.02f, 0.05f)),
            ratios = listOf(r34, r43, r11, r916), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "cybermini", "Cyber Mini", Category.DIGITAL, Body.DIGICAM, 0xFF8E9AAF, 0xFFE63946,
            "1.3 megapixel pocket digicam. Low-res softness, blown highlights, purple fringing.",
            Look(exposure = 0.15f, contrast = 1.18f, saturation = 1.2f, temp = -0.1f, pixel = 0.55f,
                chroma = 0.6f, bloom = 0.45f, grain = 0.18f, vignette = 0.1f, flash = 1f),
            ratios = listOf(r34, r43), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "glowccd", "Glow CCD", Category.DIGITAL, Body.COMPACT, 0xFFF4C7D5, 0xFF5E4B56,
            "Pastel CCD with dreamy highlight glow and rosy skin tones.",
            Look(exposure = 0.1f, contrast = 0.95f, saturation = 0.95f, temp = 0.05f, tint = 0.2f,
                fade = 0.06f, bloom = 0.7f, soft = 0.25f, grain = 0.1f, vignette = 0.12f,
                highTint = V3(0.04f, 0.0f, 0.03f), flash = 0.9f),
            ratios = listOf(r34, r43, r11), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "snappro", "Snap Pro", Category.DIGITAL, Body.COMPACT, 0xFF2B2D31, 0xFFC0A062,
            "Premium compact digital. Clean, slightly warm, gentle noise.",
            Look(contrast = 1.06f, saturation = 1.04f, temp = 0.12f, sharpen = 0.3f, grain = 0.06f,
                vignette = 0.08f, flash = 0.8f),
            ratios = listOf(r34, r23, r11, r916, r43), stamp = StampStyle.DIGI_ORANGE,
        ),

        // ---------------- Vintage 135 ----------------
        Recipe(
            "sunny200", "Sunny 200", Category.V135, Body.COMPACT, 0xFFF2E3C6, 0xFFE07A1F,
            "Warm consumer colour negative. Golden highlights, fine grain.",
            Look(contrast = 1.05f, saturation = 1.08f, temp = 0.45f, fade = 0.04f, halation = 0.25f,
                bloom = 0.15f, grain = 0.28f, vignette = 0.25f, leak = 0.35f,
                shadowTint = V3(0.0f, 0.01f, 0.03f), highTint = V3(0.05f, 0.03f, -0.02f)),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "funsaver", "Fun Saver", Category.V135, Body.DISPOSABLE, 0xFFF6C744, 0xFF1F5FBF,
            "Disposable camera. Warm, punchy, heavy flash falloff and a dated corner.",
            Look(exposure = 0.05f, contrast = 1.12f, saturation = 1.18f, temp = 0.3f, halation = 0.2f,
                grain = 0.35f, grainSize = 1.2f, vignette = 0.45f, leak = 0.4f, soft = 0.2f, flash = 1f,
                shadowTint = V3(0.0f, 0.02f, 0.0f)),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "night1600", "Night 1600", Category.V135, Body.SLR, 0xFF1D1F24, 0xFF7FD1AE,
            "High-speed film for dark scenes. Chunky grain, green shadows, glowing lights.",
            Look(exposure = 0.2f, contrast = 1.1f, saturation = 0.9f, temp = -0.05f, fade = 0.05f,
                halation = 0.45f, bloom = 0.35f, grain = 0.6f, grainSize = 1.6f, vignette = 0.3f,
                leak = 0.45f, shadowTint = V3(-0.01f, 0.04f, 0.02f)),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "slidee6", "Slide E6", Category.V135, Body.SLR, 0xFF3A3F47, 0xFF4CC9F0,
            "Reversal slide film in a card mount. Deep blacks, vivid colour.",
            Look(contrast = 1.25f, saturation = 1.3f, temp = -0.05f, gamma = 0.95f, grain = 0.12f,
                vignette = 0.3f, highTint = V3(0f, 0.01f, 0.03f)),
            ratios = listOf(r32, r23), border = Border.SLIDE, stamp = StampStyle.NONE,
        ),
        Recipe(
            "halfcam", "Half Cam", Category.V135, Body.HALF, 0xFFD9D4C7, 0xFF8C1C13,
            "Half-frame: two vertical moments side by side on one frame.",
            Look(contrast = 1.06f, saturation = 1.05f, temp = 0.25f, fade = 0.05f, grain = 0.3f,
                vignette = 0.3f, leak = 0.3f),
            ratios = listOf(Ratio(3, 4)), capture = CaptureMode.HALF, canImport = false,
        ),
        Recipe(
            "gold400", "Gold 400", Category.V135, Body.COMPACT, 0xFFE9B949, 0xFF6B2D0C,
            "Everyday film with rich reds and yellows and medium grain.",
            Look(contrast = 1.08f, saturation = 1.15f, temp = 0.35f, halation = 0.2f, grain = 0.32f,
                vignette = 0.22f, leak = 0.3f, gain = V3(1.02f, 0.99f, 0.94f),
                highTint = V3(0.04f, 0.02f, -0.03f)),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "mono400", "Mono 400", Category.V135, Body.SLR, 0xFF111111, 0xFFE0E0E0,
            "Classic black & white. Strong contrast, silver grain.",
            Look(contrast = 1.3f, mono = 1f, grain = 0.42f, grainSize = 1.2f, vignette = 0.35f,
                fade = 0.03f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "srclassic", "SR Classic", Category.V135, Body.SLR, 0xFF2A2A2A, 0xFFB08D57,
            "Manual 35mm SLR. Muted, faded tones with balanced contrast.",
            Look(contrast = 0.96f, saturation = 0.85f, temp = 0.15f, fade = 0.1f, halation = 0.15f,
                grain = 0.25f, vignette = 0.25f, leak = 0.25f,
                shadowTint = V3(0.0f, 0.02f, 0.04f), highTint = V3(0.03f, 0.02f, 0.0f)),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "expired", "Expired", Category.V135, Body.COMPACT, 0xFF9C6B98, 0xFF2EC4B6,
            "Long-expired film. Shifted hues, cross-processed colour, heavy grain.",
            Look(exposure = 0.1f, contrast = 1.1f, saturation = 1.1f, cross = 0.8f, fade = 0.08f,
                grain = 0.5f, grainSize = 1.4f, vignette = 0.35f, leak = 0.55f,
                leakColor = V3(1f, 0.2f, 0.55f), shadowTint = V3(0.0f, 0.03f, 0.05f)),
            ratios = listOf(r23, r34),
        ),

        // ---------------- Vintage 120 / toy ----------------
        Recipe(
            "toy120", "Toy 120", Category.V120, Body.TOY, 0xFF222222, 0xFF4F9D69,
            "Plastic toy camera. Dark vignette, soft corners, leaky back.",
            Look(exposure = 0.1f, contrast = 1.15f, saturation = 1.2f, temp = 0.1f, soft = 0.35f,
                chroma = 0.4f, grain = 0.3f, vignette = 0.75f, leak = 0.6f),
            ratios = listOf(r11), border = Border.FILM,
        ),
        Recipe(
            "medium67", "Medium 6x7", Category.V120, Body.MEDIUM, 0xFF34373C, 0xFFD4D4D4,
            "Medium format 6x7. Creamy tones, very fine grain, film rebate edges.",
            Look(contrast = 1.02f, saturation = 1.02f, temp = 0.12f, fade = 0.04f, grain = 0.15f,
                grainSize = 0.8f, vignette = 0.15f, highTint = V3(0.02f, 0.015f, 0.0f)),
            ratios = listOf(Ratio(4, 5), r11), border = Border.FILM, stamp = StampStyle.NONE,
        ),
        Recipe(
            "quadlens", "Quad Lens", Category.V120, Body.QUAD, 0xFF2D6CDF, 0xFFFFD23F,
            "Four lenses fire in sequence — a 2×2 action strip in one shot.",
            Look(contrast = 1.1f, saturation = 1.2f, temp = 0.15f, grain = 0.3f, vignette = 0.35f,
                leak = 0.2f),
            ratios = listOf(r34), capture = CaptureMode.QUAD, canImport = false,
        ),
        Recipe(
            "wiggle3d", "3D Wiggle", Category.V120, Body.STEREO, 0xFF1B1B1B, 0xFFFF6B6B,
            "Three-lens stereo camera. Saves a looping wiggle video.",
            Look(contrast = 1.08f, saturation = 1.1f, temp = 0.2f, grain = 0.3f, vignette = 0.25f),
            ratios = listOf(r34, r11), capture = CaptureMode.WIGGLE, canImport = false,
        ),

        // ---------------- Instant ----------------
        Recipe(
            "instasq", "Insta Square", Category.INST, Body.INSTANT, 0xFFEDEAE3, 0xFF2B2B2B,
            "Square instant print in a white frame. Soft contrast, creamy whites.",
            Look(exposure = 0.12f, contrast = 0.9f, saturation = 0.9f, temp = 0.1f, fade = 0.12f,
                soft = 0.15f, grain = 0.08f, vignette = 0.2f, bloom = 0.2f,
                shadowTint = V3(0.0f, 0.02f, 0.05f), highTint = V3(0.03f, 0.02f, -0.02f), flash = 0.9f),
            ratios = listOf(r11), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),
        Recipe(
            "instamini", "Insta Mini", Category.INST, Body.INSTANT, 0xFFA8DADC, 0xFF1D3557,
            "Credit-card instant print. Cool tint, bright flash.",
            Look(exposure = 0.15f, contrast = 1.05f, saturation = 1.05f, temp = -0.2f, fade = 0.08f,
                soft = 0.1f, grain = 0.08f, vignette = 0.2f, bloom = 0.25f, flash = 1f),
            ratios = listOf(Ratio(46, 62)), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),
        Recipe(
            "instawide", "Insta Wide", Category.INST, Body.INSTANT, 0xFF3D405B, 0xFFF2CC8F,
            "Wide instant print for groups and landscapes.",
            Look(exposure = 0.1f, contrast = 0.95f, saturation = 0.95f, temp = 0.05f, fade = 0.1f,
                soft = 0.1f, grain = 0.08f, vignette = 0.2f, bloom = 0.2f),
            ratios = listOf(Ratio(99, 62)), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),

        // ---------------- Video ----------------
        Recipe(
            "super8", "Super 8", Category.VIDEO, Body.SUPER8, 0xFF3B3B3B, 0xFFE9C46A,
            "8mm home movies. Gate weave, flicker, warm fade, dust and scratches.",
            Look(contrast = 1.1f, saturation = 1.05f, temp = 0.45f, fade = 0.1f, halation = 0.3f,
                soft = 0.3f, grain = 0.45f, grainSize = 1.5f, vignette = 0.5f, leak = 0.3f,
                weave = 1f, flicker = 1f, dust = 0.8f),
            ratios = listOf(r43, r34, r916), isVideo = true, videoFps = 18, stamp = StampStyle.NONE,
        ),
        Recipe(
            "16mil", "16 Mil", Category.VIDEO, Body.CINE, 0xFF5C5C5C, 0xFFB5838D,
            "16mm cine film. Finer grain, cinematic colour, slight gate weave.",
            Look(contrast = 1.08f, saturation = 0.95f, temp = 0.2f, fade = 0.06f, halation = 0.25f,
                grain = 0.3f, vignette = 0.3f, weave = 0.5f, flicker = 0.4f, dust = 0.35f,
                shadowTint = V3(0.0f, 0.02f, 0.04f), highTint = V3(0.04f, 0.02f, 0.0f)),
            ratios = listOf(r43, r169, r916, r34), isVideo = true, videoFps = 24, stamp = StampStyle.NONE,
        ),
        Recipe(
            "vhsc", "VHS-C", Category.VIDEO, Body.VHS, 0xFF1F1F1F, 0xFFE63946,
            "Home video tape. Scanlines, colour bleed, tracking noise, PLAY overlay.",
            Look(contrast = 1.05f, saturation = 1.25f, temp = 0.05f, soft = 0.45f, chroma = 0.6f,
                bloom = 0.3f, grain = 0.25f, vignette = 0.2f, scan = 0.8f, vhs = 1f),
            ratios = listOf(r43, r34, r916), isVideo = true, videoFps = 30, stamp = StampStyle.VHS,
        ),
        Recipe(
            "handycam99", "Handycam 99", Category.VIDEO, Body.HANDYCAM, 0xFF4A4E69, 0xFFC9ADA7,
            "Late-90s digital camcorder. Over-sharpened, punchy, date and time burned in.",
            Look(contrast = 1.12f, saturation = 1.15f, temp = -0.1f, sharpen = 0.8f, chroma = 0.25f,
                grain = 0.15f, vignette = 0.1f, scan = 0.25f, bloom = 0.2f),
            ratios = listOf(r43, r34, r916), isVideo = true, videoFps = 30, stamp = StampStyle.CAMCORDER,
        ),
        Recipe(
            "projector", "Projector", Category.VIDEO, Body.PROJECTOR, 0xFF6D597A, 0xFFEAAC8B,
            "Home movie projected on a wall. Hot centre, flicker, dusty light.",
            Look(exposure = 0.1f, contrast = 1.15f, saturation = 0.85f, temp = 0.35f, fade = 0.12f,
                bloom = 0.4f, soft = 0.35f, grain = 0.4f, vignette = 0.7f, flicker = 1.2f, dust = 0.6f,
                weave = 0.6f),
            ratios = listOf(r43, r34), isVideo = true, videoFps = 18, stamp = StampStyle.NONE,
        ),
        Recipe(
            "dreamblur", "Dream Blur", Category.VIDEO, Body.CINE, 0xFFCDB4DB, 0xFF6A4C93,
            "Slow-shutter video. Light trails and dreamy motion blur, low frame rate.",
            Look(contrast = 1.05f, saturation = 1.1f, temp = 0.1f, fade = 0.05f, bloom = 0.45f,
                halation = 0.2f, grain = 0.2f, vignette = 0.25f),
            ratios = listOf(r916, r34, r43), isVideo = true, videoFps = 12, manual = true,
            stamp = StampStyle.NONE,
        ),
        Recipe(
            "manualv", "Manual V", Category.VIDEO, Body.MANUAL, 0xFF14213D, 0xFFFCA311,
            "Full manual video: ISO, shutter and frame rate. Drop the FPS for slow-shutter trails.",
            Look(contrast = 1.02f, grain = 0.08f, vignette = 0.05f),
            ratios = listOf(r916, r34, r43, r11, r169), isVideo = true, videoFps = 30, manual = true,
            stamp = StampStyle.NONE,
        ),
        Recipe(
            "manualp", "Manual P", Category.DIGITAL, Body.MANUAL, 0xFF1B263B, 0xFF7BDFF2,
            "Manual photo camera: set ISO and shutter for long exposures and light trails.",
            Look(contrast = 1.02f, grain = 0.05f),
            ratios = listOf(r34, r23, r11, r916), manual = true, stamp = StampStyle.NONE,
        ),
    )

    val default: Recipe get() = all.first { it.id == "funsaver" }

    fun byId(id: String?): Recipe = all.firstOrNull { it.id == id } ?: default

    fun inCategory(c: Category) = all.filter { it.category == c }
}

/** Shutter speeds offered in manual modes, in seconds. */
val SHUTTER_SPEEDS = listOf(
    1 / 2000f, 1 / 1000f, 1 / 500f, 1 / 250f, 1 / 125f, 1 / 60f, 1 / 30f,
    1 / 15f, 1 / 8f, 1 / 4f, 1 / 2f, 1f
)

fun shutterLabel(s: Float): String =
    if (s >= 1f) "${s.toInt()}\"" else "1/${(1f / s).let { kotlin.math.round(it).toInt() }}"

val ISO_VALUES = listOf(0, 50, 100, 200, 400, 800, 1600, 3200) // 0 = auto
val FPS_VALUES = listOf(1, 2, 4, 6, 8, 12, 15, 18, 24, 30)
