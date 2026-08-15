package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.camera.ptp.BulkChannel
import dev.bondarenko.fujirecipes.camera.ptp.ContainerType
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.ptp.packContainer
import dev.bondarenko.fujirecipes.camera.ptp.packPtpString
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import dev.bondarenko.fujirecipes.camera.ptp.unpackContainer
import java.io.ByteArrayOutputStream

/**
 * A camera that speaks PTP back, over the same [BulkChannel] the USB transport implements.
 *
 * Modelled on `fuji-recipes-book/tests/support/fake-camera.ts` @ `0c17106`.
 *
 * This is what makes the session and the write executor testable at all. `architecture.md` C1
 * says the phone's only USB port is occupied by the camera during every test that matters, so
 * a transport whose logic can only be exercised with hardware attached is a transport nobody
 * exercises. Everything above the bulk endpoints — transaction ids, multi-read reassembly,
 * stale replies, refusals, disconnection mid-write — is driven from here.
 */
class FakeCamera(
    var model: String = "X100VI",
    private val propertyCodes: List<Int> = listOf(0xd18c, 0xd18d, 0xd18e),
) : BulkChannel {

    override val productName: String = "Fujifilm camera (fake)"

    /** Response code per operation, when it should not be OK. */
    val refuseOperation: MutableMap<Int, Int> = mutableMapOf()

    /** Response code per property code on `SetDevicePropValue`, when it should not be OK. */
    val refuseProperty: MutableMap<Int, Int> = mutableMapOf()

    /** Values `GetDevicePropValue` answers with. */
    val propertyValues: MutableMap<Int, ByteArray> = mutableMapOf()

    /** Datasets `GetDevicePropDesc` answers with, keyed by the code that was asked about. */
    val describeAnswers: MutableMap<Int, ByteArray> = mutableMapOf()

    /** A camera that has stopped answering: every read times out. */
    var silent: Boolean = false

    /** Every property write, in the order the camera received it. */
    val writes: MutableList<Write> = mutableListOf()

    /** Splits every reply across this many bytes per read, to exercise reassembly. */
    var chunkSize: Int = Int.MAX_VALUE

    /** After this many property writes, behave as though the cable was pulled. */
    var unplugAfterWrites: Int? = null

    /** Queued in front of the next real reply, to exercise the stale-reply check. */
    var staleReplyBeforeNextRead: ByteArray? = null

    var closed: Boolean = false
        private set

    data class Write(val property: Int, val payload: ByteArray) {
        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is Write && property == other.property && payload.contentEquals(other.payload))

        override fun hashCode(): Int = 31 * property + payload.contentHashCode()
    }

    private val outgoing = ArrayDeque<ByteArray>()
    private var pendingSetProperty: Int? = null
    private var unplugged = false

    // ─── BulkChannel ────────────────────────────────────────────────────────

    override fun write(bytes: ByteArray) {
        if (unplugged) throw PtpTimeoutError(5_000)

        val container = unpackContainer(bytes)
        when (container.type) {
            ContainerType.COMMAND -> onCommand(container.code, container.transactionId, container.params)
            ContainerType.DATA -> onData(container.data)
            else -> error("The fake camera was sent a container of type ${container.type}")
        }
    }

    override fun read(maxBytes: Int): ByteArray {
        if (unplugged) throw PtpTimeoutError(5_000)

        staleReplyBeforeNextRead?.let {
            staleReplyBeforeNextRead = null
            return it
        }

        // An empty queue is a camera that has nothing to say, which on the wire is a read
        // that never completes.
        val next = outgoing.removeFirstOrNull() ?: throw PtpTimeoutError(5_000)

        if (next.size <= chunkSize) return next

        outgoing.addFirst(next.copyOfRange(chunkSize, next.size))
        return next.copyOfRange(0, chunkSize)
    }

    override fun close() {
        closed = true
    }

    // ─── The camera's side ──────────────────────────────────────────────────

    private fun onCommand(operation: Int, transactionId: Int, params: List<Int>) {
        if (silent) return

        refuseOperation[operation]?.let { code ->
            reply(code, transactionId)
            return
        }

        when (operation) {
            Operation.OPEN_SESSION, Operation.CLOSE_SESSION -> reply(ResponseCode.OK, transactionId)

            Operation.GET_DEVICE_INFO -> {
                data(operation, transactionId, deviceInfoDataset())
                reply(ResponseCode.OK, transactionId)
            }

            Operation.GET_DEVICE_PROP_VALUE -> {
                val value = propertyValues[params.firstOrNull() ?: 0]
                if (value == null) {
                    reply(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, transactionId)
                } else {
                    data(operation, transactionId, value)
                    reply(ResponseCode.OK, transactionId)
                }
            }

            Operation.GET_DEVICE_PROP_DESC -> {
                val dataset = describeAnswers[params.firstOrNull() ?: 0]
                if (dataset == null) {
                    reply(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, transactionId)
                } else {
                    data(operation, transactionId, dataset)
                    reply(ResponseCode.OK, transactionId)
                }
            }

            Operation.SET_DEVICE_PROP_VALUE -> {
                val code = params.firstOrNull() ?: 0
                pendingSetProperty = code
                reply(refuseProperty[code] ?: ResponseCode.OK, transactionId)
            }

            else -> reply(ResponseCode.OPERATION_NOT_SUPPORTED, transactionId)
        }
    }

    private fun onData(payload: ByteArray) {
        val code = pendingSetProperty ?: error("The fake camera got a data phase it did not expect")
        pendingSetProperty = null
        writes += Write(code, payload)

        unplugAfterWrites?.let { limit -> if (writes.size >= limit) unplugged = true }
    }

    private fun reply(code: Int, transactionId: Int) {
        outgoing += packContainer(ContainerType.RESPONSE, code, transactionId)
    }

    private fun data(operation: Int, transactionId: Int, payload: ByteArray) {
        outgoing += packContainer(ContainerType.DATA, operation, transactionId, data = payload)
    }

    /** A response left on the wire from an earlier, timed-out request. */
    fun stageStaleReply(transactionId: Int) {
        staleReplyBeforeNextRead =
            packContainer(ContainerType.RESPONSE, ResponseCode.OK, transactionId)
    }

    private fun deviceInfoDataset(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(packU16(100))
        out.write(packU32(6))
        out.write(packU16(100))
        out.write(packPtpString("fujifilm.co.jp: 1.0;"))
        out.write(packU16(0))

        fun u16Array(values: List<Int>) {
            out.write(packU32(values.size))
            values.forEach { out.write(packU16(it)) }
        }

        u16Array(
            listOf(
                Operation.GET_DEVICE_INFO,
                Operation.OPEN_SESSION,
                Operation.CLOSE_SESSION,
                Operation.GET_DEVICE_PROP_DESC,
                Operation.GET_DEVICE_PROP_VALUE,
                Operation.SET_DEVICE_PROP_VALUE,
            ),
        )
        u16Array(emptyList())
        u16Array(propertyCodes)
        u16Array(emptyList())
        u16Array(listOf(0x3801))

        out.write(packPtpString("FUJIFILM"))
        out.write(packPtpString(model))
        out.write(packPtpString("1.20"))
        out.write(packPtpString("A1B2C3D4"))

        return out.toByteArray()
    }
}
