package com.funcam.app.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaSaver {
    private const val ALBUM = "FUN CAM"

    private fun name(prefix: String, ext: String) =
        prefix + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "." + ext

    fun saveJpeg(ctx: Context, bmp: Bitmap, cameraName: String): Uri? {
        val fileName = name("FUNCAM_", "jpg")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.DESCRIPTION, "Shot on FUN CAM · $cameraName")
        }
        return insert(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values, Environment.DIRECTORY_PICTURES, fileName) { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }

    fun saveVideo(ctx: Context, file: File): Uri? {
        val fileName = name("FUNCAM_", "mp4")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        val uri = insert(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values, Environment.DIRECTORY_MOVIES, fileName) { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        file.delete()
        return uri
    }

    @Suppress("DEPRECATION")
    private fun insert(
        ctx: Context, collection: Uri, values: ContentValues, dir: String, fileName: String,
        write: (java.io.OutputStream) -> Unit,
    ): Uri? {
        val resolver = ctx.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/$ALBUM")
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            val folder = File(Environment.getExternalStoragePublicDirectory(dir), ALBUM).apply { mkdirs() }
            values.put(MediaStore.MediaColumns.DATA, File(folder, fileName).absolutePath)
        }
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use(write)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /** Loads an image honoring EXIF orientation, downsampled so the long edge is <= [maxLong]. */
    fun loadBitmap(ctx: Context, uri: Uri, maxLong: Int = 4096): Pair<Bitmap, Long?>? {
        val resolver = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxLong) sample *= 2
        val bmp = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        var rotation = 0
        var taken: Long? = null
        resolver.openInputStream(uri)?.use {
            val exif = ExifInterface(it)
            rotation = exif.rotationDegrees
            val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            taken = raw?.let { s -> runCatching { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(s)?.time }.getOrNull() }
        }
        val out = if (rotation != 0) {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                .also { if (it !== bmp) bmp.recycle() }
        } else bmp
        return out to taken
    }
}
