package dev.bondarenko.fujirecipes.camera.plan

/**
 * What a body says about itself, in one shareable page of text.
 *
 * **The problem this solves.** Every encoding in `CameraEncoding.kt` and every row in
 * `CameraModels.kt` came from captures of one X100VI. Bodies outside that set are refused a
 * write not because they cannot take one but because nobody has checked. The way out is not to
 * buy more cameras: it is for a body's *own* answers to `GetDevicePropDesc` — declared type,
 * writable flag, allowed range, allowed enumeration — to reach whoever maintains the tables.
 * That is what this report is, and it is why the shape matters more than the values.
 *
 * **The serial number is deliberately absent.** A report is written to be pasted into a public
 * issue. Model and firmware are what make it useful; the serial identifies the owner's
 * hardware and would make a stranger's bug report carry something it has no reason to carry.
 * The app shows it on the camera screen, where it stays local.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest` — and no `camera/ptp/` either,
 * which is the rule `StepPayload.kt` states from the other side: `camera/plan/` stays reachable
 * without the byte layer. So a property's data type arrives here already named, and the allowed
 * values arrive as the bounds themselves rather than as a `PropertyForm`. Rendering is then a
 * plain function of the model and can be asserted character-for-character.
 */

/** How far a single property got when the report asked about it. */
enum class ProbeOutcome {
    /** `GetDevicePropDesc` answered — type, writable and allowed values are all known. */
    DESCRIBED,

    /** Only `GetDevicePropValue` answered. The bytes are recorded; their meaning is not. */
    VALUE_ONLY,

    /** The body would not answer either. The response code says which refusal it was. */
    REFUSED,
}

data class ProbedProperty(
    val code: Int,
    /** Whether `GetDeviceInfo`'s own property list named this code. */
    val listedByBody: Boolean,
    val outcome: ProbeOutcome,
    /** The declared type, already named by the byte layer — `UInt16`, `String`, `UInt8[]`. */
    val dataTypeLabel: String? = null,
    val writable: Boolean? = null,
    /** The current value, already rendered — a number, a quoted string, or raw hex. */
    val value: String? = null,
    /** The allowed values, already rendered: `1..7 step 1`, or `1, 2, 3`. */
    val allowed: String? = null,
    /** The response name, when [outcome] is [ProbeOutcome.REFUSED]. */
    val refusal: String? = null,
)

/**
 * One custom slot, read with the selector pointed at it.
 *
 * The preset registers follow the selector, so a single reading of them is one recipe rather
 * than a statement about the body. Seven readings laid side by side are a different thing: a
 * value that is identical across all seven is a constant the camera keeps there, and one that
 * varies is a setting. That distinction is not obtainable from one slot, and it is what settles
 * codes like `0xD1A1`, which `eggricesoy/filmkit` calls a sentinel and this project's own table
 * calls a noise-reduction encoding.
 */
data class SlotProbe(
    val slot: Int,
    /** The camera's own name for the slot, or null when it would not give one. */
    val name: String?,
    /** Preset code to its raw 16-bit value, or null where the body refused it. */
    val values: Map<Int, Int?>,
)

data class CameraReport(
    val appVersion: String,
    /** Supplied by the caller; the pure layer has no clock. */
    val capturedAt: String,
    val manufacturer: String,
    val model: String,
    val firmware: String,
    val standardVersion: Int,
    val vendorExtensionId: Long,
    val vendorExtensionVersion: Int,
    val vendorExtensionDescription: String,
    val usbMode: UsbMode,
    /** The raw `0xD16E` value, so an unrecognised mode is reported as its number. */
    val usbModeRaw: Int?,
    val operationsSupported: List<Int> = emptyList(),
    val eventsSupported: List<Int> = emptyList(),
    val propertiesListed: List<Int> = emptyList(),
    val captureFormats: List<Int> = emptyList(),
    val imageFormats: List<Int> = emptyList(),
    /** Which custom slot the selector was pointed at while [properties] were read. */
    val selectedSlot: Int?,
    /** Everything outside the preset block, walked once. */
    val properties: List<ProbedProperty> = emptyList(),
    /** The preset block, walked once per slot. Empty when the body has no selector. */
    val slots: List<SlotProbe> = emptyList(),
)

// ─── Rendering ──────────────────────────────────────────────────────────────

private const val NOT_REPORTED = "not reported"

fun renderCameraReport(report: CameraReport): String = buildString {
    appendLine("Fuji Recipes — camera report")
    appendLine("app ${report.appVersion} · captured ${report.capturedAt}")
    appendLine()

    appendLine("DEVICE")
    appendLine(pair("manufacturer", report.manufacturer))
    appendLine(pair("model", report.model))
    appendLine(pair("firmware", report.firmware))
    appendLine(pair("PTP standard", report.standardVersion.toString()))
    appendLine(
        pair(
            "vendor extension",
            "${report.vendorExtensionId} v${report.vendorExtensionVersion} " +
                "“${report.vendorExtensionDescription}”",
        ),
    )
    appendLine(pair("USB mode", usbModeLine(report)))
    appendLine(pair("slot selector", report.selectedSlot?.let { "C$it" } ?: NOT_REPORTED))
    appendLine(pair("serial", "omitted on purpose"))
    appendLine()

    appendCodes("OPERATIONS SUPPORTED", report.operationsSupported, ::operationName)
    appendCodes("EVENTS SUPPORTED", report.eventsSupported) { null }
    appendCodes("CAPTURE FORMATS", report.captureFormats) { null }
    appendCodes("IMAGE FORMATS", report.imageFormats) { null }

    appendLine(
        "PROPERTIES (${report.properties.size} probed, " +
            "${report.propertiesListed.size} listed by the body)",
    )
    if (report.properties.isEmpty()) {
        appendLine("  none")
    } else {
        report.properties.forEach { appendLine(propertyLine(it)) }
    }
    appendLine()

    appendSlots(report.slots)

    appendLine("A code with no name is one this build will not guess at; its declared type and")
    appendLine("allowed values above are what identify it. “·” marks a code the body did not")
    appendLine("list in GetDeviceInfo but answered for anyway. In the slot table a value is the")
    appendLine("raw 16 bits the body returned, and “————” means it refused that code for that slot;")
    appendLine("a row identical across all seven slots is a constant, not a setting.")
}

