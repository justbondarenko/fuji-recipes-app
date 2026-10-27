package dev.bondarenko.fujirecipes.camera

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.ptp.unpackU32
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import org.junit.Assume.assumeTrue
import org.junit.Test
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackup
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothCamera
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Opt in with -e bluetoothProbe true after pairing the X-T50 1.32 through XApp.
 * Grant BLUETOOTH_CONNECT to the debug target and stop XApp before running.
 * Add -e bluetoothRoundTrip true to restore only the freshly downloaded, unchanged backup.
 * Or -e bluetoothColorProof true with an independently captured Color reference in cache.
 * Or -e bluetoothRecipeProof true with independently checked C7 from/to references in cache.
 * Captured protocol: specs/plans/bluetooth-recipes.md. No general recipe encoder exists here.
 * Secure reconnect follows the MIT-licensed petabyt/libfuji lib/bluetooth.c handshake.
 */
@SuppressLint("MissingPermission")
class CameraBluetoothHardwareTest {
    /** Exercises the shipping reader, including refresh on the same GATT connection. */
    @Test
    fun readRecipesWithAppTransport() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothAppRead") == "true")
        val reconnectPause = InstrumentationRegistry.getArguments()
            .getString("bluetoothReconnectPauseMs", "3000").toLong()
        require(reconnectPause in 3_000..300_000) { "Reconnect pause must be 3000–300000 ms." }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        var baseline: List<dev.bondarenko.fujirecipes.camera.usb.SlotRecipe>? = null
        repeat(3) { cycle ->
            if (cycle > 0) Thread.sleep(reconnectPause)
            Log.i(TAG, "Shipping reader connection ${cycle + 1}/3; reconnect pause=${reconnectPause}ms")
            BluetoothCamera(context) {}.use { camera ->
                camera.connect(device)
                val first = camera.readRecipes()
                check(first.map { it.slot } == (1..7).toList() && first.all { it.settings.size == 16 })
                check(camera.readRecipes() == first) { "Refreshed recipes differ from the initial read." }
                check(baseline == null || first == baseline) { "Recipes differ after reconnecting." }
                baseline = first
            }
            Log.i(TAG, "Shipping reader connection ${cycle + 1}/3: both reads verified and GATT client closed")
        }
        checkXAppStopped()
    }

    /** Separate opt-in: exercise the shipping slot writer, verify independently, then restore. */
    @Test
    @RequiresApi(37)
    fun writeSlotWithAppTransportAndRestore() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothAppWrite") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val slot = InstrumentationRegistry.getArguments().getString("bluetoothWriteSlot", "7").toInt()
        require(slot in 1..7)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        fun independentBackup() = BleBackupProbe(context).use { probe -> probe.connect(device); probe.download() }
        val original = independentBackup()
        val recipe = BluetoothBackup.decode(original).single { it.slot == slot }
        val name = recipe.name.takeIf { it.isNotBlank() }
        val originalFile = File(context.noBackupFilesDir, "bluetooth-slot-$slot-hardware-original.dat")
        check(!originalFile.exists()) { "A previous slot proof is unresolved; inspect its saved original before retrying." }
        originalFile.writeBytes(original)
        val controller = (context.applicationContext as dev.bondarenko.fujirecipes.FujiRecipesApp).container.cameraController
        suspend fun disconnect() {
            controller.disconnect()
            withTimeout(20_000) { controller.state.first { it == CameraState.Disconnected } }
            delay(3_000)
        }
        suspend fun connect() {
            controller.connect()
            val state = withTimeout(30_000) { controller.state.first { it is CameraState.Connected || it is CameraState.Error } }
            check(state is CameraState.Connected && state.canWrite) { "C$slot writer could not connect: $state" }
        }
        val color = (recipe.settings.getValue("color") as Number).toInt()
        val changedColor = if (color == 0) 1 else 0
        var changed = false
        try {
            controller.selectTransport(CameraTransport.BLUETOOTH)
            withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
            delay(3_000)
            connect()
            try {
                val result = controller.writeBluetoothRecipe(slot, name, buildJsonObject { put("color", changedColor) })
                check(result is WriteResult.Success && result.outcome.wholeBackupVerified) { "Forward write unverified: $result" }
                changed = true
                disconnect()
                val actual = independentBackup()
                val expected = File(context.noBackupFilesDir, "bluetooth-c7-recovery/target.dat").readBytes()
                check(actual.contentEquals(expected)) { "Independent read differs from the app's verified target." }
                check(BluetoothBackup.decode(actual)[slot - 1].settings["color"] == changedColor)
                val expectedRecipes = original.copyOfRange(63356, original.size)
                expectedRecipes[(slot - 1) * 1024 + 248] = if (changedColor == 0) 0 else 6
                check(actual.copyOfRange(63356, actual.size).contentEquals(expectedRecipes)) {
                    "Independent read found recipe changes beyond the selected Color byte."
                }
            } finally {
                // Only a verified forward write is automatically reversed; uncertain uploads require inspection.
                if (changed) {
                    disconnect()
                    connect()
                    val result = controller.writeBluetoothRecipe(slot, name, buildJsonObject { put("color", color) })
                    check(result is WriteResult.Success && result.outcome.wholeBackupVerified) { "Original C$slot restoration unverified: $result" }
                    changed = false
                    disconnect()
                    val restored = independentBackup()
                    check(restored.contentEquals(original)) { "Original whole backup differs after restoration." }
                    originalFile.delete()
                }
            }
        } finally {
            controller.disconnect()
            withTimeout(20_000) { controller.state.first { it == CameraState.Disconnected } }
            checkXAppStopped()
        }
    }

    /** Cancel after the fresh backup is saved, while no restore data has been sent. */
    @Test
    @RequiresApi(37)
    fun cancelC7BeforeUploadLeavesCameraUnchanged() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothAppCancel") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        fun independentBackup() = BleBackupProbe(context).use { it.connect(device); it.download() }
        val original = independentBackup()
        val c7 = BluetoothBackup.decode(original)[6]
        val controller = (context.applicationContext as dev.bondarenko.fujirecipes.FujiRecipesApp).container.cameraController
        try {
            controller.selectTransport(CameraTransport.BLUETOOTH)
            withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
            delay(3_000)
            controller.connect()
            val state = withTimeout(30_000) { controller.state.first { it is CameraState.Connected || it is CameraState.Error } }
            check(state is CameraState.Connected) { "Cancellation proof could not connect: $state" }
            var checks = 0
            val result = controller.writeBluetoothRecipe(7, c7.name, buildJsonObject { put("color", 0) }) { ++checks >= 2 }
            check(checks == 2 && result is WriteResult.Failure && result.warning == null) { "Cancellation failed: $result" }
            check(!File(context.noBackupFilesDir, "bluetooth-c7-recovery/pending").exists()) { "An upload was started despite cancellation." }
            delay(3_000)
            check(independentBackup().contentEquals(original)) { "Camera settings changed after cancellation." }
        } finally {
            controller.disconnect()
            withTimeout(20_000) { controller.state.first { it == CameraState.Disconnected } }
            checkXAppStopped()
        }
    }

    /** Simulate a lost acknowledgement against a real checkpoint, without uploading settings. */
    @Test
    @RequiresApi(37)
    fun unresolvedWriteRefusesChangedCameraBeforeUpload(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothPendingGuard") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val directory = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val pending = File(directory, "pending")
        check(!dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothRecoveryStore(directory).pending) {
            "A real pending write must be inspected before this diagnostic."
        }
        val original = File(directory, "original.dat").readBytes()
        val target = File(directory, "target.dat").readBytes()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        fun independentBackup() = BleBackupProbe(context).use { it.connect(device); it.download() }
        val before = independentBackup()
        check(!before.contentEquals(original) && !before.contentEquals(target)) {
            "This check requires camera settings changed since the last completed checkpoint."
        }
        val controller = (context.applicationContext as dev.bondarenko.fujirecipes.FujiRecipesApp).container.cameraController
        controller.selectTransport(CameraTransport.BLUETOOTH)
        withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
        delay(3_000)
        controller.connect()
        val state = withTimeout(30_000) { controller.state.first { it is CameraState.Connected || it is CameraState.Error } }
        check(state is CameraState.Connected) { "Pending-write diagnostic could not connect: $state" }
        try {
            // Only the marker is simulated. Retain the real original/intended checkpoint.
            pending.outputStream().use { it.write(1); it.fd.sync() }
            var cancellationChecks = 0
            val result = controller.writeBluetoothRecipe(1, null, buildJsonObject {}) {
                // A second boundary is before restore preparation, even if the guard regresses.
                ++cancellationChecks >= 2
            }
            check(result is WriteResult.Failure && !result.retryable && result.warning == null && result.recoveryRequired) {
                "An unresolved checkpoint was not refused: $result"
            }
            check(cancellationChecks == 0) { "The writer passed the unresolved-checkpoint preflight." }
            check(pending.exists()) { "The unresolved marker was discarded." }
            check(File(directory, "original.dat").readBytes().contentEquals(original))
            check(File(directory, "target.dat").readBytes().contentEquals(target))
            delay(3_000)
            val after = independentBackup()
            check(after.copyOfRange(63356, after.size).contentEquals(before.copyOfRange(63356, before.size))) {
                "Recipe records changed during the refusal check."
            }
            // Read-only downloads can change nonrecipe bytes; production write verification remains exact.
            Log.i(TAG, "Refusal check nonrecipe differences: ${(0 until 63356).filter { before[it] != after[it] }}")
        } finally {
            // Remove only this diagnostic's marker; it never represented a settings upload.
            check(pending.delete() || !pending.exists()) { "Could not remove the diagnostic marker." }
            controller.refreshBluetoothRecovery()
            controller.disconnect()
            withTimeout(20_000) { controller.state.first { it == CameraState.Disconnected } }
            checkXAppStopped()
        }
    }

    @Test
    @RequiresApi(37)
    fun downloadSettingsBackup() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothProbe") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.cacheDir, "camera-ble-backup.dat")
        check(!output.exists() || output.delete()) { "Could not remove the previous probe output." }
        checkXAppStopped()
        val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        check(adapter.isEnabled) { "Turn Bluetooth on first." }
        val device = requireNotNull(adapter.bondedDevices.singleOrNull { it.name == "X-T50" }) {
            "Pair this phone with one X-T50 through XApp first."
        }
        val roundTrip = InstrumentationRegistry.getArguments().getString("bluetoothRoundTrip") == "true"
        val colorProof = InstrumentationRegistry.getArguments().getString("bluetoothColorProof") == "true"
        val recipeProof = InstrumentationRegistry.getArguments().getString("bluetoothRecipeProof") == "true"
        check(listOf(roundTrip, colorProof, recipeProof).count { it } <= 1) { "Choose one restore diagnostic." }
        val bytes = BleBackupProbe(context).use { probe ->
            probe.connect(device)
            val bytes = probe.download()
            checkXAppStopped()
            Log.i(TAG, "Verified backup download: ${bytes.size} bytes")
            if (recipeProof) {
                probe.restoreRecipeReference()
            } else if (colorProof) {
                probe.restoreColorReference()
            } else {
                if (roundTrip) probe.restoreUnchanged()
                bytes
            }
        }
        if (roundTrip || colorProof || recipeProof) {
            // ponytail: X-T50 1.32 rejects an immediate operation switch; replace this observed pause with a readiness signal when identified.
            Thread.sleep(3_000)
            BleBackupProbe(context).use { probe ->
                probe.connect(device)
                check(probe.download().contentEquals(bytes)) { "Restored backup differs on independent read-back." }
            }
            checkXAppStopped()
            Log.i(TAG, "Verified restore: independent read-back matches all ${bytes.size} bytes")
        }
        checkXAppStopped()
        output.writeBytes(bytes)
    }

    private fun checkXAppStopped() {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pidof com.fujifilm.xapp")
        val running = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        check(running.isBlank()) { "Stop XApp before this independent Bluetooth probe." }
    }
}

