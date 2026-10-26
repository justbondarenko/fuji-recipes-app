package dev.bondarenko.fujirecipes.camera.bluetooth

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
import android.os.SystemClock
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.ptp.unpackU32
import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

private val SECURE_STATUS = UUID.fromString("f557d96b-8284-4667-8793-b971c1deca2a")
private val CLIENT_NAME = UUID.fromString("85b9163e-62d1-49ff-a6f5-054b4630d4a1")
private val CONNECTION_STATUS = UUID.fromString("f9150137-5d40-4801-a8dc-f7fc5b01da50")
private val OPERATION_STATUS = UUID.fromString("11438c83-cfb0-4511-841b-759e0d2321c8")
private val BACKUP_COMMAND = UUID.fromString("f14e7a88-6f57-4872-a69b-b47ef3295c9b")
private val FILE_STATUS = UUID.fromString("2e27ed9f-5506-41cd-ba48-dac06669ad95")
private val FILE_DATA = UUID.fromString("ac0c799a-fa6c-4df5-bbc5-bb95cce7e6ea")
private val FILE_INFO = UUID.fromString("c922ac69-9480-4348-8f4b-9ee29bc30d1d")


/** Read-only X-T50 1.32 BLE transport, measured by CameraBluetoothHardwareTest.
 * Secure reconnect follows petabyt/libfuji (MIT); transfer framing comes from our HCI captures.
 * Call serially on Dispatchers.IO. There is deliberately no restore/settings-write entry point.
 */
@SuppressLint("MissingPermission")
class BluetoothCamera(private val context: Context, private val onDisconnected: () -> Unit) : AutoCloseable {
    private data class Reply(
        val operation: String,
        val uuid: UUID? = null,
        val status: Int = BluetoothGatt.GATT_SUCCESS,
        val data: ByteArray = byteArrayOf(),
    )

    private val replies = LinkedBlockingQueue<Reply>(64)
    private val mtuReplies = LinkedBlockingQueue<Reply>(64)
    private val notices = LinkedBlockingQueue<Pair<UUID, ByteArray>>(128)
    private var lastDownloadAt = 0L
    @Volatile private var closed = false
    private lateinit var gatt: BluetoothGatt
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            replies.offer(Reply(if (newState == BluetoothProfile.STATE_CONNECTED) "connect" else "disconnect", status = status))
            if (newState == BluetoothProfile.STATE_DISCONNECTED && !closed) onDisconnected()
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

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33) replies.offer(Reply("read", characteristic.uuid, status, characteristic.value?.copyOf() ?: byteArrayOf()))
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) onCharacteristicChanged(gatt, characteristic, characteristic.value?.copyOf() ?: byteArrayOf())
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
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (Build.VERSION.SDK_INT >= 37 && device.getEncryptionStatus(BluetoothDevice.TRANSPORT_LE) == null) {
            val failure = replies.peek()
            check(failure?.operation != "disconnect") { "Bluetooth encryption failed: status ${failure?.status}." }
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for Bluetooth encryption." }
            Thread.sleep(50)
        }
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
        write(CLIENT_NAME, clientName)
        val model = read(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")).toString(Charsets.UTF_8).trim('\u0000', ' ')
        val firmware = read(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")).toString(Charsets.UTF_8).trim('\u0000', ' ')
        // Device Information exposes a model code, not the advertised "X-T50" name.
        check(model == "FF240001" && firmware == "01.32") { "Bluetooth recipes currently require X-T50 firmware 1.32." }
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
        // A failed previous transfer can leave the bonded camera's CCCDs enabled.
        subscribe(FILE_STATUS, false, false)
        subscribe(OPERATION_STATUS, true, false)
    }

    fun readRecipes(): List<SlotRecipe> = BluetoothBackup.decode(download())

    private fun download(): ByteArray {
        // ponytail: observed X-T50 operation-switch pause; use a readiness signal when identified.
        val pause = 3_000 - (SystemClock.elapsedRealtime() - lastDownloadAt)
        if (pause > 0) Thread.sleep(pause)
        subscribe(FILE_STATUS, false)
        subscribe(OPERATION_STATUS, true)
        val availability = read(UUID.fromString("a68e3f66-0fcc-4395-8d4c-aa980b5877fa"))
        check(availability.size == 2) { "Invalid camera availability response." }
        notices.clear()
        write(BACKUP_COMMAND, packU16(1))
        val prepared = read(OPERATION_STATUS)
        check(prepared.size == 2) { "Invalid backup preparation response." }
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
            check(sequence < 0xfffe && compressed.size() + length <= BluetoothBackup.SIZE) { "Backup exceeds probe limits." }
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
        check(size in 1..BluetoothBackup.SIZE.toLong()) { "Invalid backup size." }
        check(size == BluetoothBackup.SIZE.toLong()) { "Unsupported Bluetooth backup length." }
        val result = BluetoothBackup.inflate(bytes, size.toInt(), checksum)
        write(UUID.fromString("68052e8a-fb91-404f-8847-0eb4be24308c"), packU16(3))
        fileNotice(5, 0xffff)
        subscribe(FILE_STATUS, false, false)
        subscribe(OPERATION_STATUS, true, false)
        lastDownloadAt = SystemClock.elapsedRealtime()
        return result
    }

    private fun characteristic(uuid: UUID): BluetoothGattCharacteristic =
        requireNotNull(gatt.services.mapNotNull { it.getCharacteristic(uuid) }.singleOrNull()) {
            "Camera does not expose expected characteristic $uuid."
        }

    private fun read(uuid: UUID): ByteArray {
        check(gatt.readCharacteristic(characteristic(uuid))) { "Could not start BLE read for $uuid." }
        return await("read", uuid)
    }

    @Suppress("DEPRECATION")
    private fun write(uuid: UUID, value: ByteArray) {
        val ch = characteristic(uuid)
        val started = if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeCharacteristic(ch, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == 0
        } else {
            ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ch.value = value
            gatt.writeCharacteristic(ch)
        }
        check(started) {
            "Could not start BLE write for $uuid."
        }
        await("write", uuid)
    }

    @Suppress("DEPRECATION")
    private fun subscribe(uuid: UUID, indication: Boolean, enabled: Boolean = true) {
        val characteristic = characteristic(uuid)
        check(gatt.setCharacteristicNotification(characteristic, enabled)) { "Could not configure notifications for $uuid." }
        val descriptor = requireNotNull(characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")))
        val value = if (!enabled) BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE else
            if (indication) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val started = if (Build.VERSION.SDK_INT >= 33) gatt.writeDescriptor(descriptor, value) == 0 else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }
        check(started) { "Could not subscribe to $uuid." }
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
        closed = true
        if (::gatt.isInitialized) {
            gatt.disconnect()
            gatt.close()
        }
    }
}
