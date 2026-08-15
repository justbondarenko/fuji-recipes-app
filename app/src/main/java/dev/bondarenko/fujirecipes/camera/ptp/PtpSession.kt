package dev.bondarenko.fujirecipes.camera.ptp

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
}
