package dev.bondarenko.fujirecipes.camera.bluetooth

import android.util.AtomicFile
import dev.bondarenko.fujirecipes.data.exporting.zipOfBytes
import java.io.File

data class BluetoothRecoveryStatus(
    val original: BluetoothBackup.Match? = null,
    val target: BluetoothBackup.Match? = null,
    val damaged: Boolean = false,
    val accepted: Boolean = false,
)

/** Called only under CameraController's connection lock, on Dispatchers.IO. */
class BluetoothRecoveryStore(private val directory: File) {
    private val settingsPolicy = "xt50-132-settings-v1".toByteArray(Charsets.US_ASCII)
    private fun file(name: String) = AtomicFile(File(directory, name))
    private fun exists(name: String) = listOf("", ".bak", ".new").any { File(directory, name + it).exists() }

    // A process dying during AtomicFile's commit must never make an uncertain write disappear.
    val pending: Boolean
        get() = exists("pending")

    fun prepare(original: ByteArray, target: ByteArray) {
        check(!pending) { "The previous Bluetooth write still needs inspection. No new write was sent." }
        BluetoothBackup.decode(original)
        BluetoothBackup.decode(target)
        check(directory.isDirectory || directory.mkdirs()) { "Could not create the private camera backup folder." }
        save("original.dat", original)
        save("target.dat", target)
        save("verification-policy", settingsPolicy)
        for (name in listOf("readback.dat", "inspection.dat", "accepted")) {
            file(name).delete()
            check(!exists(name)) { "Old recovery evidence could not be removed; no new write was sent." }
        }
    }

    fun markUploadStarted() = save("pending", byteArrayOf(1))
    fun markAccepted() = save("accepted", byteArrayOf(1))

    /** Keep the first verification evidence separate from later read-only inspections. */
    fun saveReadback(raw: ByteArray) = save("readback.dat", raw)

    fun inspect(raw: ByteArray): Boolean {
        check(pending) { "There is no pending Bluetooth write to inspect." }
        save("inspection.dat", raw)
        return resolve(raw)
    }

    fun resolve(raw: ByteArray): Boolean {
        val original = file("original.dat").readFully()
        val target = file("target.dat").readFully()
        val originalMatch = BluetoothBackup.compare(raw, original)
        val targetMatch = BluetoothBackup.compare(raw, target)
        val settingsAllowed = allowsSettingsMatch()
        val matches = listOf(originalMatch, targetMatch).any {
            it == BluetoothBackup.Match.COMPLETE || settingsAllowed && it == BluetoothBackup.Match.SETTINGS
        }
        if (matches) {
            file("pending").delete()
            check(!pending) { "The saved settings match, but the recovery lock could not be cleared." }
        }
        return matches
    }

    fun status(): BluetoothRecoveryStatus? {
        if (!pending) return null
        val accepted = exists("accepted")
        return try {
            allowsSettingsMatch() // Unknown or incomplete policies remain a damaged, locked checkpoint.
            val original = file("original.dat").readFully()
            val target = file("target.dat").readFully()
            BluetoothBackup.decode(original)
            BluetoothBackup.decode(target)
            val latest = listOf("inspection.dat", "readback.dat").firstOrNull(::exists)
                ?: return BluetoothRecoveryStatus(accepted = accepted)
            val raw = file(latest).readFully()
            BluetoothRecoveryStatus(BluetoothBackup.compare(raw, original), BluetoothBackup.compare(raw, target), accepted = accepted)
        } catch (_: Exception) {
            BluetoothRecoveryStatus(damaged = true, accepted = accepted)
        }
    }

    /** Explicit allowlist: never include HCI captures, Android bond storage, or unrelated app files. */
    fun archive(): ByteArray {
        check(pending) { "There is no pending Bluetooth write to export." }
        val entries = linkedMapOf<String, ByteArray>()
        for (name in listOf("original.dat", "target.dat", "readback.dat", "inspection.dat", "pending", "accepted", "verification-policy")) {
            for (suffix in listOf("", ".bak", ".new")) {
                val source = File(directory, name + suffix)
                if (source.isFile) {
                    check(source.length() <= BluetoothBackup.SIZE) { "Recovery file ${source.name} is too large to export." }
                    entries[source.name] = source.readBytes()
                }
            }
        }
        entries["README.txt"] = ("Fuji Recipes Bluetooth recovery evidence\n" +
            "original.dat: camera backup before upload\ntarget.dat: intended upload\n" +
            "readback.dat: verification read-back, if received\ninspection.dat: latest later camera check, if received\n" +
            "accepted: camera acknowledged the complete upload, if recorded\n" +
            "verification-policy: verification rule saved before this upload, if present\n" +
            "A saved check is historical evidence, not proof of the camera's current state.\n" +
            "Without a policy, only an exact complete match to original or target resolves the pending write.\n" +
            "xt50-132-settings-v1 requires matching raw ranges [0,59920) and [63356,70524), plus valid layout and checksum.\n" +
            "Only the firmware's non-restored memory interval [59920,63356) may differ under that policy.\n" +
            "Unknown or unreadable policies keep the write locked. Resolution always requires a fresh camera read.\n" +
            "These backups may contain camera identifiers and private settings. This bundle is not an automatic restore file.\n").toByteArray()
        return zipOfBytes(entries)
    }

    private fun allowsSettingsMatch(): Boolean {
        if (!exists("verification-policy")) return false // Retain the legacy exact-only contract.
        check(!File(directory, "verification-policy.new").exists()) { "Incomplete Bluetooth recovery verification policy." }
        check(file("verification-policy").readFully().contentEquals(settingsPolicy)) {
            "Unsupported Bluetooth recovery verification policy. Writes remain blocked."
        }
        return true
    }

    private fun save(name: String, bytes: ByteArray) {
        require(bytes.size <= BluetoothBackup.SIZE) { "Recovery data exceeds the supported backup length." }
        val atomic = file(name)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
        // AtomicFile can log a failed rename without throwing. Verify before any upload.
        check(atomic.readFully().contentEquals(bytes)) { "Could not verify the saved recovery file $name." }
    }
}
