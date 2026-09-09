package dev.bondarenko.fujirecipes.core.store

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraTransferStoreTest {

    private val directory: File = Files.createTempDirectory("camera-transfer").toFile()
    private val file = File(directory, CameraTransferStore.FILE_NAME)
    private val store = CameraTransferStore(file)

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun batch(total: Int = 3) = CameraTransferRecord(
        batchId = "b-1",
        treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADCIM",
        startedAt = "2026-09-09T10:00:00.000Z",
        totalFiles = total,
    )

    /**
     * The whole reason the record exists: nothing of ours runs when Android kills a cached
     * process, so "is there a file" has to be the entire signal that a batch did not finish.
     */
    @Test
    fun `a device that has never downloaded has no interrupted batch`() {
        assertNull(store.read())
    }

    @Test
    fun `a batch in flight is readable back in full`() {
        var record = store.begin(batch())
        record = store.fileStarted(record, "DSCF0001.JPG")
        record = store.fileFinished(record, "DSCF0001.JPG")
        store.fileStarted(record, "DSCF0002.JPG")

        val recovered = store.read()!!

        assertEquals("b-1", recovered.batchId)
        assertEquals(3, recovered.totalFiles)
        assertEquals(listOf("DSCF0001.JPG"), recovered.completedFiles)
        assertEquals(1, recovered.completedCount)
        assertEquals("DSCF0002.JPG", recovered.pendingFile)
        assertEquals("2026-09-09T10:00:00.000Z", recovered.startedAt)
        assertTrue(recovered.treeUri.startsWith("content://"))
    }

    /**
     * The distinction the recovery notice rests on: the finished files are whole and are the
     * user's, and only the pending one may be truncated.
     */
    @Test
    fun `a finished file stops being the pending one`() {
        var record = store.begin(batch())
        record = store.fileStarted(record, "DSCF0001.JPG")
        store.fileFinished(record, "DSCF0001.JPG")

        val recovered = store.read()!!

        assertNull(recovered.pendingFile)
        assertEquals(listOf("DSCF0001.JPG"), recovered.completedFiles)
    }

    @Test
    fun `a batch that reached an end leaves nothing behind`() {
        val record = store.begin(batch())
        store.fileStarted(record, "DSCF0001.JPG")

        store.clear()

        assertNull(store.read())
        assertFalse(file.exists())
    }

    @Test
    fun `clearing a batch that was never started is not an error`() {
        store.clear()

        assertNull(store.read())
    }

    /**
     * A record nobody can parse must not become a permanent blocker: it would report an
     * interruption on every launch and could never be dismissed by finishing a download.
     */
    @Test
    fun `an unreadable record reads as no record`() {
        file.parentFile?.mkdirs()
        file.writeText("{ this is not json")

        assertNull(store.read())
    }

    @Test
    fun `a record missing the fields that identify it reads as no record`() {
        file.parentFile?.mkdirs()
        file.writeText("""{"startedAt":"2026-09-09T10:00:00.000Z","totalFiles":2}""")

        assertNull(store.read())
    }

    /** Filenames go through JSON, so the ones with characters JSON cares about must survive. */
    @Test
    fun `a filename with quotes and backslashes survives the round trip`() {
        val awkward = """DSCF"0001\.JPG"""
        val record = store.begin(batch())
        store.fileStarted(record, awkward)

        assertEquals(awkward, store.read()?.pendingFile)
    }

    @Test
    fun `a record with no pending file omits it rather than storing a null`() {
        store.begin(batch())

        assertNull(store.read()?.pendingFile)
        assertFalse(file.readText().contains("pendingFile"))
    }
}
