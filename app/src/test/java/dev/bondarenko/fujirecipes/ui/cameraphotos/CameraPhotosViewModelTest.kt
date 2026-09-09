package dev.bondarenko.fujirecipes.ui.cameraphotos

import dev.bondarenko.fujirecipes.camera.ptp.PtpObject
import dev.bondarenko.fujirecipes.camera.ptp.PtpObjectInfo
import dev.bondarenko.fujirecipes.camera.CameraTransferState
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
import dev.bondarenko.fujirecipes.core.store.CameraTransferRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CameraPhotosViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val jpeg = CameraMediaObject(1, objectInfo("DSCF0001.JPG", PtpObject.FORMAT_JPEG, 10))
    private val raw = CameraMediaObject(2, objectInfo("DSCF0001.RAF", 0xb103, 30))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh lists independent files and thumbnail loading is lazy`() = runTest(dispatcher) {
        val thumbnail = byteArrayOf(1, 2, 3)
        val vm = viewModel(files = listOf(jpeg, raw), thumbnail = thumbnail)

        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf(jpeg, raw), vm.state.value.files)
        assertEquals(true, vm.state.value.hasScanned)

        vm.loadThumbnail(raw.handle)
        advanceUntilIdle()
        assertContentEquals(thumbnail, vm.state.value.thumbnails[raw.handle])
    }

    @Test
    fun `filter select all and clear manage the batch`() = runTest(dispatcher) {
        val vm = viewModel(files = listOf(jpeg, raw))
        vm.refresh()
        advanceUntilIdle()

        vm.setFilter(CameraFileFilter.RAW)
        assertEquals(listOf(raw), vm.state.value.filteredFiles)
        vm.selectAllVisible()
        assertEquals(setOf(raw.handle), vm.state.value.selectedHandles)

        vm.setFilter(CameraFileFilter.JPEG)
        vm.selectAllVisible()
        assertEquals(setOf(jpeg.handle, raw.handle), vm.state.value.selectedHandles)

        vm.clearSelection()
        assertEquals(emptySet(), vm.state.value.selectedHandles)
    }

    /**
     * The batch is handed over, not run here. A download that lived in `viewModelScope` would
     * be torn down with the screen and would leave the process free to be killed part-way
     * through a file, which is the whole reason the service exists.
     */
    @Test
    fun `downloading hands the batch to the transfer service`() = runTest(dispatcher) {
        var started: List<CameraMediaObject> = emptyList()
        var folder: String? = null
        val transfers = MutableStateFlow<CameraTransferState?>(null)
        val vm = viewModel(
            files = listOf(jpeg, raw),
            transferState = transfers,
            startTransfer = { files, treeUri ->
                started = files
                folder = treeUri
            },
        )
        vm.refresh()
        advanceUntilIdle()
        vm.selectAllVisible()

        vm.downloadSelected("content://folder")
        advanceUntilIdle()

        assertEquals(listOf(jpeg, raw), started)
        assertEquals("content://folder", folder)
    }

    @Test
    fun `progress from the service is what the screen shows`() = runTest(dispatcher) {
        val transfers = MutableStateFlow<CameraTransferState?>(null)
        val vm = viewModel(files = listOf(jpeg, raw), transferState = transfers)
        advanceUntilIdle()

        val progress = CameraExportProgress(2, 2, raw.info.filename, 30, 40)
        transfers.value = CameraTransferState.Running(progress)
        advanceUntilIdle()

        assertEquals(progress, vm.state.value.download)
    }

    /** The end of a batch has to be reported even though this screen never ran it. */
    @Test
    fun `a finished batch clears the selection and says how many were saved`() =
        runTest(dispatcher) {
            var acknowledged = false
            val transfers = MutableStateFlow<CameraTransferState?>(null)
            val vm = viewModel(
                files = listOf(jpeg, raw),
                transferState = transfers,
                acknowledgeTransfer = { acknowledged = true },
            )
            vm.refresh()
            advanceUntilIdle()
            vm.selectAllVisible()

            transfers.value = CameraTransferState.Finished(savedCount = 2)
            advanceUntilIdle()

            assertNull(vm.state.value.download)
            assertEquals(emptySet(), vm.state.value.selectedHandles)
            assertEquals("2 files saved", vm.state.value.message)
            assertTrue(acknowledged, "a terminal state must be consumed, not repeated")
        }

    @Test
    fun `a cancelled batch reads as cancelled rather than as a failure`() = runTest(dispatcher) {
        val transfers = MutableStateFlow<CameraTransferState?>(null)
        val vm = viewModel(files = listOf(jpeg), transferState = transfers)
        advanceUntilIdle()

        transfers.value = CameraTransferState.Cancelled
        advanceUntilIdle()

        assertEquals("Download cancelled", vm.state.value.message)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a failed batch surfaces the reason`() = runTest(dispatcher) {
        val transfers = MutableStateFlow<CameraTransferState?>(null)
        val vm = viewModel(files = listOf(jpeg), transferState = transfers)
        advanceUntilIdle()

        transfers.value = CameraTransferState.Failed("The card was removed.")
        advanceUntilIdle()

        assertEquals("The card was removed.", vm.state.value.error)
    }

    @Test
    fun `cancelling only does something while a batch is running`() = runTest(dispatcher) {
        var cancels = 0
        val transfers = MutableStateFlow<CameraTransferState?>(null)
        val vm = viewModel(
            files = listOf(jpeg),
            transferState = transfers,
            cancelTransfer = { cancels++ },
        )
        advanceUntilIdle()

        vm.cancelDownload()
        assertEquals(0, cancels)

        transfers.value = CameraTransferState.Running(
            CameraExportProgress(1, 1, jpeg.info.filename, 0, 10),
        )
        advanceUntilIdle()
        vm.cancelDownload()

        assertEquals(1, cancels)
    }

    // ─── Recovery after a process kill ──────────────────────────────────────

    private val interrupted = CameraTransferRecord(
        batchId = "b-1",
        treeUri = "content://folder",
        startedAt = "2026-09-09T10:00:00.000Z",
        totalFiles = 3,
        completedFiles = listOf("DSCF0001.JPG"),
        pendingFile = "DSCF0002.JPG",
    )

    @Test
    fun `an unfinished batch from a killed process is reported on arrival`() =
        runTest(dispatcher) {
            val vm = viewModel(files = listOf(jpeg), readInterrupted = { interrupted })
            advanceUntilIdle()

            assertEquals(interrupted, vm.state.value.interrupted)
        }

    /**
     * Only the file that was in flight. The ones before it are whole and are the user's — an
     * app that deleted them because a later file failed would be throwing away its own work.
     */
    @Test
    fun `deleting the partial removes the pending file and nothing else`() = runTest(dispatcher) {
        val deleted = mutableListOf<Pair<String, String>>()
        val vm = viewModel(
            files = listOf(jpeg),
            readInterrupted = { interrupted },
            deletePartial = { treeUri, filename ->
                deleted += treeUri to filename
                true
            },
        )
        advanceUntilIdle()

        vm.deleteInterruptedPartial()
        advanceUntilIdle()

        assertEquals(listOf("content://folder" to "DSCF0002.JPG"), deleted)
        assertNull(vm.state.value.interrupted)
        assertEquals("Deleted the incomplete file", vm.state.value.message)
    }

    @Test
    fun `a partial that is already gone is not reported as a deletion`() = runTest(dispatcher) {
        val vm = viewModel(
            files = listOf(jpeg),
            readInterrupted = { interrupted },
            deletePartial = { _, _ -> false },
        )
        advanceUntilIdle()

        vm.deleteInterruptedPartial()
        advanceUntilIdle()

        assertEquals("That file is no longer in the folder", vm.state.value.message)
    }

    @Test
    fun `keeping the partial clears the record without touching the folder`() =
        runTest(dispatcher) {
            var cleared = false
            var deletions = 0
            val vm = viewModel(
                files = listOf(jpeg),
                readInterrupted = { interrupted },
                clearInterrupted = { cleared = true },
                deletePartial = { _, _ -> deletions++; true },
            )
            advanceUntilIdle()

            vm.dismissInterrupted()

            assertTrue(cleared)
            assertEquals(0, deletions)
            assertNull(vm.state.value.interrupted)
        }

    /** Starting a new batch overwrites the record the notice was read from. */
    @Test
    fun `starting a download clears the interruption notice`() = runTest(dispatcher) {
        val vm = viewModel(files = listOf(jpeg), readInterrupted = { interrupted })
        vm.refresh()
        advanceUntilIdle()
        vm.selectAllVisible()

        vm.downloadSelected("content://folder")
        advanceUntilIdle()

        assertNull(vm.state.value.interrupted)
    }

    private fun viewModel(
        files: List<CameraMediaObject>,
        thumbnail: ByteArray? = null,
        transferState: MutableStateFlow<CameraTransferState?> = MutableStateFlow(null),
        startTransfer: (List<CameraMediaObject>, String) -> Unit = { _, _ -> },
        cancelTransfer: () -> Unit = {},
        acknowledgeTransfer: () -> Unit = {},
        readInterrupted: () -> CameraTransferRecord? = { null },
        clearInterrupted: () -> Unit = {},
        deletePartial: suspend (String, String) -> Boolean = { _, _ -> false },
    ) = CameraPhotosViewModel(
        listFiles = { files },
        fetchThumbnail = { thumbnail },
        transferState = transferState,
        startTransfer = startTransfer,
        cancelTransfer = cancelTransfer,
        acknowledgeTransfer = acknowledgeTransfer,
        readInterrupted = readInterrupted,
        clearInterrupted = clearInterrupted,
        deletePartial = deletePartial,
    )

    private fun objectInfo(filename: String, format: Int, size: Long) = PtpObjectInfo(
        storageId = 1,
        format = format,
        protectionStatus = 0,
        compressedSize = size,
        thumbFormat = PtpObject.FORMAT_JPEG,
        thumbCompressedSize = 3,
        thumbWidth = 320,
        thumbHeight = 240,
        imageWidth = 6240,
        imageHeight = 4160,
        imageBitDepth = 24,
        parentObject = 0,
        associationType = 0,
        associationDescription = 0,
        sequenceNumber = 0,
        filename = filename,
        captureDate = "20260909T120000",
        modificationDate = "20260909T120000",
        keywords = "",
    )
}
