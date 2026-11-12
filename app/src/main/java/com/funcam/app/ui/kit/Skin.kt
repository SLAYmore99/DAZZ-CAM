package com.funcam.app.ui.kit

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.funcam.app.R

/** The three engine-v4 interfaces. Shared parts (sheets, sliders, switches, dial) draw themselves per kind. */
enum class Kind { DOT, GLASS, FIELD }

val Doto = FontFamily(Font(R.font.doto_bold, FontWeight.Normal), Font(R.font.doto_bold, FontWeight.Bold))
val Grotesk = FontFamily(Font(R.font.grotesk_medium, FontWeight.Medium), Font(R.font.grotesk_medium, FontWeight.Normal), Font(R.font.grotesk_bold, FontWeight.Bold))

data class Skin(
    val kind: Kind,
    val bg: Color,
    /** Sheet background. */
    val panel: Color,
    /** Cards, rows and unselected chips. */
    val card: Color,
    val ink: Color,
    val dim: Color,
    val faint: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    /** Headline font. */
    val display: FontFamily,
    /** Body and label font. */
    val label: FontFamily,
    val mono: FontFamily = FontFamily.Monospace,
    val sheetCorner: Dp,
    /** Labels in capitals with wide tracking. */
    val caps: Boolean,
    val dark: Boolean = true,
)

val LocalSkin = staticCompositionLocalOf<Skin> { error("no skin") }

fun HapticFeedback.tick() = performHapticFeedback(HapticFeedbackType.TextHandleMove)
fun HapticFeedback.thud() = performHapticFeedback(HapticFeedbackType.LongPress)

/** Small label in the skin's voice: capitals with tracking for DOT and FIELD, sentence case for GLASS. */
@Composable
fun Label(
    text: String, modifier: Modifier = Modifier, color: Color = LocalSkin.current.dim, size: TextUnit = 10.sp,
    weight: FontWeight = FontWeight.Medium, align: TextAlign? = null, maxLines: Int = 1,
) {
    val s = LocalSkin.current
    Text(
        if (s.caps) text.uppercase() else text, modifier = modifier, color = color, fontSize = size, fontWeight = weight,
        fontFamily = if (s.kind == Kind.DOT) s.mono else s.label, letterSpacing = if (s.caps) (size.value * 0.16f).sp else 0.sp,
        textAlign = align, maxLines = maxLines, overflow = TextOverflow.Ellipsis, lineHeight = (size.value * 1.35f).sp,
    )
}

/** Headline text (camera names, sheet titles, read-outs). */
@Composable
fun Display(
    text: String, modifier: Modifier = Modifier, color: Color = LocalSkin.current.ink, size: TextUnit = 28.sp,
    align: TextAlign? = null, weight: FontWeight = FontWeight.Bold,
) {
    val s = LocalSkin.current
    Text(
        if (s.kind == Kind.GLASS) text else text.uppercase(), modifier = modifier, color = color, fontSize = size, fontFamily = s.display,
        fontWeight = weight, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, textAlign = align,
        style = TextStyle(lineHeight = (size.value * 1.05f).sp), letterSpacing = if (s.kind == Kind.GLASS) (-0.3).sp else 0.sp,
    )
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = LocalSkin.current.dim, size: TextUnit = 12.sp, maxLines: Int = 4) {
    val s = LocalSkin.current
    Text(text, modifier = modifier, color = color, fontSize = size, fontFamily = if (s.kind == Kind.DOT) s.mono else s.label,
        lineHeight = (size.value * 1.4f).sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
