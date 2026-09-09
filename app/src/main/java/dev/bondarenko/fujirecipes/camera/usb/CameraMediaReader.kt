package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpObject
import dev.bondarenko.fujirecipes.camera.ptp.PtpObjectInfo
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.parseObjectInfo
import java.io.OutputStream

const val MAX_CAMERA_JPEG_BYTES = 50L * 1024 * 1024
const val MAX_CAMERA_RAF_BYTES = 128L * 1024 * 1024

private val RAF_SIGNATURE = "FUJIFILMCCD-RAW ".toByteArray(Charsets.US_ASCII)

data class CameraMediaObject(val handle: Int, val info: PtpObjectInfo)

enum class CameraMediaType { JPEG, RAW }

val CameraMediaObject.mediaType: CameraMediaType?
    get() = when {
        info.filename.endsWith(".raf", ignoreCase = true) -> CameraMediaType.RAW
        info.format == PtpObject.FORMAT_JPEG ||
            info.filename.endsWith(".jpg", ignoreCase = true) ||
            info.filename.endsWith(".jpeg", ignoreCase = true) -> CameraMediaType.JPEG
        else -> null
    }

enum class CameraMediaFailure {
    UNSUPPORTED,
    NO_STORAGE,
    TOO_LARGE,
    INCOMPLETE_TRANSFER,
}

