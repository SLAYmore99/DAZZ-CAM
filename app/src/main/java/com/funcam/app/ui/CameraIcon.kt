package com.funcam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funcam.app.model.Body
import com.funcam.app.model.Recipe

/** Original camera-body artwork, drawn from primitives with shading (mirrored in web/icons.js). */
@Composable
fun CameraIcon(recipe: Recipe, modifier: Modifier = Modifier, width: Dp = 64.dp) {
    val body = Color(recipe.bodyColor)
    val accent = Color(recipe.accentColor)
    Canvas(modifier.size(width, width * 0.72f)) { drawCamera(recipe.body, body, accent) }
}

private class Pen(val d: DrawScope, val body: Color, val accent: Color) {
    val w = d.size.width
    val h = d.size.height
    val dark = lerp(body, Color.Black, 0.5f)
    val light = lerp(body, Color.White, 0.3f)

    fun shade(c: Color, y: Float, bh: Float) = Brush.verticalGradient(
        listOf(lerp(c, Color.White, 0.22f), c, lerp(c, Color.Black, 0.22f)), startY = y * h, endY = (y + bh) * h,
    )

    fun box(x: Float, y: Float, bw: Float, bh: Float, c: Color, r: Float = 0.06f, flat: Boolean = false) {
        if (flat) d.drawRoundRect(c, Offset(x * w, y * h), Size(bw * w, bh * h), CornerRadius(r * w))
        else d.drawRoundRect(shade(c, y, bh), Offset(x * w, y * h), Size(bw * w, bh * h), CornerRadius(r * w))
    }

    /** Main body with a soft top highlight and bottom shadow. */
    fun shell(x: Float, y: Float, bw: Float, bh: Float, c: Color = body, r: Float = 0.07f) {
        d.drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(x * w + w * 0.012f, y * h + h * 0.03f), Size(bw * w, bh * h), CornerRadius(r * w))
        box(x, y, bw, bh, c, r)
        d.drawLine(Color.White.copy(alpha = 0.35f), Offset((x + r) * w, y * h + 1.5f), Offset((x + bw - r) * w, y * h + 1.5f), w * 0.01f)
    }

    /** Leatherette band with a fine stipple. */
    fun leather(x: Float, y: Float, bw: Float, bh: Float, c: Color = Color(0xFF1B1B1B)) {
        box(x, y, bw, bh, c, 0.02f)
        var yy = y + 0.02f
        while (yy < y + bh - 0.01f) {
            var xx = x + 0.015f
            while (xx < x + bw - 0.01f) {
                d.drawCircle(Color.White.copy(alpha = 0.06f), w * 0.004f, Offset(xx * w, yy * h)); xx += 0.03f
            }
            yy += 0.05f
        }
    }

    fun lens(cx: Float, cy: Float, r: Float, ring: Color = accent) {
        val c = Offset(cx * w, cy * h)
        val rr = r * w
        d.drawCircle(Color.Black.copy(alpha = 0.35f), rr * 1.04f, c + Offset(w * 0.01f, h * 0.02f))
        d.drawCircle(Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF121212)), c - Offset(rr, rr), c + Offset(rr, rr)), rr, c)
        d.drawCircle(ring, rr * 0.84f, c, style = Stroke(rr * 0.09f))
        d.drawCircle(Brush.radialGradient(listOf(Color(0xFF3B5B8A), Color(0xFF0E1626), Color(0xFF03050A)), c, rr * 0.7f), rr * 0.68f, c)
        d.drawCircle(Color(0xFF7FA6D6).copy(alpha = 0.25f), rr * 0.4f, c, style = Stroke(rr * 0.05f))
        d.drawCircle(Color.White.copy(alpha = 0.75f), rr * 0.12f, c + Offset(-rr * 0.25f, -rr * 0.25f))
        d.drawCircle(Color.White.copy(alpha = 0.3f), rr * 0.06f, c + Offset(rr * 0.22f, rr * 0.18f))
    }

    fun flash(x: Float, y: Float, fw: Float, fh: Float) {
        box(x, y, fw, fh, Color(0xFFD9D9D9), 0.015f)
        d.drawRoundRect(
            Brush.verticalGradient(listOf(Color.White, Color(0xFFBFC6CC)), startY = y * h, endY = (y + fh) * h),
            Offset((x + 0.008f) * w, (y + 0.015f) * h), Size((fw - 0.016f) * w, (fh - 0.03f) * h), CornerRadius(w * 0.01f),
        )
        var lx = x + 0.02f
        while (lx < x + fw - 0.01f) {
            d.drawLine(Color(0xFF9FA7AE), Offset(lx * w, (y + 0.02f) * h), Offset(lx * w, (y + fh - 0.02f) * h), w * 0.004f); lx += 0.025f
        }
    }

    fun window(x: Float, y: Float, ww: Float, wh: Float) = d.drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFF8FB3D9), Color(0xFF1E2C3D)), Offset(x * w, y * h), Offset((x + ww) * w, (y + wh) * h)),
        Offset(x * w, y * h), Size(ww * w, wh * h), CornerRadius(w * 0.012f),
    )

    fun button(x: Float, y: Float, bw: Float, bh: Float, c: Color = Color(0xFFD64545)) = box(x, y, bw, bh, c, 0.02f)

    fun reel(cx: Float, cy: Float, r: Float) {
        d.drawCircle(Brush.radialGradient(listOf(light, dark), Offset(cx * w, cy * h), r * w), r * w, Offset(cx * w, cy * h))
        d.drawCircle(accent, r * w * 0.3f, Offset(cx * w, cy * h))
        for (i in 0 until 3) {
            val a = Math.toRadians(i * 120.0 + 30).toFloat()
            d.drawCircle(dark, r * w * 0.22f, Offset(cx * w + kotlin.math.cos(a) * r * w * 0.6f, cy * h + kotlin.math.sin(a) * r * w * 0.6f))
        }
    }

    fun poly(c: Color, vararg pts: Float) {
        val p = Path().apply {
            moveTo(pts[0] * w, pts[1] * h)
            var i = 2
            while (i < pts.size) { lineTo(pts[i] * w, pts[i + 1] * h); i += 2 }
            close()
        }
        d.drawPath(p, c)
    }
}

