package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpObject
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.raw.RAW_CONVERSION_TRIGGER_PROPERTY
import dev.bondarenko.fujirecipes.camera.raw.RAW_PROFILE_PROPERTY
import dev.bondarenko.fujirecipes.camera.raw.RawProfilePatch
import dev.bondarenko.fujirecipes.camera.raw.UnsupportedRawProfile
import dev.bondarenko.fujirecipes.camera.raw.fujiRawObjectInfo
import dev.bondarenko.fujirecipes.camera.raw.patchRawDevelopmentProfile
import java.io.File
import java.io.FileOutputStream
import java.io.DataInputStream
import kotlinx.serialization.json.JsonObject

sealed interface RawDevelopmentStage {
    data object Preparing : RawDevelopmentStage
    data class Uploading(val written: Long, val total: Long) : RawDevelopmentStage
    data object ApplyingRecipe : RawDevelopmentStage
    data object Processing : RawDevelopmentStage
    data class Downloading(val written: Long, val total: Long) : RawDevelopmentStage
    data object CleaningUp : RawDevelopmentStage
}

/**
 * How much picture a render has to produce.
 *
 * The lab re-renders while someone is editing, and a full-resolution JPEG per edit is a
 * multi-megabyte download each time. [PREVIEW] asks for the camera's own thumbnail of the
 * conversion result first and falls back to the full file when the body does not offer a
 * usable one, so the fast path is taken where it exists and nothing is claimed where it does
 * not.
 *
 * The conversion **trigger** is deliberately not part of this yet. Implementations disagree
 * about whether `0xD183 = 0` means preview or full resolution, and this build's X-T50 and
 * X100VI values came from hardware that treated `0` as full. Until that probe is run
 * (`specs/plans/raw-development-lab.md` §2, Gate B) both qualities trigger the same verified
 * conversion and differ only in what is fetched afterwards.
 */
enum class RawRenderQuality { PREVIEW, FULL }

data class RawDevelopmentResult(
    val jpeg: File,
    val patch: RawProfilePatch,
    val outputHandle: Int,
    val width: Int? = null,
    val height: Int? = null,
    /** What was asked for. [fromThumbnail] says what the camera actually gave back. */
    val quality: RawRenderQuality = RawRenderQuality.FULL,
    /** True when the file is the camera's thumbnail rather than the converted JPEG itself. */
    val fromThumbnail: Boolean = false,
)

class RawDevelopmentError(message: String, cause: Throwable? = null) : Exception(message, cause)

class RawProfileCalibrationRequired(
    val cameraModel: String,
    val profile: ByteArray,
    cause: UnsupportedRawProfile,
) : Exception(cause.message, cause)

/**
 * Sends the RAF the camera will develop.
 *
 * Requires the RAW capability set first: an upload started against a body that cannot convert
 * leaves a loaded object behind for nothing.
 */
fun uploadRaf(
    session: PtpSession,
    raf: File,
    onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
) {
    requireRawCapabilities(session)
    if (!raf.isFile || raf.length() == 0L) throw RawDevelopmentError("The selected RAF is unavailable.")

    session.sendFujiRawObjectInfo(fujiRawObjectInfo(raf.length()))
    raf.inputStream().use { input ->
        session.sendFujiRawObject(input, raf.length(), onProgress)
    }
}

/**
 * Reads the camera's own profile for the loaded RAF, patches it, and stages it.
 *
 * The base is re-read every time rather than cached: it is the camera's answer for *this* RAF
 * on *this* body, and every field the adapter does not touch is carried from it.
 */
