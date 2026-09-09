package dev.bondarenko.fujirecipes.core.store

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaType
import dev.bondarenko.fujirecipes.camera.usb.mediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CameraDownloadResult(val filenames: List<String>)

data class CameraExportProgress(
    val currentFile: Int,
    val totalFiles: Int,
    val filename: String,
    val written: Long,
    val totalBytes: Long,
)

/** Streams card objects into a user-selected Storage Access Framework folder. */
class CameraPhotoExporter(
    private val resolver: ContentResolver,
    private val cameraController: CameraController,
) {
    suspend fun download(
        files: List<CameraMediaObject>,
        treeUri: Uri,
        onProgress: (CameraExportProgress) -> Unit = {},
    ): CameraDownloadResult = withContext(Dispatchers.IO) {
        require(files.isNotEmpty()) { "Select at least one camera file." }
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val total = files.sumOf { it.info.compressedSize }
        var completed = 0L
        val saved = mutableListOf<String>()
        val created = mutableListOf<Uri>()

        try {
            for ((index, media) in files.withIndex()) {
                val filename = media.info.filename.safeFilename()
                val type = requireNotNull(media.mediaType) { "$filename is not a JPEG or RAF file." }
                val mimeType = if (type == CameraMediaType.JPEG) "image/jpeg" else "image/x-fuji-raf"
                val document = DocumentsContract.createDocument(resolver, parent, mimeType, filename)
                    ?: error("The selected folder could not create $filename.")
                created += document
                resolver.openOutputStream(document, "w")?.use { output ->
                    val progress: (Long, Long) -> Unit = { written, _ ->
                        onProgress(
                            CameraExportProgress(
                                currentFile = index + 1,
                                totalFiles = files.size,
                                filename = filename,
                                written = completed + written,
                                totalBytes = total,
                            ),
                        )
                    }
                    if (type == CameraMediaType.JPEG) {
                        cameraController.downloadCameraJpeg(media, output, progress)
                    } else {
                        cameraController.downloadCameraRaf(media, output, progress)
                    }
                } ?: error("The selected folder could not open $filename for writing.")
                completed += media.info.compressedSize
                onProgress(
                    CameraExportProgress(index + 1, files.size, filename, completed, total),
                )
                saved += filename
            }
        } catch (error: Exception) {
            created.asReversed().forEach { document ->
                runCatching { DocumentsContract.deleteDocument(resolver, document) }
            }
            throw error
        }
        CameraDownloadResult(saved)
    }

    private fun String.safeFilename(): String =
        substringAfterLast('/').substringAfterLast('\\').ifBlank { "camera-photo" }
}
