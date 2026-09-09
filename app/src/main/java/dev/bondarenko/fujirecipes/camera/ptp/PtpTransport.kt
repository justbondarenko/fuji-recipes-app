package dev.bondarenko.fujirecipes.camera.ptp

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.InputStream

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

    /** Writes [prefix] and exactly [payloadLength] bytes as one logical bulk transfer. */
    fun writeStream(
        prefix: ByteArray,
        input: InputStream,
        payloadLength: Long,
        onProgress: (written: Long, total: Long) -> Unit,
    )

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
 * [cancelled] means the caller asked to stop part-way and the rest of the payload was read and
 * discarded rather than written. The bytes still had to cross the wire: a data phase abandoned
 * half-read leaves the camera mid-container and the next command reading the tail of this one.
 * Draining costs the remainder of one object and keeps the session usable.
 */
data class StreamedCommandResult(
    val code: Int,
    val params: List<Int>,
    val bytesWritten: Long,
    val cancelled: Boolean = false,
)

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

/**
 * The user stopped a transfer that was under way.
 *
 * Not a [PtpFramingError]: nothing is out of step and the session needs no reset. It is thrown
 * rather than returned because no caller can do anything with half an object — every one of
 * them has to discard the partial file and stop.
 */
class PtpTransferCancelled(val handle: Int) : Exception(
    "The transfer of object 0x${handle.toString(16)} was cancelled.",
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
     * A command whose data phase is written directly to [output].
     *
     * Full-resolution JPEGs routinely exceed [MAX_CONTAINER_BYTES]. Buffering one inside the
     * protocol layer would briefly keep several copies alive (USB chunk, growing array and the
     * final image). This variant validates the same framing and transaction fields as [command]
     * but retains only the twelve-byte container header.
     *
     * [isCancelled] is polled between chunks. A cancel stops the *writing*, not the reading:
     * see [StreamedCommandResult.cancelled] for why the remainder is drained instead.
     */
    fun commandTo(
        operation: Int,
        params: List<Int> = emptyList(),
        output: OutputStream,
        maxDataBytes: Long,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): StreamedCommandResult {
        require(maxDataBytes >= 0) { "maxDataBytes must not be negative" }

        val transactionId = nextTransaction()
        send(packContainer(ContainerType.COMMAND, operation, transactionId, params))

        val first = readAtLeast(CONTAINER_HEADER_SIZE)
        val declared = containerLength(first)
        if (declared < CONTAINER_HEADER_SIZE) {
            throw PtpFramingError("The camera declared a $declared-byte container.")
        }

        val header = unpackContainer(first.copyOfRange(0, CONTAINER_HEADER_SIZE))
        validateReply(header, transactionId)

        if (header.type == ContainerType.RESPONSE) {
            if (declared > MAX_CONTAINER_BYTES) {
                throw PtpFramingError("The camera declared an oversized $declared-byte response.")
            }
            val responseBytes = ByteArrayOutputStream(declared.toInt())
            responseBytes.write(first)
            while (responseBytes.size().toLong() < declared) {
                val next = channel.read((declared - responseBytes.size()).toInt())
                if (next.isEmpty()) {
                    throw PtpFramingError(
                        "The camera stopped after ${responseBytes.size()} of $declared bytes.",
                    )
                }
                responseBytes.write(next)
            }
            if (responseBytes.size().toLong() != declared) {
                throw PtpFramingError("A response container carried unexpected trailing bytes.")
            }
            val response = unpackContainer(responseBytes.toByteArray())
            validateReply(response, transactionId)
            return StreamedCommandResult(response.code, response.params, 0)
        }
        if (header.type != ContainerType.DATA || header.code != operation) {
            throw PtpFramingError(
                "Expected data for operation 0x${operation.toString(16)}, got type " +
                    "0x${header.type.toString(16)} and code 0x${header.code.toString(16)}.",
            )
        }

        val total = declared - CONTAINER_HEADER_SIZE
        if (total > maxDataBytes) {
            throw PtpFramingError(
                "The camera declared a $total-byte payload, beyond the $maxDataBytes-byte limit.",
            )
        }

        var read = 0L
        var written = 0L
        var cancelled = false
        fun writePayload(bytes: ByteArray) {
            val remaining = total - read
            if (bytes.size.toLong() > remaining) {
                throw PtpFramingError("The camera sent bytes beyond its declared container length.")
            }
            read += bytes.size
            // Once cancelled the bytes are still read off the wire, but nothing downstream is
            // told about them: a progress bar that keeps climbing after Cancel is a lie.
            if (cancelled) return
            output.write(bytes)
            written += bytes.size
            onProgress(written, total)
        }

        cancelled = isCancelled()
        writePayload(first.copyOfRange(CONTAINER_HEADER_SIZE, first.size))
        while (read < total) {
            if (!cancelled && isCancelled()) cancelled = true
            val next = channel.read(minOf(READ_CHUNK_BYTES.toLong(), total - read).toInt())
            if (next.isEmpty()) {
                throw PtpFramingError("The camera stopped after $read of $total payload bytes.")
            }
            writePayload(next)
        }

        val response = receive(transactionId)
        requireResponse(response)
        return StreamedCommandResult(response.code, response.params, written, cancelled)
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

    /** A data-out command whose payload is too large to materialise as one byte array. */
    fun commandWithDataFrom(
        operation: Int,
        params: List<Int>,
        input: InputStream,
        length: Long,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
    ): CommandResult {
        val transactionId = nextTransaction()
        send(packContainer(ContainerType.COMMAND, operation, transactionId, params))
        channel.writeStream(
            prefix = packDataContainerHeader(operation, transactionId, length),
            input = input,
            payloadLength = length,
            onProgress = onProgress,
        )

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
        val first = readAtLeast(4)

        val declared = containerLength(first)
        if (declared < CONTAINER_HEADER_SIZE || declared > MAX_CONTAINER_BYTES) {
            throw PtpFramingError(
                "The camera declared a $declared-byte container, beyond the " +
                    "$CONTAINER_HEADER_SIZE..$MAX_CONTAINER_BYTES-byte range.",
            )
        }
        if (first.size.toLong() > declared) {
            throw PtpFramingError("The camera sent bytes beyond its declared container length.")
        }

        val bytes = ByteArrayOutputStream(declared.toInt())
        bytes.write(first)
        while (bytes.size().toLong() < declared) {
            val more = channel.read(READ_CHUNK_BYTES)
            if (more.isEmpty()) {
                throw PtpFramingError(
                    "The camera stopped after ${bytes.size()} of $declared bytes.",
                )
            }
            bytes.write(more)
        }

        val container = unpackContainer(bytes.toByteArray())

        validateReply(container, expectedTransactionId)
        return container
    }

    private fun readAtLeast(count: Int): ByteArray {
        val bytes = ByteArrayOutputStream(count)
        while (bytes.size() < count) {
            val next = channel.read(READ_CHUNK_BYTES)
            if (next.isEmpty()) {
                throw PtpFramingError(
                    "The camera stopped after ${bytes.size()} bytes; $count were needed.",
                )
            }
            bytes.write(next)
        }
        return bytes.toByteArray()
    }

    private fun validateReply(container: PtpContainer, expectedTransactionId: Int) {

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
    }
}
