package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.BATTERY_LEVEL_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.BatteryLevel
import dev.bondarenko.fujirecipes.camera.plan.STANDARD_BATTERY_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.plausibleBatteryLevel
import dev.bondarenko.fujirecipes.camera.plan.CameraDetails
import dev.bondarenko.fujirecipes.camera.plan.LENS_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.TOTAL_SHOT_COUNT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.USB_MODE_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.plan.plausibleLensName
import dev.bondarenko.fujirecipes.camera.plan.plausibleShutterCount
import dev.bondarenko.fujirecipes.camera.plan.usbModeFor
import dev.bondarenko.fujirecipes.camera.plan.usbModeFrom
import dev.bondarenko.fujirecipes.camera.ptp.DeviceInfo
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.unpackPtpString
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.ptp.unpackU32

/**
 * Asks a connected body what mode it is in and what it will say about itself.
 *
 * Both readers run once, at connect, straight after `GetDeviceInfo`. Four round trips on a link
 * where reading one slot is dozens, so the cost is not worth optimising; what *is* worth being
 * careful about is that **neither of these may break a connection that would otherwise work.**
 * Every failure below is swallowed and reported as "not answered", including the ones that are
 * not refusals — a timeout here means the pipe is already gone and the very next operation will
 * say so properly, and failing the connect on a diagnostic read would turn a working camera
 * into an unusable one for the sake of a battery percentage.
 *
 * **`GetDevicePropDesc` first, `GetDevicePropValue` second.** The description carries the data
 * type *and* the current value in one dataset, so a body that answers it needs no guess about
 * width or signedness — which is what `PtpSession.describeProperty` was written for and, until
 * now, the reason nothing called it. A body that refuses the description falls back to reading
 * the raw value and decoding by payload length: still a guess, but a narrow one.
 *
 * Blocking, like everything else on a session. Callers run it on `Dispatchers.IO`.
 */

/**
 * Which USB mode the camera's menu is set to.
 *
 * A refusal is [UsbMode.UNREPORTED] rather than an error: a body whose firmware predates the
 * property lands there, and that is not a fault. Prefer the two-argument overload, which can
 * also name card-reader mode — this one has only the property to go on.
 */
fun readUsbMode(session: PtpSession): UsbMode =
    readUsbModeRaw(session)?.let(::usbModeFor) ?: UsbMode.UNREPORTED

/**
 * The same, with `GetDeviceInfo` as a second source.
 *
 * A card-reader body refuses `0xD16E` and always will, so the property alone can never name
 * that mode. The MTP signature in the device info can, at no extra cost — the session read it
 * when it opened. The property still wins wherever it answers; the signature is consulted only
 * on a refusal, and can only ever conclude "card reader" or "still could not tell".
 */
fun readUsbMode(session: PtpSession, deviceInfo: DeviceInfo?): UsbMode = usbModeFrom(
    reportedValue = readUsbModeRaw(session),
    operationsSupported = deviceInfo?.operationsSupported.orEmpty(),
    devicePropertiesSupported = deviceInfo?.devicePropertiesSupported.orEmpty(),
)

/**
 * The raw `0xD16E` value, or null.
 *
 * The report needs the number as well as the name, so that a body answering a mode this build
 * has not seen is recorded as the value it actually gave rather than only as "unrecognised" —
 * the number is the whole finding in that case.
 */
fun readUsbModeRaw(session: PtpSession): Int? =
    readNumber(session, USB_MODE_PROPERTY)?.toInt()

/**
 * What the body will report about itself.
 *
 * [deviceInfo] supplies firmware and serial, which the session already has in hand from its own
 * `GetDeviceInfo` and which cost nothing to carry through. The other three cost a round trip
 * each and are each independently optional.
 */
fun readCameraDetails(session: PtpSession, deviceInfo: DeviceInfo?): CameraDetails =
    CameraDetails(
        battery = readBattery(session),
        shutterCount = readNumber(session, TOTAL_SHOT_COUNT_PROPERTY)
            ?.let(::plausibleShutterCount),
        lens = readText(session, LENS_NAME_PROPERTY)?.let(::plausibleLensName),
        firmware = deviceInfo?.deviceVersion?.trim()?.ifBlank { null },
        serialNumber = deviceInfo?.serialNumber?.trim()?.ifBlank { null },
    )

/**
 * The battery, from whichever of the two properties this body has.
 *
 * **Both are tried because an X-T50 has them in opposite modes**: `0x5001` answers in
 * card-reader mode and is absent in RAW conversion mode, while `0xD36A` is the other way round.
 * The standard one is asked first, because where it can be described it declares its own scale
 * — and that scale turned out to be 0–10, which is the whole reason this returns a level rather
 * than a percentage.
 */
private fun readBattery(session: PtpSession): BatteryLevel? =
    readBatteryFrom(session, STANDARD_BATTERY_PROPERTY)
        ?: readBatteryFrom(session, BATTERY_LEVEL_PROPERTY)

private fun readBatteryFrom(session: PtpSession, code: Int): BatteryLevel? {
    val described = runCatching { session.describeProperty(code) }.getOrNull()
    if (described != null) {
        val value = described.currentValue as? Long ?: return null
        return plausibleBatteryLevel(value, described.range?.max)
    }

    // No description, so no declared scale either. The assumed maximum carries a measurement
    // behind it (see ASSUMED_BATTERY_MAX) and is marked as assumed in the result.
    val raw = rawValue(session, code)?.let(::numberFromBytes) ?: return null
    return plausibleBatteryLevel(raw)
}

// ─── Reading one property ───────────────────────────────────────────────────

/**
 * One property as an unsigned number, or null if the body would not say.
 *
 * The description's `currentValue` is preferred because it is typed by the body's own declared
 * `dataType`. It is null there for an array or a 64-bit scalar, and that null is answered as
 * null rather than falling through to a raw read that would decode the same bytes worse.
 */
private fun readNumber(session: PtpSession, code: Int): Long? {
    val described = runCatching { session.describeProperty(code) }
    if (described.isSuccess) return described.getOrNull()?.currentValue as? Long

    return rawValue(session, code)?.let(::numberFromBytes)
}

/**
 * Width by payload length.
 *
 * The body sent exactly as many bytes as the property holds, so this is right whenever the
 * payload is a plain unsigned scalar — and every property read this way is one. Anything else
 * (a struct, a packed pair, a string) has no length this recognises and is dropped rather than
 * read as a number.
 */
internal fun numberFromBytes(bytes: ByteArray): Long? = when (bytes.size) {
    1 -> (bytes[0].toInt() and 0xff).toLong()
    2 -> unpackU16(bytes).toLong()
    4 -> unpackU32(bytes)
    else -> null
}

/** One property as a PTP string, or null if the body would not say. */
private fun readText(session: PtpSession, code: Int): String? {
    val described = runCatching { session.describeProperty(code) }
    if (described.isSuccess) return described.getOrNull()?.currentValue as? String

    val bytes = rawValue(session, code) ?: return null
    return runCatching { unpackPtpString(bytes) }.getOrNull()
}

private fun rawValue(session: PtpSession, code: Int): ByteArray? =
    runCatching { session.readPropertyBytes(code) }.getOrNull()
