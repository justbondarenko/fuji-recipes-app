package dev.bondarenko.fujirecipes.ui.cameraphotos

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaType
import dev.bondarenko.fujirecipes.camera.usb.mediaType
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.store.CameraDownloadResult
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
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

class CameraPhotosViewModel(
    private val listFiles: suspend ((Int, Int) -> Unit) -> List<CameraMediaObject>,
    private val fetchThumbnail: suspend (Int) -> ByteArray?,
    private val exportFiles: suspend (
        List<CameraMediaObject>,
        String,
        (CameraExportProgress) -> Unit,
    ) -> CameraDownloadResult,
) : ViewModel() {
    private val thumbnailLoads = mutableSetOf<Int>()
    private val _state = MutableStateFlow(CameraPhotosUiState())
    val state: StateFlow<CameraPhotosUiState> = _state.asStateFlow()

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
        val initial = CameraExportProgress(
            currentFile = 1,
            totalFiles = selected.size,
            filename = selected.first().info.filename,
            written = 0,
            totalBytes = selected.sumOf { it.info.compressedSize },
        )
        _state.update { it.copy(download = initial, error = null, message = null) }
        viewModelScope.launch {
            runCatching {
                exportFiles(selected, folderUri) { progress ->
                    _state.update { it.copy(download = progress) }
                }
            }.onSuccess { result ->
                val count = result.filenames.size
                _state.update {
                    it.copy(
                        selectedHandles = emptySet(),
                        download = null,
                        message = "$count file${if (count == 1) "" else "s"} saved",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        download = null,
                        error = error.message ?: "The selected files could not be downloaded.",
                    )
                }
            }
        }
    }

    fun clearNotice() = _state.update { it.copy(message = null, error = null) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CameraPhotosViewModel(
                    listFiles = { progress -> container.cameraController.listCameraFiles(progress) },
                    fetchThumbnail = container.cameraController::readCameraPhotoThumbnail,
                    exportFiles = { files, folderUri, progress ->
                        container.cameraPhotoExporter.download(
                            files = files,
                            treeUri = Uri.parse(folderUri),
                            onProgress = progress,
                        )
                    },
                )
            }
        }
    }
}
