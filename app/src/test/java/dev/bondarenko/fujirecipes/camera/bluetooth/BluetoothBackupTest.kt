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

    @Test fun verificationDistinguishesWholeBackupFromAllRecipeRecords() {
        val raw = fixture()
        assertEquals(BluetoothBackup.Match.COMPLETE, BluetoothBackup.compare(raw.copyOf(), raw))
        for (offset in listOf(264, 63164, 63170, 63171, 63172, 63173)) {
            val drift = raw.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            assertEquals(BluetoothBackup.Match.RECIPES_ONLY, BluetoothBackup.compare(drift, raw))
        }
        // Include undecoded bytes and every slot: matching mapped fields is insufficient.
        for (slot in 0..6) {
            val changed = raw.copyOf().also { it[63356 + slot * 1024] = (it[63356 + slot * 1024].toInt() xor 1).toByte() }
            assertEquals(BluetoothBackup.Match.DIFFERENT_RECIPES, BluetoothBackup.compare(changed, raw))
        }
        for (bad in listOf(raw.copyOf(raw.size - 1), raw.copyOf().also { it[63356 + 229] = -1 })) {
            assertThrows(IllegalArgumentException::class.java) { BluetoothBackup.compare(bad, raw) }
            assertThrows(IllegalArgumentException::class.java) { BluetoothBackup.compare(raw, bad) }
        }
    }

    @Test fun slotEncoderMatchesIndependentUsbColorChanges() {
        val measured = javaClass.getResourceAsStream("/bluetooth/xt50-132-slot-colors-measured.json")!!.bufferedReader().use {
            Json.parseToJsonElement(it.readText()).jsonArray
        }
        assertEquals((1..6).toList(), measured.map { it.jsonObject.getValue("slot").jsonPrimitive.int })
        measured.forEach { element ->
            val trial = element.jsonObject
            val slot = trial.getValue("slot").jsonPrimitive.int
            val offset = 63356 + (slot - 1) * 1024 + 248
            val raw = fixture().also {
                packU32(100_000).copyInto(it, 264)
                it[offset] = trial.getValue("sourceColorCode").jsonPrimitive.int.toByte()
            }
            val expected = raw.copyOf().also {
                it[offset] = trial.getValue("targetColorCode").jsonPrimitive.int.toByte()
                packU32(100_000 + trial.getValue("delta").jsonPrimitive.int).copyInto(it, 264)
            }
            assertArrayEquals("Measured C$slot", expected,
                BluetoothBackup.patchSlot(raw, slot, buildJsonObject { put("color", trial.getValue("color")) }))
        }
        val raw = fixture().also { it[63356 + 647] = 42 }
        val unchangedName = BluetoothBackup.decode(raw).first().name
        assertArrayEquals(raw, BluetoothBackup.patchSlot(raw, 1, buildJsonObject {}, unchangedName))
        for (slot in listOf(0, 8)) assertThrows(IllegalArgumentException::class.java) {
            BluetoothBackup.patchSlot(raw, slot, buildJsonObject {})
        }
    }

    @Test fun slotEncoderAppliesMeasuredFieldsWithoutTouchingOtherRecords() {
        val measured = javaClass.getResourceAsStream("/bluetooth/xt50-132-c7-measured.json")!!.bufferedReader().use {
            Json.parseToJsonElement(it.readText()).jsonObject
        }
        val cases = measured.getValue("cases").jsonArray
        assertEquals(566, cases.size)
        cases.forEachIndexed { index, element ->
            for (slot in 1..7) {
                val start = 63356 + (slot - 1) * 1024
                val trial = element.jsonObject
                val raw = fixture()
                packU32(100_000).copyInto(raw, 264)
                measured.getValue("sources").jsonArray[trial.getValue("source").jsonPrimitive.int].jsonObject.forEach { (offset, value) ->
                    raw[start + offset.toInt()] = value.jsonPrimitive.int.toByte()
                }
                val expected = raw.copyOf()
                trial.getValue("changes").jsonObject.forEach { (offset, value) ->
                    expected[start + offset.toInt()] = value.jsonPrimitive.int.toByte()
                }
                packU32(100_000 + trial.getValue("delta").jsonPrimitive.int).copyInto(expected, 264)
                assertArrayEquals("C$slot measured case $index", expected,
                    BluetoothBackup.patchSlot(raw, slot, trial.getValue("settings").jsonObject))
            }
        }
        val raw = fixture().also { packU32(100_000).copyInto(it, 264) }
        val named = BluetoothBackup.patchSlot(raw, 7, buildJsonObject {}, "Fuji BLE TEST")
        assertEquals("Fuji BLE TEST", BluetoothBackup.decode(named)[6].name)
        val delta = (69500 until raw.size).sumOf { (named[it].toInt() and 255) - (raw[it].toInt() and 255) }
        assertEquals(unpackU32(raw, 264) + delta, unpackU32(named, 264))
        assertArrayEquals(raw, BluetoothBackup.patchSlot(raw, 7, buildJsonObject {}))
        for ((settings, name) in listOf(
            "{\"color\":true}" to null, "{\"color\":\"4\"}" to null,
            "{\"wbShiftRed\":9.5}" to null, "{\"whiteBalance\":\"custom-1\"}" to null,
            "{\"whiteBalance\":\"color-temp\",\"colorTemperature\":5550}" to null,
            "{\"unknown\":0}" to null, "{\"monochromaticColorWc\":1}" to null,
            "{}" to "x".repeat(26), "{}" to "Тест", "{}" to "",
        )) {
            assertThrows("Accepted $settings / $name", Exception::class.java) {
                BluetoothBackup.patchSlot(raw, 7, Json.parseToJsonElement(settings).jsonObject, name)
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
