package dev.bondarenko.fujirecipes.core.store

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaError
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaFailure
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
        isCancelled: () -> Boolean = { false },
        onFileStarted: (filename: String) -> Unit = {},
        onFileFinished: (filename: String) -> Unit = {},
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
                // Between files, not only within one: a cancel arriving in the gap must not
                // start the next download just because the last chunk had already gone by.
                if (isCancelled()) {
                    throw CameraMediaError(
                        CameraMediaFailure.CANCELLED,
                        "The download was cancelled before $filename.",
                    )
                }
                onFileStarted(filename)
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
                        cameraController.downloadCameraJpeg(media, output, progress, isCancelled)
                    } else {
                        cameraController.downloadCameraRaf(media, output, progress, isCancelled)
                    }
                } ?: error("The selected folder could not open $filename for writing.")
                completed += media.info.compressedSize
                onProgress(
                    CameraExportProgress(index + 1, files.size, filename, completed, total),
                )
                onFileFinished(filename)
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

    /**
     * Removes one file this app wrote into [treeUri].
     *
     * Only ever used for the file a killed process left half-written: the exporter's own
     * cleanup handles every failure it can see, and a process death is the one case where no
     * code of ours runs at all. Returns false when the file is already gone, which is not an
     * error — the user may have deleted it themselves.
     */
    suspend fun deleteDocument(
        treeUri: Uri,
        filename: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )

        runCatching {
            resolver.query(children, columns, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) != filename) continue
                    val document = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        cursor.getString(0),
                    )
                    return@runCatching DocumentsContract.deleteDocument(resolver, document)
                }
            }
            false
        }.getOrDefault(false)
    }

    private fun String.safeFilename(): String =
        substringAfterLast('/').substringAfterLast('\\').ifBlank { "camera-photo" }
}
