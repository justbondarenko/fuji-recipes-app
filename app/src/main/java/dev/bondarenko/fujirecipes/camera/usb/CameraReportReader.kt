package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.ALWAYS_PROBED_PROPERTIES
import dev.bondarenko.fujirecipes.camera.plan.CameraReport
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.LAST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.PRESET_BLOCK
import dev.bondarenko.fujirecipes.camera.plan.PRESET_MATRIX_CODES
import dev.bondarenko.fujirecipes.camera.plan.PRESET_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.ProbeOutcome
import dev.bondarenko.fujirecipes.camera.plan.ProbedProperty
import dev.bondarenko.fujirecipes.camera.plan.SlotProbe
import dev.bondarenko.fujirecipes.camera.plan.renderAllowedValues
import dev.bondarenko.fujirecipes.camera.plan.usbModeFrom
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.dataTypeName
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.responseName
import dev.bondarenko.fujirecipes.camera.ptp.unpackPtpString
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16

/**
 * Walks a body and records what it says about every property it will discuss.
 *
 * The one operation in this app that exists for someone other than the person running it: the
 * output goes into an issue, and its job is to let the encoding tables grow to bodies nobody
 * here owns. See `camera/plan/CameraReport.kt` for why that is worth a screen.
 *
 * **Two walks, because the preset block is not like the rest.** Everything outside
 * `0xD18C`–`0xD1A5` describes the body and is read once. The preset registers follow the slot
 * selector, so reading them once describes one recipe — and the question worth answering about
 * them is whether a code *varies* between slots, which needs all seven. So they are walked per
 * slot and laid out as a table.
 *
 * **Failure handling is the opposite of `CameraFactsReader`'s, on purpose.** That one swallows
 * everything, because a battery level must never cost a connection. This is started by the user
 * and its whole output is the record of what the body said — so a *refusal* is a result and is
 * written down, while a dead pipe propagates and the screen says the camera stopped answering.
 * A report silently missing half its rows would be worse than no report.
 *
 * Blocking, and slow enough to need the progress callback: sixty-odd properties plus seven
 * passes over the preset block is a couple of hundred round trips. Callers run it on
 * `Dispatchers.IO`.
 */
fun readCameraReport(
    session: PtpSession,
    appVersion: String,
    capturedAt: String,
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    sleep: (Long) -> Unit = { Thread.sleep(it) },
): CameraReport {
    val info = session.deviceInfo
    val usbModeRaw = readUsbModeRaw(session)

    val listed = info?.devicePropertiesSupported.orEmpty()

    // The preset block is excluded here and walked per slot below; including it would record
    // whichever slot happened to be selected, under no particular label.
    val generalCodes = (listed + ALWAYS_PROBED_PROPERTIES)
        .distinct()
        .filterNot { it in PRESET_BLOCK }
        .sorted()

    // A body that will not take the selector has no slots to walk, and finding that out costs
    // one round trip rather than a hundred and eighty refusals.
    val hasSlots = runCatching {
        session.setPropertyBytes(PRESET_SLOT_PROPERTY, packU16(FIRST_SLOT))
        sleep(SLOT_SETTLE_MS)
    }.isSuccess

    val slotCount = if (hasSlots) LAST_SLOT - FIRST_SLOT + 1 else 0
    val total = generalCodes.size + slotCount * (1 + PRESET_MATRIX_CODES.size)
    var done = 0

    val properties = generalCodes.map { code ->
        onProgress(++done, total)
        probe(session, code, listedByBody = code in listed)
    }

    val slots = if (!hasSlots) {
        emptyList()
    } else {
        (FIRST_SLOT..LAST_SLOT).map { slot ->
            readSlot(session, slot, sleep) { onProgress(++done, total) }
        }
    }

    return CameraReport(
        appVersion = appVersion,
        capturedAt = capturedAt,
        manufacturer = info?.manufacturer.orEmpty().ifBlank { "not reported" },
        model = info?.model.orEmpty().ifBlank { "not reported" },
        firmware = info?.deviceVersion.orEmpty().ifBlank { "not reported" },
        standardVersion = info?.standardVersion ?: 0,
        vendorExtensionId = info?.vendorExtensionId ?: 0,
        vendorExtensionVersion = info?.vendorExtensionVersion ?: 0,
        vendorExtensionDescription = info?.vendorExtensionDescription.orEmpty(),
        usbMode = usbModeFrom(
            reportedValue = usbModeRaw,
            operationsSupported = info?.operationsSupported.orEmpty(),
            devicePropertiesSupported = listed,
        ),
        usbModeRaw = usbModeRaw,
        operationsSupported = info?.operationsSupported.orEmpty(),
        eventsSupported = info?.eventsSupported.orEmpty(),
        propertiesListed = listed,
        captureFormats = info?.captureFormats.orEmpty(),
        imageFormats = info?.imageFormats.orEmpty(),
        selectedSlot = if (hasSlots) FIRST_SLOT else null,
        properties = properties,
        slots = slots,
    )
}