fun applyRawSettings(
    session: PtpSession,
    settings: JsonObject,
    onStage: (RawDevelopmentStage) -> Unit = {},
): RawProfilePatch {
    val baseProfile = session.readPropertyBytes(RAW_PROFILE_PROPERTY)
    if (baseProfile.isEmpty()) throw RawDevelopmentError("The camera returned an empty RAW profile.")
    onStage(RawDevelopmentStage.ApplyingRecipe)
    val patch = try {
        patchRawDevelopmentProfile(session.productName, baseProfile, settings)
    } catch (error: UnsupportedRawProfile) {
        // Keep the exact native block: this is the fixture needed to add a model adapter
        // without copying offsets from a different generation of camera.
        throw RawProfileCalibrationRequired(session.productName, baseProfile, error)
    }
    session.setPropertyBytes(RAW_PROFILE_PROPERTY, patch.bytes)
    // A successful SetDevicePropValue response is the only acknowledgement this staged
    // property provides. Physical X-T50 testing showed that reading D185 immediately after
    // the write exposes a normalised/internal view rather than the staged bytes. FilmKit,
    // libfuji and rawji all proceed directly from this successful set to D183 as well.
    return patch
}

/**
 * Triggers the conversion, fetches the result and removes the camera-side object.
 *
 * The handle snapshot is taken here, immediately before the trigger, so the object this
 * deletes is one this call watched appear.
 */
fun convertAndFetch(
    session: PtpSession,
    patch: RawProfilePatch,
    output: File,
    onStage: (RawDevelopmentStage) -> Unit = {},
    renderTimeoutMs: Long = 30_000,
    quality: RawRenderQuality = RawRenderQuality.FULL,
): RawDevelopmentResult {
    val before = session.getObjectHandles(PtpObject.ALL_STORAGES).toSet()
    val trigger = conversionTrigger(session.productName)
    session.setPropertyBytes(RAW_CONVERSION_TRIGGER_PROPERTY, packU16(trigger))
    onStage(RawDevelopmentStage.Processing)

    val outputHandle = waitForOneNewHandle(session, before, renderTimeoutMs)
    try {
        output.parentFile?.mkdirs()

        val thumbnail = if (quality == RawRenderQuality.PREVIEW) {
            previewThumbnail(session, outputHandle)
        } else {
            null
        }

        if (thumbnail != null) {
            output.writeBytes(thumbnail)
        } else {
            FileOutputStream(output).use { sink ->
                // Named, not trailing: `getObject` takes a cancellation predicate after the
                // progress callback, and a trailing lambda would silently bind to that instead.
                session.getObject(
                    handle = outputHandle,
                    output = sink,
                    maxBytes = MAX_CAMERA_JPEG_BYTES,
                    onProgress = { written, total ->
                        onStage(RawDevelopmentStage.Downloading(written, total))
                    },
                )
                sink.fd.sync()
            }
        }

        if (!output.hasJpegSignature()) {
            output.delete()
            throw RawDevelopmentError("The camera's conversion result was not a JPEG.")
        }
        val dimensions = output.readJpegDimensions()
        return RawDevelopmentResult(
            output,
            patch,
            outputHandle,
            width = dimensions?.first,
            height = dimensions?.second,
            quality = quality,
            fromThumbnail = thumbnail != null,
        )
    } catch (error: Exception) {
        output.delete()
        throw error
    } finally {
        onStage(RawDevelopmentStage.CleaningUp)
        runCatching { session.deleteObject(outputHandle) }
    }
}

/**
 * The camera's thumbnail of the conversion result, when it is big enough to judge a recipe by.
 *
 * Unverified on hardware, hence the size floor and the silent fallback: a body that attaches a
 * 160 px contact sheet to the result would otherwise turn the preview into a smudge, and a
 * body that attaches nothing must simply take the full path.
 */
private fun previewThumbnail(session: PtpSession, handle: Int): ByteArray? {
    val bytes = readCameraThumbnail(session, handle) ?: return null
    val dimensions = bytes.readJpegDimensions() ?: return null
    val longEdge = maxOf(dimensions.first, dimensions.second)
    return bytes.takeIf { longEdge >= MIN_PREVIEW_LONG_EDGE }
}

/** The camera's native profile for whatever RAF is already loaded. */
fun readRawProfile(session: PtpSession): ByteArray =
    session.readPropertyBytes(RAW_PROFILE_PROPERTY).also {
        if (it.isEmpty()) throw RawDevelopmentError("The camera returned an empty RAW profile.")
    }

