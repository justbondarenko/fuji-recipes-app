package dev.bondarenko.fujirecipes.camera.ptp

/**
 * The `GetDeviceInfo` and `GetDevicePropDesc` datasets, parsed.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/session.ts` at commit `0c17106`
 * (`coding-standards.md` P3) — the dataset parsers only; the session itself is
 * `camera/usb/PtpSession.kt`, because that half needs a transport and this half does not.
 *
 * **Layout source.** ISO 15740 for both datasets, cross-checked field-for-field against
 * `eggricesoy/filmkit`'s `getDeviceInfo`, which reads the same fourteen fields in the same
 * order, independently arrived at.
 *
 * Pure: no `android.*` (P4).
 */

/**
 * A cursor over a PTP dataset.
 *
 * Every read is bounds-checked and every failure names the dataset, because the alternative
 * is an index exception with no context from a parser that has already walked off the end of
 * something.
 */
internal class Cursor(private val bytes: ByteArray, private val what: String) {

    private var at = 0

    val remaining: Int get() = bytes.size - at

    private fun require(count: Int) {
        if (at + count > bytes.size) {
            throw PtpFramingError(
                "$what ran out after ${bytes.size} bytes: $count more were needed at offset $at.",
            )
        }
    }

    fun u8(): Int {
        require(1)
        return (bytes[at++].toInt() and 0xff)
    }

    fun u16(): Int {
        require(2)
        val value = unpackU16(bytes, at)
        at += 2
        return value
    }

    fun u32(): Long {
        require(4)
        val value = unpackU32(bytes, at)
        at += 4
        return value
    }

    fun skip(count: Int) {
        require(count)
        at += count
    }

    /** `uint32` count, then that many `uint16`s. */
    fun u16Array(): List<Int> {
        val count = u32()
        // The count is four bytes on the wire, so a parse that has drifted by two can claim
        // four billion elements. Checked before the loop rather than inside it: the message
        // then says the dataset is too short for what it declared, which is the actual fault.
        if (count > Int.MAX_VALUE / 2) {
            throw PtpFramingError("$what declared $count elements, which cannot be read.")
        }
        require(count.toInt() * 2)

        return List(count.toInt()) { u16() }
    }

    /**
     * A PTP string: one byte of character count including the terminator, then UCS-2 LE.
     *
     * The cursor advances by the **declared** count, not by what decoded. A string whose NUL
     * arrives early still occupies its declared bytes, and advancing by the decoded length
     * would leave the cursor inside the remainder — which then reads as the next field and
     * produces a plausible wrong number.
     */
    fun ptpString(): String {
        require(1)
        val characters = bytes[at].toInt() and 0xff
        val size = if (characters == 0) 1 else 1 + characters * 2
        require(size)

        val value = unpackPtpString(bytes, at)
        at += size
        return value
    }

    /**
     * One value of the given data type, as a [Long], a [String], or `null`.
     *
     * `null` means "skipped, correctly" — a 64- or 128-bit scalar (no property this app
     * touches uses one) or an array. The cursor still advances by the right number of bytes,
     * which is the part that matters: the reference implementation guesses four bytes for
     * every type it does not know, and a wrong guess here silently shifts every field after
     * it.
     */
    fun value(dataType: Int): Any? {
        if (dataType == DataType.STRING) return ptpString()

        if (dataType and ARRAY_TYPE_FLAG != 0) {
            val size = scalarSize(dataType and ARRAY_TYPE_FLAG.inv(), dataType)
            val count = u32()
            skip(count.toInt() * size)
            return null
        }

        val size = scalarSize(dataType, dataType)
        if (size > 4) {
            skip(size)
            return null
        }

        require(size)
        val value = scalar(dataType, size)
        at += size
        return value
    }

    private fun scalarSize(scalar: Int, dataType: Int): Int =
        dataTypeSize(scalar) ?: throw PtpFramingError(
            "$what declared data type ${dataTypeName(dataType)}, which this build cannot " +
                "measure, so the rest of it cannot be read.",
        )

    private fun scalar(dataType: Int, size: Int): Long = when (size) {
        1 -> if (dataType == DataType.INT8) {
            bytes[at].toLong()
        } else {
            (bytes[at].toInt() and 0xff).toLong()
        }

        2 -> if (dataType == DataType.INT16) {
            unpackI16(bytes, at).toLong()
        } else {
            unpackU16(bytes, at).toLong()
        }

        else -> if (dataType == DataType.INT32) {
            unpackU32(bytes, at).toInt().toLong()
        } else {
            unpackU32(bytes, at)
        }
    }
}

// ─── DeviceInfo ─────────────────────────────────────────────────────────────

data class DeviceInfo(
    val standardVersion: Int,
    val vendorExtensionId: Long,
    val vendorExtensionVersion: Int,
    val vendorExtensionDescription: String,
    val functionalMode: Int,
    val operationsSupported: List<Int>,
    val eventsSupported: List<Int>,
    /** Every property code this body admits to having. */
    val devicePropertiesSupported: List<Int>,
    val captureFormats: List<Int>,
    val imageFormats: List<Int>,
    val manufacturer: String,
    val model: String,
    val deviceVersion: String,
    val serialNumber: String,
)

