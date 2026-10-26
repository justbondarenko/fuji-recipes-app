package dev.bondarenko.fujirecipes.camera.bluetooth

import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import dev.bondarenko.fujirecipes.camera.ptp.unpackU32
import org.junit.Assert.*
import org.junit.Test

class BluetoothBackupTest {
    private fun fixture() = javaClass.getResourceAsStream("/bluetooth/xt50-132-sanitized.dat")!!.use { it.readBytes() }

    @Test fun c7EncoderMatchesIndependentMeasurementsAndPreservesEveryOtherByte() {
        val measured = javaClass.getResourceAsStream("/bluetooth/xt50-132-c7-measured.json")!!.bufferedReader().use {
            Json.parseToJsonElement(it.readText()).jsonObject
        }
        val cases = measured.getValue("cases").jsonArray
        assertEquals(566, cases.size)
        cases.forEachIndexed { index, element ->
            val trial = element.jsonObject
            val raw = fixture()
            packU32(100_000).copyInto(raw, 264)
            measured.getValue("sources").jsonArray[trial.getValue("source").jsonPrimitive.int].jsonObject.forEach { (offset, value) ->
                raw[69500 + offset.toInt()] = value.jsonPrimitive.int.toByte()
            }
            val expected = raw.copyOf()
            trial.getValue("changes").jsonObject.forEach { (offset, value) ->
                expected[69500 + offset.toInt()] = value.jsonPrimitive.int.toByte()
            }
            packU32(100_000 + trial.getValue("delta").jsonPrimitive.int).copyInto(expected, 264)
            assertArrayEquals("Measured case $index", expected,
                BluetoothBackup.patchC7(raw, trial.getValue("settings").jsonObject))
        }
        val raw = fixture().also { packU32(100_000).copyInto(it, 264) }
        val named = BluetoothBackup.patchC7(raw, buildJsonObject {}, "Fuji BLE TEST")
        BluetoothBackup.requireResolvedWrite(raw, raw, named)
        BluetoothBackup.requireResolvedWrite(named, raw, named)
        for (offset in listOf(264, 63164, 63356, 69748)) {
            val unrelated = named.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            assertThrows(IllegalArgumentException::class.java) {
                BluetoothBackup.requireResolvedWrite(unrelated, raw, named)
            }
        }
        assertEquals("Fuji BLE TEST", BluetoothBackup.decode(named)[6].name)
        val delta = (69500 until raw.size).sumOf { (named[it].toInt() and 255) - (raw[it].toInt() and 255) }
        assertEquals(unpackU32(raw, 264) + delta, unpackU32(named, 264))
        assertArrayEquals(raw, BluetoothBackup.patchC7(raw, buildJsonObject {}))
        for ((settings, name) in listOf(
            "{\"color\":true}" to null, "{\"color\":\"4\"}" to null,
            "{\"wbShiftRed\":9.5}" to null, "{\"whiteBalance\":\"custom-1\"}" to null,
            "{\"whiteBalance\":\"color-temp\",\"colorTemperature\":5550}" to null,
            "{\"unknown\":0}" to null, "{\"monochromaticColorWc\":1}" to null,
            "{}" to "x".repeat(26), "{}" to "Тест", "{}" to "",
        )) {
            assertThrows("Accepted $settings / $name", Exception::class.java) {
                BluetoothBackup.patchC7(raw, Json.parseToJsonElement(settings).jsonObject, name)
            }
        }
    }

    @Test fun completeBackupMatchesIndependentGoldenAndRejectsInvalidData() {
        val raw = fixture()
        val decoded = BluetoothBackup.decode(raw)
        val actual = buildJsonArray {
            decoded.forEach { slot ->
                add(buildJsonObject {
                    put("slot", slot.slot)
                    put("name", slot.name)
                    put("settings", buildJsonObject {
                        slot.settings.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Number -> put(key, value)
                                else -> error("Unexpected field type")
                            }
                        }
                    })
                })
            }
        }
        val expected = javaClass.getResourceAsStream("/bluetooth/xt50-132-expected.json")!!.bufferedReader().use { it.readText() }
        assertEquals(Json.parseToJsonElement(expected), actual)
        assertEquals("", decoded[2].name)
        val compressed = ByteArrayOutputStream().also { out -> DeflaterOutputStream(out).use { it.write(raw) } }.toByteArray()
        val checksum = compressed.sumOf { it.toInt() and 0xff } and 0xffff
        assertArrayEquals(raw, BluetoothBackup.inflate(compressed, raw.size, checksum))
        for (data in listOf(raw.copyOf(raw.size - 1), raw.copyOf().also { it[0] = 0 },
            raw.copyOf().also { it[20] = 0 }, raw.copyOf().also { it[69500 + 229] = -1 },
            raw.copyOf().also { it[69500 + 114] = 1 }, raw.copyOf().also { it[63356 + 116] = 19 },
            raw.copyOf().also { it[63356 + 622] = -1 })) {
            assertThrows(IllegalArgumentException::class.java) { BluetoothBackup.decode(data) }
        }
        assertThrows(IllegalArgumentException::class.java) { BluetoothBackup.inflate(compressed, raw.size - 1, checksum) }
        assertThrows(IllegalArgumentException::class.java) { BluetoothBackup.inflate(compressed, raw.size, checksum xor 1) }
        for (data in listOf(compressed.copyOf(compressed.size - 1), compressed + byteArrayOf(0))) {
            assertThrows(IllegalArgumentException::class.java) {
                BluetoothBackup.inflate(data, raw.size, data.sumOf { it.toInt() and 0xff } and 0xffff)
            }
        }
    }
}