/**
 * The preset block, one column per slot.
 *
 * A matrix rather than seven blocks: the question these registers have to answer is whether a
 * code varies between slots, and that is a question about a row. Seven separate listings put
 * the seven readings of `0xD1A1` a hundred and eighty lines apart.
 */
private fun StringBuilder.appendSlots(slots: List<SlotProbe>) {
    if (slots.isEmpty()) {
        appendLine("CUSTOM SLOTS (0)")
        appendLine("  none — the body would not take the slot selector")
        appendLine()
        return
    }

    appendLine("CUSTOM SLOT NAMES")
    slots.forEach { slot ->
        appendLine("  C${slot.slot}  " + (slot.name?.let { "“$it”" } ?: "(unnamed)"))
    }
    appendLine()

    appendLine("CUSTOM SLOTS (${slots.size}) — raw 16-bit value per slot")
    append("  ").append("code".padEnd(8)).append("name".padEnd(28))
    slots.forEach { append("C${it.slot}".padEnd(6)) }
    appendLine()

    PRESET_MATRIX_CODES.forEach { code ->
        append("  ").append(hex(code).padEnd(8)).append((propertyName(code) ?: "").padEnd(28))
        slots.forEach { slot ->
            val value = slot.values[code]
            append((value?.let { raw16(it) } ?: "————").padEnd(6))
        }
        appendLine()
    }
    appendLine()
}

/** A slot value as the four hex digits the body actually returned. */
private fun raw16(value: Int): String =
    (value and 0xffff).toString(16).uppercase().padStart(4, '0')

private fun usbModeLine(report: CameraReport): String {
    val name = when (report.usbMode) {
        UsbMode.TETHER_SHOOTING -> "tether shooting"
        UsbMode.RAW_CONVERSION -> "RAW conv./backup restore"
        UsbMode.WEBCAM -> "webcam"
        // Inferred from the MTP signature, not read from 0xD16E, so there is no number to
        // print beside it — `usbModeRaw` is null by definition here.
        UsbMode.CARD_READER -> "card reader (inferred from the MTP signature)"
        UsbMode.UNRECOGNISED -> "unrecognised"
        UsbMode.UNREPORTED -> return NOT_REPORTED
    }
    return report.usbModeRaw?.let { "$it ($name)" } ?: name
}

private fun pair(label: String, value: String): String =
    "  " + label.padEnd(18) + value

private fun StringBuilder.appendCodes(
    heading: String,
    codes: List<Int>,
    nameOf: (Int) -> String?,
) {
    appendLine("$heading (${codes.size})")
    if (codes.isEmpty()) {
        appendLine("  none")
    } else {
        codes.forEach { code ->
            appendLine("  " + hex(code) + (nameOf(code)?.let { "  $it" } ?: ""))
        }
    }
    appendLine()
}

private fun propertyLine(property: ProbedProperty): String {
    val marker = if (property.listedByBody) " " else "·"
    val name = propertyName(property.code) ?: ""

    val detail = when (property.outcome) {
        ProbeOutcome.REFUSED -> "refused: ${property.refusal ?: "unknown"}"

        ProbeOutcome.VALUE_ONLY ->
            "value ${property.value ?: NOT_REPORTED}  (no description)"

        ProbeOutcome.DESCRIBED -> buildString {
            append(property.dataTypeLabel ?: "?")
            append(if (property.writable == true) "  rw" else "  r ")
            append("  ")
            append(property.value ?: NOT_REPORTED)
            property.allowed?.let { append("  [$it]") }
        }
    }

    return "$marker ${hex(property.code)}  ${name.padEnd(28)}$detail"
}

private fun hex(code: Int): String =
    "0x" + (code and 0xffff).toString(16).uppercase().padStart(4, '0')

/**
 * A described property's allowed values, or null when it declares none.
 *
 * Takes the bounds rather than a `PropertyForm` so that this file needs nothing from
 * `camera/ptp/`: a property that declares a range has [min] and [max], one that declares an
 * enumeration has [enumeration], and one that declares neither has nothing to render.
 *
 * An enumeration is truncated. Some bodies enumerate several hundred shutter speeds, and a
 * report nobody scrolls to the end of is worse than one that says how many it left out.
 */
fun renderAllowedValues(
    min: Long? = null,
    max: Long? = null,
    step: Long? = null,
    enumeration: List<Any>? = null,
    limit: Int = ENUM_RENDER_LIMIT,
): String? {
    if (min != null && max != null) return "$min..$max step ${step ?: 1}"

    val values = enumeration.orEmpty()
    return when {
        values.isEmpty() -> null
        values.size <= limit -> values.joinToString(", ")
        else -> values.take(limit).joinToString(", ") + ", … ${values.size - limit} more"
    }
}

const val ENUM_RENDER_LIMIT = 24
