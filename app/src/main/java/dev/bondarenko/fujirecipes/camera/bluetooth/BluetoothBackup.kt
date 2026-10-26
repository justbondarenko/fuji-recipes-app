package dev.bondarenko.fujirecipes.camera.bluetooth

import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import java.util.zip.Inflater

/** Measured X-T50 1.32 layout; see specs/plans/bluetooth-recipes.md. No camera I/O or encoder. */
object BluetoothBackup {
    const val SIZE = 70524
    private const val START = 63356
    private const val STRIDE = 1024
    private val simulations = mapOf(
        1 to "provia", 4 to "velvia", 2 to "astia", 14 to "pro-neg-hi", 13 to "pro-neg-std",
        9 to "monochrome", 11 to "monochrome-ye", 10 to "monochrome-r", 12 to "monochrome-g",
        6 to "sepia", 15 to "classic-chrome", 22 to "acros", 24 to "acros-ye", 23 to "acros-r",
        25 to "acros-g", 19 to "eterna", 16 to "classic-negative", 20 to "eterna-bleach-bypass",
        17 to "nostalgic-negative", 18 to "reala-ace",
    )
    private val wbModes = mapOf(
        0 to "auto", 2 to "auto-ambience-priority", 3 to "daylight", 4 to "shade",
        5 to "fluorescent-1", 6 to "fluorescent-2", 7 to "fluorescent-3",
        8 to "incandescent", 9 to "underwater", 10 to "color-temp",
    )
    private val wbBanks = mapOf(0 to 116, 2 to 124, 3 to 144, 4 to 148, 5 to 152,
        6 to 156, 7 to 160, 8 to 164, 9 to 168, 10 to 140)
    private val colors = mapOf(0 to 0, 3 to 4, 4 to 3, 5 to 2, 6 to 1, 7 to -2, 8 to -1, 9 to -3, 10 to -4)

    /** Names remain blank here so the slot grid can distinguish an unnamed slot. */
    fun decode(raw: ByteArray): List<SlotRecipe> {
        require(raw.size == SIZE && raw.copyOfRange(0, 16).contentEquals("FUJIFILMX-BACKUP".toByteArray()) &&
            raw.copyOfRange(20, 25).toString(Charsets.US_ASCII) == "X-T50") {
            "Unsupported Bluetooth backup format. This build reads X-T50 firmware 1.32."
        }
        return (1..7).map { slot ->
            val start = START + (slot - 1) * STRIDE
            fun code(offset: Int) = raw[start + offset].toInt() and 0xff
            fun ranged(offset: Int, range: IntRange, field: String): Int = code(offset).also {
                require(it in range) { "Unsupported $field code $it in C$slot." }
            }
            fun <T : Any> mapped(offset: Int, values: Map<Int, T>, field: String): T =
                requireNotNull(values[code(offset)]) { "Unsupported $field code ${code(offset)} in C$slot." }
            val wb = mapped(114, wbModes, "white balance")
            val bank = wbBanks.getValue(code(114))
            val nameBytes = raw.copyOfRange(start + 622, start + 648)
            val end = nameBytes.indexOf(0)
            require(end in 0..25 && nameBytes.take(end).all { (it.toInt() and 0xff) in 32..126 }) {
                "Unsupported name encoding in C$slot. This build reads ASCII camera names."
            }
            val kelvin = unpackU16(raw, start + 16)
            require(kelvin in 2500..10000) { "Invalid color temperature in C$slot." }
            SlotRecipe(slot, nameBytes.copyOf(end).toString(Charsets.US_ASCII), linkedMapOf(
                "filmSimulation" to mapped(229, simulations, "film simulation"),
                "dynamicRange" to mapped(245, mapOf(1 to "dr100", 2 to "dr200", 3 to "dr400"), "dynamic range"),
                "whiteBalance" to wb,
                "colorTemperature" to kelvin,
                "wbShiftRed" to (9 - ranged(bank, 0..18, "WB red shift")),
                "wbShiftBlue" to (9 - ranged(bank + 2, 0..18, "WB blue shift")),
                "color" to mapped(248, colors, "color"),
                "sharpness" to (4 - ranged(250, 0..8, "sharpness")),
                "highIsoNR" to (ranged(172, 0..8, "high ISO NR") - 4),
                "clarity" to (ranged(175, 1..11, "clarity") - 6),
                "highlightTone" to (ranged(252, 0..12, "highlight tone") / 2.0 - 2),
                "shadowTone" to (ranged(254, 0..12, "shadow tone") / 2.0 - 2),
                "colorChromeEffect" to mapped(256, mapOf(0 to "off", 1 to "weak", 2 to "strong"), "Color Chrome"),
                "colorChromeFxBlue" to mapped(257, mapOf(0 to "off", 1 to "weak", 2 to "strong"), "Color Chrome Blue"),
                "grainEffect" to mapped(258, mapOf(0 to "strong", 1 to "weak", 2 to "off"), "grain effect"),
                "grainSize" to mapped(259, mapOf(0 to "small", 1 to "large"), "grain size"),
            ))
        }
    }

    fun inflate(compressed: ByteArray, declaredSize: Int, checksum: Int): ByteArray {
        require(declaredSize == SIZE) { "Unsupported Bluetooth backup length: $declaredSize." }
        require(compressed.isNotEmpty() && compressed.size <= SIZE &&
            compressed.sumOf { it.toInt() and 0xff } and 0xffff == checksum) { "Bluetooth backup checksum failed." }
        val result = ByteArray(SIZE + 1)
        val inflater = Inflater()
        try {
            inflater.setInput(compressed)
            var written = 0
            while (!inflater.finished() && written < result.size) {
                val count = inflater.inflate(result, written, result.size - written)
                require(count > 0 || inflater.finished()) { "Incomplete Bluetooth backup." }
                written += count
            }
            require(inflater.finished() && inflater.remaining == 0 && written == SIZE) { "Bluetooth backup compression or length failed." }
            return result.copyOf(SIZE)
        } finally {
            inflater.end()
        }
    }
}
