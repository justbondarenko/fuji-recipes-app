package dev.bondarenko.fujirecipes.core.store

import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaError
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaFailure
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.downloadCameraJpeg
import java.io.File
import java.io.FileOutputStream

/** App-private, disposable files downloaded from the camera for photo analysis. */
class CameraMediaCache(private val directory: File) {
    init {
        directory.mkdirs()
        // A recreated process has no analysis state pointing at an older download, so every
        // prior cache entry is stale. This also removes interrupted .part files.
        directory.listFiles()?.forEach { it.delete() }
    }

    fun download(
        session: PtpSession,
        media: CameraMediaObject,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
    ): File {
        val stem = media.info.filename.substringBeforeLast('.').sanitize().ifEmpty { "photo" }
        val target = File(directory, "${stem}-${media.handle.unsignedHex()}.jpg")
        val partial = File(directory, "${target.name}.part")

        partial.delete()
        try {
            FileOutputStream(partial).use { output ->
                downloadCameraJpeg(session, media, output, onProgress = onProgress)
                output.fd.sync()
            }
            if (!partial.hasJpegSignature()) {
                throw CameraMediaError(
                    CameraMediaFailure.INCOMPLETE_TRANSFER,
                    "${media.info.filename} did not contain a valid JPEG.",
                )
            }
            if (target.exists() && !target.delete()) {
                error("The previous cached copy of ${target.name} could not be replaced.")
            }
            if (!partial.renameTo(target)) {
                error("The completed camera photo could not be moved into the cache.")
            }
            return target
        } catch (error: Exception) {
            partial.delete()
            throw error
        }
    }

    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun File.hasJpegSignature(): Boolean = inputStream().use { input ->
        input.read() == 0xff && input.read() == 0xd8 && input.read() == 0xff
    }

    private fun String.sanitize(): String =
        replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)

    private fun Int.unsignedHex(): String =
        (toLong() and 0xffffffffL).toString(16).padStart(8, '0')

    companion object {
        const val DIRECTORY_NAME = "camera-media"
    }
}