/**
 * One slot's preset registers, as the raw 16 bits the body returned.
 *
 * Raw rather than decoded on purpose. A report exists to record what the camera said, and a
 * decoded value would be this build's *reading* of it — which is the thing the report is meant
 * to let someone else check.
 */
private fun readSlot(
    session: PtpSession,
    slot: Int,
    sleep: (Long) -> Unit,
    onStep: () -> Unit,
): SlotProbe {
    runCatching {
        session.setPropertyBytes(PRESET_SLOT_PROPERTY, packU16(slot))
        sleep(SLOT_SETTLE_MS)
    }

    onStep()
    val name = runCatching { session.readPropertyBytes(PRESET_NAME_PROPERTY) }
        .getOrNull()
        ?.let { runCatching { unpackPtpString(it).trim() }.getOrNull() }
        ?.takeIf { it.isNotEmpty() }

    val values = PRESET_MATRIX_CODES.associateWith { code ->
        onStep()
        runCatching { session.readPropertyBytes(code) }
            .getOrNull()
            ?.takeIf { it.size >= 2 }
            ?.let { unpackU16(it) }
    }

    return SlotProbe(slot = slot, name = name, values = values)
}

/**
 * One property, described if the body will, read raw if it will only do that, and recorded as
 * refused if it will do neither.
 *
 * The fallback is not a rare path. An X-T50 in RAW conversion mode advertises
 * `GetDevicePropDesc` and then refuses it for every property, so every row on that body comes
 * from the raw read.
 *
 * Only [PtpError] is caught. Anything else — a timeout, a framing error — is the pipe rather
 * than the property, and every later row would be answering an earlier question.
 */
private fun probe(session: PtpSession, code: Int, listedByBody: Boolean): ProbedProperty {
    val described = try {
        session.describeProperty(code)
    } catch (error: PtpError) {
        null
    }

    if (described != null) {
        return ProbedProperty(
            code = code,
            listedByBody = listedByBody,
            outcome = ProbeOutcome.DESCRIBED,
            dataTypeLabel = dataTypeName(described.dataType),
            writable = described.writable,
            value = renderValue(described.currentValue),
            allowed = renderAllowedValues(
                min = described.range?.min,
                max = described.range?.max,
                step = described.range?.step,
                enumeration = described.enumeration,
            ),
        )
    }

    return try {
        ProbedProperty(
            code = code,
            listedByBody = listedByBody,
            outcome = ProbeOutcome.VALUE_ONLY,
            value = renderBytes(session.readPropertyBytes(code)),
        )
    } catch (error: PtpError) {
        ProbedProperty(
            code = code,
            listedByBody = listedByBody,
            outcome = ProbeOutcome.REFUSED,
            refusal = responseName(error.code),
        )
    }
}

/** A described value as text: numbers decimal, strings quoted, anything else absent. */
private fun renderValue(value: Any?): String? = when (value) {
    null -> null
    is String -> "“$value”"
    else -> value.toString()
}

/**
 * Undescribed bytes as text where they are unmistakably a PTP string, hex otherwise.
 *
 * Without a declared type there is no width and no signedness to decode a *number* by, and one
 * invented here would be read as the camera's own answer. A string is the exception, because it
 * carries its own length and that length is checkable: `“Kodak Gold 200”` is worth incomparably
 * more to a reader than the first sixteen bytes of its UCS-2.
 */
private fun renderBytes(bytes: ByteArray, limit: Int = RAW_BYTES_LIMIT): String {
    if (bytes.isEmpty()) return "(empty)"

    ptpStringOrNull(bytes)?.let { text ->
        return if (text.length <= STRING_LIMIT) {
            "“$text”"
        } else {
            "“${text.take(STRING_LIMIT)}…” (${text.length} characters)"
        }
    }

    val shown = bytes.take(limit).joinToString(" ") {
        (it.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')
    }

    return if (bytes.size <= limit) shown else "$shown … ${bytes.size - limit} more bytes"
}

/**
 * The payload as a PTP string, or null when it is not one.
 *
 * The length check is what makes this safe rather than a guess. A PTP string is a character
 * count *including* its terminator followed by exactly that many UCS-2 characters, so a payload
 * that is a string has a size its own first byte predicts. A two-byte number `0F 00` would
 * claim fifteen characters and thirty-one bytes, and a four-byte `0A 00 00 00` would claim ten
 * and twenty-one; neither can be mistaken for one.
 *
 * At least one real character is required. A three-byte payload whose first byte is 1 decodes
 * to the empty string under these rules, and calling that a string rather than three bytes of
 * something else would be the guess this is written to avoid.
 */
private fun ptpStringOrNull(bytes: ByteArray): String? {
    val count = bytes[0].toInt() and 0xff
    if (count < 2) return null
    if (bytes.size != 1 + count * 2) return null

    val decoded = runCatching { unpackPtpString(bytes) }.getOrNull() ?: return null

    // A short decode means an early NUL, so the declared length described something other than
    // a run of characters.
    if (decoded.length != count - 1) return null
    if (decoded.any { it.isISOControl() }) return null

    return decoded
}

private const val RAW_BYTES_LIMIT = 16
private const val STRING_LIMIT = 64
