package dev.bondarenko.fujirecipes.ui.cameraphotos

import dev.bondarenko.fujirecipes.camera.ptp.PtpObject
import dev.bondarenko.fujirecipes.camera.ptp.PtpObjectInfo
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.core.store.CameraDownloadResult
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
import kotlinx.coroutines.Dispatchers
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

    @Test
    fun `downloads every selected file as one batch`() = runTest(dispatcher) {
        var exported: List<CameraMediaObject> = emptyList()
        var chosenFolder: String? = null
        val vm = CameraPhotosViewModel(
            listFiles = { listOf(jpeg, raw) },
            fetchThumbnail = { null },
            exportFiles = { files, folder, progress ->
                exported = files
                chosenFolder = folder
                progress(CameraExportProgress(2, 2, raw.info.filename, 40, 40))
                CameraDownloadResult(files.map { it.info.filename })
            },
        )
        vm.refresh()
        advanceUntilIdle()
        vm.selectAllVisible()

        vm.downloadSelected("content://folder")
        advanceUntilIdle()

        assertEquals(listOf(jpeg, raw), exported)
        assertEquals("content://folder", chosenFolder)
        assertNull(vm.state.value.download)
        assertEquals(emptySet(), vm.state.value.selectedHandles)
        assertEquals("2 files saved", vm.state.value.message)
    }

    private fun viewModel(
        files: List<CameraMediaObject>,
        thumbnail: ByteArray? = null,
    ) = CameraPhotosViewModel(
        listFiles = { files },
        fetchThumbnail = { thumbnail },
        exportFiles = { selected, _, _ -> CameraDownloadResult(selected.map { it.info.filename }) },
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
