package dev.bondarenko.fujirecipes.camera

import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackup
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothRecoveryStore
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import android.net.Uri
import android.system.Os
import android.system.OsConstants
import dev.bondarenko.fujirecipes.FujiRecipesApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.time.Instant

class BluetoothRecoveryTest {
    /** Explicit operator acceptance only; never treats the historical upload as verified. */
    @Test fun archiveExternallyRepairedLegacyCheckpoint() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("legacyExternalRecovery") == "accept-current-other-settings")
        check(arguments.getString("recoveryInspection") == "true") { "A fresh production inspection is required." }
        inspectExistingCheckpointWithoutUploading()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val archive = File(context.noBackupFilesDir,
            "bluetooth-recovery-archive/${Instant.now().toString().replace(':', '-')}-external-repair")
        retireLegacyCheckpoint(directory, File(context.noBackupFilesDir, "bluetooth-slot-1-hardware-original.dat"),
            File(context.filesDir, "library.json"), File(directory, "inspection.dat").readBytes(), archive)
        runBlocking {
            val controller = (context.applicationContext as FujiRecipesApp).container.cameraController
            controller.refreshBluetoothRecovery()
            assertNull(controller.recovery.value)
        }
    }

    /** Opt-in live download only. Never calls the writer or replays a saved backup. */
    @Test fun inspectExistingCheckpointWithoutUploading() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("recoveryInspection") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val store = BluetoothRecoveryStore(directory)
        check(store.pending) { "This check requires an existing unresolved write; never create one for this test." }
        val protected = listOf(File(directory, "original.dat"), File(directory, "target.dat"), File(context.filesDir, "library.json"))
        val before = protected.associateWith { it.readBytes() }
        val pending = File(directory, "pending")
        val marker = pending.readBytes()
        val controller = (context.applicationContext as FujiRecipesApp).container.cameraController
        controller.selectTransport(CameraTransport.BLUETOOTH)
        withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
        try {
            controller.connect()?.join()
            check(controller.state.value is CameraState.Connected) { "Recovery inspection could not connect: ${controller.state.value}" }
            val resolved = controller.inspectBluetoothRecovery()
            val actual = File(directory, "inspection.dat").readBytes()
            val original = BluetoothBackup.compare(actual, before.getValue(File(directory, "original.dat")))
            val target = BluetoothBackup.compare(actual, before.getValue(File(directory, "target.dat")))
            assertEquals(original == BluetoothBackup.Match.COMPLETE || target == BluetoothBackup.Match.COMPLETE, resolved)
            assertEquals(!resolved, store.pending)
            if (!resolved) {
                assertArrayEquals(marker, pending.readBytes())
                assertEquals(original, controller.recovery.value!!.original)
                assertEquals(target, controller.recovery.value!!.target)
            } else assertNull(controller.recovery.value)
        } finally {
            protected.forEach { assertArrayEquals(before.getValue(it), it.readBytes()) }
            controller.disconnect()
            withTimeout(20_000) { controller.state.first { it == CameraState.Disconnected } }
        }
    }

    /** Opt in only on a phone already carrying a genuine checkpoint; never create/delete it. */
    @Test fun existingCheckpointBlocksUploadsAndExportsWithoutChangingProtectedFiles() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("recoveryCheckpoint") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val protected = listOf(File(directory, "original.dat"), File(directory, "target.dat"), File(directory, "pending"),
            File(context.filesDir, "library.json"))
        val before = protected.associateWith { it.readBytes() }
        val controller = (context.applicationContext as FujiRecipesApp).container.cameraController
        for (suffix in listOf("", ".new", ".bak")) assertFalse(File(directory, "verification-policy$suffix").exists())
        controller.refreshBluetoothRecovery()
        assertNotNull(controller.recovery.value)
        assertFalse(controller.recovery.value!!.damaged)
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
            for (suffix in listOf("", ".new", ".bak")) assertFalse(File(directory, "verification-policy$suffix").exists())
            assertNotNull(controller.recovery.value)
        } finally {
            output.delete()
        }
    }

    @Test fun uncertainWriteSurvivesRestartInspectionAndExportUntilAnExactMatch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "recovery-test-${UUID.randomUUID()}")
        // Entirely synthetic, valid layout; no camera or real recovery files are touched.
        val original = syntheticBackup()
        val target = BluetoothBackup.patchSlot(original, 1, buildJsonObject { put("color", 3) })
        val readback = target.copyOf().also { it[63170] = 1 }
        try {
            var store = BluetoothRecoveryStore(directory)
            store.prepare(original, target)
            // Simulate the legacy checkpoint contract only in this isolated cache directory.
            assertTrue(File(directory, "verification-policy").delete())
            store.markUploadStarted()
            store.markAccepted()
            store.saveReadback(readback)
            store = BluetoothRecoveryStore(directory)
            assertTrue(store.pending)
            assertTrue(store.status()!!.accepted)
            assertEquals(BluetoothBackup.Match.SETTINGS, store.status()!!.target)
            assertThrows(IllegalStateException::class.java) { store.prepare(readback, original) }
            assertArrayEquals(original, File(directory, "original.dat").readBytes())
            assertArrayEquals(target, File(directory, "target.dat").readBytes())
            val corrupt = original.copyOf().also { it[264] = (it[264].toInt() xor 1).toByte() }
            assertThrows(IllegalArgumentException::class.java) { store.inspect(corrupt) }
            assertTrue(store.pending)
            assertTrue(store.status()!!.damaged)
            assertArrayEquals(corrupt, File(directory, "inspection.dat").readBytes())
            for (offset in listOf(63164, 63170, 63171, 63172, 63173)) {
                assertFalse(store.inspect(original.copyOf().also { it[offset] = 1 }))
                assertTrue(store.pending)
            }
            assertTrue(store.pending)
            assertEquals(BluetoothBackup.Match.SETTINGS, store.status()!!.original)
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

    @Test fun newCheckpointsVerifySettingsAndRejectUnknownOrIncompletePolicies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "recovery-test-${UUID.randomUUID()}")
        val original = syntheticBackup()
        val target = BluetoothBackup.patchSlot(original, 1, buildJsonObject { put("color", 3) })
        val readback = target.copyOf().also { it.fill(1, 59920, 63356) }
        val store = BluetoothRecoveryStore(directory)
        try {
            store.prepare(original, target)
            store.markUploadStarted()
            store.saveReadback(readback)
            assertEquals(BluetoothBackup.Match.SETTINGS, store.status()!!.target)
            assertTrue(store.pending) // Cached status cannot resolve a write.
            val changed = target.copyOf().also {
                it[168] = 1
                packU32((168 until it.size).sumOf { offset ->
                    if (offset in 264..267 || offset in 59920..63355) 0 else it[offset].toInt() and 255
                }).copyInto(it, 264)
            }
            assertFalse(store.inspect(changed))
            assertTrue(store.pending)
            assertArrayEquals(readback, File(directory, "readback.dat").readBytes())
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(store.archive().inputStream()).use { zip ->
                while (true) { val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes() }
            }
            assertEquals("xt50-132-settings-v1", entries.getValue("verification-policy").toString(Charsets.US_ASCII))
            assertArrayEquals(original, entries.getValue("original.dat"))
            assertArrayEquals(target, entries.getValue("target.dat"))
            assertTrue(store.inspect(readback))
            assertFalse(store.pending)
            assertNull(store.status())

            store.prepare(original, target)
            store.markUploadStarted()
            assertTrue(store.inspect(original.copyOf().also { it[63170] = 1 }))
            assertFalse(store.pending)

            store.prepare(original, target)
            store.markUploadStarted()
            for (suffix in listOf("", ".new", ".bak")) {
                File(directory, "verification-policy").delete()
                File(directory, "verification-policy.new").delete()
                if (suffix.isNotEmpty()) File(directory, "verification-policy").writeText("xt50-132-settings-v1")
                File(directory, "verification-policy$suffix").writeText("unknown")
                assertThrows(Exception::class.java) { store.inspect(target) }
                assertTrue(store.pending)
                assertTrue(store.status()!!.damaged)
                assertArrayEquals(target, File(directory, "inspection.dat").readBytes())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun legacyRetirementPreservesEvidenceAndRefusesUnrepairedRecipes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "retirement-test-${UUID.randomUUID()}")
        val directory = File(root, "checkpoint")
        val hardwareOriginal = File(root, "hardware-original.dat")
        val library = File(root, "library.json")
        val archive = File(root, "archive")
        val original = BluetoothBackup.patchSlot(syntheticBackup(), 1, buildJsonObject { put("color", 3) })
        val target = BluetoothBackup.patchSlot(original, 1, buildJsonObject { put("color", 0) })
        val fresh = original.copyOf().also { raw ->
            raw[80] = 1 // A valid header difference, without claiming its meaning.
            raw[63170] = 1
        }
        val store = BluetoothRecoveryStore(directory)
        try {
            store.prepare(original, target)
            store.markUploadStarted()
            hardwareOriginal.writeBytes(original)
            library.writeText("synthetic library")
            assertThrows(IllegalStateException::class.java) {
                retireLegacyCheckpoint(directory, hardwareOriginal, library, fresh, archive)
            }
            assertTrue(File(directory, "verification-policy").delete()) // Cache-only legacy simulation.
            for (bad in listOf(target, fresh.copyOf().also { it[264] = (it[264].toInt() xor 1).toByte() })) {
                assertThrows(Exception::class.java) { retireLegacyCheckpoint(directory, hardwareOriginal, library, bad, archive) }
                assertTrue(store.pending)
                assertFalse(archive.exists())
                assertArrayEquals(original, File(directory, "original.dat").readBytes())
                assertArrayEquals(target, File(directory, "target.dat").readBytes())
                assertArrayEquals(original, hardwareOriginal.readBytes())
            }
            assertTrue(archive.mkdir())
            assertThrows(IllegalStateException::class.java) {
                retireLegacyCheckpoint(directory, hardwareOriginal, library, fresh, archive)
            }
            assertTrue(store.pending)
            assertTrue(hardwareOriginal.exists())
            assertTrue(archive.delete()) // Only this empty synthetic cache destination.
            retireLegacyCheckpoint(directory, hardwareOriginal, library, fresh, archive)
            assertFalse(store.pending)
            assertFalse(directory.exists())
            assertFalse(hardwareOriginal.exists())
            assertArrayEquals(original, File(archive, "original.dat").readBytes())
            assertArrayEquals(target, File(archive, "target.dat").readBytes())
            assertArrayEquals(byteArrayOf(1), File(archive, "pending").readBytes())
            assertArrayEquals(original, File(archive, "hardware-original.dat").readBytes())
            assertArrayEquals(fresh, File(archive, "retirement-readback.dat").readBytes())
            assertEquals("synthetic library", library.readText())
            assertTrue(File(archive, "retirement.json").readText().contains("\"historicalWriteVerified\":false"))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun retireLegacyCheckpoint(directory: File, hardwareOriginal: File, library: File, fresh: ByteArray, archive: File) {
        check(BluetoothRecoveryStore(directory).pending)
        check(File(directory, "pending").readBytes().contentEquals(byteArrayOf(1)))
        check(directory.listFiles()!!.all { it.isFile && !it.name.endsWith(".new") && !it.name.endsWith(".bak") })
        check(!File(directory, "verification-policy").exists()) { "Only the known legacy checkpoint may be retired." }
        val original = File(directory, "original.dat").readBytes()
        val target = File(directory, "target.dat").readBytes()
        check(BluetoothBackup.decode(original).first().settings["color"] == 3)
        check(BluetoothBackup.patchSlot(original, 1, buildJsonObject { put("color", 0) }).contentEquals(target))
        val match = BluetoothBackup.compare(fresh, original)
        check(match != BluetoothBackup.Match.DIFFERENT_RECIPES && match != BluetoothBackup.Match.COMPLETE) {
            "Recipes must be repaired; an exact match should use normal recovery resolution."
        }
        val hardware = hardwareOriginal.readBytes()
        BluetoothBackup.decode(hardware)
        check(hardware.copyOfRange(63356, hardware.size).contentEquals(original.copyOfRange(63356, original.size)))
        val libraryBytes = library.readBytes()
        val retained = directory.listFiles()!!.associate { it.name to it.readBytes() }
        val additions = listOf("retirement-readback.dat", "retirement.json", "hardware-original.dat")
        check(!archive.exists() && additions.none { File(directory, it).exists() }) { "Inspect the existing retirement evidence before retrying." }
        check(archive.parentFile!!.isDirectory || archive.parentFile!!.mkdirs())
        fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val record = buildJsonObject {
            put("acceptedAt", Instant.now().toString())
            put("acceptance", "Externally repaired recipes; keep current other camera settings")
            put("historicalWriteVerified", false)
            put("archive", archive.name)
            put("commitRule", "Committed only when the complete directory is in the recovery archive")
            put("originalMatch", match.name)
            put("comparedPrefixDifferences", (0 until 59920).count { fresh[it] != original[it] })
            put("nonRestoredTailDifferences", (59920 until 63356).count { fresh[it] != original[it] })
            put("retainedFiles", buildJsonObject { retained.forEach { (name, bytes) -> put(name, hash(bytes)) } })
            put("freshSha256", hash(fresh))
            put("hardwareOriginalSha256", hash(hardware))
            put("librarySha256", hash(libraryBytes))
        }.toString().toByteArray()
        for ((name, bytes) in listOf("retirement-readback.dat" to fresh, "retirement.json" to record)) {
            val file = File(directory, name)
            file.outputStream().use { it.write(bytes); it.fd.sync() }
            check(file.readBytes().contentEquals(bytes))
        }
        syncDirectory(directory)
        check(hardwareOriginal.renameTo(File(directory, "hardware-original.dat")))
        syncDirectory(directory)
        syncDirectory(hardwareOriginal.parentFile!!)
        retained.forEach { (name, bytes) -> check(File(directory, name).readBytes().contentEquals(bytes)) }
        check(File(directory, "hardware-original.dat").readBytes().contentEquals(hardware))
        check(library.readBytes().contentEquals(libraryBytes))
        check(directory.renameTo(archive)) { "Checkpoint still locked; inspect the prepared retirement record." }
        syncDirectory(archive.parentFile!!)
        syncDirectory(directory.parentFile!!)
        retained.forEach { (name, bytes) -> check(File(archive, name).readBytes().contentEquals(bytes)) }
        check(File(archive, "retirement-readback.dat").readBytes().contentEquals(fresh))
        check(File(archive, "retirement.json").readBytes().contentEquals(record))
        check(File(archive, "hardware-original.dat").readBytes().contentEquals(hardware))
        check(library.readBytes().contentEquals(libraryBytes))
    }

    private fun syncDirectory(directory: File) {
        check(directory.isDirectory)
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
    }

    private fun syntheticBackup() = ByteArray(BluetoothBackup.SIZE).also { raw ->
        "FUJIFILMX-BACKUP0100".toByteArray().copyInto(raw)
        "X-T50".toByteArray().copyInto(raw, 20)
        packU32(168).copyInto(raw, 152)
        packU32(raw.size - 168).copyInto(raw, 160)
        for (slot in 0..6) {
            val start = 63356 + slot * 1024
            packU16(5500).copyInto(raw, start + 16)
            raw[start + 229] = 1
            raw[start + 245] = 1
            raw[start + 175] = 6
        }
        packU32((63356 until raw.size).sumOf { raw[it].toInt() and 255 }).copyInto(raw, 264)
    }
}
