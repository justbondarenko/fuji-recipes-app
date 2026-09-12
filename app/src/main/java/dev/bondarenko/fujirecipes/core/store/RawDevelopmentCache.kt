package dev.bondarenko.fujirecipes.core.store

import java.io.File
import java.io.InputStream
import java.io.OutputStream

const val MAX_RAF_BYTES = 128L * 1024 * 1024
private val RAF_SIGNATURE = "FUJIFILMCCD-RAW ".toByteArray(Charsets.US_ASCII)

class RawDevelopmentCache(private val directory: File) {
    init {
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }
    }

    fun importRaf(
        displayName: String,
        input: InputStream,
        expectedLength: Long? = null,
        onProgress: (written: Long, total: Long?) -> Unit = { _, _ -> },
    ): File {
        if (expectedLength != null && expectedLength > MAX_RAF_BYTES) {
            throw IllegalArgumentException("$displayName is larger than the 128 MB RAF limit.")
        }
        val safeName = displayName.substringBeforeLast('.').sanitize().ifEmpty { "selected" }
        val partial = File(directory, "$safeName.raf.part")
        val target = File(directory, "$safeName.raf")
        partial.delete()

        try {
            partial.outputStream().use { output ->
                copyBounded(input, output, expectedLength, onProgress)
            }
            if (!partial.hasRafSignature()) {
                throw IllegalArgumentException("$displayName is not a Fujifilm RAF file.")
            }
            target.delete()
            if (!partial.renameTo(target)) error("The completed RAF could not be moved into the cache.")
            return target
        } catch (error: Exception) {
            partial.delete()
            throw error
        }
    }

    /**
     * Where render number [serial] of [source] goes.
     *
     * Numbered rather than overwritten: an image loader keys on the path, so a second render
     * written over the first would keep showing the first. Older renders are removed as each
     * new one lands, so the cache holds one picture and not a session's worth.
     */
    fun renderFile(source: File, serial: Int): File =
        File(directory, "${source.nameWithoutExtension}-render-$serial.jpg")

    /** Deletes every render except [keep] — called once the new one is safely on disk. */
    fun clearRenders(keep: File? = null) {
        directory.listFiles()
            ?.filter { it.name.contains("-render-") && it.name.endsWith(".jpg") }
            ?.filterNot { keep != null && it.absolutePath == keep.absolutePath }
            ?.forEach { it.delete() }
    }

    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun copyBounded(
        input: InputStream,
        output: OutputStream,
        expectedLength: Long?,
        onProgress: (Long, Long?) -> Unit,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            written += read
            if (written > MAX_RAF_BYTES) throw IllegalArgumentException("The RAF exceeds 128 MB.")
            output.write(buffer, 0, read)
            onProgress(written, expectedLength)
        }
        if (expectedLength != null && written != expectedLength) {
            throw IllegalArgumentException("The RAF stopped after $written of $expectedLength bytes.")
        }
    }

    private fun File.hasRafSignature(): Boolean = inputStream().use { input ->
        val actual = ByteArray(RAF_SIGNATURE.size)
        input.read(actual) == actual.size && actual.contentEquals(RAF_SIGNATURE)
    }

    private fun String.sanitize(): String = replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)

    companion object {
        const val DIRECTORY_NAME = "raw-development"
    }
}
