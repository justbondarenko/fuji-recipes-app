package dev.bondarenko.fujirecipes.camera.ptp

import java.io.OutputStream
import java.io.InputStream

/**
 * A PTP session over one transport.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/session.ts` at commit `0c17106`
 * (`coding-standards.md` P3), minus `dumpProperties` — that is the diagnostic the sibling's
 * assumption-diff page is built on, and nothing in FEAT-005 or FEAT-006 calls it.
 *
 * Owns the session lifetime and the device info. [open] does the whole sequence a caller
 * wants — open the session, read the device info — because each is useless without the other
 * and the model is needed the moment the connection exists.
 *
 * Pure: no `android.*` (P4). Everything below is exercised on the JVM against `FakeCamera`.
 */

/** Any non-zero id will do; PTP reserves 0. */
const val DEFAULT_SESSION_ID = 1

class PtpSession(
    private val transport: PtpTransport,
    private val sessionId: Int = DEFAULT_SESSION_ID,
) {

    /** The last device info read, kept after [close] so a message can still name the body. */
    var deviceInfo: DeviceInfo? = null
        private set

    var isOpen: Boolean = false
        private set

    /** The camera's own model string when there is one, the USB product name otherwise. */
    val productName: String
        get() = deviceInfo?.model?.trim().orEmpty().ifEmpty { transport.productName }

    /**
     * Opens a session and reads the device info.
     *
     * The session comes before `GetDeviceInfo`, which PTP permits outside one: every property
     * this app reads or writes needs a session, so opening first means a body that refuses to
     * open one fails on that, rather than after a successful read that suggests everything is
     * fine.
     */
    fun open(): DeviceInfo {
        try {
            val result = transport.command(Operation.OPEN_SESSION, listOf(sessionId))

            // `SessionAlreadyOpen` is a success: a previous run of the app, or a restart while
            // the cable stayed in, leaves the camera's side open. Treating it as a failure
            // would make the app unusable until the cable was pulled.
            if (result.code != ResponseCode.OK &&
                result.code != ResponseCode.SESSION_ALREADY_OPEN
            ) {
                throw PtpError(result.code, Operation.OPEN_SESSION)
            }

            isOpen = true
            return readDeviceInfo().also { deviceInfo = it }
        } catch (error: Exception) {
            // Release the interface, but do not try to close the session: whatever just failed
            // has left the pipe in an unknown state, and a `CloseSession` that also times out
            // would add five seconds to a failure the user is already waiting on.
            isOpen = false
            transport.close()
            throw error
        }
    }

    /** Closes the session and releases the device. Never throws. */
    fun close() {
        if (isOpen) {
            runCatching { transport.command(Operation.CLOSE_SESSION) }
            // An unplugged camera cannot be told the session is over. The release below is the
            // part that matters to the next connection.
        }

        isOpen = false
        transport.close()
    }

    private fun readDeviceInfo(): DeviceInfo {
        val result = transport.command(Operation.GET_DEVICE_INFO)
        expectOk(Operation.GET_DEVICE_INFO, result.code)
        return parseDeviceInfo(result.data)
    }

    /** Whether the device's own list contains an operation code. */
    fun supportsOperation(operation: Int): Boolean =
        deviceInfo?.operationsSupported?.contains(operation) ?: false

    /** Whether the device's own list contains a property code. */
    fun supportsProperty(code: Int): Boolean =
        deviceInfo?.devicePropertiesSupported?.contains(code) ?: false

    /**
     * Describes one property.
     *
     * The returned code is checked against the one asked for. Same reasoning as the
     * transport's transaction-id check: a description that belongs to another property would
     * be read as this one's allowed values, and every later decision — writable, range,
     * enumeration — would be made about the wrong setting.
     */
    fun describeProperty(code: Int): DevicePropertyDescription {
        val result = transport.command(Operation.GET_DEVICE_PROP_DESC, listOf(code))
        expectOk(Operation.GET_DEVICE_PROP_DESC, result.code)

        val description = parseDevicePropertyDescription(result.data)
        if (description.code != code) {
            throw PtpFramingError(
                "Asked about property ${propertyHex(code)}, the camera described " +
                    propertyHex(description.code) + ".",
            )
        }

        return description
    }

    /**
     * Reads one property's raw value.
     *
     * Bytes, not a number: the width and signedness come from the caller's expectation, and on
     * a body that refuses `GetDevicePropDesc` there is nothing else to go on.
     */
    fun readPropertyBytes(code: Int): ByteArray {
        val result = transport.command(Operation.GET_DEVICE_PROP_VALUE, listOf(code))
        expectOk(Operation.GET_DEVICE_PROP_VALUE, result.code)
        return result.data
    }

    /**
     * Writes one property's raw value.
     *
     * Bytes in, for the same reason [readPropertyBytes] gives bytes out: the width and
     * signedness are the plan's decision, made in `camera/ptp/StepPayload.kt` from the field's
     * own packing.
     *
     * Lets [PtpError] out untouched. It carries the response code, and the write executor
     * needs exactly that to say *which* property the camera refused and what it said — P5,
     * and the difference between a warning about one setting and abandoning the write.
     */
    fun setPropertyBytes(code: Int, value: ByteArray) {
        val result = transport.commandWithData(
            Operation.SET_DEVICE_PROP_VALUE,
            listOf(code),
            value,
        )
        expectOk(Operation.SET_DEVICE_PROP_VALUE, result.code)
    }

    // ─── Objects ────────────────────────────────────────────────────────────
    //
    // Settings backup and ordinary card browsing. Backup operations retain raw bytes because
    // the layout is the caller's knowledge, and
    // `PtpFujiObjectInfo` is *not* the ISO 15740 `ObjectInfo` — Fuji moves fields after
    // `protection`, so a parse written against one silently produces wrong numbers for the
    // other. `camera/plan/CameraBackup.kt` reads only the leading fields the two layouts
    // agree on, and treats the payload's own length as the truth about size.

    /**
     * One object's `ObjectInfo` dataset, raw.
     *
     * `libfuji` calls this before `GetObject` on the backup path and does nothing with the
     * result. Kept because the order is what was observed working, and because its refusal is
     * the one clean signal that a body has no backup object to give — which is a different
     * message to the user than a transfer that broke.
     */
    fun getObjectInfo(handle: Int): ByteArray {
        val result = transport.command(Operation.GET_OBJECT_INFO, listOf(handle))
        expectOk(Operation.GET_OBJECT_INFO, result.code)
        return result.data
    }

    fun getStorageIds(): List<Int> {
        val result = transport.command(Operation.GET_STORAGE_IDS)
        expectOk(Operation.GET_STORAGE_IDS, result.code)
        return parseU32Array(result.data, "The storage ID dataset")
    }

    fun getObjectHandles(
        storageId: Int = PtpObject.ALL_STORAGES,
        format: Int = PtpObject.ALL_FORMATS,
        parent: Int = PtpObject.ROOT,
    ): List<Int> {
        val result = transport.command(
            Operation.GET_OBJECT_HANDLES,
            listOf(storageId, format, parent),
        )
        expectOk(Operation.GET_OBJECT_HANDLES, result.code)
        return parseU32Array(result.data, "The object handle dataset")
    }

    fun getThumb(handle: Int): ByteArray {
        val result = transport.command(Operation.GET_THUMB, listOf(handle))
        expectOk(Operation.GET_THUMB, result.code)
        return result.data
    }

    fun deleteObject(handle: Int) {
        val result = transport.command(Operation.DELETE_OBJECT, listOf(handle, 0))
        expectOk(Operation.DELETE_OBJECT, result.code)
    }

    /** One object's contents. */
    fun getObject(handle: Int): ByteArray {
        val result = transport.command(Operation.GET_OBJECT, listOf(handle))
        expectOk(Operation.GET_OBJECT, result.code)
        return result.data
    }

    /**
     * Streams an object without retaining the complete image in the protocol layer.
     *
     * Throws [PtpTransferCancelled] if [isCancelled] turned true part-way. The session is still
     * usable afterwards — the remainder of the object was drained rather than abandoned.
     */
    fun getObject(
        handle: Int,
        output: OutputStream,
        maxBytes: Long,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): Long {
        val result = transport.commandTo(
            Operation.GET_OBJECT,
            listOf(handle),
            output,
            maxBytes,
            onProgress,
            isCancelled,
        )
        expectOk(Operation.GET_OBJECT, result.code)
        if (result.cancelled) throw PtpTransferCancelled(handle)
        return result.bytesWritten
    }

    /**
     * Announces an object about to be sent.
     *
     * Both parameters are zero on the backup path — storage and parent handle — which is what
     * the captures show and what `libfuji` sends.
     */
    fun sendObjectInfo(dataset: ByteArray) {
        val result = transport.commandWithData(
            Operation.SEND_OBJECT_INFO,
            listOf(0, 0),
            dataset,
        )
        expectOk(Operation.SEND_OBJECT_INFO, result.code)
    }

    /** The bytes of the object announced by the preceding [sendObjectInfo]. */
    fun sendObject(bytes: ByteArray) {
        val result = transport.commandWithData(Operation.SEND_OBJECT, emptyList(), bytes)
        expectOk(Operation.SEND_OBJECT, result.code)
    }

    /** Announces a RAF to Fuji's RAW-conversion service. */
    fun sendFujiRawObjectInfo(dataset: ByteArray) {
        val result = transport.commandWithData(
            Operation.FUJI_SEND_OBJECT_INFO,
            listOf(0, 0, 0),
            dataset,
        )
        expectOk(Operation.FUJI_SEND_OBJECT_INFO, result.code)
    }

    /** Streams the RAF bytes through Fuji's vendor SendObject2 operation. */
    fun sendFujiRawObject(
        input: InputStream,
        length: Long,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        val result = transport.commandWithDataFrom(
            Operation.FUJI_SEND_OBJECT,
            emptyList(),
            input,
            length,
            onProgress,
        )
        expectOk(Operation.FUJI_SEND_OBJECT, result.code)
    }
}
