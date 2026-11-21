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
 * The probe is read-only. Opt in to the C2 production writer test with -e bluetoothAppWrite true.
 * Captured protocol: specs/plans/bluetooth-recipes.md.
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

    /** Compare the two readers and a refresh without sending any settings restore. */
    @Test
    @RequiresApi(37)
    fun traceReadOnlyBackupState() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothReadStateTrace") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        val checkpoint = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val reference = File(context.noBackupFilesDir, "bluetooth-slot-1-hardware-original.dat")
        val protected = (checkpoint.listFiles()?.filter { it.isFile }.orEmpty() + reference + File(context.filesDir, "library.json"))
            .associateWith { it.readBytes() }
        val directory = File(context.cacheDir, "camera-ble-read-state-${UUID.randomUUID()}")
        val snapshots = mutableListOf<ByteArray>()
        fun keep(name: String, raw: ByteArray) { snapshots += saveBackupEvidence(directory, name, raw) }
        try {
            BleBackupProbe(context).use { probe -> probe.connect(device); keep("independent-before.dat", probe.download()) }
            Thread.sleep(3_000)
            BluetoothCamera(context) {}.use { camera ->
                camera.connect(device)
                camera.readBackup { keep("shipping-first.dat", it) }
                Thread.sleep(3_000)
                camera.readBackup { keep("shipping-refresh.dat", it) }
            }
            Thread.sleep(3_000)
            BleBackupProbe(context).use { probe -> probe.connect(device); keep("independent-after.dat", probe.download()) }
            check(snapshots.size == 4)
            snapshots.forEachIndexed { index, raw ->
                BluetoothBackup.decode(raw)
                check(raw.copyOfRange(63356, raw.size).contentEquals(protected.getValue(reference).copyOfRange(63356, raw.size))) {
                    "Recipe records differ in read-only snapshot ${index + 1}."
                }
                Log.i(TAG, "Read-only snapshot ${index + 1}: AF-area X=${raw[2136].toInt() and 255}; " +
                    "prefix differences=${(0 until 59920).filter { raw[it] != snapshots.first()[it] }}")
            }
        } finally {
            protected.forEach { (file, bytes) -> check(file.readBytes().contentEquals(bytes)) { "Protected evidence changed: ${file.name}" } }
            checkXAppStopped()
        }
    }

    /** Separate opt-in: exercise the shipping slot writer, verify independently, then restore. */
    @Test
    @RequiresApi(37)
    fun writeSlotWithAppTransportAndRestore() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothAppWrite") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val slot = 2
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalFile = File(context.noBackupFilesDir, "bluetooth-slot-$slot-hardware-original.dat")
        check(!originalFile.exists()) { "A previous slot proof is unresolved; inspect its saved original before retrying." }
        val evidence = File(context.noBackupFilesDir, "bluetooth-slot-$slot-proof-${UUID.randomUUID()}")
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        fun independentBackup() = BleBackupProbe(context).use { probe -> probe.connect(device); probe.download() }
        val original = saveBackupEvidence(evidence, "independent-before.dat", independentBackup())
        val recipe = BluetoothBackup.decode(original).single { it.slot == slot }
        val name = recipe.name
        val testName = "TEST Bluetooth C2"
        val color = (recipe.settings.getValue("color") as Number).toInt()
        val changedColor = if (color == 0) 1 else 0
        val changedSettings = buildJsonObject { put("color", changedColor) }
        val originalSettings = buildJsonObject { put("color", color) }
        val expectedForward = BluetoothBackup.patchSlot(original, slot, changedSettings, testName)
        check(BluetoothBackup.patchSlot(expectedForward, slot, originalSettings, name).contentEquals(original)) {
            "The current C2 recipe cannot be restored exactly by this test."
        }
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
        var forwardVerified = false
        try {
            controller.selectTransport(CameraTransport.BLUETOOTH)
            withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
            delay(3_000)
            connect()
            try {
                val result = controller.writeBluetoothRecipe(slot, testName, changedSettings)
                check(result is WriteResult.Success && result.outcome.bluetoothSettingsVerified) { "Forward write unverified: $result" }
                val recovery = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
                for (name in listOf("original", "target", "readback")) {
                    saveBackupEvidence(evidence, "forward-$name.dat", File(recovery, "$name.dat").readBytes())
                }
                disconnect()
                val actual = saveBackupEvidence(evidence, "independent-forward.dat", independentBackup())
                val expected = File(context.noBackupFilesDir, "bluetooth-c7-recovery/target.dat").readBytes()
                check(BluetoothBackup.compare(actual, expected).settingsVerified) { "Independent settings differ from the verified target." }
                val written = BluetoothBackup.decode(actual)[slot - 1]
                check(written.settings["color"] == changedColor && written.name == testName)
                val expectedRecipes = original.copyOfRange(63356, original.size)
                val start = (slot - 1) * 1024
                expectedRecipes[start + 248] = if (changedColor == 0) 0 else 6
                expectedRecipes.fill(0, start + 622, start + 648)
                testName.toByteArray(Charsets.US_ASCII).copyInto(expectedRecipes, start + 622)
                check(actual.copyOfRange(63356, actual.size).contentEquals(expectedRecipes)) {
                    "Independent read found recipe changes beyond the C2 name and Color."
                }
                forwardVerified = true
            } finally {
                // Only an independently verified forward state may be reversed; otherwise stop for inspection.
                if (forwardVerified) {
                    disconnect()
                    connect()
                    val result = controller.writeBluetoothRecipe(slot, name, originalSettings)
                    check(result is WriteResult.Success && result.outcome.bluetoothSettingsVerified) { "Original C$slot restoration unverified: $result" }
                    val recovery = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
                    for (name in listOf("original", "target", "readback")) {
                        saveBackupEvidence(evidence, "restore-$name.dat", File(recovery, "$name.dat").readBytes())
                    }
                    forwardVerified = false
                    disconnect()
                    val restored = saveBackupEvidence(evidence, "independent-restored.dat", independentBackup())
                    check(BluetoothBackup.compare(restored, original).settingsVerified) { "Original backed-up settings differ after restoration." }
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
    fun cancelC2BeforeUploadLeavesCameraUnchanged() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothAppCancel") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java)
            .adapter.bondedDevices.singleOrNull { it.name == "X-T50" })
        fun independentBackup() = BleBackupProbe(context).use { it.connect(device); it.download() }
        val original = independentBackup()
        val controller = (context.applicationContext as dev.bondarenko.fujirecipes.FujiRecipesApp).container.cameraController
        try {
            controller.selectTransport(CameraTransport.BLUETOOTH)
            withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
            delay(3_000)
            controller.connect()
            val state = withTimeout(30_000) { controller.state.first { it is CameraState.Connected || it is CameraState.Error } }
            check(state is CameraState.Connected) { "Cancellation proof could not connect: $state" }
            var checks = 0
            val result = controller.writeBluetoothRecipe(2, "TEST C2 Cancel", buildJsonObject { put("color", 0) }) { ++checks >= 2 }
            check(checks == 2 && result is WriteResult.Failure && result.warning == null) { "Cancellation failed: $result" }
            check(!File(context.noBackupFilesDir, "bluetooth-c7-recovery/pending").exists()) { "An upload was started despite cancellation." }
            delay(3_000)
            check(BluetoothBackup.compare(independentBackup(), original).settingsVerified) { "Backed-up camera settings changed after cancellation." }
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
            val result = controller.writeBluetoothRecipe(2, "TEST C2 Guard", buildJsonObject {}) {
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
            // Read-only downloads can change non-restored memory; retain the complete raw difference list.
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
    fun compareBackupWithReadOnlyDeviceInformation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("bluetoothDeviceInfo") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 37)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        checkXAppStopped()
        val recovery = File(context.noBackupFilesDir, "bluetooth-c7-recovery")
        val protected = listOf("original.dat", "target.dat", "pending").map { File(recovery, it) } +
            File(context.filesDir, "library.json")
        val saved = protected.associateWith { it.readBytes() }
        val directory = File(context.cacheDir, "camera-ble-info-probe")
        check(!directory.exists() || directory.deleteRecursively()) { "Could not clear old device-info probe results." }
        check(directory.mkdirs())
        val device = requireNotNull(context.getSystemService(BluetoothManager::class.java).adapter
            .bondedDevices.singleOrNull { it.name == "X-T50" })
        try {
            BleBackupProbe(context).use { probe ->
                probe.connect(device)
                val before = probe.download()
                File(directory, "before.dat").writeBytes(before)
                val info = probe.readDeviceInformation()
                info.forEach { (name, bytes) -> File(directory, "$name.dat").writeBytes(bytes) }
                // No identities or raw characteristic values in logs; private files retain the evidence.
                Log.i(TAG, "Read-only device information: ${info.mapValues { it.value.size }}")
                Thread.sleep(3_000)
                val after = probe.download()
                File(directory, "after.dat").writeBytes(after)
                for (raw in listOf(before, after)) {
                    BluetoothBackup.decode(raw)
                    check(raw.copyOfRange(63356, raw.size).contentEquals(saved.getValue(File(recovery, "original.dat"))
                        .copyOfRange(63356, raw.size))) { "The camera recipe records differ from the retained original." }
                }
                Log.i(TAG, "Device-info probe backup differences: ${before.indices.filter { before[it] != after[it] }}")
            }
            checkXAppStopped()
        } finally {
            protected.forEach { check(it.readBytes().contentEquals(saved.getValue(it))) { "Protected file changed: ${it.name}" } }
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
        val fileInventory = InstrumentationRegistry.getArguments().getString("bluetoothFileInventory") == "true"
        val bytes = BleBackupProbe(context).use { probe ->
            probe.connect(device)
            val bytes = probe.download(inspectFileCount = fileInventory)
            if (fileInventory) BluetoothBackup.decode(bytes)
            checkXAppStopped()
            Log.i(TAG, "Verified backup download: ${bytes.size} bytes")
            bytes
        }
        checkXAppStopped()
        output.writeBytes(bytes)
    }

    private fun saveBackupEvidence(directory: File, name: String, raw: ByteArray): ByteArray {
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, name)
        check(!file.exists()) { "Proof evidence must not be overwritten." }
        file.outputStream().use { it.write(raw); it.fd.sync() }
        check(file.readBytes().contentEquals(raw))
        return raw
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
private val FILE_STATUS = UUID.fromString("2e27ed9f-5506-41cd-ba48-dac06669ad95")
private val FILE_DATA = UUID.fromString("ac0c799a-fa6c-4df5-bbc5-bb95cce7e6ea")
private val FILE_INFO = UUID.fromString("c922ac69-9480-4348-8f4b-9ee29bc30d1d")

/** Sequential GATT operations for independent X-T50 1.32 backup reads. */
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
            if (characteristic.uuid == CONNECTION_STATUS || characteristic.uuid == OPERATION_STATUS || characteristic.uuid == FILE_STATUS) {
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
    }

    fun download(inspectFileCount: Boolean = false): ByteArray {
        subscribe(FILE_STATUS, false)
        subscribe(OPERATION_STATUS, true)
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
        if (inspectFileCount) {
            val count = read(UUID.fromString("e3fbbfcf-f326-4b0f-82cf-b00ae1b107a2"))
            check(count.size == 4) { "Invalid backup file-count length." }
            Log.i(TAG, "Prepared settings backup file count: ${unpackU32(count)}")
        }
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
        return result
    }

    /** Standard Device Information and the two lens read characteristics documented by libfuji. */
    fun readDeviceInformation(): Map<String, ByteArray> {
        val identifiers = linkedMapOf(
            "manufacturer" to "00002a29-0000-1000-8000-00805f9b34fb",
            "model" to "00002a24-0000-1000-8000-00805f9b34fb",
            "serial" to "00002a25-0000-1000-8000-00805f9b34fb",
            "hardware" to "00002a27-0000-1000-8000-00805f9b34fb",
            "firmware" to "00002a26-0000-1000-8000-00805f9b34fb",
            "software" to "00002a28-0000-1000-8000-00805f9b34fb",
            "lens-name" to "68887a7a-10a7-4a4a-aadd-6f9db1abaa0e",
            "lens-firmware" to "621a98d8-6314-4c3e-a683-eea1d6166606",
        )
        return identifiers.mapNotNull { (name, value) ->
            val uuid = UUID.fromString(value)
            if (gatt.services.none { ((it.getCharacteristic(uuid)?.properties ?: 0) and BluetoothGattCharacteristic.PROPERTY_READ) != 0 }) null
            else name to read(uuid).also { check(it.size <= 512) { "Device information exceeds the GATT value limit." } }
        }.toMap()
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
