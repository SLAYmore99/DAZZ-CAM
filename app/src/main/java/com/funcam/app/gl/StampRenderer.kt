package com.funcam.app.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.funcam.app.model.StampStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Draws the burned-in date / camcorder / VHS overlays as a transparent bitmap. */
object StampRenderer {

    /** Text that changes over time; used as a cache key so we only redraw when it changes. */
    fun key(style: StampStyle, time: Long): String = when (style) {
        StampStyle.CAMCORDER, StampStyle.VHS -> style.name + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date(time))
        StampStyle.NONE, StampStyle.PROJECTOR -> ""
        else -> style.name + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(time))
    }

    fun render(style: StampStyle, w: Int, h: Int, time: Long): Bitmap? {
        if (style == StampStyle.NONE || style == StampStyle.PROJECTOR) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val d = Date(time)
        val unit = minOf(w, h).toFloat()
        when (style) {
            StampStyle.FILM_ORANGE, StampStyle.DIGI_ORANGE -> {
                val text = if (style == StampStyle.FILM_ORANGE)
                    SimpleDateFormat("''yy  M  d", Locale.US).format(d)
                else SimpleDateFormat("yyyy.MM.dd", Locale.US).format(d)
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textSize = unit * 0.058f
                    textSkewX = -0.12f
                    letterSpacing = 0.06f
                    color = Color.rgb(255, 156, 52)
                    setShadowLayer(textSize * 0.45f, 0f, 0f, Color.argb(220, 255, 90, 0))
                }
                val tw = p.measureText(text)
                c.drawText(text, w - tw - unit * 0.07f, h - unit * 0.07f, p)
                // second pass for a hotter core
                p.setShadowLayer(textSize(p) * 0.12f, 0f, 0f, Color.argb(255, 255, 120, 20))
                p.color = Color.rgb(255, 196, 120)
                p.alpha = 150
                c.drawText(text, w - tw - unit * 0.07f, h - unit * 0.07f, p)
            }
            StampStyle.CAMCORDER -> {
                val date = SimpleDateFormat("MMM. d yyyy", Locale.US).format(d).uppercase(Locale.US)
                val clock = SimpleDateFormat("h:mm:ss a", Locale.US).format(d)
                val size = unit * 0.055f
                outlined(c, date, unit * 0.07f, h - unit * 0.07f - size * 1.25f, size, Typeface.DEFAULT_BOLD)
                outlined(c, clock, unit * 0.07f, h - unit * 0.07f, size, Typeface.DEFAULT_BOLD)
            }
            StampStyle.VHS -> {
                val mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                val size = unit * 0.062f
                outlined(c, "PLAY ▶", unit * 0.07f, unit * 0.07f + size, size, mono)
                outlined(c, "SP", w - unit * 0.07f - size * 1.4f, unit * 0.07f + size, size, mono)
                val clock = SimpleDateFormat("hh:mm a", Locale.US).format(d)
                val date = SimpleDateFormat("MMM.dd yyyy", Locale.US).format(d).uppercase(Locale.US)
                outlined(c, clock, unit * 0.07f, h - unit * 0.07f - size * 1.3f, size, mono)
                outlined(c, date, unit * 0.07f, h - unit * 0.07f, size, mono)
            }
            else -> {}
        }
        return bmp
    }

    private fun textSize(p: Paint) = p.textSize

    private fun outlined(c: Canvas, text: String, x: Float, y: Float, size: Float, tf: Typeface) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf; textSize = size; style = Paint.Style.STROKE
            strokeWidth = size * 0.12f; color = Color.argb(200, 0, 0, 0)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf; textSize = size; color = Color.rgb(245, 245, 240)
        }
        c.drawText(text, x, y, stroke)
        c.drawText(text, x, y, fill)
    }
}
