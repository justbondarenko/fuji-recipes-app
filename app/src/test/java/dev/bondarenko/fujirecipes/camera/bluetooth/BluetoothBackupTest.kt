package dev.bondarenko.fujirecipes.camera.bluetooth

import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class BluetoothBackupTest {
    private fun fixture() = javaClass.getResourceAsStream("/bluetooth/xt50-132-sanitized.dat")!!.use { it.readBytes() }

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
