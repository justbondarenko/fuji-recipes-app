package dev.bondarenko.fujirecipes.camera.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import dev.bondarenko.fujirecipes.camera.ptp.BulkChannel
import dev.bondarenko.fujirecipes.camera.ptp.DEFAULT_TIMEOUT_MS
import dev.bondarenko.fujirecipes.camera.ptp.PtpFramingError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import java.io.InputStream

/**
 * The USB half of the transport: everything that touches hardware, and nothing else.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/transport.ts` at commit `0c17106`
 * (`coding-standards.md` P3), with WebUSB swapped for `android.hardware.usb`. The
 * command/data/response phases are *not* here — they are pure and live in
 * `camera/ptp/PtpTransport.kt`, so they can be tested without a camera.
 *
 * Two behaviours are carried over deliberately, because the reference paid for both with real
 * failures rather than reasoning:
 *
 * 1. **Interfaces are ranked and tried in turn, not indexed.** Claiming interface 0 blindly
 *    fails on a body whose operating-system photo service already holds it, while the PTP
 *    interface sits free beside it.
 * 2. **Endpoint addresses are discovered.** They vary between bodies, and hard-coding them is
 *    how this breaks on the second camera anyone tries.
 */

/** Fujifilm's USB vendor id. `res/xml/device_filter.xml` carries its decimal form, 1227. */
const val FUJI_VENDOR_ID = 0x04cb

/**
 * Bodies the protocol reference has seen, for naming a device before its session is open.
 *
 * A fallback only — `UsbDevice.productName` is usually present, and the camera's own model
 * string replaces both the moment `GetDeviceInfo` returns.
 */
val KNOWN_PRODUCT_IDS: Map<Int, String> = mapOf(
    0x02e3 to "X-T30",
    0x02e5 to "X100V",
    0x02e7 to "X-T4",
    0x0305 to "X100VI",
)

/** A still camera's PTP interface: still-image class, still-image capture protocol. */
const val STILL_IMAGE_CLASS = UsbConstants.USB_CLASS_STILL_IMAGE
const val STILL_IMAGE_SUBCLASS = 0x01
const val STILL_IMAGE_PROTOCOL = 0x01

/**
 * Why a connection attempt did not produce a camera.
 *
 * Separate from the message because the UI branches on it, and because P5 forbids a busy
 * interface and a wrong USB mode rendering as the same failure — they have different
 * remedies.
 */
enum class ConnectFailure {
    NO_USB_HOST,
    NO_DEVICE,
    PERMISSION_DENIED,
    DEVICE_BUSY,
    NO_BULK_ENDPOINTS,
    OTHER,
}

