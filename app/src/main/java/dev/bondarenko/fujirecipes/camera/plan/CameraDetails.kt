package dev.bondarenko.fujirecipes.camera.plan

/**
 * The read-only facts a body will report about itself, and the properties that carry them.
 *
 * **Source.** `petabyt/libfuji`'s `lib/fujiptp.h`, which names ~200 Fuji vendor properties.
 * Only `0xD36A` has a helper written against it there (`fuji_get_battery_percent`); the other
 * two are named in the header and nothing more. That difference is the whole reason for the
 * plausibility guards at the bottom of this file.
 *
 * **Nothing here is verified on hardware.** `field-definitions.md` covers the preset block and
 * says nothing about these, and no capture in this project's provenance chain touched them. So
 * every value is treated as a claim to be checked rather than a number to be shown: a property
 * that answers, answers *something*, and if that something is outside the range the name
 * implies then this build has the wrong code and says nothing rather than printing a plausible
 * wrong number — the same rule `CameraEncoding.kt` and `DeviceInfo.kt` already apply to the
 * preset block.
 *
 * **Firmware and serial are not in here** — they come free in the `GetDeviceInfo` dataset the
 * session already reads, are part of ISO 15740 rather than a vendor guess, and so need no
 * round trip and no plausibility check.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

/** `PTP_DPC_FUJI_BatteryInfo1`. The one property below that `libfuji` actually reads. */
const val BATTERY_LEVEL_PROPERTY = 0xd36a

/** `PTP_DPC_FUJI_TotalShotCount`. Named in `fujiptp.h`; no code in that project reads it. */
const val TOTAL_SHOT_COUNT_PROPERTY = 0xd310

/** `PTP_DPC_FUJI_LensNameAndSerial`. A string property, and the only string of the three. */
const val LENS_NAME_PROPERTY = 0xd36d

/**
 * What the body said about itself. Every field is optional and absent means *not reported* —
 * either the property was refused, or it answered something this build will not stand behind.
 */
data class CameraDetails(
    /** 0–100, as a percentage. */
    val batteryPercent: Int? = null,
    val shutterCount: Int? = null,
    val lens: String? = null,
    /** `GetDeviceInfo`'s `deviceVersion` — the body's firmware. */
    val firmware: String? = null,
    /** `GetDeviceInfo`'s `serialNumber`. */
    val serialNumber: String? = null,
) {
    /** Whether the body reported nothing at all, so the UI can omit the section entirely. */
    val isEmpty: Boolean
        get() = batteryPercent == null &&
            shutterCount == null &&
            lens == null &&
            firmware == null &&
            serialNumber == null

    /**
     * Whether any field came from a reverse-engineered vendor property rather than from
     * `GetDeviceInfo`, so the screen can footnote exactly those and not the two that are
     * standard PTP.
     */
    val hasUnverifiedFields: Boolean
        get() = batteryPercent != null || shutterCount != null || lens != null
}

/**
 * A battery percentage, or null.
 *
 * `libfuji` reads this property and calls the result a percentage, so 0–100 is the range the
 * name implies. Anything else means `0xD36A` holds something other than a percentage on this
 * body — bars, a level enum, a packed struct — and the honest answer is then to show nothing.
 */
fun plausibleBatteryPercent(raw: Long): Int? =
    if (raw in 0L..100L) raw.toInt() else null

/**
 * A shutter actuation count, or null.
 *
 * Zero is excluded along with the negatives: a body that has never fired its shutter does not
 * exist in a user's hands, so a zero here is a property that means something else. The ceiling
 * is an order of magnitude past any real body's rated life, which is enough to reject a value
 * that is actually a bit pattern.
 */
fun plausibleShutterCount(raw: Long): Int? =
    if (raw in 1L..9_999_999L) raw.toInt() else null

/**
 * A lens name, or null.
 *
 * `LensNameAndSerial` is a name and a serial in one string on the bodies `fujiptp.h` was
 * written from, and this shows it as the camera gives it rather than trying to split a format
 * nobody here has seen. Control characters mean the payload was not really a PTP string.
 */
fun plausibleLensName(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.any { it.isISOControl() }) return null
    return trimmed
}
