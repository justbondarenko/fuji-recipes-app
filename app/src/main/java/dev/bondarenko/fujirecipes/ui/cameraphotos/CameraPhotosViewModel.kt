package dev.bondarenko.fujirecipes.ui.cameraphotos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.CameraTransferJob
import dev.bondarenko.fujirecipes.camera.CameraTransferService
import dev.bondarenko.fujirecipes.camera.CameraTransferState
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaType
import dev.bondarenko.fujirecipes.camera.usb.mediaType
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
import dev.bondarenko.fujirecipes.core.store.CameraTransferRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CameraFileFilter { ALL, JPEG, RAW }

data class CameraPhotosUiState(
    val files: List<CameraMediaObject> = emptyList(),
    val filter: CameraFileFilter = CameraFileFilter.ALL,
    val selectedHandles: Set<Int> = emptySet(),
    val thumbnails: Map<Int, ByteArray> = emptyMap(),
    val hasScanned: Boolean = false,
    val isLoading: Boolean = false,
    val scanCurrent: Int = 0,
    val scanTotal: Int = 0,
    val download: CameraExportProgress? = null,
    /** A batch the last process never finished, for the notice on the way in. */
    val interrupted: CameraTransferRecord? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val filteredFiles: List<CameraMediaObject>
        get() = when (filter) {
            CameraFileFilter.ALL -> files
            CameraFileFilter.JPEG -> files.filter { it.mediaType == CameraMediaType.JPEG }
            CameraFileFilter.RAW -> files.filter { it.mediaType == CameraMediaType.RAW }
        }

    val selectedFiles: List<CameraMediaObject>
        get() = files.filter { it.handle in selectedHandles }
}

/**
 * The screen's state, but **not** the download.
 *
 * A batch runs in [CameraTransferService] and reports through a process-wide flow, so this
 * observes it rather than owning it. That is the whole point: a transfer tied to
 * `viewModelScope` dies with the screen, and leaves Android free to kill a cached process
 * part-way through a file.
 */
