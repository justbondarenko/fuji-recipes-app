package dev.bondarenko.fujirecipes.camera.plan

/**
 * The read-only facts a body will report about itself, and the properties that carry them.
 *
 * **Sources.** `petabyt/libfuji`'s `lib/fujiptp.h` for the vendor codes; ISO 15740 for
 * `0x5001`. Only `0xD36A` has code written against it in that project
 * (`fuji_get_battery_percent`); `0xD310` and `0xD36D` are names in a header and nothing more.
 *
 * **What an X-T50 (firmware 1.32) actually reports**, from two diagnostic runs on hardware:
 *
 * | property | card reader | RAW conv. |
 * |---|---|---|
 * | `0x5001` battery | `UInt8`, `10`, declared `[0..10 step 1]` | absent |
 * | `0xD36A` battery | refused | `UInt32`, `10` |
 * | `0xD310` shutter count | refused | refused |
 * | `0xD36D` lens | refused | refused |
 *
 * Three things follow, and they are why this file looks the way it does. The two battery
 * properties are **present in opposite modes**, so both are tried. The standard one **declares
 * its own scale and that scale is 0–10, not 0–100** — so a level is not a percentage, and
 * calling it one turned a full battery into "10%". And shutter count and lens **do not exist on
 * this body at all**, in either mode, so their absence is the normal case rather than a fault.
 *
 * Everything here is still treated as a claim to be checked: a value outside the range its name
 * implies means this build has the wrong code, and it says nothing rather than printing a
 * plausible wrong number — the rule `CameraEncoding.kt` and `DeviceInfo.kt` already apply to
 * the preset block.
 *
 * **Firmware and serial are not in here** — they come free in the `GetDeviceInfo` dataset the
 * session already reads, are part of ISO 15740 rather than a vendor guess, and so need no round
 * trip and no plausibility check.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

/** `PTP_DPC_BatteryLevel`, ISO 15740. Tried first: it is the one that declares its own scale. */
const val STANDARD_BATTERY_PROPERTY = 0x5001

/** `PTP_DPC_FUJI_BatteryInfo1`. The only battery property an X-T50 has in RAW conversion mode. */
const val BATTERY_LEVEL_PROPERTY = 0xd36a

/** `PTP_DPC_FUJI_TotalShotCount`. Refused by an X-T50 in both modes; kept for other bodies. */
const val TOTAL_SHOT_COUNT_PROPERTY = 0xd310

/** `PTP_DPC_FUJI_LensNameAndSerial`. Also refused by an X-T50 in both modes. */
const val LENS_NAME_PROPERTY = 0xd36d

/**
 * The top of the battery scale to assume when the body will not declare one.
 *
 * Ten, because that is what the same body's `0x5001` declared in the mode where it could be
 * asked, and because `0xD36A` read exactly ten at the same charge. An assumption, and marked as
 * one in [BatteryLevel.maxDeclared] — but an assumption with a measurement behind it, which is
 * a different thing from a guess.
 */
const val ASSUMED_BATTERY_MAX = 10

/**
 * A battery reading, and the scale it is on.
 *
 * Deliberately not a percentage. The one body this has been run against reports a level out of
 * ten, and the whole reason this type exists is that rendering that as "10%" was wrong in the
 * most misleading possible direction.
 */
data class BatteryLevel(
    val value: Int,
    val max: Int,
    /** Whether [max] is the body's own declared maximum rather than [ASSUMED_BATTERY_MAX]. */
    val maxDeclared: Boolean,
) {
    /** Whether the scale is such that showing a percentage is honest. */
    val isPercentage: Boolean get() = max == 100
}

/**
 * A battery level, or null.
 *
 * [declaredMax] is the maximum the body itself gave in its `GetDevicePropDesc`, when it gave
 * one. A value above the top of its own scale means the property is not a battery level on this
 * body, and the honest answer is then to show nothing.
 */
fun plausibleBatteryLevel(raw: Long, declaredMax: Long? = null): BatteryLevel? {
    val max = declaredMax ?: ASSUMED_BATTERY_MAX.toLong()
    if (max !in 1L..100L) return null
    if (raw !in 0L..max) return null

    return BatteryLevel(
        value = raw.toInt(),
        max = max.toInt(),
        maxDeclared = declaredMax != null,
    )
}

/** What the body said about itself. Every field is optional; absent means *not reported*. */
data class CameraDetails(
    val battery: BatteryLevel? = null,
    val shutterCount: Int? = null,
    val lens: String? = null,
    /** `GetDeviceInfo`'s `deviceVersion` — the body's firmware. */
    val firmware: String? = null,
    /** `GetDeviceInfo`'s `serialNumber`. */
    val serialNumber: String? = null,
) {
    /** Whether the body reported nothing at all, so the UI can omit the section entirely. */
    val isEmpty: Boolean
        get() = battery == null &&
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
        get() = battery != null || shutterCount != null || lens != null
}

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
