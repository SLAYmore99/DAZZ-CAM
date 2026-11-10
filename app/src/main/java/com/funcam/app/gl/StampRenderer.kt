package com.funcam.app.gl

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.funcam.app.model.Border
import com.funcam.app.model.StampStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Burned-in overlays: the classic 7-segment orange date of film-camera data backs, digicam dates,
 * camcorder/VHS on-screen text, and film edge markings.
 */
object StampRenderer {

    fun key(style: StampStyle, time: Long): String = when (style) {
        StampStyle.CAMCORDER, StampStyle.VHS -> style.name + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date(time))
        StampStyle.NONE -> ""
        else -> style.name + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(time))
    }

    // segments: a b c d e f g
    private val DIGITS = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd", '4' to "fgbc",
        '5' to "afgcd", '6' to "afgedc", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg",
    )

    /** One 7-segment glyph at (x, y) top-left, height h. Returns advance width. */
    private fun segDigit(c: Canvas, ch: Char, x: Float, y: Float, h: Float, paint: Paint): Float {
        val w = h * 0.52f
        val t = h * 0.12f
        if (ch == '\'') {
            val p = Path().apply { moveTo(x + t * 0.6f, y); lineTo(x + t * 1.6f, y); lineTo(x + t * 0.8f, y + h * 0.3f); lineTo(x + t * 0.2f, y + h * 0.3f); close() }
            c.drawPath(p, paint)
            return t * 2.4f
        }
        if (ch == ' ') return w * 0.7f
        if (ch == '.') { c.drawCircle(x + t, y + h - t * 0.6f, t * 0.6f, paint); return t * 2.2f }
        val segs = DIGITS[ch] ?: return w
        val half = h / 2f
        fun hSeg(sx: Float, sy: Float) {
            val p = Path().apply {
                moveTo(sx + t * 0.5f, sy); lineTo(sx + t, sy - t / 2); lineTo(sx + w - t, sy - t / 2); lineTo(sx + w - t * 0.5f, sy)
                lineTo(sx + w - t, sy + t / 2); lineTo(sx + t, sy + t / 2); close()
            }
            c.drawPath(p, paint)
        }
        fun vSeg(sx: Float, sy: Float, len: Float) {
            val p = Path().apply {
                moveTo(sx, sy + t * 0.5f); lineTo(sx + t / 2, sy + t); lineTo(sx + t / 2, sy + len - t); lineTo(sx, sy + len - t * 0.5f)
                lineTo(sx - t / 2, sy + len - t); lineTo(sx - t / 2, sy + t); close()
            }
            c.drawPath(p, paint)
        }
        val l = x + t / 2
        val r = x + w - t / 2
        for (s in segs) when (s) {
            'a' -> hSeg(x, y + t / 2)
            'g' -> hSeg(x, y + half)
            'd' -> hSeg(x, y + h - t / 2)
            'f' -> vSeg(l, y + t / 2, half - t / 2)
            'b' -> vSeg(r, y + t / 2, half - t / 2)
            'e' -> vSeg(l, y + half, half - t / 2)
            'c' -> vSeg(r, y + half, half - t / 2)
        }
        return w + t * 0.9f
    }

    private fun segText(c: Canvas, text: String, rightX: Float, bottomY: Float, h: Float, paint: Paint) {
        val probe = Canvas()
        var width = 0f
        for (ch in text) width += segDigit(probe, ch, 0f, 0f, h, Paint())
        var x = rightX - width
        val y = bottomY - h
        c.save()
        c.skew(-0.08f, 0f)
        c.translate(y * 0.08f, 0f)
        for (ch in text) x += segDigit(c, ch, x, y, h, paint)
        c.restore()
    }

    /** Picture-space overlay (transparent), or null. */
    fun render(style: StampStyle, w: Int, h: Int, time: Long): Bitmap? {
        if (style == StampStyle.NONE) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        val d = Date(time)
        val unit = minOf(w, h).toFloat()
        when (style) {
            StampStyle.FILM_ORANGE -> {
                val yy = "%02d".format(cal.get(Calendar.YEAR) % 100)
                val text = "'$yy ${"%2d".format(cal.get(Calendar.MONTH) + 1).replace(' ', ' ')} ${"%2d".format(cal.get(Calendar.DAY_OF_MONTH))}"
                val hgt = unit * 0.042f
                val right = w - unit * 0.075f
                val bottom = h - unit * 0.07f
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(200, 255, 80, 0); maskFilter = BlurMaskFilter(hgt * 0.35f, BlurMaskFilter.Blur.NORMAL)
                }
                segText(c, text, right, bottom, hgt, glow)
                segText(c, text, right, bottom, hgt, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 150, 45) })
                segText(c, text, right, bottom, hgt, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(140, 255, 225, 170); maskFilter = BlurMaskFilter(hgt * 0.05f, BlurMaskFilter.Blur.INNER)
                })
            }
            StampStyle.DIGI_ORANGE -> {
                val text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(d)
                val size = unit * 0.042f
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = size; letterSpacing = 0.04f
                    color = Color.rgb(255, 170, 40); setShadowLayer(size * 0.25f, 0f, 0f, Color.argb(200, 255, 100, 0))
                }
                c.drawText(text, w - p.measureText(text) - unit * 0.06f, h - unit * 0.06f, p)
            }
            StampStyle.CAMCORDER -> {
                val size = unit * 0.05f
                val date = SimpleDateFormat("MMM. d yyyy", Locale.US).format(d).uppercase(Locale.US)
                val clock = SimpleDateFormat("h:mm:ss a", Locale.US).format(d)
                outlined(c, date, unit * 0.07f, h - unit * 0.07f - size * 1.25f, size, Typeface.DEFAULT_BOLD)
                outlined(c, clock, unit * 0.07f, h - unit * 0.07f, size, Typeface.DEFAULT_BOLD)
            }
            StampStyle.VHS -> {
                val mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                val size = unit * 0.058f
                outlined(c, "PLAY ▶", unit * 0.07f, unit * 0.07f + size, size, mono)
                outlined(c, "SP", w - unit * 0.07f - size * 1.4f, unit * 0.07f + size, size, mono)
                outlined(c, SimpleDateFormat("a hh:mm", Locale.US).format(d), unit * 0.07f, h - unit * 0.07f - size * 1.3f, size, mono)
                outlined(c, SimpleDateFormat("MMM.dd yyyy", Locale.US).format(d).uppercase(Locale.US), unit * 0.07f, h - unit * 0.07f, size, mono)
            }
            StampStyle.NONE -> {}
        }
        return bmp
    }

    /** Output-space overlay with film edge print for negative / sprocket frames, or null. */
    fun marks(border: Border, edgeText: String, w: Int, h: Int, rect: FloatArray, frameNo: Int): Bitmap? {
        if (edgeText.isEmpty() || (border != Border.NEGATIVE && border != Border.SPROCKET)) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.argb(215, 255, 166, 60)
            setShadowLayer(2f, 0f, 0f, Color.argb(120, 255, 120, 0))
        }
        val top = (1f - rect[3]) * h      // picture top in px (canvas y down)
        val bottom = (1f - rect[1]) * h
        if (border == Border.NEGATIVE) {
            val band = top
            p.textSize = band * 0.32f
            c.drawText(edgeText, w * 0.06f, band * 0.62f, p)
            c.drawText("▶ $frameNo", w * 0.62f, band * 0.62f, p)
            c.drawText("$frameNo", w * 0.08f, bottom + (h - bottom) * 0.62f, p)
            c.drawText("${frameNo}A  ▶", w * 0.7f, bottom + (h - bottom) * 0.62f, p)
        } else {
            // sprocket: small print inside the hole bands
            val band = (bottom - top) * 0.13f
            p.textSize = band * 0.26f
            p.color = Color.argb(150, 255, 170, 70)
            c.drawText("$edgeText  ▶ $frameNo", w * 0.2f, top + band * 0.96f, p)
            c.drawText("${frameNo}A", w * 0.66f, bottom - band * 0.04f, p)
        }
        return bmp
    }

    private fun outlined(c: Canvas, text: String, x: Float, y: Float, size: Float, tf: Typeface) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf; textSize = size; style = Paint.Style.STROKE; strokeWidth = size * 0.12f; color = Color.argb(200, 0, 0, 0)
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; color = Color.rgb(245, 245, 240) }
        c.drawText(text, x, y, stroke)
        c.drawText(text, x, y, fill)
    }
}
