package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.camera.ptp.BulkChannel
import dev.bondarenko.fujirecipes.camera.ptp.ContainerType
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import dev.bondarenko.fujirecipes.camera.plan.PRESET_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.ptp.packContainer
import dev.bondarenko.fujirecipes.camera.ptp.packPtpString
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import dev.bondarenko.fujirecipes.camera.ptp.unpackContainer
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import java.io.ByteArrayOutputStream
import java.io.InputStream

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

    /**
     * Extra codes for `GetDeviceInfo` to advertise, on top of the Fuji ones.
     *
     * What a body advertises is how card-reader mode is identified, so a test needs to be able
     * to dress this one up as an MTP device rather than a camera.
     */
    val extraOperations: MutableList<Int> = mutableListOf()

    val extraProperties: MutableList<Int> = mutableListOf()

    /** A camera that has stopped answering: every read times out. */
    var silent: Boolean = false

    /**
     * Whether a written property reads back as what was written.
     *
     * True is the ordinary body. False is the one that takes a value and will not describe or
     * return it — which some bodies do for this whole property block, and which the executor
     * must record as *unverified* rather than as a failure.
     */
    var echoWrites: Boolean = true

    /** Properties whose read-back answers something else, for the mismatch path. */
    val readBackAs: MutableMap<Int, ByteArray> = mutableMapOf()

    /**
     * What each custom slot holds, keyed C1..C7. A body switches its name register when the
     * slot selector changes, and the slot reader depends on exactly that.
     */
    val slotNames: MutableMap<Int, String?> = mutableMapOf()

    /** Slots whose name the body will not report — the "unreadable", not "unnamed", case. */
    val refuseNameForSlots: MutableSet<Int> = mutableSetOf()

    /**
     * What each slot's settings properties hold, keyed slot → property code → raw value.
     *
     * A real body switches its whole property block with the selector, not just the name, and
     * the importer depends on that: read the wrong slot's registers and every recipe comes back
     * as a copy of C1.
     */
    val slotSettings: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()

    /** Property codes the body will not report at all, whichever slot is selected. */
    val refuseProperties: MutableSet<Int> = mutableSetOf()

    /** Every property read, in order — so a test can prove a shared property is read once. */
    val reads: MutableList<Int> = mutableListOf()

    /** The slot the selector currently points at. */
    var selectedSlot: Int? = null
        private set

    /** Every property write, in the order the camera received it. */
    val writes: MutableList<Write> = mutableListOf()

    /** Test hook for behavior caused by a property write, such as a completed RAW render. */
    var afterPropertyWrite: ((property: Int, payload: ByteArray) -> Unit)? = null

    // ─── Objects (the settings backup) ──────────────────────────────────────

    /** What `GetObjectInfo` answers, keyed by handle. An absent handle is refused. */
    val objectInfos: MutableMap<Int, ByteArray> = mutableMapOf()

    /** What `GetObject` answers, keyed by handle. An absent handle is refused. */
    val objects: MutableMap<Int, ByteArray> = mutableMapOf()

    /** Storage and catalogue facts used by the standard card-browsing operations. */
    val storageIds: MutableList<Int> = mutableListOf(0x00010001)
    val objectStorageIds: MutableMap<Int, Int> = mutableMapOf()
    val objectFormats: MutableMap<Int, Int> = mutableMapOf()
    val objectParents: MutableMap<Int, Int> = mutableMapOf()

    /** What `GetThumb` answers, keyed by handle. */
    val thumbnails: MutableMap<Int, ByteArray> = mutableMapOf()

    /** The dataset the last `SendObjectInfo` carried. */
    var sentObjectInfo: ByteArray? = null
        private set

    /** The bytes the last `SendObject` carried. */
    var sentObject: ByteArray? = null
        private set

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
    private var pendingObjectOperation: Int? = null

    /**
     * A data phase belonging to a command the body already refused.
     *
     * The transport sends the command container and the data container back to back and only
     * then reads, so a refusal at command time is still followed by a payload. A real device
     * discards it; without this the fake would throw and a refusal test could not be written.
     */
    private var discardNextData = false
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

    override fun writeStream(
        prefix: ByteArray,
        input: InputStream,
        payloadLength: Long,
        onProgress: (written: Long, total: Long) -> Unit,
    ) {
        val header = unpackContainer(prefix)
        require(header.type == ContainerType.DATA)
        val out = ByteArrayOutputStream(payloadLength.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        val buffer = ByteArray(64 * 1024)
        var readTotal = 0L
        while (readTotal < payloadLength) {
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), payloadLength - readTotal).toInt())
            if (count < 0) error("Stream ended early")
            if (count == 0) continue
            out.write(buffer, 0, count)
            readTotal += count
            onProgress(readTotal, payloadLength)
        }
        onData(out.toByteArray())
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

        val take = minOf(next.size, chunkSize, maxBytes)
        if (take == next.size) return next

        outgoing.addFirst(next.copyOfRange(take, next.size))
        return next.copyOfRange(0, take)
    }

    override fun close() {
        closed = true
    }

    // ─── The camera's side ──────────────────────────────────────────────────

    private fun onCommand(operation: Int, transactionId: Int, params: List<Int>) {
        if (silent) return

        refuseOperation[operation]?.let { code ->
            if (operation in DATA_OUT_OPERATIONS) discardNextData = true
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
                val code = params.firstOrNull() ?: 0
                reads += code

                if (code in refuseProperties) {
                    reply(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, transactionId)
                    return
                }

                // The settings registers follow the selector, exactly as the name register
                // does. A slot with no entry answers as unsupported, which is how an
                // unconfigured slot reads.
                slotSettings[selectedSlot]?.let { registers ->
                    if (code != PRESET_NAME_PROPERTY && code != PRESET_SLOT_PROPERTY) {
                        val value = registers[code]
                        if (value == null) {
                            reply(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, transactionId)
                        } else {
                            data(operation, transactionId, packU16(value))
                            reply(ResponseCode.OK, transactionId)
                        }
                        return
                    }
                }

                // The name register follows the selector, so it is answered from `slotNames`
                // rather than from whatever was last written to the property.
                if (code == PRESET_NAME_PROPERTY && slotNames.isNotEmpty()) {
                    val slot = selectedSlot
                    if (slot != null && slot in refuseNameForSlots) {
                        reply(ResponseCode.DEVICE_PROP_NOT_SUPPORTED, transactionId)
                    } else {
                        data(operation, transactionId, packPtpString(slotNames[slot] ?: ""))
                        reply(ResponseCode.OK, transactionId)
                    }
                    return
                }

                val value = propertyValues[code]
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

            Operation.GET_STORAGE_IDS -> {
                data(operation, transactionId, u32Array(storageIds))
                reply(ResponseCode.OK, transactionId)
            }

            Operation.GET_OBJECT_HANDLES -> {
                val storage = params.getOrNull(0) ?: -1
                val format = params.getOrNull(1) ?: 0
                val parent = params.getOrNull(2) ?: 0
                val handles = objectInfos.keys.filter { handle ->
                    (storage == -1 || objectStorageIds[handle] == storage) &&
                        (format == 0 || objectFormats[handle] == format) &&
                        (parent == 0 || objectParents[handle] == parent)
                }
                data(operation, transactionId, u32Array(handles))
                reply(ResponseCode.OK, transactionId)
            }

            Operation.SET_DEVICE_PROP_VALUE -> {
                val code = params.firstOrNull() ?: 0
                pendingSetProperty = code
                reply(refuseProperty[code] ?: ResponseCode.OK, transactionId)
            }

            Operation.GET_OBJECT_INFO -> answerObject(
                operation,
                transactionId,
                objectInfos[params.firstOrNull() ?: 0],
            )

            Operation.GET_OBJECT -> answerObject(
                operation,
                transactionId,
                objects[params.firstOrNull() ?: 0],
            )

            Operation.GET_THUMB -> answerObject(
                operation,
                transactionId,
                thumbnails[params.firstOrNull() ?: 0],
            )

            Operation.SEND_OBJECT_INFO,
            Operation.SEND_OBJECT,
            Operation.FUJI_SEND_OBJECT_INFO,
            Operation.FUJI_SEND_OBJECT,
            -> {
                pendingObjectOperation = operation
                reply(ResponseCode.OK, transactionId)
            }

            Operation.DELETE_OBJECT -> {
                val handle = params.firstOrNull() ?: 0
                if (objectInfos.remove(handle) == null && objects.remove(handle) == null) {
                    reply(ResponseCode.INVALID_OBJECT_HANDLE, transactionId)
                } else {
                    objects.remove(handle)
                    thumbnails.remove(handle)
                    reply(ResponseCode.OK, transactionId)
                }
            }

            else -> reply(ResponseCode.OPERATION_NOT_SUPPORTED, transactionId)
        }
    }

    private fun answerObject(operation: Int, transactionId: Int, payload: ByteArray?) {
        if (payload == null) {
            reply(ResponseCode.INVALID_OBJECT_HANDLE, transactionId)
        } else {
            data(operation, transactionId, payload)
            reply(ResponseCode.OK, transactionId)
        }
    }

    private fun onData(payload: ByteArray) {
        if (discardNextData) {
            discardNextData = false
            return
        }

        when (pendingObjectOperation) {
            Operation.SEND_OBJECT_INFO, Operation.FUJI_SEND_OBJECT_INFO -> {
                sentObjectInfo = payload
                pendingObjectOperation = null
                return
            }

            Operation.SEND_OBJECT, Operation.FUJI_SEND_OBJECT -> {
                sentObject = payload
                pendingObjectOperation = null
                return
            }
        }

        val code = pendingSetProperty ?: error("The fake camera got a data phase it did not expect")
        pendingSetProperty = null
        writes += Write(code, payload)

        if (code == PRESET_SLOT_PROPERTY) selectedSlot = unpackU16(payload)

        // A real body holds what it was told, which is what makes the executor's read-back
        // check mean anything. A property it refused holds nothing new.
        if (echoWrites && refuseProperty[code] == null) {
            propertyValues[code] = readBackAs[code] ?: payload
        }

        if (refuseProperty[code] == null) afterPropertyWrite?.invoke(code, payload)

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

    private companion object {
        val DATA_OUT_OPERATIONS = setOf(
            Operation.SET_DEVICE_PROP_VALUE,
            Operation.SEND_OBJECT_INFO,
            Operation.SEND_OBJECT,
            Operation.FUJI_SEND_OBJECT_INFO,
            Operation.FUJI_SEND_OBJECT,
        )
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
            extraOperations + listOf(
                Operation.GET_DEVICE_INFO,
                Operation.OPEN_SESSION,
                Operation.CLOSE_SESSION,
                Operation.GET_STORAGE_IDS,
                Operation.GET_OBJECT_HANDLES,
                Operation.GET_OBJECT_INFO,
                Operation.GET_OBJECT,
                Operation.GET_THUMB,
                Operation.DELETE_OBJECT,
                Operation.GET_DEVICE_PROP_DESC,
                Operation.GET_DEVICE_PROP_VALUE,
                Operation.SET_DEVICE_PROP_VALUE,
                Operation.FUJI_SEND_OBJECT_INFO,
                Operation.FUJI_SEND_OBJECT,
            ),
        )
        u16Array(emptyList())
        u16Array(propertyCodes + extraProperties)
        u16Array(emptyList())
        u16Array(listOf(0x3801))

        out.write(packPtpString("FUJIFILM"))
        out.write(packPtpString(model))
        out.write(packPtpString("1.20"))
        out.write(packPtpString("A1B2C3D4"))

        return out.toByteArray()
    }

    private fun u32Array(values: Collection<Int>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(packU32(values.size))
        values.forEach { out.write(packU32(it)) }
        return out.toByteArray()
    }
}
