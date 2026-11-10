package com.funcam.app.model

/**
 * The FUN CAM roster: 30 photo + 12 video cameras. See docs/CAMERAS.md for the research behind each
 * one (which classic camera / film it recreates). Names, icons and code are FUN CAM's own; every
 * camera is free.
 */
object Cameras {
    private val r34 = Ratio(3, 4)
    private val r43 = Ratio(4, 3)
    private val r11 = Ratio(1, 1)
    private val r23 = Ratio(2, 3)
    private val r32 = Ratio(3, 2)
    private val r916 = Ratio(9, 16)
    private val r169 = Ratio(16, 9)

    val all: List<Recipe> = listOf(
        // ===================================================== DIGITAL
        Recipe(
            "plain", "Plain", Category.DIGITAL, Body.PHONE, 0xFF2B2D31, 0xFF9AA0A6,
            "Clean phone colour with full manual ISO and shutter — long exposures and light trails.",
            Look(),
            ratios = listOf(r34, r11, r916, r43, r23), stamp = StampStyle.NONE, manual = true,
        ),
        Recipe(
            "ccd05", "CCD-05", Category.DIGITAL, Body.DIGICAM, 0xFFC4C8CE, 0xFF3C6E9E,
            "Early-2000s CCD compact. Saturated and crisp, cool-cyan whites, a hard direct flash and an orange digital date.",
            Look(lut = "ccd", temp = -0.15f, sharpen = 0.5f, noise = 0.35f, bloom = 0.25f, vignette = 0.12f, chroma = 0.3f, flash = 1f),
            ratios = listOf(r43, r34, r11), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "grd", "GR-D", Category.DIGITAL, Body.COMPACT, 0xFF1C1C1E, 0xFFB0B0B0,
            "Pocket street digital. High contrast, deep blacks, muted cool colour and gritty sensor noise.",
            Look(lut = "grd", sharpen = 0.4f, noise = 0.5f, grain = 0.12f, vignette = 0.3f, flash = 0.9f),
            ratios = listOf(r34, r11, r23, r169), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "pastel", "Pastel", Category.DIGITAL, Body.COMPACT, 0xFFF3C9D6, 0xFF8E5A6E,
            "Soft-focus digicam. Bright, dreamy and glowing — best in daylight.",
            Look(lut = "astia", exposure = 0.25f, contrast = 0.9f, saturation = 0.9f, fade = 0.12f, bloom = 0.7f, softAll = 0.35f,
                highTint = V3(0.03f, 0f, 0.02f), grain = 0.06f, noise = 0.12f),
            ratios = listOf(r34, r11, r43), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "finex", "Fine X", Category.DIGITAL, Body.DIGICAM, 0xFF8C96A3, 0xFF2E7D5B,
            "2000s Fuji-style CCD. Clean greens and cyans, crisp detail, punchy flash.",
            Look(lut = "provia100f", lutMix = 0.7f, saturation = 1.1f, temp = -0.05f, tint = -0.06f, sharpen = 0.45f, noise = 0.3f, bloom = 0.2f, flash = 1f),
            ratios = listOf(r43, r34, r11), stamp = StampStyle.DIGI_ORANGE,
        ),
        Recipe(
            "collage", "Collage", Category.DIGITAL, Body.QUAD, 0xFFF7E6A1, 0xFFE76F51,
            "Take four separate shots — they're laid out together on one white card.",
            Look(lut = "superia200", lutMix = 0.6f, grain = 0.1f, vignette = 0.1f),
            ratios = listOf(r34), capture = CaptureMode.COLLAGE, stamp = StampStyle.NONE, canImport = false,
        ),

        // ===================================================== VINTAGE 135
        Recipe(
            "range", "Range", Category.V135, Body.RANGEFINDER, 0xFFD8D3C8, 0xFF2A2A2A,
            "Classic rangefinder on warm colour negative. Nostalgic, fine grain, gentle halation.",
            Look(lut = "portra400", temp = 0.1f, fade = 0.05f, halation = 0.25f, bloom = 0.15f, grain = 0.3f, vignette = 0.25f, leak = 0.25f, soft = 0.15f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "grmono", "GR Mono", Category.V135, Body.COMPACT, 0xFF151515, 0xFF6E6E6E,
            "Street compact loaded with black & white. Contrasty, atmospheric, strong grain.",
            Look(lut = "trix", mono = 1f, contrast = 1.15f, grain = 0.55f, grainSize = 1.2f, vignette = 0.35f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "t2", "T2", Category.V135, Body.COMPACT, 0xFFB9AE96, 0xFF333333,
            "Premium titanium compact. Sharp lens, a slight magenta cast and strong, rich grain.",
            Look(lut = "portra800", tint = 0.18f, contrast = 1.08f, saturation = 1.05f, grain = 0.5f, grainSize = 1.2f, halation = 0.3f,
                bloom = 0.15f, vignette = 0.2f, sharpen = 0.2f, leak = 0.2f),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "fade35", "Fade 35", Category.V135, Body.RANGEFINDER, 0xFF8A7F6A, 0xFFC99B5B,
            "An old rangefinder with a long-forgotten roll. Faded colours, cyan shadows, light leaks.",
            Look(lut = "agfavista", contrast = 0.85f, saturation = 0.75f, fade = 0.3f, shadowTint = V3(0f, 0.02f, 0.04f), temp = 0.2f,
                grain = 0.4f, vignette = 0.3f, leak = 0.55f, halation = 0.2f),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "t4", "T4 Super", Category.V135, Body.COMPACT, 0xFF26292E, 0xFFD64545,
            "Cult point-and-shoot with a razor lens. Punchy, high-saturation, slide-like contrast.",
            Look(lut = "velvia50", lutMix = 0.75f, contrast = 1.1f, saturation = 1.15f, grain = 0.25f, vignette = 0.25f, halation = 0.15f, flash = 0.9f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "wiggle3d", "3D Wiggle", Category.V135, Body.STEREO, 0xFF1E1E1E, 0xFFE9C46A,
            "Four-lens stereo camera. Saves a wobbling 3D clip.",
            Look(lut = "superia200", grain = 0.3f, vignette = 0.2f, halation = 0.15f),
            ratios = listOf(r34, r11), capture = CaptureMode.WIGGLE, canImport = false,
        ),
        Recipe(
            "slidene", "Slide NE", Category.V135, Body.SLR, 0xFF2F3237, 0xFF4CC9F0,
            "35mm slide film scanned with its thick black frame. Clean, vivid colour.",
            Look(lut = "provia100f", contrast = 1.1f, saturation = 1.1f, grain = 0.2f, vignette = 0.2f),
            ratios = listOf(r32, r23), border = Border.BLACK, stamp = StampStyle.NONE,
        ),
        Recipe(
            "funsaver", "Fun Saver", Category.V135, Body.DISPOSABLE, 0xFF2A9D8F, 0xFFF4A261,
            "Single-use camera. Flash always ready, warm colour, heavy vignette, soft corners, date in the corner.",
            Look(lut = "elite400", temp = 0.2f, contrast = 1.08f, saturation = 1.1f, grain = 0.45f, grainSize = 1.3f, vignette = 0.5f, soft = 0.45f,
                chroma = 0.4f, halation = 0.2f, leak = 0.35f, flash = 1f),
            ratios = listOf(r23, r34), flashDefault = true,
        ),
        Recipe(
            "infra", "Infra", Category.V135, Body.SLR, 0xFF3B1F2B, 0xFFE63973,
            "Colour-infrared film. Leaves turn magenta-red, skies deep aqua — surreal false colour.",
            Look(lut = "infra", grain = 0.35f, vignette = 0.25f, halation = 0.2f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "slrclassic", "SLR Classic", Category.V135, Body.SLR, 0xFF1F1F1F, 0xFFC0C0C0,
            "Solid 80s SLR on fine portrait film. Warm, restrained and timeless, with a date imprint.",
            Look(lut = "portra160", temp = 0.2f, contrast = 0.95f, saturation = 0.9f, fade = 0.06f, grain = 0.25f, halation = 0.2f, vignette = 0.2f),
            ratios = listOf(r23, r34),
        ),
        Recipe(
            "simple", "Simple", Category.V135, Body.DISPOSABLE, 0xFF3A7D44, 0xFFF2E8CF,
            "Budget disposable. Green-leaning colour, gritty grain and a bright flash.",
            Look(lut = "superia400", tint = -0.08f, contrast = 1.05f, grain = 0.45f, grainSize = 1.3f, vignette = 0.45f, soft = 0.4f,
                shadowTint = V3(0f, 0.025f, 0.01f), leak = 0.3f, flash = 1f),
            ratios = listOf(r23, r34), flashDefault = true,
        ),
        Recipe(
            "quick800", "Quick 800", Category.V135, Body.DISPOSABLE, 0xFF264653, 0xFFE9C46A,
            "Fast-film disposable for parties and nights. Deep colour and chunky grain.",
            Look(lut = "superia800", contrast = 1.12f, saturation = 1.15f, grain = 0.65f, grainSize = 1.6f, vignette = 0.4f, soft = 0.35f,
                halation = 0.25f, leak = 0.3f, flash = 1f),
            ratios = listOf(r23, r34), flashDefault = true,
        ),
        Recipe(
            "byu16", "Byu 16", Category.V135, Body.MULTI16, 0xFFEFEFEF, 0xFFFF6B9A,
            "Sixteen tiny lenses fire one after another — a 4×4 flip-book of the moment.",
            Look(lut = "superia200", grain = 0.3f, vignette = 0.2f),
            ratios = listOf(r34), capture = CaptureMode.GRID16, canImport = false,
        ),
        Recipe(
            "sure85", "Sure 85", Category.V135, Body.COMPACT, 0xFF2C2C2C, 0xFFB8B8B8,
            "The classic 90s point-and-shoot. Pale, bluish, nostalgic colour with the orange date in the corner.",
            Look(lut = "superia200", temp = -0.3f, contrast = 0.92f, saturation = 0.82f, fade = 0.1f, highTint = V3(-0.01f, 0.01f, 0.03f),
                shadowTint = V3(0f, 0.01f, 0.04f), grain = 0.3f, halation = 0.15f, bloom = 0.15f, vignette = 0.25f, flash = 0.9f),
            ratios = listOf(r23, r34, r11),
        ),
        Recipe(
            "sprocket", "Sprocket", Category.V135, Body.PANORAMA, 0xFF3D3D3D, 0xFFE05A3A,
            "Panoramic toy camera that exposes right over the sprocket holes.",
            Look(lut = "portra400", contrast = 1.1f, saturation = 1.15f, grain = 0.45f, vignette = 0.45f, soft = 0.3f, leak = 0.35f),
            ratios = listOf(Ratio(2, 1)), border = Border.SPROCKET, stamp = StampStyle.NONE, edgeText = "FUNCOLOR 400",
        ),
        Recipe(
            "half", "Half", Category.V135, Body.HALF, 0xFFD6D1C4, 0xFF8C1C13,
            "Half-frame camera: two vertical moments side by side on one frame.",
            Look(lut = "elite200", temp = 0.15f, grain = 0.35f, vignette = 0.3f, leak = 0.3f, halation = 0.2f),
            ratios = listOf(r34), capture = CaptureMode.HALF, canImport = false,
        ),
        Recipe(
            "slide", "Slide", Category.V135, Body.SLR, 0xFF41464D, 0xFFF2E3BC,
            "Bright reversal film, framed in a card slide mount.",
            Look(lut = "ektachrome", contrast = 1.08f, grain = 0.2f, vignette = 0.3f),
            ratios = listOf(r32, r23), border = Border.SLIDE, stamp = StampStyle.NONE,
        ),

        // ===================================================== VINTAGE 120
        Recipe(
            "tlr", "TLR", Category.V120, Body.TLR, 0xFF1A1A1A, 0xFFC5A572,
            "Twin-lens reflex on medium format. Square, black film frame, deep saturated colour.",
            Look(lut = "ektar100", contrast = 1.12f, saturation = 1.15f, grain = 0.2f, grainSize = 0.9f, vignette = 0.3f, halation = 0.1f),
            ratios = listOf(r11), border = Border.BLACK, stamp = StampStyle.NONE,
        ),
        Recipe(
            "toy120", "Toy 120", Category.V120, Body.TOY, 0xFF222222, 0xFF4F9D69,
            "Plastic medium-format toy. Heavy vignette, uneven cut-off edges, soft corners and light leaks.",
            Look(lut = "fuji400h", saturation = 1.2f, contrast = 1.1f, vignette = 0.85f, soft = 0.7f, chroma = 0.5f, grain = 0.35f, leak = 0.55f, halation = 0.2f),
            ratios = listOf(r11), border = Border.TOY, stamp = StampStyle.NONE,
        ),
        Recipe(
            "six7", "Six7", Category.V120, Body.MEDIUM, 0xFF2E2E2E, 0xFFBDBDBD,
            "Big 6×7 medium-format SLR. Creamy film colour, very fine grain, black film frame.",
            Look(lut = "portra400", temp = 0.05f, grain = 0.15f, grainSize = 0.8f, halation = 0.15f, vignette = 0.15f),
            ratios = listOf(Ratio(6, 7), Ratio(7, 6)), border = Border.BLACK, stamp = StampStyle.NONE,
        ),
        Recipe(
            "kiev", "Kiev", Category.V120, Body.MEDIUM, 0xFF4A4F45, 0xFFD4D4D4,
            "Square medium format scanned with the negative's edge markings. Cool, quiet colour.",
            Look(lut = "fuji160c", temp = -0.2f, contrast = 1.05f, grain = 0.2f, vignette = 0.2f, halation = 0.1f),
            ratios = listOf(r11), border = Border.NEGATIVE, stamp = StampStyle.NONE, edgeText = "FUN 160C",
        ),

        // ===================================================== INST
        Recipe(
            "instamini", "Insta Mini", Category.INST, Body.INSTANT, 0xFFA8DADC, 0xFF1D3557,
            "Credit-card instant print. Gentle, slightly subdued colour and a bright flash.",
            Look(lut = "fp100c", saturation = 0.85f, contrast = 0.92f, exposure = 0.15f, fade = 0.06f, soft = 0.15f, bloom = 0.2f,
                grain = 0.1f, vignette = 0.2f, flash = 0.95f),
            ratios = listOf(Ratio(46, 62)), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),
        Recipe(
            "instamono", "Insta Mono", Category.INST, Body.INSTANT_SQ, 0xFF2B2B2B, 0xFFEDEDED,
            "Square instant in high-contrast black & white.",
            Look(lut = "fp3000b", mono = 1f, contrast = 1.15f, grain = 0.15f, vignette = 0.25f, bloom = 0.15f),
            ratios = listOf(r11), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),
        Recipe(
            "instasq", "Insta SQ", Category.INST, Body.INSTANT_SQ, 0xFFEFE6D2, 0xFFE07A5F,
            "Square instant colour print. Pick the frame colour in Settings.",
            Look(lut = "polaroid669", lutMix = 0.8f, saturation = 0.95f, exposure = 0.1f, fade = 0.05f, bloom = 0.2f, soft = 0.1f, grain = 0.1f, vignette = 0.2f),
            ratios = listOf(r11, Ratio(99, 62)), border = Border.INSTANT, stamp = StampStyle.NONE,
        ),

        // ===================================================== VIDEO
        Recipe(
            "plainv", "Plain V", Category.VIDEO, Body.PHONE, 0xFF202124, 0xFFE53935,
            "Manual video: ISO, shutter and frame rate. Slow the shutter (try ISO 160, 1/4) for dreamy light trails.",
            Look(),
            ratios = listOf(r916, r34, r43, r11, r169), isVideo = true, manual = true, stamp = StampStyle.NONE,
        ),
        Recipe(
            "classicv", "Classic V", Category.VIDEO, Body.CINE, 0xFF5C4B3B, 0xFFE9C46A,
            "Film-negative home video. Warm, grainy, gently weaving, 24 fps.",
            Look(lut = "portra400", grain = 0.35f, halation = 0.25f, bloom = 0.15f, vignette = 0.25f, weave = 0.3f, flicker = 0.2f),
            ratios = listOf(r43, r169, r916, r34), isVideo = true, videoFps = 24, stamp = StampStyle.NONE,
        ),
        Recipe(
            "16mil", "16 Mil", Category.VIDEO, Body.CINE, 0xFF5E5E5E, 0xFFB5838D,
            "16mm reversal film. Cinematic colour, slightly rounded gate, weave and dust.",
            Look(lut = "ektachrome", lutMix = 0.85f, contrast = 1.05f, grain = 0.4f, grainSize = 1.2f, halation = 0.25f, vignette = 0.3f,
                weave = 0.5f, flicker = 0.35f, dust = 0.4f),
            ratios = listOf(r43, r34, r916), border = Border.GATE16, isVideo = true, videoFps = 24, stamp = StampStyle.NONE,
        ),
        Recipe(
            "super8", "Super 8", Category.VIDEO, Body.SUPER8, 0xFF3B3B3B, 0xFFE9C46A,
            "Home movies on classic reversal film. Rounded gate, flicker, dust and hairs, 18 fps.",
            Look(lut = "kodachrome64", temp = 0.25f, contrast = 1.08f, grain = 0.6f, grainSize = 1.6f, halation = 0.3f, soft = 0.35f,
                vignette = 0.5f, weave = 1f, flicker = 0.9f, dust = 0.8f, leak = 0.3f),
            ratios = listOf(r43, r34), border = Border.GATE8, isVideo = true, videoFps = 18, stamp = StampStyle.NONE,
        ),
        Recipe(
            "vhs", "VHS", Category.VIDEO, Body.VHS, 0xFF1F1F1F, 0xFFE63946,
            "Home video tape. Colour bleed, wobbly lines, tracking noise and the PLAY overlay.",
            Look(lut = "vhs", softAll = 0.2f, chroma = 0.5f, bloom = 0.3f, noise = 0.4f, vignette = 0.15f, scan = 0.6f, vhs = 1f),
            ratios = listOf(r43, r34, r916), isVideo = true, stamp = StampStyle.VHS,
        ),
        Recipe(
            "kino", "Kino", Category.VIDEO, Body.CINE, 0xFF111827, 0xFF38BDF8,
            "Widescreen cinema: 2.39:1 frame, teal-and-orange grade, film grain, 24 fps.",
            Look(lut = "tealorange", lutMix = 0.6f, contrast = 1.05f, grain = 0.3f, halation = 0.2f, vignette = 0.3f, bloom = 0.15f),
            ratios = listOf(Ratio(239, 100), r169), isVideo = true, videoFps = 24, stamp = StampStyle.NONE,
        ),
        Recipe(
            "instav", "Insta V", Category.VIDEO, Body.INSTANT, 0xFFF1FAEE, 0xFFE63946,
            "Moving instant prints — video inside an instant frame.",
            Look(lut = "polaroid690", saturation = 0.9f, exposure = 0.1f, fade = 0.08f, bloom = 0.2f, grain = 0.15f, vignette = 0.25f),
            ratios = listOf(r11, Ratio(46, 62)), border = Border.INSTANT, isVideo = true, videoFps = 24, stamp = StampStyle.NONE,
        ),
        Recipe(
            "funsaverv", "Fun Saver V", Category.VIDEO, Body.DISPOSABLE, 0xFF2A9D8F, 0xFFF4A261,
            "The disposable look as video, date stamp and all.",
            Look(lut = "elite400", temp = 0.2f, contrast = 1.08f, saturation = 1.1f, grain = 0.45f, grainSize = 1.3f, vignette = 0.5f, soft = 0.45f,
                chroma = 0.4f, halation = 0.2f, leak = 0.35f),
            ratios = listOf(r34, r43, r916), isVideo = true, videoFps = 24,
        ),
        Recipe(
            "handycam", "Handycam", Category.VIDEO, Body.HANDYCAM, 0xFF3D405B, 0xFFC9ADA7,
            "Late-90s digital camcorder. Over-sharpened, punchy, interlaced, with date and time burned in.",
            Look(lut = "dcr", sharpen = 0.8f, noise = 0.3f, chroma = 0.3f, interlace = 0.6f, bloom = 0.2f, vignette = 0.1f),
            ratios = listOf(r43, r34, r916), isVideo = true, stamp = StampStyle.CAMCORDER,
        ),
        Recipe(
            "glowv", "Glow V", Category.VIDEO, Body.CINE, 0xFFF6BD60, 0xFFF28482,
            "Dreamy glowing video. Soft pastel light that blooms around every highlight.",
            Look(lut = "astia", exposure = 0.2f, bloom = 0.9f, softAll = 0.45f, halation = 0.3f, fade = 0.1f, grain = 0.15f, highTint = V3(0.03f, 0f, 0.02f)),
            ratios = listOf(r916, r34, r43), isVideo = true, stamp = StampStyle.NONE,
        ),
        Recipe(
            "projector", "Projector", Category.VIDEO, Body.PROJECTOR, 0xFF6D597A, 0xFFEAAC8B,
            "Old film projected on a wall. Hot centre, rounded gate, flicker and dust.",
            Look(lut = "kodachrome64", exposure = 0.1f, contrast = 1.15f, saturation = 0.9f, temp = 0.35f, bloom = 0.4f, softAll = 0.25f,
                grain = 0.45f, vignette = 0.75f, flicker = 1.2f, dust = 0.6f, weave = 0.6f),
            ratios = listOf(r43, r34), border = Border.GATE8, isVideo = true, videoFps = 18, stamp = StampStyle.NONE,
        ),
        Recipe(
            "dreamblur", "Dream Blur", Category.VIDEO, Body.PHONE, 0xFF3A2E14, 0xFFFFC94D,
            "The viral slow-shutter night look: warm golden light, smeared motion and glowing trails.",
            Look(lut = "candlelight", lutMix = 0.7f, temp = 0.4f, saturation = 1.15f, bloom = 0.45f, halation = 0.25f, grain = 0.25f, vignette = 0.3f),
            ratios = listOf(r916, r34, r43), isVideo = true, videoFps = 12, manual = true, stamp = StampStyle.NONE,
        ),
    )

    val default: Recipe get() = all.first { it.id == "sure85" }

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

val ISO_VALUES = listOf(0, 50, 100, 160, 200, 400, 800, 1600, 3200) // 0 = auto
val FPS_VALUES = listOf(1, 2, 4, 6, 8, 12, 15, 18, 24, 30)

/** Focal lengths shown on the lens button, as in classic camera apps (35mm-equivalent). */
fun focalLabel(zoom: Float): String = "${kotlin.math.round(24f * zoom).toInt()}"
