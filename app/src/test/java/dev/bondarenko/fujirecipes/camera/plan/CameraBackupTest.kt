package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CameraBackupTest {

    // ─── The SendObjectInfo dataset ─────────────────────────────────────────

    @Test
    fun `the dataset is the fixed size the captures show, whatever the payload`() {
        assertEquals(1088, sendObjectInfoDataset(1_000).size)
        assertEquals(1088, sendObjectInfoDataset(90_000).size)
    }

    /**
     * The twelve bytes that carry anything: storage 0, format 0x5000, protection 0, size.
     * Everything after them is zero, and a stray non-zero would be read by the camera as a
     * field it was never given.
     */
    @Test
    fun `the dataset carries storage, format, protection and size, then nothing`() {
        val dataset = sendObjectInfoDataset(0x1234)

        assertEquals(listOf(0, 0, 0, 0), dataset.take(4).map { it.toInt() and 0xff })
        // 0x5000 little-endian.
        assertEquals(0x00, dataset[4].toInt() and 0xff)
        assertEquals(0x50, dataset[5].toInt() and 0xff)
        assertEquals(listOf(0, 0), dataset.slice(6..7).map { it.toInt() and 0xff })
        // The size, little-endian across four bytes.
        assertEquals(0x34, dataset[8].toInt() and 0xff)
        assertEquals(0x12, dataset[9].toInt() and 0xff)
        assertEquals(0x00, dataset[10].toInt() and 0xff)
        assertEquals(0x00, dataset[11].toInt() and 0xff)

        assertEquals(0, dataset.drop(12).count { it.toInt() != 0 })
    }

    @Test
    fun `a size beyond four bytes of the payload is carried correctly`() {
        val dataset = sendObjectInfoDataset(99_999)
        // 99_999 = 0x0001869F
        assertEquals(0x9f, dataset[8].toInt() and 0xff)
        assertEquals(0x86, dataset[9].toInt() and 0xff)
        assertEquals(0x01, dataset[10].toInt() and 0xff)
        assertEquals(0x00, dataset[11].toInt() and 0xff)
    }

    /** The guard nearest the wire: a caller that skipped its own check cannot reach the camera. */
    @Test
    fun `a size no camera could have produced is refused outright`() {
        assertFailsWith<IllegalArgumentException> { sendObjectInfoDataset(0) }
        assertFailsWith<IllegalArgumentException> { sendObjectInfoDataset(MIN_BACKUP_BYTES - 1) }
        assertFailsWith<IllegalArgumentException> { sendObjectInfoDataset(MAX_BACKUP_BYTES + 1) }
    }

    // ─── Filenames ──────────────────────────────────────────────────────────

    @Test
    fun `a filename carries the model and survives a round trip`() {
        val name = backupFilename("X-T50", "20260908-2117")

        assertEquals("fuji-backup-X-T50-20260908-2117.bin", name)
        assertEquals("X-T50", modelFromBackupFilename(name))
    }

    @Test
    fun `a model with awkward characters still produces a usable name`() {
        val name = backupFilename("GFX100S II", "20260908-2117")

        assertEquals("fuji-backup-GFX100S-II-20260908-2117.bin", name)
        assertEquals("GFX100S-II", modelFromBackupFilename(name))
    }

    @Test
    fun `a body that named itself nothing still gets a filename`() {
        assertEquals("fuji-backup-camera-20260908-2117.bin", backupFilename("   ", "20260908-2117"))
    }

    @Test
    fun `a name this app did not write claims no model`() {
        assertNull(modelFromBackupFilename("backup.bin"))
        assertNull(modelFromBackupFilename("fuji-backup-X-T50.bin"))
        assertNull(modelFromBackupFilename("my copy of fuji-backup-X-T50-20260908-2117.bin"))
        assertNotNull(modelFromBackupFilename("  fuji-backup-X-T5-20260101-0000.bin  "))
    }

    // ─── Matching a file to the connected body ──────────────────────────────

    @Test
    fun `punctuation does not make two names of the same body disagree`() {
        val name = backupFilename("X-T50", "20260908-2117")

        assertEquals(BackupMatch.SAME_MODEL, backupMatch(name, "X-T50"))
        assertEquals(BackupMatch.SAME_MODEL, backupMatch(name, "XT50"))
    }

    @Test
    fun `a different body is reported as different`() {
        val name = backupFilename("X-T5", "20260908-2117")

        assertEquals(BackupMatch.DIFFERENT_MODEL, backupMatch(name, "X-T50"))
    }

    /** Weak evidence stays weak: an unrecognised name informs the confirmation, not a block. */
    @Test
    fun `a name that says nothing is unknown rather than different`() {
        assertEquals(BackupMatch.UNKNOWN_MODEL, backupMatch("settings.bin", "X-T50"))
    }

    // ─── Size guards ────────────────────────────────────────────────────────

    @Test
    fun `a plausible size has no complaint`() {
        assertNull(backupSizeProblem(MIN_BACKUP_BYTES))
        assertNull(backupSizeProblem(40_000))
        assertNull(backupSizeProblem(MAX_BACKUP_BYTES))
    }

    @Test
    fun `too small and too large each say which they are`() {
        assertNotNull(backupSizeProblem(0)?.takeIf { it.contains("too small") })
        assertNotNull(backupSizeProblem(5_000_000)?.takeIf { it.contains("wrong file") })
    }
}
