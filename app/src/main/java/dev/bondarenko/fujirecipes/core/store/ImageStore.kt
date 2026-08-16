package dev.bondarenko.fujirecipes.core.store

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Storage and optimization for recipe reference photos in app-private storage.
 *
 * Photos are stored in `filesDir/recipe_images/` as compressed WebP files.
 * User-selected camera photos (often 15MB+) are automatically downsampled (max 2048px)
 * and transcoded on save to prevent memory bloat and save storage (~300-500KB per photo).
 */
class ImageStore(
    private val directory: File,
    private val contentResolver: ContentResolver? = null,
) {
    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    /**
     * Reads an image from [uri], downscales to max [MAX_DIMENSION] px,
     * corrects orientation, compresses to WebP, and saves to [directory].
     *
     * @return The unique filename generated (e.g. "uuid.webp"), or null if saving failed.
     */
    suspend fun saveFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = contentResolver ?: return@withContext null
        runCatching {
            val inputStream = resolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.use { it.readBytes() }
            val orientation = getExifOrientation(bytes)
            val bitmap = decodeSampledBitmap(bytes, MAX_DIMENSION, MAX_DIMENSION) ?: return@withContext null
            val rotated = rotateBitmap(bitmap, orientation)

            val fileName = "${UUID.randomUUID()}.webp"
            val targetFile = File(directory, fileName)

            FileOutputStream(targetFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    rotated.compress(Bitmap.CompressFormat.WEBP_LOSSY, COMPRESSION_QUALITY, out)
                } else {
                    @Suppress("DEPRECATION")
                    rotated.compress(Bitmap.CompressFormat.WEBP, COMPRESSION_QUALITY, out)
                }
            }
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated.recycle()

            fileName
        }.getOrNull()
    }

    /**
     * Saves raw image stream directly into the store with the specified or generated [fileName].
     */
    suspend fun saveStream(fileName: String, stream: InputStream): File = withContext(Dispatchers.IO) {
        val targetFile = File(directory, fileName)
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { out ->
            stream.copyTo(out)
        }
        targetFile
    }

    /** Returns the local [File] corresponding to [fileName]. */
    fun getFile(fileName: String): File = File(directory, fileName)

    /** Checks if [fileName] exists in storage. */
    fun exists(fileName: String): Boolean = File(directory, fileName).exists()

    /** Deletes an image file by [fileName]. */
    fun delete(fileName: String): Boolean {
        val file = File(directory, fileName)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Deletes all image files in [directory] that are not in [activeFileNames].
     */
    suspend fun cleanOrphans(activeFileNames: Set<String>) = withContext(Dispatchers.IO) {
        val files = directory.listFiles() ?: return@withContext
        for (file in files) {
            if (file.isFile && file.name !in activeFileNames) {
                file.delete()
            }
        }
    }

    private fun getExifOrientation(bytes: ByteArray): Int {
        return runCatching {
            val exif = ExifInterface(bytes.inputStream())
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        const val DIRECTORY_NAME = "recipe_images"
        const val MAX_DIMENSION = 2048
        const val COMPRESSION_QUALITY = 82
        const val MAX_IMAGES_PER_RECIPE = 5
    }
}