private const val TAG = "FujiBleProbe"
private const val MAX_BACKUP_BYTES = 8 * 1024 * 1024
private val SECURE_STATUS = UUID.fromString("f557d96b-8284-4667-8793-b971c1deca2a")
private val CLIENT_NAME = UUID.fromString("85b9163e-62d1-49ff-a6f5-054b4630d4a1")
private val CONNECTION_STATUS = UUID.fromString("f9150137-5d40-4801-a8dc-f7fc5b01da50")
private val OPERATION_STATUS = UUID.fromString("11438c83-cfb0-4511-841b-759e0d2321c8")
private val BACKUP_COMMAND = UUID.fromString("f14e7a88-6f57-4872-a69b-b47ef3295c9b")
private val RESTORE_COMMAND = UUID.fromString("f1897eb9-a9aa-40f0-8ce6-2fa7915bc459")
private val RESTORE_STATUS = UUID.fromString("4b3a413c-230f-42bc-b3ed-b1db2eadee82")
private val FILE_STATUS = UUID.fromString("2e27ed9f-5506-41cd-ba48-dac06669ad95")
private val FILE_DATA = UUID.fromString("ac0c799a-fa6c-4df5-bbc5-bb95cce7e6ea")
private val FILE_INFO = UUID.fromString("c922ac69-9480-4348-8f4b-9ee29bc30d1d")