class CameraMediaError(
    val reason: CameraMediaFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Read-only access to JPEGs on a camera card. All calls are synchronous and must be made
 * through CameraController's serialised IO path.
 */
fun listCameraJpegs(
    session: PtpSession,
    onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
): List<CameraMediaObject> {
    val required = listOf(
        Operation.GET_STORAGE_IDS,
        Operation.GET_OBJECT_HANDLES,
        Operation.GET_OBJECT_INFO,
        Operation.GET_OBJECT,
    )
    if (required.any { !session.supportsOperation(it) }) {
        throw CameraMediaError(
            CameraMediaFailure.UNSUPPORTED,
            "The camera does not expose the PTP object operations needed to browse its card.",
        )
    }

    val storages = session.getStorageIds()
    if (storages.isEmpty()) {
        throw CameraMediaError(CameraMediaFailure.NO_STORAGE, "The camera reports no mounted card.")
    }

    val handles = storages.flatMap { storage ->
        try {
            session.getObjectHandles(storage, PtpObject.FORMAT_JPEG)
        } catch (error: PtpError) {
            if (error.code != ResponseCode.PARAMETER_NOT_SUPPORTED) throw error
            session.getObjectHandles(storage)
        }
    }.distinct()

    val objects = handles.mapIndexedNotNull { index, handle ->
        onProgress(index, handles.size)
        val info = parseObjectInfo(session.getObjectInfo(handle))
        if (info.format == PtpObject.FORMAT_JPEG) CameraMediaObject(handle, info) else null
    }
    onProgress(handles.size, handles.size)

    return objects.sortedWith(
        compareByDescending<CameraMediaObject> { it.info.captureDate }
            .thenByDescending { it.info.filename }
            .thenByDescending { it.handle.toLong() and 0xffffffffL },
    )
}

/**
 * RAF candidates on the card.
 *
 * Fuji bodies do not consistently advertise one PTP object-format code for RAF across the
 * available protocol reports. The catalogue therefore uses the filename only to offer a
 * candidate; [RawDevelopmentCache.download] performs the second, authoritative check against
 * the RAF file signature before the object can enter a conversion job.
 */
fun listCameraRafs(
    session: PtpSession,
    onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
): List<CameraMediaObject> {
    val required = listOf(
        Operation.GET_STORAGE_IDS,
        Operation.GET_OBJECT_HANDLES,
        Operation.GET_OBJECT_INFO,
        Operation.GET_OBJECT,
    )
    if (required.any { !session.supportsOperation(it) }) {
        throw CameraMediaError(
            CameraMediaFailure.UNSUPPORTED,
            "The camera does not expose the PTP object operations needed to browse its card.",
        )
    }
    val storages = session.getStorageIds()
    if (storages.isEmpty()) {
        throw CameraMediaError(CameraMediaFailure.NO_STORAGE, "The camera reports no mounted card.")
    }
    val handles = storages.flatMap { storage -> session.getObjectHandles(storage) }.distinct()
    val objects = handles.mapIndexedNotNull { index, handle ->
        onProgress(index, handles.size)
        val info = parseObjectInfo(session.getObjectInfo(handle))
        if (info.filename.endsWith(".raf", ignoreCase = true) && info.compressedSize > 0) {
            CameraMediaObject(handle, info)
        } else {
            null
        }
    }
    onProgress(handles.size, handles.size)
    return objects.sortedWith(
        compareByDescending<CameraMediaObject> { it.info.captureDate }
            .thenByDescending { it.info.filename }
            .thenByDescending { it.handle.toLong() and 0xffffffffL },
    )
}

/** Lists every downloadable JPEG and RAF as its own card object. */
fun listCameraFiles(
    session: PtpSession,
    onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
): List<CameraMediaObject> {
    val required = listOf(
        Operation.GET_STORAGE_IDS,
        Operation.GET_OBJECT_HANDLES,
        Operation.GET_OBJECT_INFO,
        Operation.GET_OBJECT,
    )
    if (required.any { !session.supportsOperation(it) }) {
        throw CameraMediaError(
            CameraMediaFailure.UNSUPPORTED,
            "The camera does not expose the PTP object operations needed to browse its card.",
        )
    }
    val storages = session.getStorageIds()
    if (storages.isEmpty()) {
        throw CameraMediaError(CameraMediaFailure.NO_STORAGE, "The camera reports no mounted card.")
    }
    val handles = storages.flatMap { storage -> session.getObjectHandles(storage) }.distinct()
    val media = handles.mapIndexedNotNull { index, handle ->
        onProgress(index, handles.size)
        val info = parseObjectInfo(session.getObjectInfo(handle))
        CameraMediaObject(handle, info).takeIf { it.mediaType != null && info.compressedSize > 0 }
    }
    onProgress(handles.size, handles.size)

    return media.sortedWith(
        compareByDescending<CameraMediaObject> { it.info.captureDate }
            .thenByDescending { it.info.filename }
            .thenByDescending { it.handle.toLong() and 0xffffffffL },
    )
}

/** Thumbnail failure is local to a tile; null means use the placeholder. */
fun readCameraThumbnail(session: PtpSession, objectHandle: Int): ByteArray? {
    if (!session.supportsOperation(Operation.GET_THUMB)) return null
    val bytes = try {
        session.getThumb(objectHandle)
    } catch (_: PtpError) {
        return null
    }
    return bytes.takeIf { it.hasJpegSignature() }
}

fun downloadCameraJpeg(
    session: PtpSession,
    media: CameraMediaObject,
    output: OutputStream,
    maxBytes: Long = MAX_CAMERA_JPEG_BYTES,
    onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
): Long {
    val advertised = media.info.compressedSize
    if (advertised > maxBytes) {
        throw CameraMediaError(
            CameraMediaFailure.TOO_LARGE,
            "${media.info.filename} is $advertised bytes, beyond the $maxBytes-byte limit.",
        )
    }

    val checkingOutput = PrefixCheckingOutputStream(output, 3)
    val written = session.getObject(media.handle, checkingOutput, maxBytes, onProgress)
    if (advertised != 0L && written != advertised) {
        throw CameraMediaError(
            CameraMediaFailure.INCOMPLETE_TRANSFER,
            "${media.info.filename} declared $advertised bytes but transferred $written.",
        )
    }
    if (!checkingOutput.prefix.hasJpegSignature()) {
        throw CameraMediaError(
            CameraMediaFailure.INCOMPLETE_TRANSFER,
            "${media.info.filename} did not contain a valid JPEG.",
        )
    }
    return written
}

fun downloadCameraRaf(
    session: PtpSession,
    media: CameraMediaObject,
    output: OutputStream,
    maxBytes: Long = MAX_CAMERA_RAF_BYTES,
    onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
): Long {
    val advertised = media.info.compressedSize
    if (advertised > maxBytes) {
        throw CameraMediaError(
            CameraMediaFailure.TOO_LARGE,
            "${media.info.filename} is $advertised bytes, beyond the $maxBytes-byte limit.",
        )
    }
    val checkingOutput = PrefixCheckingOutputStream(output, RAF_SIGNATURE.size)
    val written = session.getObject(media.handle, checkingOutput, maxBytes, onProgress)
    if (advertised != 0L && written != advertised) {
        throw CameraMediaError(
            CameraMediaFailure.INCOMPLETE_TRANSFER,
            "${media.info.filename} declared $advertised bytes but transferred $written.",
        )
    }
    if (!checkingOutput.prefix.contentEquals(RAF_SIGNATURE)) {
        throw CameraMediaError(
            CameraMediaFailure.INCOMPLETE_TRANSFER,
            "${media.info.filename} did not contain a Fujifilm RAF signature.",
        )
    }
    return written
}

private class PrefixCheckingOutputStream(
    private val delegate: OutputStream,
    prefixSize: Int,
) : OutputStream() {
    private val captured = ByteArray(prefixSize)
    private var capturedCount = 0
    val prefix: ByteArray get() = captured.copyOf(capturedCount)

    override fun write(value: Int) {
        if (capturedCount < captured.size) captured[capturedCount++] = value.toByte()
        delegate.write(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        val captureLength = minOf(length, captured.size - capturedCount)
        if (captureLength > 0) {
            bytes.copyInto(captured, capturedCount, offset, offset + captureLength)
            capturedCount += captureLength
        }
        delegate.write(bytes, offset, length)
    }

    override fun flush() = delegate.flush()
}

internal fun ByteArray.hasJpegSignature(): Boolean =
    size >= 3 && this[0] == 0xff.toByte() && this[1] == 0xd8.toByte() && this[2] == 0xff.toByte()
