package com.example.familyapp.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/** Owns only app-private product copies, never the user's gallery originals. */
class ProductPhotoStore(context: Context) {
    private val context = context.applicationContext
    private val directory = File(this.context.filesDir, "product_photos")

    fun file(fileName: String): File {
        require(fileName.matches(Regex("[a-f0-9-]+\\.jpg")))
        return File(directory, fileName)
    }

    fun newCameraFile(): File {
        val captures = File(context.cacheDir, "camera-captures")
        check(captures.exists() || captures.mkdirs())
        return File.createTempFile("capture-", ".jpg", captures)
    }

    fun cameraFile(fileName: String): File {
        require(fileName.matches(Regex("capture-[a-zA-Z0-9-]+\\.jpg")))
        return File(File(context.cacheDir, "camera-captures"), fileName)
    }

    fun importPhoto(uri: Uri): String {
        val source = File.createTempFile("photo-import-", ".tmp", context.cacheDir)
        val targetName = "${UUID.randomUUID()}.jpg"
        val target = file(targetName)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read photo")
            input.use { from ->
                source.outputStream().use { to ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = from.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 40L * 1024 * 1024) { "Photo is too large" }
                        to.write(buffer, 0, count)
                    }
                }
            }
            val bitmap = decode(source, 1600, orient = true) ?: throw IOException("Unsupported image")
            try {
                check(directory.exists() || directory.mkdirs())
                target.outputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output))
                }
            } finally { bitmap.recycle() }
            return targetName
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally { source.delete() }
    }

    fun delete(fileName: String?) {
        if (fileName != null) runCatching { file(fileName).delete() }
    }

    fun thumbnail(fileName: String, maxSize: Int): Bitmap? = decode(file(fileName), maxSize, orient = false)

    private fun decode(source: File, maxSize: Int, orient: Boolean): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSize) sample *= 2
        val bitmap = BitmapFactory.decodeFile(source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (!orient) return bitmap
        val orientation = runCatching {
            ExifInterface(source.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        }
        if (matrix.isIdentity) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
