package dev.bondarenko.fujirecipes.core.store

import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaError
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaFailure
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import java.io.File
import java.io.FileOutputStream
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

    fun outputFile(source: File): File =
        File(directory, "${source.nameWithoutExtension}-developed.jpg")

    /** Streams a selected card object into the same validated cache as the phone picker. */
    fun download(
        session: PtpSession,
        media: CameraMediaObject,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
    ): File {
        val expected = media.info.compressedSize
        if (expected > MAX_RAF_BYTES) {
            throw CameraMediaError(
                CameraMediaFailure.TOO_LARGE,
                "${media.info.filename} is larger than the 128 MB RAF limit.",
            )
        }
        val safeName = media.info.filename.substringBeforeLast('.').sanitize().ifEmpty { "selected" }
        val partial = File(directory, "$safeName.raf.part")
        val target = File(directory, "$safeName.raf")
        partial.delete()
        try {
            val written = FileOutputStream(partial).use { output ->
                session.getObject(media.handle, output, MAX_RAF_BYTES, onProgress).also {
                    output.fd.sync()
                }
            }
            if (written != expected) {
                throw CameraMediaError(
                    CameraMediaFailure.INCOMPLETE_TRANSFER,
                    "${media.info.filename} declared $expected bytes but transferred $written.",
                )
            }
            if (!partial.hasRafSignature()) {
                throw IllegalArgumentException(
                    "${media.info.filename} did not contain a Fujifilm RAF signature.",
                )
            }
            target.delete()
            if (!partial.renameTo(target)) error("The completed RAF could not be moved into the cache.")
            return target
        } catch (error: Exception) {
            partial.delete()
            throw error
        }
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
