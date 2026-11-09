package com.funcam.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File

/**
 * "Negatives": the undeveloped source of each photo plus the settings used, kept privately so a
 * shot can be re-developed later in the Darkroom with another camera, exposure or white balance.
 */
data class Negative(
    val id: String,
    val image: File,
    val recipeId: String,
    val ratioW: Int,
    val ratioH: Int,
    val imgAspect: Float,
    val exposure: Float,
    val temp: Float,
    val tint: Float,
    val flash: Boolean,
    val seed: Float,
    val time: Long,
)

object Negatives {
    private fun dir(ctx: Context) = File(ctx.filesDir, "negatives").apply { mkdirs() }

    fun save(ctx: Context, src: Bitmap, n: Negative): Negative {
        val img = File(dir(ctx), "${n.id}.jpg")
        val scale = minOf(1f, 2400f / maxOf(src.width, src.height))
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true) else src
        img.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (bmp !== src) bmp.recycle()
        val json = JSONObject().apply {
            put("recipe", n.recipeId); put("rw", n.ratioW); put("rh", n.ratioH); put("aspect", n.imgAspect.toDouble())
            put("exp", n.exposure.toDouble()); put("temp", n.temp.toDouble()); put("tint", n.tint.toDouble())
            put("flash", n.flash); put("seed", n.seed.toDouble()); put("time", n.time)
        }
        File(dir(ctx), "${n.id}.json").writeText(json.toString())
        // keep the most recent 60
        list(ctx).drop(60).forEach { delete(ctx, it) }
        return n.copy(image = img)
    }

    fun list(ctx: Context): List<Negative> =
        dir(ctx).listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { f ->
            try {
                val j = JSONObject(f.readText())
                val id = f.nameWithoutExtension
                Negative(
                    id, File(dir(ctx), "$id.jpg"), j.getString("recipe"), j.getInt("rw"), j.getInt("rh"),
                    j.getDouble("aspect").toFloat(), j.getDouble("exp").toFloat(), j.getDouble("temp").toFloat(),
                    j.getDouble("tint").toFloat(), j.getBoolean("flash"), j.getDouble("seed").toFloat(), j.getLong("time"),
                )
            } catch (_: Exception) {
                null
            }
        }.filter { it.image.exists() }.sortedByDescending { it.time }

    fun load(n: Negative): Bitmap? = BitmapFactory.decodeFile(n.image.absolutePath)

    fun thumb(n: Negative): Bitmap? =
        BitmapFactory.decodeFile(n.image.absolutePath, BitmapFactory.Options().apply { inSampleSize = 8 })

    fun delete(ctx: Context, n: Negative) {
        File(dir(ctx), "${n.id}.json").delete()
        n.image.delete()
    }
}
