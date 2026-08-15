package dev.bondarenko.fujirecipes.core.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handing a file to the OS share sheet — FEAT-008 T-09.
 *
 * The whole reason this app needs no Google Drive integration, no OAuth, and no storage
 * permission: the sheet already reaches the file system, Drive, mail, messaging and whatever
 * else is installed, and each of those knows how to receive a file far better than this app
 * would know how to send one.
 *
 * `PRD.md` §7.6 specifies the Storage Access Framework instead. That is one destination; this
 * is all of them. The deviation is recorded in `FEAT-008/01-functional.md` §21, and SAF
 * remains about ten lines against the same file builder if a direct "save to device" is ever
 * wanted alongside.
 */
object ShareFile {

    /**
     * Where exports are staged.
     *
     * Inside `cacheDir`, so it needs no permission and the system may reclaim it — which is
     * correct, because whatever receives the file owns the real copy. `file_paths.xml` scopes
     * the provider to exactly this directory and nothing else.
     */
    private const val EXPORT_DIR = "export"

    private const val JSON_MIME = "application/json"
    private const val ZIP_MIME = "application/zip"

    /**
     * Writes [content] as [filename] and opens the share sheet.
     *
     * The directory is emptied first. Exports are transient by definition, and a cache that
     * accumulates every backup anyone ever took is a cache that gets cleared by the system at
     * the worst moment and looks like data loss.
     *
     * @throws java.io.IOException if the file cannot be written — the caller reports it, so
     * that "the export could not be prepared" is distinguishable from "no app took it".
     */
    fun share(context: Context, filename: String, content: ByteArray) {
        val directory = File(context.cacheDir, EXPORT_DIR)
        if (directory.exists()) directory.listFiles()?.forEach { it.delete() } else directory.mkdirs()

        val file = File(directory, filename)
        file.writeBytes(content)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (filename.endsWith(".zip")) ZIP_MIME else JSON_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, filename)
            // Without this the receiving app gets a URI it is not allowed to read, which
            // presents as the share appearing to work and the file arriving empty.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // `createChooser` rather than the bare intent: it guarantees a chooser even when the
        // user has set a default for this MIME type, which for a backup is the right
        // behaviour — the destination is a decision each time.
        val chooser = Intent.createChooser(send, null).apply {
            // The chooser is started from a context that may not be an Activity.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
    }

    fun share(context: Context, filename: String, text: String) =
        share(context, filename, text.toByteArray(Charsets.UTF_8))
}