private fun requireRawCapabilities(session: PtpSession) {
    val operations = listOf(
        Operation.FUJI_SEND_OBJECT_INFO,
        Operation.FUJI_SEND_OBJECT,
        Operation.GET_OBJECT_HANDLES,
        Operation.GET_OBJECT,
        Operation.DELETE_OBJECT,
        Operation.GET_DEVICE_PROP_VALUE,
        Operation.SET_DEVICE_PROP_VALUE,
    )
    if (operations.any { !session.supportsOperation(it) }) {
        throw RawDevelopmentError(
            "The camera does not advertise the operations needed for USB RAW conversion.",
        )
    }
    if (!session.supportsProperty(RAW_PROFILE_PROPERTY) ||
        !session.supportsProperty(RAW_CONVERSION_TRIGGER_PROPERTY)
    ) {
        throw RawDevelopmentError("The camera does not advertise its RAW conversion properties.")
    }
}

private fun conversionTrigger(cameraModel: String): Int =
    when (CameraModels.normaliseModel(cameraModel)) {
        // FilmKit's conversion captures and native-profile adapter are verified on X100VI.
        "X100VI" -> 0
        // Initial X-T50 hardware acceptance value: both FilmKit and libfuji use 0. The
        // returned JPEG dimensions are surfaced so this can be checked against rawji's
        // newer claim that 1 selects full resolution.
        "XT50" -> 0
        else -> throw RawDevelopmentError(
            "The full-resolution conversion trigger has not been verified for $cameraModel.",
        )
    }

private fun waitForOneNewHandle(
    session: PtpSession,
    before: Set<Int>,
    timeoutMs: Long,
): Int {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    while (System.nanoTime() < deadline) {
        val handles = try {
            session.getObjectHandles(PtpObject.ALL_STORAGES)
        } catch (error: PtpError) {
            if (error.code == ResponseCode.DEVICE_BUSY) {
                Thread.sleep(100)
                continue
            }
            throw error
        }
        val created = handles.filterNot(before::contains)
        if (created.size == 1) return created.single()
        if (created.size > 1) {
            throw RawDevelopmentError(
                "The camera produced several new objects; none were deleted because the result " +
                    "could not be identified safely.",
            )
        }
        Thread.sleep(100)
    }
    throw RawDevelopmentError("The camera did not finish RAW conversion within ${timeoutMs / 1000}s.")
}

/** Below this, a thumbnail is a contact sheet rather than a preview worth judging. */
private const val MIN_PREVIEW_LONG_EDGE = 640

private fun File.hasJpegSignature(): Boolean = inputStream().use { input ->
    input.read() == 0xff && input.read() == 0xd8 && input.read() == 0xff
}

private fun File.readJpegDimensions(): Pair<Int, Int>? =
    inputStream().buffered().use { it.readJpegDimensions() }

private fun ByteArray.readJpegDimensions(): Pair<Int, Int>? =
    inputStream().buffered().use { it.readJpegDimensions() }

private fun java.io.InputStream.readJpegDimensions(): Pair<Int, Int>? = runCatching {
    DataInputStream(this).use { input ->
        if (input.readUnsignedShort() != 0xffd8) return@use null
        while (true) {
            var prefix = input.readUnsignedByte()
            while (prefix != 0xff) prefix = input.readUnsignedByte()
            var marker = input.readUnsignedByte()
            while (marker == 0xff) marker = input.readUnsignedByte()
            if (marker == 0xd9 || marker == 0xda) return@use null
            if (marker == 0x01 || marker in 0xd0..0xd8) continue
            val length = input.readUnsignedShort()
            if (length < 2) return@use null
            if (marker in JPEG_START_OF_FRAME_MARKERS) {
                if (length < 7) return@use null
                input.readUnsignedByte()
                val height = input.readUnsignedShort()
                val width = input.readUnsignedShort()
                return@use width to height
            }
            var remaining = length - 2
            while (remaining > 0) {
                val skipped = input.skipBytes(remaining)
                if (skipped <= 0) return@use null
                remaining -= skipped
            }
        }
        @Suppress("UNREACHABLE_CODE")
        null
    }
}.getOrNull()

private val JPEG_START_OF_FRAME_MARKERS =
    (0xc0..0xcf).filterNot { it == 0xc4 || it == 0xc8 || it == 0xcc }.toSet()
