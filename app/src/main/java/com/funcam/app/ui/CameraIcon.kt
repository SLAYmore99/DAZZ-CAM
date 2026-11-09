package com.funcam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funcam.app.model.Body
import com.funcam.app.model.Recipe

/** Original camera-body artwork, drawn from primitives so nothing is copied from anywhere. */
@Composable
fun CameraIcon(recipe: Recipe, modifier: Modifier = Modifier, width: Dp = 64.dp) {
    val body = Color(recipe.bodyColor)
    val accent = Color(recipe.accentColor)
    Canvas(modifier.size(width, width * 0.72f)) {
        val w = size.width
        val h = size.height
        val dark = lerp(body, Color.Black, 0.55f)
        val light = lerp(body, Color.White, 0.35f)
        fun box(x: Float, y: Float, bw: Float, bh: Float, c: Color, r: Float = 0.08f) =
            drawRoundRect(c, Offset(x * w, y * h), Size(bw * w, bh * h), CornerRadius(r * w, r * w))
        fun lens(cx: Float, cy: Float, r: Float) {
            val c = Offset(cx * w, cy * h)
            val rr = r * w
            drawCircle(Color(0xFF1A1A1A), rr, c)
            drawCircle(accent, rr * 0.86f, c, style = Stroke(rr * 0.1f))
            drawCircle(Color(0xFF0B0F18), rr * 0.66f, c)
            drawCircle(Color(0xFF2B3A55), rr * 0.42f, c)
            drawCircle(Color.White.copy(alpha = 0.55f), rr * 0.13f, Offset(c.x - rr * 0.22f, c.y - rr * 0.22f))
        }
        fun flash(x: Float, y: Float, fw: Float, fh: Float) {
            box(x, y, fw, fh, Color(0xFFE8E8E8), 0.02f)
            drawLine(Color(0xFFBDBDBD), Offset(x * w, (y + fh / 2) * h), Offset((x + fw) * w, (y + fh / 2) * h), w * 0.006f)
        }
        fun reel(cx: Float, cy: Float, r: Float) {
            drawCircle(dark, r * w, Offset(cx * w, cy * h))
            drawCircle(accent, r * w * 0.35f, Offset(cx * w, cy * h))
            for (i in 0 until 3) {
                val a = Math.toRadians(i * 120.0 + 30).toFloat()
                drawCircle(body, r * w * 0.22f, Offset(cx * w + kotlin.math.cos(a) * r * w * 0.62f, cy * h + kotlin.math.sin(a) * r * w * 0.62f))
            }
        }

        when (recipe.body) {
            Body.COMPACT -> {
                box(0.06f, 0.2f, 0.88f, 0.62f, body)
                box(0.06f, 0.2f, 0.88f, 0.1f, light, 0.04f)
                flash(0.12f, 0.36f, 0.2f, 0.14f)
                box(0.7f, 0.33f, 0.14f, 0.1f, Color(0xFF223344), 0.02f)
                lens(0.55f, 0.55f, 0.17f)
                box(0.74f, 0.13f, 0.12f, 0.08f, accent, 0.02f)
            }
            Body.DIGICAM -> {
                box(0.08f, 0.22f, 0.84f, 0.58f, body, 0.1f)
                drawRoundRect(Brushes.sheen(w, h), Offset(0.08f * w, 0.22f * h), Size(0.84f * w, 0.58f * h), CornerRadius(0.1f * w))
                flash(0.14f, 0.3f, 0.16f, 0.1f)
                lens(0.62f, 0.52f, 0.15f)
                box(0.16f, 0.62f, 0.2f, 0.04f, accent, 0.02f)
                box(0.68f, 0.15f, 0.14f, 0.07f, dark, 0.02f)
            }
            Body.SLR, Body.MANUAL -> {
                val p = Path().apply {
                    moveTo(0.36f * w, 0.24f * h); lineTo(0.42f * w, 0.08f * h); lineTo(0.58f * w, 0.08f * h); lineTo(0.64f * w, 0.24f * h); close()
                }
                drawPath(p, dark)
                box(0.05f, 0.22f, 0.9f, 0.6f, body)
                box(0.05f, 0.22f, 0.9f, 0.12f, light, 0.04f)
                box(0.05f, 0.5f, 0.9f, 0.32f, dark.copy(alpha = 0.6f), 0.06f)
                box(0.1f, 0.12f, 0.12f, 0.1f, accent, 0.03f)
                lens(0.5f, 0.56f, 0.21f)
            }
            Body.DISPOSABLE -> {
                box(0.06f, 0.22f, 0.88f, 0.6f, body)
                box(0.06f, 0.5f, 0.88f, 0.22f, accent, 0.0f)
                flash(0.12f, 0.3f, 0.22f, 0.14f)
                lens(0.6f, 0.44f, 0.12f)
                drawCircle(Color(0xFF333333), w * 0.035f, Offset(0.86f * w, 0.17f * h))
                box(0.7f, 0.28f, 0.12f, 0.08f, Color(0xFF223344), 0.02f)
            }
            Body.HALF -> {
                box(0.1f, 0.16f, 0.8f, 0.68f, body)
                box(0.1f, 0.16f, 0.8f, 0.12f, dark, 0.04f)
                lens(0.5f, 0.56f, 0.18f)
                box(0.16f, 0.34f, 0.14f, 0.09f, Color(0xFF223344), 0.02f)
                box(0.7f, 0.34f, 0.1f, 0.09f, accent, 0.02f)
            }
            Body.TOY, Body.MEDIUM -> {
                box(0.14f, 0.1f, 0.72f, 0.8f, body, 0.06f)
                box(0.14f, 0.1f, 0.72f, 0.14f, light, 0.04f)
                if (recipe.body == Body.MEDIUM) box(0.3f, 0.02f, 0.4f, 0.1f, dark, 0.02f)
                lens(0.5f, 0.58f, if (recipe.body == Body.TOY) 0.25f else 0.22f)
                box(0.2f, 0.16f, 0.1f, 0.06f, accent, 0.02f)
            }
            Body.QUAD -> {
                box(0.06f, 0.18f, 0.88f, 0.66f, body)
                for (i in 0 until 2) for (j in 0 until 2) lens(0.36f + i * 0.28f, 0.4f + j * 0.3f, 0.09f)
                box(0.08f, 0.2f, 0.12f, 0.08f, accent, 0.02f)
            }
            Body.STEREO -> {
                box(0.04f, 0.24f, 0.92f, 0.56f, body)
                box(0.04f, 0.24f, 0.92f, 0.1f, accent, 0.03f)
                for (i in 0 until 3) lens(0.26f + i * 0.24f, 0.56f, 0.09f)
            }
            Body.INSTANT -> {
                box(0.1f, 0.12f, 0.8f, 0.76f, body, 0.1f)
                box(0.1f, 0.64f, 0.8f, 0.24f, light, 0.08f)
                box(0.22f, 0.8f, 0.56f, 0.03f, Color(0xFF111111), 0.01f)
                drawRect(accent, Offset(0.47f * w, 0.12f * h), Size(0.06f * w, 0.52f * h))
                lens(0.5f, 0.4f, 0.16f)
                flash(0.16f, 0.18f, 0.18f, 0.1f)
                box(0.68f, 0.18f, 0.14f, 0.1f, Color(0xFF223344), 0.02f)
            }
            Body.SUPER8 -> {
                box(0.2f, 0.18f, 0.66f, 0.44f, body, 0.06f)
                val grip = Path().apply {
                    moveTo(0.46f * w, 0.6f * h); lineTo(0.62f * w, 0.6f * h); lineTo(0.58f * w, 0.95f * h); lineTo(0.44f * w, 0.95f * h); close()
                }
                drawPath(grip, dark)
                box(0.04f, 0.26f, 0.18f, 0.28f, dark, 0.02f)
                lens(0.12f, 0.4f, 0.1f)
                box(0.7f, 0.24f, 0.12f, 0.1f, accent, 0.02f)
            }
            Body.CINE -> {
                reel(0.34f, 0.24f, 0.17f); reel(0.66f, 0.24f, 0.17f)
                box(0.16f, 0.36f, 0.68f, 0.44f, body, 0.06f)
                lens(0.3f, 0.6f, 0.12f)
                box(0.6f, 0.48f, 0.18f, 0.08f, accent, 0.02f)
            }
            Body.VHS, Body.HANDYCAM -> {
                box(0.18f, 0.3f, 0.74f, 0.46f, body, 0.1f)
                box(0.3f, 0.14f, 0.5f, 0.1f, dark, 0.04f)
                box(0.34f, 0.18f, 0.06f, 0.14f, dark, 0.02f)
                box(0.72f, 0.18f, 0.06f, 0.14f, dark, 0.02f)
                box(0.04f, 0.38f, 0.22f, 0.3f, dark, 0.06f)
                lens(0.12f, 0.53f, 0.1f)
                drawCircle(FC.red, w * 0.025f, Offset(0.84f * w, 0.4f * h))
                box(0.5f, 0.6f, 0.3f, 0.06f, accent, 0.02f)
            }
            Body.PROJECTOR -> {
                reel(0.3f, 0.22f, 0.16f); reel(0.62f, 0.22f, 0.16f)
                box(0.12f, 0.38f, 0.7f, 0.42f, body, 0.06f)
                lens(0.86f, 0.58f, 0.1f)
                box(0.2f, 0.82f, 0.08f, 0.08f, dark, 0.02f)
                box(0.66f, 0.82f, 0.08f, 0.08f, dark, 0.02f)
                box(0.22f, 0.5f, 0.3f, 0.07f, accent, 0.02f)
            }
        }
    }
}

private object Brushes {
    fun sheen(w: Float, h: Float) = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.06f)),
        start = Offset(0f, 0f), end = Offset(w, h),
    )
}

@Suppress("unused")
private fun DrawScope.noop() = Unit
