package dev.bondarenko.fujirecipes.camera.plan

/**
 * Which USB mode the camera's own menu is set to.
 *
 * **Source.** `petabyt/libfuji`'s `lib/fujiptp.h` (`enum FujiUSBModes`) and `lib/fuji_usb.c`
 * `fujiusb_setup`, which reads exactly this property and branches on exactly these three
 * values. Not in `field-definitions.md`, which is only concerned with the preset block.
 *
 * **Why this matters here.** Everything this app does — reading C1–C7, writing a slot — needs
 * the body in *USB RAW CONV./BACKUP RESTORE*. In any other mode the session still opens and
 * `GetDeviceInfo` still answers, so the app connects, names the body, and then fails on the
 * first slot read with a bare `DevicePropNotSupported`. `coding-standards.md` P5 calls that
 * out by name: a wrong USB mode and a busy interface must not render as the same failure.
 * Reading `0xD16E` is what lets the screen say *which* mode the camera is actually in.
 *
 * **This is advisory and must stay advisory.** It does not gate the write path — that is
 * [dev.bondarenko.fujirecipes.camera.ModelIdentity.writable]'s job (WR-01), decided from the
 * body's sensor generation. A body that happens not to report `0xD16E` while sitting in the
 * right mode would otherwise lose the ability to write for no reason, so an unreported or
 * unrecognised mode says nothing at all rather than guessing.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

/** `PTP_DPC_FUJI_USBMode`. Read-only in practice; this app never writes it. */
const val USB_MODE_PROPERTY = 0xd16e

enum class UsbMode {
    /** `USB TETHER SHOOTING FIXED/AUTO` — the remote-capture mode. No preset block. */
    TETHER_SHOOTING,

    /** `USB RAW CONV./BACKUP RESTORE` — the one mode this app works in. */
    RAW_CONVERSION,

    /** `X WEBCAM` — the body is a UVC source and answers almost nothing else. */
    WEBCAM,

    /**
     * The body refused the property, or the read failed.
     *
     * Says nothing about the mode: card-reader/MTP mode lands here, and so does any body whose
     * firmware does not carry `0xD16E`. Never presented as a diagnosis.
     */
    UNREPORTED,

    /** The body answered a value this build has not seen. Reported as the number, not guessed. */
    UNRECOGNISED,
    ;

    /**
     * Whether this mode is known to be the wrong one.
     *
     * Only the two positively-identified wrong modes are true. [UNREPORTED] and [UNRECOGNISED]
     * are deliberately false — "we could not tell" is not "you are in the wrong mode", and
     * showing a mode warning to someone whose camera is set correctly is worse than showing
     * nothing.
     */
    val isKnownWrongMode: Boolean
        get() = this == TETHER_SHOOTING || this == WEBCAM
}

/** The raw values `0xD16E` answers with, per `libfuji`'s `enum FujiUSBModes`. */
private const val MODE_TETHER = 5
private const val MODE_RAW_CONVERSION = 6
private const val MODE_WEBCAM = 8

fun usbModeFor(value: Int): UsbMode = when (value) {
    MODE_TETHER -> UsbMode.TETHER_SHOOTING
    MODE_RAW_CONVERSION -> UsbMode.RAW_CONVERSION
    MODE_WEBCAM -> UsbMode.WEBCAM
    else -> UsbMode.UNRECOGNISED
}
