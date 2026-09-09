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
     * `USB CARD READER` — the body is an MTP mass-storage device over the still-image class.
     *
     * Not one of `libfuji`'s `FujiUSBModes` values, and it never can be: a body in this mode
     * refuses `0xD16E` outright. It is identified from `GetDeviceInfo` instead — see
     * [looksLikeCardReader] — which is why it is the one mode named without the property
     * answering. The card is browsable here and the preset block is not.
     */
    CARD_READER,

    /**
     * The body refused the property and did not look like a card reader either.
     *
     * Says nothing about the mode — a body whose firmware does not carry `0xD16E` lands here.
     * Never presented as a diagnosis.
     */
    UNREPORTED,

    /** The body answered a value this build has not seen. Reported as the number, not guessed. */
    UNRECOGNISED,
    ;

    /**
     * Whether this mode is known to be the wrong one *for the preset block*.
     *
     * Only positively-identified modes are true. [UNREPORTED] and [UNRECOGNISED] are
     * deliberately false — "we could not tell" is not "you are in the wrong mode", and showing
     * a mode warning to someone whose camera is set correctly is worse than showing nothing.
     *
     * [CARD_READER] is true because the slots really are unreachable there, even though it is
     * the *right* mode for browsing the card. Wrong is relative to the recipe slots, which is
     * what the banner carrying this is about.
     */
    val isKnownWrongMode: Boolean
        get() = this == TETHER_SHOOTING || this == WEBCAM || this == CARD_READER

    /** Whether the camera card can be browsed. The photo screen's precondition. */
    val allowsCardBrowsing: Boolean
        get() = this == CARD_READER

    /** Whether the preset block and the settings blob are reachable. */
    val allowsRecipeWork: Boolean
        get() = this == RAW_CONVERSION
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

/**
 * The MTP object-property operations, which a still-image camera in its own mode does not
 * advertise. Verified against `petabyt/libpict` `src/ptp.h:118-122`; an X-T50 in card-reader
 * mode advertises `0x9801`-`0x9803` and `0x9805`, and notably not `0x9804`.
 */
private val MTP_OBJECT_PROPERTY_OPERATIONS = listOf(0x9801, 0x9802, 0x9803)

/** `SessionInitiatorVersionInfo` and `PerceivedDeviceType`: the two MTP device properties. */
private const val MTP_SESSION_INITIATOR_VERSION_INFO = 0xd406
private const val MTP_PERCEIVED_DEVICE_TYPE = 0xd407

/**
 * Whether this body is answering as an MTP device rather than as a camera.
 *
 * Both device properties **and** the read-only object-property operations are required. Either
 * signal alone is thin; together they are what an X-T50 in `USB CARD READER` advertises and
 * what the same body in `USB RAW CONV./BACKUP RESTORE` does not.
 *
 * This is a positive identification or nothing. A body that fails the test is not thereby in
 * any other mode — it is simply one this build cannot name, which is [UsbMode.UNREPORTED].
 */
fun looksLikeCardReader(
    operationsSupported: List<Int>,
    devicePropertiesSupported: List<Int>,
): Boolean {
    val hasMtpProperties = MTP_SESSION_INITIATOR_VERSION_INFO in devicePropertiesSupported &&
        MTP_PERCEIVED_DEVICE_TYPE in devicePropertiesSupported
    val hasMtpOperations = MTP_OBJECT_PROPERTY_OPERATIONS.all { it in operationsSupported }

    return hasMtpProperties && hasMtpOperations
}

/**
 * The mode, from the property where the body answers it and from `GetDeviceInfo` where it does
 * not.
 *
 * The property wins whenever it answers: it is the body's own statement about its menu, and
 * `GetDeviceInfo` is an inference. The inference only ever runs on a refusal, and only ever
 * produces [UsbMode.CARD_READER] or [UsbMode.UNREPORTED] — never one of the values `0xD16E`
 * would have given, because guessing those would put a fabrication where a fact belongs.
 */
fun usbModeFrom(
    reportedValue: Int?,
    operationsSupported: List<Int>,
    devicePropertiesSupported: List<Int>,
): UsbMode = when {
    reportedValue != null -> usbModeFor(reportedValue)
    looksLikeCardReader(operationsSupported, devicePropertiesSupported) -> UsbMode.CARD_READER
    else -> UsbMode.UNREPORTED
}