/**
 * Parses the `GetDeviceInfo` dataset.
 *
 * Fourteen fields in a fixed order, five of them counted arrays and five of them strings — so
 * every field's offset depends on the length of the one before it. There is no way to read
 * `model` without reading all five arrays correctly first, which is why this is a cursor and
 * not a table of offsets.
 */
fun parseDeviceInfo(bytes: ByteArray): DeviceInfo {
    val cursor = Cursor(bytes, "The device info dataset")

    val standardVersion = cursor.u16()
    val vendorExtensionId = cursor.u32()
    val vendorExtensionVersion = cursor.u16()
    val vendorExtensionDescription = cursor.ptpString()
    val functionalMode = cursor.u16()

    val operationsSupported = cursor.u16Array()
    val eventsSupported = cursor.u16Array()
    val devicePropertiesSupported = cursor.u16Array()
    val captureFormats = cursor.u16Array()
    val imageFormats = cursor.u16Array()

    return DeviceInfo(
        standardVersion = standardVersion,
        vendorExtensionId = vendorExtensionId,
        vendorExtensionVersion = vendorExtensionVersion,
        vendorExtensionDescription = vendorExtensionDescription,
        functionalMode = functionalMode,
        operationsSupported = operationsSupported,
        eventsSupported = eventsSupported,
        devicePropertiesSupported = devicePropertiesSupported,
        captureFormats = captureFormats,
        imageFormats = imageFormats,
        manufacturer = cursor.ptpString(),
        model = cursor.ptpString(),
        deviceVersion = cursor.ptpString(),
        serialNumber = cursor.ptpString(),
    )
}

// ─── DevicePropDesc ─────────────────────────────────────────────────────────

enum class PropertyForm { NONE, RANGE, ENUM }

data class PropertyRange(val min: Long, val max: Long, val step: Long)

data class DevicePropertyDescription(
    val code: Int,
    val dataType: Int,
    /** `GetSet`: `false` means the camera will not accept a write at all. */
    val writable: Boolean,
    val factoryDefault: Any?,
    val currentValue: Any?,
    val form: PropertyForm,
    val range: PropertyRange? = null,
    /** The values the camera says it accepts. Strings are kept as strings. */
    val enumeration: List<Any>? = null,
)

private const val FORM_NONE = 0x00
private const val FORM_RANGE = 0x01
private const val FORM_ENUM = 0x02

/**
 * Parses a `GetDevicePropDesc` dataset.
 *
 * `code`, `dataType`, `GetSet`, factory default, current value, then a form flag and either a
 * range or an enumeration. The values are typed by `dataType`, so the same dataset is a
 * different length for a `uint16` property and for a string one.
 *
 * **A dataset that stops after the current value is accepted** as [PropertyForm.NONE] rather
 * than rejected. The form is the last field and some bodies omit it; the type and the current
 * value are already in hand at that point, and refusing the whole description would throw
 * away the part that is useful.
 */
fun parseDevicePropertyDescription(bytes: ByteArray): DevicePropertyDescription {
    val cursor = Cursor(bytes, "The property description")

    val code = cursor.u16()
    val dataType = cursor.u16()
    val writable = cursor.u8() != 0
    val factoryDefault = cursor.value(dataType)
    val currentValue = cursor.value(dataType)

    fun plain(form: PropertyForm) =
        DevicePropertyDescription(code, dataType, writable, factoryDefault, currentValue, form)

    if (cursor.remaining == 0) return plain(PropertyForm.NONE)

    return when (val formFlag = cursor.u8()) {
        FORM_NONE -> plain(PropertyForm.NONE)

        FORM_RANGE -> {
            val min = cursor.value(dataType)
            val max = cursor.value(dataType)
            val step = cursor.value(dataType)

            if (min !is Long || max !is Long || step !is Long) {
                throw PtpFramingError(
                    "Property ${propertyHex(code)} declared a range of " +
                        "${dataTypeName(dataType)}, which has no numeric bounds.",
                )
            }

            plain(PropertyForm.RANGE).copy(range = PropertyRange(min, max, step))
        }

        FORM_ENUM -> {
            val count = cursor.u16()
            val enumeration = buildList {
                repeat(count) { cursor.value(dataType)?.let(::add) }
            }
            plain(PropertyForm.ENUM).copy(enumeration = enumeration)
        }

        // 0, 1 and 2 are the only forms ISO 15740 defines. Anything else means this parse is
        // out of step with the bytes, and a made-up form would be reported as the camera's
        // own answer.
        else -> throw PtpFramingError(
            "Property ${propertyHex(code)} declared form flag " +
                "0x${formFlag.toString(16)}, which is not a form ISO 15740 defines.",
        )
    }
}

internal fun propertyHex(code: Int): String =
    "0x" + (code and 0xffff).toString(16).uppercase().padStart(4, '0')
