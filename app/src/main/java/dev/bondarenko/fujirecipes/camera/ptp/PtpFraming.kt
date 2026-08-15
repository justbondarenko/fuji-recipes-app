package dev.bondarenko.fujirecipes.camera.ptp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PTP container framing and value codecs.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/ptp.ts` at commit `0c17106`
 * (`coding-standards.md` P3). Behaviour, names and the reasons behind them are that file's;
 * only the language changed.
 *
 * Pure byte work: pack a container, unpack a container, encode and decode the value types
 * the property operations use. No transport, no `android.*`, no state — which is what lets
 * the whole of it be tested with byte arrays and no camera attached (`coding-standards.md`
 * P4, and `field-definitions.md` WR-09 for the same reason one layer up).
 *
 * **The wire format** (ISO 15740, and `eggricesoy/filmkit`'s `QUICK_REFERENCE.md`):
 *
 * ```
 *   [0-3]   uint32 LE  total length, header included
 *   [4-5]   uint16 LE  container type: 1 command, 2 data, 3 response, 4 event
 *   [6-7]   uint16 LE  operation code, or response code on a response
 *   [8-11]  uint32 LE  transaction id
 *   [12+]   up to five uint32 params, or the data payload
 * ```
 *
 * A command carries params; a data container carries a payload; a response carries params
 * again. The type decides which, and [unpackContainer] reads accordingly rather than
 * guessing from the length.
 *
 * **On 32-bit widths.** Params and transaction ids are `uint32` on the wire and `Int` here,
 * holding the raw bits. Nothing compares them as magnitudes — transaction ids are only ever
 * tested for equality, and this app reads no response params — so the sign bit is harmless.
 * [containerLength] is the exception and returns a `Long`, because the transport genuinely
 * compares it against a size.
 */

const val CONTAINER_HEADER_SIZE = 12

/** A container may carry at most five parameters (ISO 15740). */
const val MAX_PARAMS = 5

object ContainerType {
    const val COMMAND = 0x0001
    const val DATA = 0x0002
    const val RESPONSE = 0x0003
    const val EVENT = 0x0004
}

/**
 * The operations this app uses, and only those.
 *
 * The reference implements a dozen more for its RAW-conversion workflow — object handles,
 * transfers, deletes. Custom slots need none of them: all read/write goes through
 * `GetDevicePropValue` / `SetDevicePropValue`, and no vendor operation is required.
 */
object Operation {
    const val GET_DEVICE_INFO = 0x1001
    const val OPEN_SESSION = 0x1002
    const val CLOSE_SESSION = 0x1003
    const val GET_DEVICE_PROP_DESC = 0x1014
    const val GET_DEVICE_PROP_VALUE = 0x1015
    const val SET_DEVICE_PROP_VALUE = 0x1016
}

object ResponseCode {
    const val OK = 0x2001
    const val GENERAL_ERROR = 0x2002
    const val SESSION_NOT_OPEN = 0x2003
    const val INVALID_TRANSACTION_ID = 0x2004
    const val OPERATION_NOT_SUPPORTED = 0x2005
    const val PARAMETER_NOT_SUPPORTED = 0x2006
    const val INCOMPLETE_TRANSFER = 0x2007
    const val INVALID_STORAGE_ID = 0x2008
    const val INVALID_OBJECT_HANDLE = 0x2009
    const val DEVICE_PROP_NOT_SUPPORTED = 0x200a
    const val INVALID_DEVICE_PROP_FORMAT = 0x200b
    const val INVALID_DEVICE_PROP_VALUE = 0x200c
    const val ACCESS_DENIED = 0x200f
    const val DEVICE_BUSY = 0x2019
    const val INVALID_PARENT_OBJECT = 0x201a
    const val SESSION_ALREADY_OPEN = 0x201e
    const val TRANSACTION_CANCELLED = 0x201f
}

/**
 * PTP data type codes, as `DevicePropDesc` reports them.
 *
 * The array flag is bitwise-or'd onto a scalar code; `0xFFFF` is a string.
 */
object DataType {
    const val UNDEFINED = 0x0000
    const val INT8 = 0x0001
    const val UINT8 = 0x0002
    const val INT16 = 0x0003
    const val UINT16 = 0x0004
    const val INT32 = 0x0005
    const val UINT32 = 0x0006
    const val INT64 = 0x0007
    const val UINT64 = 0x0008
    const val INT128 = 0x0009
    const val UINT128 = 0x000a
    const val STRING = 0xffff
}

const val ARRAY_TYPE_FLAG = 0x4000

private val SCALAR_SIZES: Map<Int, Int> = mapOf(
    DataType.INT8 to 1,
    DataType.UINT8 to 1,
    DataType.INT16 to 2,
    DataType.UINT16 to 2,
    DataType.INT32 to 4,
    DataType.UINT32 to 4,
    DataType.INT64 to 8,
    DataType.UINT64 to 8,
    DataType.INT128 to 16,
    DataType.UINT128 to 16,
)

/**
 * Bytes one value of a scalar type occupies, or `null` for a type this build cannot measure.
 *
 * `null` rather than a default: the reference falls back to four bytes for an unknown type,
 * and a wrong guess here does not fail — it shifts every field after it in the dataset and
 * produces plausible wrong numbers.
 */
fun dataTypeSize(dataType: Int): Int? = SCALAR_SIZES[dataType]

private val DATA_TYPE_NAMES: Map<Int, String> = mapOf(
    DataType.UNDEFINED to "Undefined",
    DataType.INT8 to "Int8",
    DataType.UINT8 to "UInt8",
    DataType.INT16 to "Int16",
    DataType.UINT16 to "UInt16",
    DataType.INT32 to "Int32",
    DataType.UINT32 to "UInt32",
    DataType.INT64 to "Int64",
    DataType.UINT64 to "UInt64",
    DataType.INT128 to "Int128",
    DataType.UINT128 to "UInt128",
    DataType.STRING to "String",
)

fun dataTypeName(dataType: Int): String {
    // `0xFFFF` has the array bit set incidentally, so the string type is answered before the
    // array test — otherwise it reads as an array of `0xBFFF`.
    if (dataType == DataType.STRING) return "String"

    val scalar = dataType and ARRAY_TYPE_FLAG.inv()
    val name = DATA_TYPE_NAMES[dataType] ?: DATA_TYPE_NAMES[scalar]
    val suffix = if (dataType and ARRAY_TYPE_FLAG == 0) "" else "[]"

    return name?.plus(suffix) ?: hex4(dataType)
}

private val RESPONSE_NAMES: Map<Int, String> = mapOf(
    ResponseCode.OK to "OK",
    ResponseCode.GENERAL_ERROR to "GeneralError",
    ResponseCode.SESSION_NOT_OPEN to "SessionNotOpen",
    ResponseCode.INVALID_TRANSACTION_ID to "InvalidTransactionID",
    ResponseCode.OPERATION_NOT_SUPPORTED to "OperationNotSupported",
    ResponseCode.PARAMETER_NOT_SUPPORTED to "ParameterNotSupported",
    ResponseCode.INCOMPLETE_TRANSFER to "IncompleteTransfer",
    ResponseCode.INVALID_STORAGE_ID to "InvalidStorageID",
    ResponseCode.INVALID_OBJECT_HANDLE to "InvalidObjectHandle",
    ResponseCode.DEVICE_PROP_NOT_SUPPORTED to "DevicePropNotSupported",
    ResponseCode.INVALID_DEVICE_PROP_FORMAT to "InvalidDevicePropFormat",
    ResponseCode.INVALID_DEVICE_PROP_VALUE to "InvalidDevicePropValue",
    ResponseCode.ACCESS_DENIED to "AccessDenied",
    ResponseCode.DEVICE_BUSY to "DeviceBusy",
    ResponseCode.INVALID_PARENT_OBJECT to "InvalidParentObject",
    ResponseCode.SESSION_ALREADY_OPEN to "SessionAlreadyOpen",
    ResponseCode.TRANSACTION_CANCELLED to "TransactionCancelled",
)

/**
 * A response code, as something a person can read.
 *
 * `coding-standards.md` P5: "the camera said 0x2019" tells nobody anything, and
 * `DeviceBusy` tells them to close the other application holding the device.
 */
fun responseName(code: Int): String = RESPONSE_NAMES[code] ?: hex4(code)

private fun hex4(value: Int): String =
    "0x" + (value and 0xffff).toString(16).uppercase().padStart(4, '0')

/**
 * A response the camera returned that is not [ResponseCode.OK].
 *
 * Named and carrying the code, so the write sheet can say which operation the camera refused
 * and what it said — the difference between a retry and a support thread (P5).
 */
class PtpError(
    val code: Int,
    val operation: Int,
    message: String? = null,
) : Exception(
    message ?: "The camera refused operation ${hex4(operation)}: ${responseName(code)}",
)

/** A malformed container, or one that answers a different request. */
class PtpFramingError(message: String) : Exception(message)

data class PtpContainer(
    val type: Int,
    val code: Int,
    val transactionId: Int,
    val params: List<Int>,
    val data: ByteArray,
) {
    // ByteArray in a data class: identity equality would make two containers carrying the
    // same bytes unequal, which is exactly what a test asserting a round trip compares.
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is PtpContainer &&
                    type == other.type &&
                    code == other.code &&
                    transactionId == other.transactionId &&
                    params == other.params &&
                    data.contentEquals(other.data)
                )

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + code
        result = 31 * result + transactionId
        result = 31 * result + params.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

// ─── Container framing ──────────────────────────────────────────────────────

private val EMPTY = ByteArray(0)

fun packContainer(
    type: Int,
    code: Int,
    transactionId: Int,
    params: List<Int> = emptyList(),
    data: ByteArray = EMPTY,
): ByteArray {
    val kept = params.take(MAX_PARAMS)
    val length = CONTAINER_HEADER_SIZE + kept.size * 4 + data.size
    val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)

    buffer.putInt(length)
    buffer.putShort(type.toShort())
    buffer.putShort(code.toShort())
    buffer.putInt(transactionId)
    // Params go on as raw 32-bit patterns: they are uint32 on the wire, and a caller
    // computing one arithmetically can easily produce a signed value.
    kept.forEach { buffer.putInt(it) }
    buffer.put(data)

    return buffer.array()
}

/**
 * Reads a container back, optionally from inside a larger buffer.
 *
 * A **data** container's payload is everything after the header — no params. A **response**
 * carries up to five params and no payload. Reading params off a data container would turn
 * the first four bytes of a property value into a parameter, which is the sort of thing that
 * produces a plausible wrong number.
 */
fun unpackContainer(
    bytes: ByteArray,
    offset: Int = 0,
    length: Int = bytes.size - offset,
): PtpContainer {
    if (length < CONTAINER_HEADER_SIZE) {
        throw PtpFramingError(
            "A PTP container is at least $CONTAINER_HEADER_SIZE bytes; got $length.",
        )
    }

    val buffer = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.position(offset + 4)
    val type = buffer.short.toInt() and 0xffff
    val code = buffer.short.toInt() and 0xffff
    val transactionId = buffer.int

    val restSize = length - CONTAINER_HEADER_SIZE
    val params = mutableListOf<Int>()
    var data = EMPTY

    if (type == ContainerType.DATA) {
        data = bytes.copyOfRange(offset + CONTAINER_HEADER_SIZE, offset + length)
    } else {
        var at = 0
        while (at + 4 <= restSize && params.size < MAX_PARAMS) {
            params += buffer.int
            at += 4
        }
    }

    return PtpContainer(type, code, transactionId, params, data)
}

/**
 * The declared total length, from a container's first four bytes.
 *
 * The transport needs this before it has the whole container: a bulk read returns whatever
 * fits in one transfer, and this is how it knows whether to read again. Unsigned, because
 * the transport compares it against a size rather than for equality.
 */
fun containerLength(bytes: ByteArray, offset: Int = 0): Long {
    if (bytes.size - offset < 4) return 0
    return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and
        0xffffffffL
}

// ─── Value codecs ───────────────────────────────────────────────────────────

fun packU16(value: Int): ByteArray =
    ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((value and 0xffff).toShort())
        .array()

/**
 * Clamped rather than wrapped: a tone value out of range is a bug upstream, and silently
 * writing its two's-complement wrap would set something unrelated.
 */
fun packI16(value: Double): ByteArray =
    ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        .putShort(clamp(value, -32768, 32767).toShort()).array()

fun packI16(value: Int): ByteArray = packI16(value.toDouble())

fun packU32(value: Int): ByteArray =
    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

private fun clamp(value: Double, low: Int, high: Int): Int =
    min(high, max(low, value.roundToInt()))

fun unpackU16(bytes: ByteArray, offset: Int = 0): Int =
    ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff

fun unpackI16(bytes: ByteArray, offset: Int = 0): Int =
    ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

fun unpackU32(bytes: ByteArray, offset: Int = 0): Long =
    ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL

// ─── PTP strings ────────────────────────────────────────────────────────────

/**
 * A PTP string: one byte of length **in characters, including the terminator**, then UCS-2 LE
 * characters, then a NUL. An empty string is the single byte `0` with nothing after it — not
 * a `1` and a NUL.
 *
 * The length byte counts characters rather than bytes. Callers truncate before they get here;
 * this refuses rather than writing a length that disagrees with the payload.
 */
const val MAX_PTP_STRING_LENGTH = 254

fun packPtpString(value: String): ByteArray {
    val characters = value.toCharArray()
    if (characters.isEmpty()) return byteArrayOf(0)
    if (characters.size > MAX_PTP_STRING_LENGTH) {
        throw PtpFramingError(
            "A PTP string holds at most $MAX_PTP_STRING_LENGTH characters; " +
                "got ${characters.size}.",
        )
    }

    val buffer = ByteBuffer.allocate(1 + (characters.size + 1) * 2).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put((characters.size + 1).toByte())
    // UCS-2 has no room above the BMP, so an emoji in a preset name loses its surrogate pair
    // rather than corrupting the rest of the string. Names are ASCII in practice.
    characters.forEach { buffer.putShort(it.code.toShort()) }
    buffer.putShort(0)

    return buffer.array()
}

fun unpackPtpString(bytes: ByteArray, offset: Int = 0): String {
    if (offset >= bytes.size) return ""
    val count = bytes[offset].toInt() and 0xff
    if (count == 0) return ""

    val out = StringBuilder()
    for (at in 0 until count) {
        val position = offset + 1 + at * 2
        if (position + 2 > bytes.size) break
        val character = unpackU16(bytes, position)
        if (character == 0) break
        out.append(character.toChar())
    }
    return out.toString()
}

// ─── Response checking ──────────────────────────────────────────────────────

/** Throws [PtpError] unless the camera said OK. */
fun expectOk(operation: Int, code: Int) {
    if (code != ResponseCode.OK) throw PtpError(code, operation)
}
