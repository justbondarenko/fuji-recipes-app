package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.ALWAYS_PROBED_PROPERTIES
import dev.bondarenko.fujirecipes.camera.plan.CameraReport
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.ProbeOutcome
import dev.bondarenko.fujirecipes.camera.plan.ProbedProperty
import dev.bondarenko.fujirecipes.camera.plan.renderAllowedValues
import dev.bondarenko.fujirecipes.camera.plan.usbModeFor
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.dataTypeName
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.responseName

/**
 * Walks a body and records what it says about every property it will discuss.
 *
 * The one operation in this app that exists for someone other than the person running it: the
 * output goes into an issue, and its job is to let the encoding tables grow to bodies nobody
 * here owns. See `camera/plan/CameraReport.kt` for why that is worth a screen.
 *
 * **Failure handling is the opposite of `CameraFactsReader`'s, on purpose.** That one swallows
 * everything, because a battery percentage must never cost a connection. This is started by
 * the user and its whole output is the record of what the body said — so a *refusal* is a
 * result and is written down, while a dead pipe propagates and the screen says the camera
 * stopped answering. A report silently missing half its rows would be worse than no report.
 *
 * Blocking, and slow enough to need the progress callback: sixty-odd properties at up to two
 * round trips each is seconds of work with a cable in. Callers run it on `Dispatchers.IO`.
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

    // The preset block follows the slot selector, so a report taken with the selector wherever
    // the last operation left it would record one slot's values under no particular label.
    // C1 makes it reproducible, and the report says which slot it was. A body that refuses the
    // selector is recorded as having no slot, not as an error — the rest of the walk is still
    // worth having.
    val selectedSlot = runCatching {
        session.setPropertyBytes(PRESET_SLOT_PROPERTY, packU16(FIRST_SLOT))
        sleep(SLOT_SETTLE_MS)
        FIRST_SLOT
    }.getOrNull()

    val listed = info?.devicePropertiesSupported.orEmpty()
    val codes = (listed + ALWAYS_PROBED_PROPERTIES).distinct().sorted()

    val properties = codes.mapIndexed { index, code ->
        onProgress(index + 1, codes.size)
        probe(session, code, listedByBody = code in listed)
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
        usbMode = usbModeRaw?.let(::usbModeFor) ?: UsbMode.UNREPORTED,
        usbModeRaw = usbModeRaw,
        operationsSupported = info?.operationsSupported.orEmpty(),
        eventsSupported = info?.eventsSupported.orEmpty(),
        propertiesListed = listed,
        captureFormats = info?.captureFormats.orEmpty(),
        imageFormats = info?.imageFormats.orEmpty(),
        selectedSlot = selectedSlot,
        properties = properties,
    )
}

/**
 * One property, described if the body will, read raw if it will only do that, and recorded as
 * refused if it will do neither.
 *
 * Only [PtpError] is caught. Anything else — a timeout, a framing error — is the pipe rather
 * than the property, and every later row would be answering an earlier question.
 */
private fun probe(session: PtpSession, code: Int, listedByBody: Boolean): ProbedProperty {
    // A refusal here is not the end: some bodies carry a property and will not describe it,
    // and its current value is still worth recording.
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
 * Undescribed bytes as hex.
 *
 * Not decoded: without the body's declared type there is no width and no signedness to decode
 * by, and a number invented here would be read as the camera's own answer. Long payloads are
 * truncated — a report is for reading.
 */
private fun renderBytes(bytes: ByteArray, limit: Int = RAW_BYTES_LIMIT): String {
    if (bytes.isEmpty()) return "(empty)"

    val shown = bytes.take(limit).joinToString(" ") {
        (it.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')
    }

    return if (bytes.size <= limit) shown else "$shown … ${bytes.size - limit} more bytes"
}

private const val RAW_BYTES_LIMIT = 16