class CameraPhotosViewModel(
    private val listFiles: suspend ((Int, Int) -> Unit) -> List<CameraMediaObject>,
    private val fetchThumbnail: suspend (Int) -> ByteArray?,
    transferState: StateFlow<CameraTransferState?>,
    private val startTransfer: (List<CameraMediaObject>, String) -> Unit,
    private val cancelTransfer: () -> Unit,
    private val acknowledgeTransfer: () -> Unit,
    private val readInterrupted: () -> CameraTransferRecord? = { null },
    private val clearInterrupted: () -> Unit = {},
    private val deletePartial: suspend (treeUri: String, filename: String) -> Boolean =
        { _, _ -> false },
) : ViewModel() {
    private val thumbnailLoads = mutableSetOf<Int>()
    private val _state = MutableStateFlow(CameraPhotosUiState())
    val state: StateFlow<CameraPhotosUiState> = _state.asStateFlow()

    init {
        // Read before anything else can start a new batch and clear the record.
        _state.update { it.copy(interrupted = readInterrupted()) }
        viewModelScope.launch { transferState.collect { applyTransfer(it) } }
    }

    fun refresh() {
        if (_state.value.isLoading || _state.value.download != null) return
        _state.update {
            it.copy(isLoading = true, scanCurrent = 0, scanTotal = 0, error = null, message = null)
        }
        viewModelScope.launch {
            runCatching {
                listFiles { current, total ->
                    _state.update { it.copy(scanCurrent = current, scanTotal = total) }
                }
            }.onSuccess { files ->
                thumbnailLoads.clear()
                _state.update {
                    it.copy(
                        files = files,
                        selectedHandles = emptySet(),
                        thumbnails = emptyMap(),
                        hasScanned = true,
                        isLoading = false,
                        scanCurrent = 0,
                        scanTotal = 0,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        hasScanned = true,
                        isLoading = false,
                        error = error.message ?: "The camera card could not be read.",
                    )
                }
            }
        }
    }

    fun setFilter(filter: CameraFileFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun toggleFile(handle: Int) {
        if (_state.value.download != null) return
        _state.update { state ->
            val selected = if (handle in state.selectedHandles) {
                state.selectedHandles - handle
            } else {
                state.selectedHandles + handle
            }
            state.copy(selectedHandles = selected)
        }
    }

    /** Selects the current filter results, preserving selections made under another filter. */
    fun selectAllVisible() {
        if (_state.value.download != null) return
        _state.update { state ->
            state.copy(selectedHandles = state.selectedHandles + state.filteredFiles.map { it.handle })
        }
    }

    fun clearSelection() {
        if (_state.value.download != null) return
        _state.update { it.copy(selectedHandles = emptySet()) }
    }

    fun loadThumbnail(handle: Int) {
        if (handle in _state.value.thumbnails || !thumbnailLoads.add(handle)) return
        if (_state.value.files.none { it.handle == handle }) {
            thumbnailLoads -= handle
            return
        }
        viewModelScope.launch {
            try {
                val thumbnail = runCatching { fetchThumbnail(handle) }.getOrNull() ?: return@launch
                _state.update { it.copy(thumbnails = it.thumbnails + (handle to thumbnail)) }
            } finally {
                thumbnailLoads -= handle
            }
        }
    }

    fun downloadSelected(folderUri: String) {
        val selected = _state.value.selectedFiles
        if (selected.isEmpty() || _state.value.download != null) return

        // Clearing the notice here rather than on the next launch: a new batch is about to
        // overwrite the record the notice was read from.
        clearInterrupted()
        _state.update { it.copy(interrupted = null, error = null, message = null) }
        startTransfer(selected, folderUri)
    }

    fun cancelDownload() {
        if (_state.value.download == null) return
        cancelTransfer()
    }

    /**
     * The batch's own state, rendered.
     *
     * A terminal state is acknowledged as it is shown, so it is reported once and does not
     * come back the next time this screen is opened.
     */
    private fun applyTransfer(transfer: CameraTransferState?) {
        when (transfer) {
            null -> _state.update { it.copy(download = null) }

            is CameraTransferState.Running ->
                _state.update { it.copy(download = transfer.progress) }

            is CameraTransferState.Finished -> {
                val count = transfer.savedCount
                _state.update {
                    it.copy(
                        selectedHandles = emptySet(),
                        download = null,
                        message = "$count file${if (count == 1) "" else "s"} saved",
                    )
                }
                acknowledgeTransfer()
            }

            CameraTransferState.Cancelled -> {
                _state.update { it.copy(download = null, message = "Download cancelled") }
                acknowledgeTransfer()
            }

            is CameraTransferState.Failed -> {
                _state.update {
                    it.copy(
                        download = null,
                        error = transfer.message.ifBlank {
                            "The selected files could not be downloaded."
                        },
                    )
                }
                acknowledgeTransfer()
            }
        }
    }

    /** Leaves the half-written file where it is; the user may want to look at it first. */
    fun dismissInterrupted() {
        clearInterrupted()
        _state.update { it.copy(interrupted = null) }
    }

    /**
     * Deletes the one file that was in flight when the process died.
     *
     * Only that one: the files before it are whole, are what the user asked for, and deleting
     * them because a later file failed would be the app throwing away its own work.
     */
    fun deleteInterruptedPartial() {
        val record = _state.value.interrupted ?: return
        val pending = record.pendingFile ?: return dismissInterrupted()

        viewModelScope.launch {
            val deleted = runCatching { deletePartial(record.treeUri, pending) }
                .getOrDefault(false)
            clearInterrupted()
            _state.update {
                it.copy(
                    interrupted = null,
                    message = if (deleted) {
                        "Deleted the incomplete file"
                    } else {
                        "That file is no longer in the folder"
                    },
                )
            }
        }
    }

    fun clearNotice() = _state.update { it.copy(message = null, error = null) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = container.applicationContext
                CameraPhotosViewModel(
                    listFiles = { progress -> container.cameraController.listCameraFiles(progress) },
                    fetchThumbnail = container.cameraController::readCameraPhotoThumbnail,
                    transferState = container.cameraTransfers.state,
                    startTransfer = { files, folderUri ->
                        container.cameraTransfers.request(
                            CameraTransferJob(
                                files = files,
                                treeUri = folderUri,
                                // The model, or nothing pretending to be it. The USB mode is
                                // not part of the label: a card-reader body refuses the mode
                                // property, so naming it here would be a guess.
                                cameraLabel = (
                                    container.cameraController.state.value
                                        as? CameraState.Connected
                                    )?.identity?.model
                                    ?: context.getString(R.string.camera_transfer_unknown_camera),
                            ),
                        )
                        CameraTransferService.start(context)
                    },
                    cancelTransfer = { CameraTransferService.cancel(context) },
                    acknowledgeTransfer = container.cameraTransfers::acknowledge,
                    readInterrupted = container.cameraTransferStore::read,
                    clearInterrupted = container.cameraTransferStore::clear,
                    deletePartial = { treeUri, filename ->
                        container.cameraPhotoExporter.deleteDocument(Uri.parse(treeUri), filename)
                    },
                )
            }
        }
    }
}