fun DrawScope.drawCamera(type: Body, body: Color, accent: Color) {
    val p = Pen(this, body, accent)
    with(p) {
        when (type) {
            Body.COMPACT -> {
                shell(0.05f, 0.2f, 0.9f, 0.62f)
                flash(0.1f, 0.3f, 0.2f, 0.13f); window(0.71f, 0.29f, 0.14f, 0.1f)
                lens(0.55f, 0.56f, 0.18f); button(0.73f, 0.13f, 0.13f, 0.07f, accent)
            }
            Body.DIGICAM -> {
                shell(0.07f, 0.22f, 0.86f, 0.58f, r = 0.1f)
                flash(0.12f, 0.29f, 0.17f, 0.1f); lens(0.62f, 0.52f, 0.16f)
                box(0.14f, 0.64f, 0.22f, 0.04f, accent, 0.02f); button(0.68f, 0.15f, 0.14f, 0.07f, dark)
            }
            Body.SLR, Body.MEDIUM -> {
                val big = type == Body.MEDIUM
                poly(dark, 0.34f, 0.25f, 0.41f, 0.06f, 0.59f, 0.06f, 0.66f, 0.25f)
                shell(0.04f, 0.22f, 0.92f, if (big) 0.66f else 0.6f)
                leather(0.06f, 0.42f, 0.88f, if (big) 0.42f else 0.36f)
                box(0.09f, 0.12f, 0.12f, 0.1f, Color(0xFF9E9E9E), 0.02f); button(0.78f, 0.13f, 0.1f, 0.09f, accent)
                lens(0.5f, 0.58f, if (big) 0.25f else 0.22f)
            }
            Body.RANGEFINDER -> {
                shell(0.04f, 0.2f, 0.92f, 0.62f, Color(0xFFCFCFCF))
                leather(0.05f, 0.4f, 0.9f, 0.4f, body)
                window(0.1f, 0.26f, 0.12f, 0.08f); window(0.3f, 0.26f, 0.08f, 0.08f)
                button(0.78f, 0.12f, 0.1f, 0.08f, accent); lens(0.6f, 0.58f, 0.19f)
            }
            Body.DISPOSABLE -> {
                shell(0.05f, 0.22f, 0.9f, 0.6f)
                box(0.05f, 0.48f, 0.9f, 0.26f, accent, 0.0f)
                box(0.05f, 0.56f, 0.9f, 0.03f, Color.White.copy(alpha = 0.6f), 0f, flat = true)
                flash(0.1f, 0.29f, 0.22f, 0.14f); lens(0.6f, 0.43f, 0.12f)
                window(0.72f, 0.27f, 0.12f, 0.08f)
                drawCircle(Color(0xFF2A2A2A), size.width * 0.04f, Offset(0.86f * size.width, 0.17f * size.height))
            }
            Body.HALF -> {
                shell(0.12f, 0.14f, 0.76f, 0.72f)
                leather(0.13f, 0.4f, 0.74f, 0.42f)
                window(0.17f, 0.22f, 0.13f, 0.09f); button(0.68f, 0.06f, 0.12f, 0.08f, accent); lens(0.5f, 0.56f, 0.17f)
            }
            Body.TOY -> {
                shell(0.14f, 0.12f, 0.72f, 0.78f, r = 0.05f)
                box(0.3f, 0.04f, 0.4f, 0.1f, dark, 0.02f)
                drawCircle(Color(0xFF2B2B2B), 0.29f * size.width, Offset(0.5f * size.width, 0.57f * size.height))
                for (i in 0 until 24) {
                    val a = i * 15f * (Math.PI / 180).toFloat()
                    drawLine(Color(0xFF444444), Offset(0.5f * size.width + kotlin.math.cos(a) * 0.24f * size.width, 0.57f * size.height + kotlin.math.sin(a) * 0.24f * size.width),
                        Offset(0.5f * size.width + kotlin.math.cos(a) * 0.29f * size.width, 0.57f * size.height + kotlin.math.sin(a) * 0.29f * size.width), size.width * 0.012f)
                }
                lens(0.5f, 0.57f, 0.22f); box(0.2f, 0.17f, 0.1f, 0.06f, accent, 0.02f)
            }
            Body.TLR -> {
                shell(0.24f, 0.08f, 0.52f, 0.86f, r = 0.04f)
                box(0.26f, 0.02f, 0.48f, 0.08f, dark, 0.02f)
                leather(0.26f, 0.12f, 0.48f, 0.78f)
                box(0.3f, 0.2f, 0.4f, 0.66f, Color(0xFF2E2E2E), 0.03f)
                lens(0.5f, 0.34f, 0.12f, Color(0xFFBDBDBD)); lens(0.5f, 0.68f, 0.13f)
                box(0.33f, 0.47f, 0.34f, 0.05f, accent, 0.01f)
            }
            Body.QUAD, Body.MULTI16 -> {
                shell(0.05f, 0.16f, 0.9f, 0.7f)
                val n = if (type == Body.QUAD) 2 else 4
                val step = if (n == 2) 0.24f else 0.12f
                val r = if (n == 2) 0.085f else 0.045f
                val x0 = 0.5f - step * (n - 1) / 2f + 0.06f
                val y0 = 0.51f - step * 1.3f * (n - 1) / 2f
                for (i in 0 until n) for (j in 0 until n) lens(x0 + i * step, y0 + j * step * 1.3f, r)
                box(0.08f, 0.2f, 0.1f, 0.08f, accent, 0.02f)
            }
            Body.STEREO, Body.PANORAMA -> {
                shell(0.02f, 0.24f, 0.96f, 0.54f)
                if (type == Body.STEREO) {
                    box(0.02f, 0.24f, 0.96f, 0.1f, accent, 0.03f)
                    for (i in 0 until 4) lens(0.2f + i * 0.2f, 0.56f, 0.075f)
                } else {
                    leather(0.03f, 0.44f, 0.94f, 0.3f)
                    drawCircle(dark, 0.06f * size.width, Offset(0.12f * size.width, 0.2f * size.height))
                    drawCircle(dark, 0.06f * size.width, Offset(0.88f * size.width, 0.2f * size.height))
                    lens(0.5f, 0.55f, 0.15f); button(0.7f, 0.17f, 0.08f, 0.07f, accent)
                }
            }
            Body.INSTANT, Body.INSTANT_SQ -> {
                val sq = type == Body.INSTANT_SQ
                shell(if (sq) 0.16f else 0.1f, 0.1f, if (sq) 0.68f else 0.8f, 0.8f, r = 0.12f)
                if (sq) box(0.16f, 0.62f, 0.68f, 0.28f, light, 0.1f)
                else drawCircle(lerp(body, Color.Black, 0.12f), 0.3f * size.width, Offset(0.47f * size.width, 0.52f * size.height))
                lens(if (sq) 0.5f else 0.47f, if (sq) 0.4f else 0.52f, if (sq) 0.15f else 0.17f)
                flash(if (sq) 0.6f else 0.68f, 0.17f, 0.17f, 0.11f); window(if (sq) 0.21f else 0.15f, 0.17f, 0.12f, 0.09f)
                box(if (sq) 0.24f else 0.18f, 0.82f, if (sq) 0.52f else 0.6f, 0.03f, Color(0xFF151515), 0.01f)
            }
            Body.PHONE -> {
                shell(0.2f, 0.06f, 0.6f, 0.88f, r = 0.12f)
                box(0.25f, 0.12f, 0.32f, 0.4f, lerp(body, Color.White, 0.08f), 0.08f)
                lens(0.35f, 0.22f, 0.065f, Color(0xFF555555)); lens(0.35f, 0.42f, 0.065f, Color(0xFF555555)); lens(0.49f, 0.32f, 0.065f, Color(0xFF555555))
                drawCircle(Color(0xFFFFF3C4), 0.022f * size.width, Offset(0.49f * size.width, 0.17f * size.height))
                drawCircle(accent, 0.07f * size.width, Offset(0.32f * size.width, 0.78f * size.height))
                poly(Color.White, 0.305f, 0.745f, 0.305f, 0.815f, 0.345f, 0.78f)
            }
            Body.SUPER8 -> {
                shell(0.22f, 0.16f, 0.66f, 0.46f)
                poly(dark, 0.46f, 0.6f, 0.62f, 0.6f, 0.58f, 0.96f, 0.44f, 0.96f)
                box(0.04f, 0.24f, 0.2f, 0.3f, Color(0xFF2B2B2B), 0.02f)
                lens(0.12f, 0.39f, 0.1f); box(0.7f, 0.22f, 0.12f, 0.1f, accent, 0.02f)
            }
            Body.CINE -> {
                reel(0.34f, 0.24f, 0.17f); reel(0.66f, 0.24f, 0.17f)
                shell(0.14f, 0.36f, 0.72f, 0.46f)
                lens(0.3f, 0.6f, 0.13f); box(0.58f, 0.48f, 0.2f, 0.08f, accent, 0.02f)
            }
            Body.VHS, Body.HANDYCAM -> {
                shell(0.18f, 0.3f, 0.76f, 0.46f, r = 0.1f)
                box(0.3f, 0.13f, 0.5f, 0.1f, dark, 0.04f); box(0.34f, 0.18f, 0.06f, 0.14f, dark, 0.02f); box(0.72f, 0.18f, 0.06f, 0.14f, dark, 0.02f)
                box(0.03f, 0.37f, 0.24f, 0.32f, Color(0xFF2B2B2B), 0.06f)
                lens(0.12f, 0.53f, 0.105f)
                drawCircle(FC.red, size.width * 0.025f, Offset(0.86f * size.width, 0.4f * size.height))
                box(0.5f, 0.6f, 0.32f, 0.06f, accent, 0.02f)
            }
            Body.PROJECTOR -> {
                reel(0.3f, 0.22f, 0.16f); reel(0.62f, 0.22f, 0.16f)
                shell(0.1f, 0.38f, 0.72f, 0.42f)
                lens(0.86f, 0.58f, 0.1f)
                box(0.18f, 0.82f, 0.08f, 0.09f, dark, 0.02f); box(0.66f, 0.82f, 0.08f, 0.09f, dark, 0.02f)
                box(0.2f, 0.5f, 0.32f, 0.07f, accent, 0.02f)
            }
        }
    }
}
