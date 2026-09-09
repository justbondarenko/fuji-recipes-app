package dev.bondarenko.fujirecipes.core.store

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * What a camera download batch was doing when the process last stopped.
 *
 * The exporter cleans up after itself when a transfer throws
 * ([CameraPhotoExporter.download] deletes every document it created). It cannot clean up after
 * a process kill, because no code of ours runs at all — Android reclaims a cached process
 * without unwinding anything. So a batch that dies that way leaves finished files and one
 * half-written file in the user's folder with nothing to say so.
 *
 * This record is the only thing that survives that. It is written before each file begins and
 * deleted when the batch finishes, so its mere existence at launch means the last batch did not
 * finish, and [pendingFile] names the one file that may be truncated.
 *
 * The completed files are deliberately not treated as rubbish: they are whole, they are what
 * the user asked for, and only the pending one is worth offering to delete.
 */
data class CameraTransferRecord(
    val batchId: String,
    val treeUri: String,
    val startedAt: String,
    val totalFiles: Int,
    /** Files fully written before the interruption. These are whole and are the user's. */
    val completedFiles: List<String> = emptyList(),
    /** In flight when the record was last written — the one that may be truncated. */
    val pendingFile: String? = null,
) {
    val completedCount: Int get() = completedFiles.size
}

/**
 * The record, on disk.
 *
 * In `filesDir`, never `cacheDir`: the OS is free to reclaim a cache directory, and a record
 * that can vanish is one that reports "nothing went wrong" after something did.
 *
 * Synchronous on purpose. Every caller is already on `Dispatchers.IO` inside the transfer, and
 * one of them is the exporter's per-file callback, which is not a suspending context. The file
 * holds a handful of filenames, so a write costs less than the USB read that follows it.
 */
class CameraTransferStore(private val file: File) {

    /** The interrupted batch, or null when the last one finished (or none has ever run). */
    fun read(): CameraTransferRecord? {
        if (!file.exists()) return null

        // A record that will not parse is treated as no record. The alternative is refusing to
        // start a new download because of a file nobody can read, which helps no one.
        return runCatching {
            val root = json.parseToJsonElement(file.readText()).jsonObject
            val batchId = root["batchId"]?.jsonPrimitive?.content ?: return null
            val treeUri = root["treeUri"]?.jsonPrimitive?.content ?: return null

            CameraTransferRecord(
                batchId = batchId,
                treeUri = treeUri,
                startedAt = root["startedAt"]?.jsonPrimitive?.content.orEmpty(),
                totalFiles = root["totalFiles"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                completedFiles = root["completedFiles"]?.jsonArray.orEmpty()
                    .map { it.jsonPrimitive.content },
                pendingFile = root["pendingFile"]?.jsonPrimitive?.content,
            )
        }.getOrNull()
    }

    fun begin(record: CameraTransferRecord): CameraTransferRecord = record.also(::write)

    /** Marks [filename] as the file now in flight. */
    fun fileStarted(record: CameraTransferRecord, filename: String): CameraTransferRecord =
        record.copy(pendingFile = filename).also(::write)

    /** Marks the in-flight file as whole. */
    fun fileFinished(record: CameraTransferRecord, filename: String): CameraTransferRecord =
        record.copy(
            completedFiles = record.completedFiles + filename,
            pendingFile = null,
        ).also(::write)

    /** The batch reached an end this process knows about — success, failure or cancel. */
    fun clear() {
        runCatching { file.delete() }
    }

    // A record that could not be written costs the interruption notice, not the download.
    private fun write(record: CameraTransferRecord) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(
                buildJsonObject {
                    put("batchId", record.batchId)
                    put("treeUri", record.treeUri)
                    put("startedAt", record.startedAt)
                    put("totalFiles", record.totalFiles)
                    val completed = record.completedFiles.map { JsonPrimitive(it) }
                    put("completedFiles", JsonArray(completed))
                    record.pendingFile?.let { put("pendingFile", it) }
                }.toString(),
            )
        }
    }

    companion object {
        const val FILE_NAME = "camera-transfer.json"
    }
}

private val json = Json { ignoreUnknownKeys = true }
