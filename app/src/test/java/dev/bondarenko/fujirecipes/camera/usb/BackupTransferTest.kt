package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.FakeCamera
import dev.bondarenko.fujirecipes.camera.plan.BACKUP_OBJECT_HANDLE
import dev.bondarenko.fujirecipes.camera.plan.MAX_BACKUP_BYTES
import dev.bondarenko.fujirecipes.camera.plan.MIN_BACKUP_BYTES
import dev.bondarenko.fujirecipes.camera.plan.SEND_OBJECT_INFO_DATASET_BYTES
import dev.bondarenko.fujirecipes.camera.ptp.Operation
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.ResponseCode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTransferTest {

    private fun connected(camera: FakeCamera): PtpSession =
        PtpSession(PtpTransport(camera)).also { it.open() }

    /** A body carrying a settings blob at handle 0, as one in the right USB mode does. */
    private fun cameraWithBackup(blob: ByteArray): FakeCamera = FakeCamera().apply {
        objectInfos[BACKUP_OBJECT_HANDLE] = ByteArray(64)
        objects[BACKUP_OBJECT_HANDLE] = blob
    }

    private fun blob(size: Int): ByteArray = ByteArray(size) { (it % 251).toByte() }

    // ─── Download ───────────────────────────────────────────────────────────

    @Test
    fun `reads the whole settings blob off the body`() {
        val expected = blob(40_000)
        val session = connected(cameraWithBackup(expected))

        assertContentEquals(expected, downloadBackup(session))
    }

    /**
     * A body with no object at handle 0 is a body in the wrong USB mode, and that is the
     * sentence the user needs — not a bare response code.
     */
    @Test
    fun `a body with no backup object says so in terms of the USB mode`() {
        val session = connected(FakeCamera())

        val error = assertFailsWith<BackupError> { downloadBackup(session) }

        assertTrue(error.message!!.contains("USB RAW CONV./BACKUP RESTORE"))
        assertEquals(ResponseCode.INVALID_OBJECT_HANDLE, error.ptpCode)
    }

    @Test
    fun `a body that describes an object then refuses to send it is a different failure`() {
        val camera = FakeCamera().apply { objectInfos[BACKUP_OBJECT_HANDLE] = ByteArray(64) }
        val session = connected(camera)

        val error = assertFailsWith<BackupError> { downloadBackup(session) }

        assertTrue(error.message!!.contains("refused to send"))
    }

    /**
     * The failure worth guarding: a file that is written, named after the camera, and junk on
     * the day it is needed.
     */
    @Test
    fun `a blob too short to be a backup is refused rather than saved`() {
        val session = connected(cameraWithBackup(blob(MIN_BACKUP_BYTES - 1)))

        val error = assertFailsWith<BackupError> { downloadBackup(session) }

        assertTrue(error.message!!.contains("Nothing was saved"))
    }

    // ─── Restore ────────────────────────────────────────────────────────────

    @Test
    fun `a restore announces the object then sends it`() {
        val camera = FakeCamera()
        val session = connected(camera)
        val payload = blob(12_345)

        restoreBackup(session, payload)

        assertEquals(SEND_OBJECT_INFO_DATASET_BYTES, camera.sentObjectInfo!!.size)
        assertContentEquals(payload, camera.sentObject)
    }

    /** The announced size and the payload must agree, or the camera writes a truncated blob. */
    @Test
    fun `the announced size is the size actually sent`() {
        val camera = FakeCamera()
        val session = connected(camera)
        val payload = blob(0x1234)

        restoreBackup(session, payload)

        val dataset = camera.sentObjectInfo!!
        val announced = (dataset[8].toInt() and 0xff) or
            ((dataset[9].toInt() and 0xff) shl 8) or
            ((dataset[10].toInt() and 0xff) shl 16) or
            ((dataset[11].toInt() and 0xff) shl 24)

        assertEquals(payload.size, announced)
    }

    /** The guard nearest the wire. Nothing reaches the camera at all. */
    @Test
    fun `a file that cannot be a backup never reaches the camera`() {
        val camera = FakeCamera()
        val session = connected(camera)

        assertFailsWith<BackupError> { restoreBackup(session, blob(4)) }
        assertFailsWith<BackupError> { restoreBackup(session, blob(MAX_BACKUP_BYTES + 1)) }

        assertNull(camera.sentObjectInfo)
        assertNull(camera.sentObject)
    }

    /**
     * A refusal at the announcement is the safe failure: the camera has not been told to expect
     * anything, so the message may promise that nothing was written.
     */
    @Test
    fun `a refused announcement promises nothing was written`() {
        val camera = FakeCamera()
        val session = connected(camera)
        camera.refuseOperation[Operation.SEND_OBJECT_INFO] = ResponseCode.ACCESS_DENIED

        val error = assertFailsWith<BackupError> { restoreBackup(session, blob(9_000)) }

        assertTrue(error.message!!.contains("Nothing was written"))
        assertEquals(ResponseCode.ACCESS_DENIED, error.ptpCode)
        assertNull(camera.sentObject)
    }

    /**
     * A refusal *after* the announcement is the unsafe one, and the message must not claim
     * the camera is untouched — the difference between a user who checks and one who assumes.
     */
    @Test
    fun `a failure partway through does not claim the camera is untouched`() {
        val camera = FakeCamera()
        val session = connected(camera)
        camera.refuseOperation[Operation.SEND_OBJECT] = ResponseCode.GENERAL_ERROR

        val error = assertFailsWith<BackupError> { restoreBackup(session, blob(9_000)) }

        assertTrue(error.message!!.contains("failed partway"))
        assertTrue(error.message!!.contains("Check the camera"))
    }

    // ─── Round trip ─────────────────────────────────────────────────────────

    @Test
    fun `what comes off a body is what goes back to it`() {
        val original = blob(37_501)
        val source = connected(cameraWithBackup(original))
        val downloaded = downloadBackup(source)

        val target = FakeCamera()
        restoreBackup(connected(target), downloaded)

        assertContentEquals(original, target.sentObject)
    }

    /** Large payloads cross more than one bulk read, so reassembly is part of the contract. */
    @Test
    fun `a blob larger than one bulk transfer is reassembled intact`() {
        val expected = blob(80_000)
        val camera = cameraWithBackup(expected).apply { chunkSize = 4_096 }

        assertContentEquals(expected, downloadBackup(connected(camera)))
    }
}