class UsbConnectError(
    val failure: ConnectFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class UsbBulkChannel private constructor(
    private val connection: UsbDeviceConnection,
    private val claimed: UsbInterface,
    private val endpointIn: UsbEndpoint,
    private val endpointOut: UsbEndpoint,
    override val productName: String,
    private val timeoutMs: Int,
) : BulkChannel {

    override fun write(bytes: ByteArray) {
        writeRaw(bytes, bytes.size)
        terminateIfExactMultiple(bytes.size.toLong())
    }

    private fun writeRaw(bytes: ByteArray, count: Int) {
        var sent = 0
        while (sent < count) {
            val moved = connection.bulkTransfer(
                endpointOut,
                bytes,
                sent,
                count - sent,
                timeoutMs,
            )
            // Android reports a timeout and a transport error identically, as a negative
            // return. The timeout is by far the likelier of the two with a camera on the
            // other end, and it is the one with a remedy the user can act on.
            if (moved < 0) throw PtpTimeoutError(timeoutMs)
            if (moved == 0) throw PtpFramingError("The USB write moved no bytes.")
            sent += moved
        }
    }

    override fun writeStream(
        prefix: ByteArray,
        input: InputStream,
        payloadLength: Long,
        onProgress: (written: Long, total: Long) -> Unit,
    ) {
        require(payloadLength >= 0)
        val packet = endpointOut.maxPacketSize.coerceAtLeast(1)
        // The 511-packet transfer size mirrors X RAW Studio/libfuji captures while ensuring
        // every intermediate Android bulk transfer ends on a packet boundary.
        val buffer = ByteArray(packet * 511)
        prefix.copyInto(buffer)
        var buffered = prefix.size
        var payloadWritten = 0L

        while (payloadWritten < payloadLength) {
            val wanted = minOf(buffer.size - buffered.toLong(), payloadLength - payloadWritten).toInt()
            val read = input.read(buffer, buffered, wanted)
            if (read < 0) {
                throw PtpFramingError(
                    "The upload source stopped after $payloadWritten of $payloadLength bytes.",
                )
            }
            if (read == 0) continue
            buffered += read
            payloadWritten += read

            if (buffered == buffer.size) {
                writeRaw(buffer, buffered)
                buffered = 0
                onProgress(payloadWritten, payloadLength)
            }
        }

        if (buffered > 0) writeRaw(buffer, buffered)
        terminateIfExactMultiple(prefix.size + payloadLength)
        onProgress(payloadWritten, payloadLength)
    }

    /**
     * A zero-length packet after a transfer that exactly fills its last packet.
     *
     * Bulk USB ends a transfer with a short packet. A container whose total length is an exact
     * multiple of the endpoint's maximum packet size has no short packet to end it, so the
     * device keeps waiting for more and the transfer hangs until the timeout — and the session
     * is then out of step for good.
     *
     * Nothing in this app could reach the case until the settings backup: every other container
     * is a handful of bytes plus a 12-byte header. A restore sends tens of kilobytes of
     * whatever length the body's blob happens to be, so roughly one restore in
     * `maxPacketSize` would have hung. That is exactly the kind of failure that looks like a
     * flaky cable and is not.
     */
    private fun terminateIfExactMultiple(size: Long) {
        val packet = endpointOut.maxPacketSize
        if (packet <= 0 || size == 0L || size % packet != 0L) return

        // A failure here is not fatal on its own — the camera may already have taken the
        // transfer as complete — so it is not worth turning a working write into an error.
        connection.bulkTransfer(endpointOut, ByteArray(0), 0, 0, timeoutMs)
    }

    override fun read(maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes)
        val moved = connection.bulkTransfer(endpointIn, buffer, 0, maxBytes, timeoutMs)

        if (moved < 0) throw PtpTimeoutError(timeoutMs)
        return buffer.copyOf(moved)
    }

    /** Never throws: this runs on paths that are already failing. */
    override fun close() {
        runCatching { connection.releaseInterface(claimed) }
        runCatching { connection.close() }
    }

    companion object {

        /** The attached Fujifilm body, or null. */
        fun findCamera(manager: UsbManager): UsbDevice? =
            manager.deviceList.values.firstOrNull { it.vendorId == FUJI_VENDOR_ID }

        fun nameFor(device: UsbDevice): String =
            device.productName
                ?: KNOWN_PRODUCT_IDS[device.productId]
                ?: "Fujifilm camera (0x${device.productId.toString(16).padStart(4, '0')})"

        /**
         * Opens the device and claims a PTP interface.
         *
         * @throws UsbConnectError with a [ConnectFailure] the UI can branch on.
         */
        fun open(
            manager: UsbManager,
            device: UsbDevice,
            timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        ): UsbBulkChannel {
            val connection = manager.openDevice(device)
                ?: throw UsbConnectError(
                    ConnectFailure.PERMISSION_DENIED,
                    "The camera could not be opened. This usually means USB permission was " +
                        "not granted.",
                )

            val candidates = candidateInterfaces(device)
            if (candidates.isEmpty()) {
                connection.close()
                throw UsbConnectError(
                    ConnectFailure.NO_BULK_ENDPOINTS,
                    "The camera exposed no interface with bulk endpoints. It is probably in " +
                        "the wrong USB mode — set it to the RAW-conversion/backup mode.",
                )
            }

            return claimOneOf(connection, device, candidates, timeoutMs)
        }

        /**
         * Interfaces that could carry PTP, best first.
         *
         * A bulk pair is the hard requirement — the command, data and response phases all
         * need one — and the still-image class is the tie-break. Anything without both
         * directions is dropped rather than ranked last: claiming it could only fail later,
         * and it would fail with a timeout rather than a message.
         */
        private fun candidateInterfaces(device: UsbDevice): List<UsbInterface> =
            (0 until device.interfaceCount)
                .map(device::getInterface)
                .filter { bulkPair(it) != null }
                .sortedByDescending { isStillImage(it) }

        /**
         * Claims the first candidate that will have us.
         *
         * The first failure is kept and rethrown if every candidate refuses, because that is
         * the one worth showing: the reason the *most likely* interface gave, rather than the
         * reason a fallback gave.
         */
        private fun claimOneOf(
            connection: UsbDeviceConnection,
            device: UsbDevice,
            candidates: List<UsbInterface>,
            timeoutMs: Int,
        ): UsbBulkChannel {
            candidates.forEach { candidate ->
                // `force = true`: on some devices the platform's own MTP handler has the
                // interface, and taking it from that is exactly what is wanted here.
                if (connection.claimInterface(candidate, true)) {
                    val (input, output) = bulkPair(candidate)!!
                    return UsbBulkChannel(
                        connection = connection,
                        claimed = candidate,
                        endpointIn = input,
                        endpointOut = output,
                        productName = nameFor(device),
                        timeoutMs = timeoutMs,
                    )
                }
            }

            connection.close()
            throw UsbConnectError(
                ConnectFailure.DEVICE_BUSY,
                "The camera could not be claimed. Another app is probably holding it — a " +
                    "file-transfer or photo-import app that opened when the cable went in.",
            )
        }

        private fun isStillImage(candidate: UsbInterface): Boolean =
            candidate.interfaceClass == STILL_IMAGE_CLASS &&
                candidate.interfaceSubclass == STILL_IMAGE_SUBCLASS &&
                candidate.interfaceProtocol == STILL_IMAGE_PROTOCOL

        /** The interface's bulk in/out endpoints, or null when it lacks either. */
        private fun bulkPair(candidate: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {
            var input: UsbEndpoint? = null
            var output: UsbEndpoint? = null

            for (index in 0 until candidate.endpointCount) {
                val endpoint = candidate.getEndpoint(index)
                if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
                if (endpoint.direction == UsbConstants.USB_DIR_IN) input = input ?: endpoint
                if (endpoint.direction == UsbConstants.USB_DIR_OUT) output = output ?: endpoint
            }

            return if (input != null && output != null) input to output else null
        }
    }
}
