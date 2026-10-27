package dev.bondarenko.fujirecipes.camera

import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackup
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothRecoveryStore
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import android.net.Uri
import dev.bondarenko.fujirecipes.FujiRecipesApp
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class BluetoothRecoveryTest {
    /** Opt in only on a phone already carrying a genuine checkpoint; never create/delete it. */
    @Test fun existingCheckpointBlocksUploadsAndExportsWithoutChangingProtectedFiles() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("recoveryCheckpoint") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val protected = listOf(File(directory, "original.dat"), File(directory, "target.dat"), File(directory, "pending"),
            File(context.filesDir, "library.json"))
        val before = protected.associateWith { it.readBytes() }
        val controller = (context.applicationContext as FujiRecipesApp).container.cameraController
        controller.refreshBluetoothRecovery()
        assertNotNull(controller.recovery.value)
        val result = controller.writeBluetoothRecipe(1, null, buildJsonObject { put("color", 0) })
        assertTrue(result is WriteResult.Failure && result.recoveryRequired && !result.retryable)
        val output = File(context.cacheDir, "recovery-export-test.zip")
        try {
            controller.exportBluetoothRecovery(Uri.fromFile(output))
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(output.inputStream()).use { zip ->
                while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
            }
            for (name in listOf("original.dat", "target.dat", "pending")) {
                assertArrayEquals(before.getValue(File(directory, name)), entries.getValue(name))
            }
            protected.forEach { assertArrayEquals(before.getValue(it), it.readBytes()) }
            assertNotNull(controller.recovery.value)
        } finally {
            output.delete()
        }
    }

    @Test fun uncertainWriteSurvivesRestartInspectionAndExportUntilAnExactMatch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "recovery-test-${UUID.randomUUID()}")
        // Entirely synthetic, valid layout; no camera or real recovery files are touched.
        val original = ByteArray(BluetoothBackup.SIZE).also { raw ->
            "FUJIFILMX-BACKUP".toByteArray().copyInto(raw)
            "X-T50".toByteArray().copyInto(raw, 20)
            for (slot in 0..6) {
                val start = 63356 + slot * 1024
                packU16(5500).copyInto(raw, start + 16)
                raw[start + 229] = 1
                raw[start + 245] = 1
                raw[start + 175] = 6
            }
        }
        val target = BluetoothBackup.patchSlot(original, 1, buildJsonObject { put("color", 3) })
        val readback = target.copyOf().also { it[63170] = 1 }
        try {
            var store = BluetoothRecoveryStore(directory)
            store.prepare(original, target)
            store.markUploadStarted()
            store.markAccepted()
            store.saveReadback(readback)
            store = BluetoothRecoveryStore(directory)
            assertTrue(store.pending)
            assertTrue(store.status()!!.accepted)
            assertEquals(BluetoothBackup.Match.RECIPES_ONLY, store.status()!!.target)
            assertThrows(IllegalStateException::class.java) { store.prepare(readback, original) }
            assertArrayEquals(original, File(directory, "original.dat").readBytes())
            assertArrayEquals(target, File(directory, "target.dat").readBytes())
            for (offset in listOf(264, 63164, 63170, 63171, 63172, 63173)) {
                assertFalse(store.inspect(original.copyOf().also { it[offset] = 1 }))
                assertTrue(store.pending)
            }
            assertTrue(store.pending)
            assertEquals(BluetoothBackup.Match.RECIPES_ONLY, store.status()!!.original)
            assertArrayEquals(readback, File(directory, "readback.dat").readBytes())
            File(directory, "pairing-secret").writeText("never export")
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(store.archive().inputStream()).use { zip ->
                while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
            }
            assertEquals(setOf("original.dat", "target.dat", "readback.dat", "inspection.dat", "pending", "accepted", "README.txt"), entries.keys)
            assertArrayEquals(original, entries.getValue("original.dat"))
            assertArrayEquals(target, entries.getValue("target.dat"))
            assertArrayEquals(readback, entries.getValue("readback.dat"))
            assertTrue(store.inspect(original))
            assertFalse(store.pending)
            assertNull(store.status())
            store.prepare(original, target)
            store.markUploadStarted()
            assertTrue(store.inspect(target))
            assertFalse(store.pending)

            store.prepare(original, target)
            // A crash before the atomic marker is renamed still blocks writes.
            File(directory, "pending.new").writeBytes(byteArrayOf(1))
            store = BluetoothRecoveryStore(directory)
            assertTrue(store.pending)
            assertNull(store.status()!!.original)
            File(directory, "target.dat").delete()
            assertTrue(store.status()!!.damaged)
            assertThrows(Exception::class.java) { store.inspect(original) }
            assertTrue(store.pending)
            assertThrows(IllegalStateException::class.java) { store.prepare(original, target) }
            assertTrue(store.archive().isNotEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }
}