/** Sequential GATT operations, limited to X-T50 1.32 backup and guarded restore diagnostics. */
@RequiresApi(37)
@SuppressLint("MissingPermission")
private class BleBackupProbe(private val context: Context) : AutoCloseable {
    private data class Reply(
        val operation: String,
        val uuid: UUID? = null,
        val status: Int = BluetoothGatt.GATT_SUCCESS,
        val data: ByteArray = byteArrayOf(),
    )

    private val replies = LinkedBlockingQueue<Reply>()
    private val mtuReplies = LinkedBlockingQueue<Reply>()
    private val notices = LinkedBlockingQueue<Pair<UUID, ByteArray>>()
    private var downloadedCompressed: ByteArray? = null
    private var downloadedRaw: ByteArray? = null
    private lateinit var gatt: BluetoothGatt
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            replies.offer(Reply(if (newState == BluetoothProfile.STATE_CONNECTED) "connect" else "disconnect", status = status))
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            replies.offer(Reply("discover", status = status))
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuReplies.offer(Reply("mtu", status = status, data = packU16(mtu)))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            replies.offer(Reply("read", characteristic.uuid, status, value.copyOf()))
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            replies.offer(Reply("write", characteristic.uuid, status))
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            replies.offer(Reply("subscribe", descriptor.characteristic.uuid, status))
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == CONNECTION_STATUS || characteristic.uuid == OPERATION_STATUS || characteristic.uuid == RESTORE_STATUS || characteristic.uuid == FILE_STATUS) {
                notices.offer(characteristic.uuid to value.copyOf())
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        gatt = requireNotNull(device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE))
        await("connect")
        Log.i(TAG, "BLE link connected; waiting for encryption")
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (device.getEncryptionStatus(BluetoothDevice.TRANSPORT_LE) == null) {
            val failure = replies.peek()
            check(failure?.operation != "disconnect") { "Bluetooth encryption failed: status ${failure?.status}." }
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for Bluetooth encryption." }
            Thread.sleep(50)
        }
        Log.i(TAG, "BLE link encrypted; discovering services")
        check(gatt.discoverServices()) { "Could not start GATT service discovery." }
        await("discover")
        check(gatt.requestMtu(517)) { "Could not request a BLE MTU." }
        check(unpackU16(await("mtu")) >= 251) { "Camera MTU is too small for the captured transfer format." }
        val status = read(SECURE_STATUS)
        check(status.size == 4) { "Unexpected secure reconnect status length." }
        status[3] = 0x20
        write(SECURE_STATUS, status)
        val phoneName = requireNotNull(context.getSystemService(BluetoothManager::class.java).adapter.name)
        // XApp uses the paired phone's Bluetooth name with spaces replaced by hyphens.
        // ponytail: captured -2065 suffix; identify its meaning before widening compatibility.
        val clientName = "${phoneName.replace(' ', '-')}-2065\u0000".toByteArray(Charsets.UTF_8)
        check(phoneName.isNotBlank() && clientName.size <= 64 && '\u0000' !in phoneName) { "Invalid Bluetooth client identity." }
        val identityReference = File(context.cacheDir, "camera-ble-client-name.bin")
        if (identityReference.exists()) {
            check(identityReference.readBytes().contentEquals(clientName)) { "Client identity differs from the captured pairing identity." }
        }
        write(CLIENT_NAME, clientName)
        val model = read(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")).toString(Charsets.UTF_8).trim('\u0000', ' ')
        val firmware = read(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")).toString(Charsets.UTF_8).trim('\u0000', ' ')
        // Device Information exposes a model code, not the advertised "X-T50" name.
        check(model == "FF240001" && firmware == "01.32") { "This probe requires X-T50 firmware 1.32." }
        Log.i(TAG, "Connected and discovered X-T50 1.32 characteristics")
        subscribe(UUID.fromString("a68e3f66-0fcc-4395-8d4c-aa980b5877fa"), true)
        subscribe(UUID.fromString("bd17ba04-b76b-4892-a545-b73ba1f74dae"), true)
        subscribe(CONNECTION_STATUS, false)
        subscribe(UUID.fromString("ad06c7b7-f41a-46f4-a29a-712055319122"), false)
        var ready = read(CONNECTION_STATUS)
        val readyDeadline = SystemClock.elapsedRealtime() + 15_000
        while (ready.contentEquals(packU16(0))) {
            val remaining = readyDeadline - SystemClock.elapsedRealtime()
            check(remaining > 0) { "Timed out waiting for the camera connection to become ready." }
            ready = notice(CONNECTION_STATUS, remaining)
        }
        check(ready.contentEquals(packU16(1))) { "Camera connection is not ready (status ${ready.toList()})." }
        notices.removeIf { it.first == CONNECTION_STATUS }
        Log.i(TAG, "Camera connection handshake ready")
        // A failed previous transfer can leave the bonded camera's CCCDs enabled.
        subscribe(FILE_STATUS, false, false)
        subscribe(OPERATION_STATUS, true, false)
        subscribe(FILE_STATUS, false)
        subscribe(OPERATION_STATUS, true)
    }

    fun download(): ByteArray {
        val availability = read(UUID.fromString("a68e3f66-0fcc-4395-8d4c-aa980b5877fa"))
        check(availability.size == 2) { "Invalid camera availability response." }
        Log.i(TAG, "Camera availability: 0x${unpackU16(availability).toString(16)}")
        notices.clear()
        write(BACKUP_COMMAND, packU16(1))
        val prepared = read(OPERATION_STATUS)
        check(prepared.size == 2) { "Invalid backup preparation response." }
        Log.i(TAG, "Backup preparation state: ${unpackU16(prepared)}")
        check(prepared.contentEquals(packU16(1)) || notice(OPERATION_STATUS).contentEquals(packU16(1))) {
            "Camera refused to prepare its settings backup."
        }
        notices.removeIf { it.first == OPERATION_STATUS }
        write(UUID.fromString("7f3400fe-17e7-4b80-8a0e-81b0343c1b49"), packU32(120))
        write(UUID.fromString("051dd980-df9d-4472-a2e1-35811dd24ee1"), packU32(0))
        fileNotice(1, 0)
        val compressed = ByteArrayOutputStream()
        var expectedSequence = 0
        var checksum = -1
        while (true) {
            val chunk = read(FILE_DATA)
            check(chunk.size == 248) { "Unexpected file chunk size: ${chunk.size}." }
            val sequence = unpackU16(chunk)
            val length = unpackU32(chunk, 2)
            check(sequence == expectedSequence && length <= 242) { "Missing or malformed backup chunk." }
            if (sequence == 0xffff) {
                check(length == 2L) { "Invalid backup checksum trailer." }
                checksum = unpackU16(chunk, 6)
                fileNotice(3, 0xffff)
                break
            }
            check(sequence < 0xfffe && compressed.size() + length <= MAX_BACKUP_BYTES) { "Backup exceeds probe limits." }
            compressed.write(chunk, 6, length.toInt())
            val next = notice(FILE_STATUS)
            check(next.size == 4 && unpackU16(next) == 2) { "Unexpected file progress notification." }
            expectedSequence = unpackU16(next, 2)
            check(expectedSequence == sequence + 1 || expectedSequence == 0xffff) { "Backup chunk sequence skipped." }
        }
        val bytes = compressed.toByteArray()
        check(bytes.sumOf { it.toInt() and 0xff } and 0xffff == checksum) { "Backup transfer checksum failed." }
        val info = read(FILE_INFO)
        check(info.size == 44) { "Invalid file metadata length." }
        check(info.copyOfRange(0, 40).toString(Charsets.UTF_8).trimEnd('\u0000') == "backup.dat") { "Camera returned a different file." }
        val size = unpackU32(info, 40)
        check(size in 1..MAX_BACKUP_BYTES.toLong()) { "Invalid backup size." }
        val raw = ByteArray(size.toInt() + 1)
        val inflater = Inflater()
        try {
            inflater.setInput(bytes)
            var written = 0
            while (!inflater.finished() && written < raw.size) {
                val count = inflater.inflate(raw, written, raw.size - written)
                check(count > 0 || inflater.finished()) { "Incomplete compressed backup." }
                written += count
            }
            check(inflater.finished() && inflater.remaining == 0 && written.toLong() == size) { "Backup size or compression integrity failed." }
        } finally {
            inflater.end()
        }
        val result = raw.copyOf(size.toInt())
        check(result.size >= 16 && result.copyOfRange(0, 16).contentEquals("FUJIFILMX-BACKUP".toByteArray())) { "Unknown backup format." }
        write(UUID.fromString("68052e8a-fb91-404f-8847-0eb4be24308c"), packU16(3))
        fileNotice(5, 0xffff)
        subscribe(FILE_STATUS, false, false)
        subscribe(OPERATION_STATUS, true, false)
        downloadedCompressed = bytes.copyOf()
        downloadedRaw = result.copyOf()
        return result
    }

    /** Accepts no caller payload: this diagnostic can restore only its own fresh download. */
    fun restoreUnchanged() {
        val compressed = requireNotNull(downloadedCompressed) { "Download a fresh backup before restoring." }
        restoreCompressed(compressed)
    }

    /** Only two observed C7 Color states; preserve fresh background bytes in the proof. */
    fun restoreColorReference(): ByteArray {
        val fresh = requireNotNull(downloadedRaw) { "Read the current camera state first." }
        val file = File(context.cacheDir, "camera-ble-color-reference.dat")
        check(fresh.size == 70524 && file.length() == fresh.size.toLong()) { "Missing X-T50 Color reference backup." }
        val reference = file.readBytes()
        val from = fresh[69748].toInt() and 0xff
        val to = reference[69748].toInt() and 0xff
        check((from == 3 && to == 0) || (from == 0 && to == 3)) { "Only observed Color +4 / 0 states are allowed." }
        // ponytail: these two bytes varied independently with identical integrity metadata in this session only.
        check(fresh.indices.all { it in 264..267 || it == 69748 || it in 63164..63165 || fresh[it] == reference[it] }) {
            "Reference differs from fresh state outside the observed Color change; refusing stale restore."
        }
        check(unpackU32(reference, 264) - unpackU32(fresh, 264) == (to - from).toLong()) {
            "Reference metadata differs from the observed Color transition."
        }
        val target = fresh.copyOf()
        reference.copyInto(target, 264, 264, 268)
        target[69748] = reference[69748]
        // ponytail: only the measured Color transition; general backup integrity/encoding is still unknown.
        val compressed = ByteArrayOutputStream()
        DeflaterOutputStream(compressed).use { it.write(target) }
        restoreCompressed(compressed.toByteArray())
        return target
    }

    /** Bounded C7 experiment, using references independently reproduced from USB measurements. */
    fun restoreRecipeReference(): ByteArray {
        val fresh = requireNotNull(downloadedRaw) { "Read the current camera state first." }
        val from = File(context.cacheDir, "camera-ble-recipe-from.dat").readBytes()
        val to = File(context.cacheDir, "camera-ble-recipe-to.dat").readBytes()
        check(fresh.size == 70524 && from.size == fresh.size && to.size == fresh.size) { "Missing X-T50 recipe references." }
        val fields = setOf(69516, 69517, 69614, 69640, 69642, 69672, 69675, 69729,
            69745, 69748, 69750, 69752, 69754, 69756, 69757, 69758, 69759) + (70122..70147)
        check(from.indices.all { it in fields || it in 264..267 || from[it] == to[it] }) {
            "References differ outside the measured C7 recipe experiment."
        }
        check(fresh.copyOfRange(63356, fresh.size).contentEquals(from.copyOfRange(63356, from.size))) {
            "Current recipes differ from the experiment baseline; refusing stale recipe write."
        }
        val delta = fields.sumOf { (to[it].toInt() and 0xff) - (from[it].toInt() and 0xff) }
        check(delta != 0 && unpackU32(to, 264) - unpackU32(from, 264) == delta.toLong()) {
            "Reference metadata differs from the independently measured additive update."
        }
        for (reference in listOf(from, to)) {
            check(reference.copyOfRange(0, 16).contentEquals("FUJIFILMX-BACKUP".toByteArray()))
            check(reference.copyOfRange(20, 25).toString(Charsets.US_ASCII) == "X-T50")
            fun code(at: Int) = reference[at].toInt() and 0xff
            check(code(69614) in setOf(0, 10) && unpackU16(reference, 69516) in 2500..10000)
            check(code(69640) in 0..18 && code(69642) in 0..18)
            check(code(69672) in 0..8 && code(69675) in 1..11)
            check(code(69729) in setOf(17, 18) && code(69745) in 1..3)
            check(code(69748) in setOf(0, 3, 4, 5, 6, 7, 8, 9, 10) && code(69750) in 0..8)
            check(code(69752) in 0..12 && code(69754) in 0..12)
            check(code(69756) in 0..2 && code(69757) in 0..2 && code(69758) in 0..2 && code(69759) in 0..1)
            val name = reference.copyOfRange(70122, 70148)
            check(name.indexOf(0.toByte()) in 1..25 && name.all { it == 0.toByte() || (it.toInt() and 0xff) in 32..126 }) {
                "Only measured ASCII recipe names are allowed in this experiment."
            }
        }
        val metadata = unpackU32(fresh, 264) + delta
        check(metadata in 0..0xffffffffL) { "Metadata update overflow." }
        val target = fresh.copyOf()
        fields.forEach { target[it] = to[it] }
        packU32(metadata.toInt()).copyInto(target, 264)
        val compressed = ByteArrayOutputStream()
        DeflaterOutputStream(compressed).use { it.write(target) }
        // ponytail: measured relative C7 update only; other slots and a general checksum algorithm remain unverified.
        restoreCompressed(compressed.toByteArray())
        return target
    }

    private fun restoreCompressed(compressed: ByteArray) {
        check(compressed.size <= MAX_BACKUP_BYTES && (compressed.size + 119) / 120 < 0xffff) {
            "Compressed backup exceeds the observed transfer limits."
        }
        val checksum = packU16(compressed.sumOf { it.toInt() and 0xff } and 0xffff)
        // ponytail: observed X-T50 1.32 operation-switch pause; readiness signal not yet identified.
        Thread.sleep(3_000)
        subscribe(FILE_STATUS, false)
        subscribe(RESTORE_STATUS, true)
        notices.clear()
        write(RESTORE_COMMAND, packU16(1))
        val prepared = read(RESTORE_STATUS)
        check(prepared.contentEquals(packU16(1)) || notice(RESTORE_STATUS).contentEquals(packU16(1))) {
            "Camera refused to prepare settings restore."
        }
        notices.removeIf { it.first == RESTORE_STATUS }
        write(UUID.fromString("7f3400fe-17e7-4b80-8a0e-81b0343c1b49"), packU32(120))
        write(UUID.fromString("051dd980-df9d-4472-a2e1-35811dd24ee1"), packU32(0))
        val info = ByteArray(44)
        "backup.dat".toByteArray(Charsets.UTF_8).copyInto(info)
        packU32(compressed.size).copyInto(info, 40)
        write(FILE_INFO, info)
        fileNotice(1, 0)
        var offset = 0
        var sequence = 0
        while (offset < compressed.size) {
            val length = minOf(120, compressed.size - offset)
            val last = offset + length == compressed.size
            // ponytail: reproduce XApp's observed final checksum twice; other upload variants unverified.
            val payload = compressed.copyOfRange(offset, offset + length) + if (last) checksum else byteArrayOf()
            val packet = packU16(if (last) 0xffff else sequence) + packU32(payload.size) +
                payload + if (last) checksum else packU16(0)
            write(FILE_DATA, packet)
            offset += length
            sequence++
            if (last) fileNotice(3, 0xffff) else fileNotice(2, sequence)
        }
        write(UUID.fromString("68052e8a-fb91-404f-8847-0eb4be24308c"), packU16(3))
        fileNotice(5, 0xffff)
        subscribe(FILE_STATUS, false, false)
        subscribe(RESTORE_STATUS, true, false)
        Log.i(TAG, "Settings restore transfer acknowledged; reconnecting for verification")
    }

    private fun characteristic(uuid: UUID): BluetoothGattCharacteristic =
        requireNotNull(gatt.services.mapNotNull { it.getCharacteristic(uuid) }.singleOrNull()) {
            "Camera does not expose expected characteristic $uuid."
        }

    private fun read(uuid: UUID): ByteArray {
        check(gatt.readCharacteristic(characteristic(uuid))) { "Could not start BLE read for $uuid." }
        return await("read", uuid)
    }

    private fun write(uuid: UUID, value: ByteArray) {
        check(gatt.writeCharacteristic(characteristic(uuid), value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == 0) {
            "Could not start BLE write for $uuid."
        }
        await("write", uuid)
    }

    private fun subscribe(uuid: UUID, indication: Boolean, enabled: Boolean = true) {
        val characteristic = characteristic(uuid)
        check(gatt.setCharacteristicNotification(characteristic, enabled)) { "Could not configure notifications for $uuid." }
        val descriptor = requireNotNull(characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")))
        val value = if (!enabled) BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE else
            if (indication) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        check(gatt.writeDescriptor(descriptor, value) == 0) { "Could not subscribe to $uuid." }
        await("subscribe", uuid)
    }

    private fun await(operation: String, uuid: UUID? = null): ByteArray {
        // Android may report an existing link's MTU before any request from this client.
        val queue = if (operation == "mtu") mtuReplies else replies
        val reply = queue.poll(15, TimeUnit.SECONDS) ?: error("Timed out waiting for BLE $operation $uuid.")
        check(reply.operation == operation && reply.uuid == uuid && reply.status == BluetoothGatt.GATT_SUCCESS) {
            "BLE $operation $uuid failed: ${reply.operation}, status ${reply.status}."
        }
        return reply.data
    }

    private fun notice(uuid: UUID, timeoutMs: Long = 15_000): ByteArray {
        val notice = notices.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: error("Timed out waiting for camera notification $uuid.")
        check(notice.first == uuid) { "Unexpected notification ${notice.first}, expected $uuid." }
        return notice.second
    }

    private fun fileNotice(state: Int, sequence: Int) {
        val value = notice(FILE_STATUS)
        check(value.size == 4 && unpackU16(value) == state && unpackU16(value, 2) == sequence) {
            "Unexpected file transfer state; expected $state/$sequence."
        }
    }

    override fun close() {
        if (::gatt.isInitialized) {
            gatt.disconnect()
            gatt.close()
        }
    }
}
