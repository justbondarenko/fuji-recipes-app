package dev.bondarenko.fujirecipes.camera.ptp

/**
 * The command / data / response machinery, over an abstract pair of bulk endpoints.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/transport.ts` at commit `0c17106`
 * (`coding-standards.md` P3), split differently: the reference's `PtpTransport` owns both the
 * WebUSB device *and* the phase logic. Here the two are separated at [BulkChannel], because
 * the phase logic — transaction ids, multi-read reassembly, stale-reply rejection — is where
 * the bugs live, and separating it means all of it is tested on the JVM against a fake camera
 * rather than only on a phone with the camera in its only USB port (`architecture.md` C1).
 *
 * Pure: no `android.*` (P4). The Android half is `camera/usb/UsbBulkChannel.kt`.
 *
 * **Blocking, not suspending.** `UsbDeviceConnection.bulkTransfer` is a blocking call with its
 * own timeout, so wrapping this in `suspend` would add colour without adding a single
 * suspension point. Callers run it on `Dispatchers.IO`.
 */

/** One transfer's worth of bytes in each direction. Implemented by USB, and by the fake. */
interface BulkChannel {
    /** Writes every byte, or throws. */
    fun write(bytes: ByteArray)

    /** Reads up to [maxBytes]; an empty array means the device sent nothing. */
    fun read(maxBytes: Int): ByteArray

    /** Releases whatever was claimed. Never throws — it runs on failing paths. */
    fun close()

    /** What to call the device when the camera has not named itself yet. */
    val productName: String
}

data class CommandResult(val code: Int, val params: List<Int>, val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is CommandResult &&
                    code == other.code &&
                    params == other.params &&
                    data.contentEquals(other.data)
                )

    override fun hashCode(): Int =
        31 * (31 * code + params.hashCode()) + data.contentHashCode()
}

/**
 * The camera did not answer in time.
 *
 * Distinct from [PtpFramingError] because the remedy is different and P5 requires the two not
 * to render the same: a timeout means check the cable and that the body is awake; a framing
 * error means the pipe is out of step and the session needs resetting.
 */
class PtpTimeoutError(val timeoutMs: Int) : Exception(
    "The camera did not answer within ${timeoutMs}ms. Check the cable and that the camera " +
        "is still awake.",
)

const val DEFAULT_TIMEOUT_MS = 5_000

const val READ_CHUNK_BYTES = 64 * 1024

/** A container beyond this is a parse that has gone wrong, not a camera with a lot to say. */
const val MAX_CONTAINER_BYTES = 8L * 1024 * 1024

class PtpTransport(private val channel: BulkChannel) {

    private var transaction = 0

    val productName: String get() = channel.productName

    /**
     * Transaction ids start at 1 and never repeat within a session.
     *
     * `0xFFFFFFFF` is reserved by ISO 15740, so the counter wraps back to 1 rather than
     * through it. At a handful of properties per write this will never be reached; it is here
     * because a counter that silently produces a reserved value is the kind of bug that only
     * shows up on a long session.
     */
    private fun nextTransaction(): Int {
        transaction = if (transaction == 0xfffffffe.toInt()) 1 else transaction + 1
        return transaction
    }

    /**
     * A command with no outgoing payload: the camera may answer with a data container and
     * then a response, or with a response alone.
     */
    fun command(operation: Int, params: List<Int> = emptyList()): CommandResult {
        val transactionId = nextTransaction()

        send(packContainer(ContainerType.COMMAND, operation, transactionId, params))

        var container = receive(transactionId)
        var data = ByteArray(0)

        if (container.type == ContainerType.DATA) {
            data = container.data
            container = receive(transactionId)
        }

        requireResponse(container)
        return CommandResult(container.code, container.params, data)
    }

    /**
     * A command that sends a payload — `SetDevicePropValue`, and nothing else in this app.
     * Command container, then data container, then the response.
     */
    fun commandWithData(operation: Int, params: List<Int>, data: ByteArray): CommandResult {
        val transactionId = nextTransaction()

        send(packContainer(ContainerType.COMMAND, operation, transactionId, params))
        send(packContainer(ContainerType.DATA, operation, transactionId, data = data))

        val container = receive(transactionId)
        requireResponse(container)
        return CommandResult(container.code, container.params, ByteArray(0))
    }

    fun close() = channel.close()

    private fun requireResponse(container: PtpContainer) {
        if (container.type != ContainerType.RESPONSE) {
            throw PtpFramingError(
                "Expected a response container, got type 0x${container.type.toString(16)}.",
            )
        }
    }

    // Every container this app sends is well under one bulk packet, so a payload that is an
    // exact multiple of the endpoint's maximum packet size — the case that needs a
    // zero-length packet to terminate it — cannot arise. If a future operation sends a large
    // payload, that is the thing to check first.
    private fun send(bytes: ByteArray) = channel.write(bytes)

    /**
     * Reads one container, reassembling it across as many bulk reads as it takes.
     *
     * **The transaction id is checked**, and that is not pedantry. A read that timed out
     * leaves its answer queued, so the next read can return the *late* answer to the
     * *previous* request. Without this check that answer is silently accepted as the current
     * one, and a property read returns another property's value. Rejecting it is recoverable;
     * believing it is not.
     */
    private fun receive(expectedTransactionId: Int): PtpContainer {
        var bytes = channel.read(READ_CHUNK_BYTES)

        val declared = containerLength(bytes)
        if (declared > MAX_CONTAINER_BYTES) {
            throw PtpFramingError(
                "The camera declared a $declared-byte container, beyond the " +
                    "$MAX_CONTAINER_BYTES-byte limit.",
            )
        }

        while (bytes.size < declared) {
            val more = channel.read(READ_CHUNK_BYTES)
            if (more.isEmpty()) {
                throw PtpFramingError(
                    "The camera stopped after ${bytes.size} of $declared bytes.",
                )
            }
            bytes += more
        }

        val container = unpackContainer(bytes)

        // An event container is unsolicited and belongs to no transaction; the camera can
        // send one at any time and it is not the answer to anything.
        if (container.type == ContainerType.EVENT) {
            throw PtpFramingError(
                "The camera sent an event (0x${container.code.toString(16)}) where a reply " +
                    "was expected.",
            )
        }

        if (container.transactionId != expectedTransactionId) {
            throw PtpFramingError(
                "Reply is for transaction ${container.transactionId}, not " +
                    "$expectedTransactionId — a late answer to an earlier request. The " +
                    "session needs resetting.",
            )
        }

        return container
    }
}
