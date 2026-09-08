package dev.bondarenko.fujirecipes.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.data.photo.MatchResult
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.PhotoReadResult
import dev.bondarenko.fujirecipes.data.photo.PhotoRecipe
import dev.bondarenko.fujirecipes.data.photo.findMatches
import dev.bondarenko.fujirecipes.data.photo.parseRecipeFromJpeg
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.net.URI

/**
 * What the Read screen is showing — FEAT-009 T-09.
 *
 * Everything here is local: the photo is read from the device and decoded on it. Nothing is
 * uploaded, which is both a privacy property worth having and what makes the screen work with
 * no signal.
 */
data class AnalyzedPhoto(
    val uri: String,
    val recipe: PhotoRecipe,
    val matches: MatchResult,
)

sealed interface PhotoReaderStage {

    /** Nothing chosen yet. */
    data object Empty : PhotoReaderStage

    data object Reading : PhotoReaderStage

    data object CameraLoading : PhotoReaderStage

    data class CameraBrowser(
        val photos: List<CameraMediaObject>,
        val selectedHandles: Set<Int> = emptySet(),
        val thumbnails: Map<Int, ByteArray> = emptyMap(),
    ) : PhotoReaderStage

    data class CameraDownloading(val done: Int, val total: Int, val filename: String) : PhotoReaderStage

    data class CameraFailed(val message: String) : PhotoReaderStage

    data class Result(
        val photos: List<AnalyzedPhoto>,
        val selectedIndex: Int = 0,
    ) : PhotoReaderStage {
        constructor(recipe: PhotoRecipe, matches: MatchResult, photoUri: String? = null) : this(
            photos = listOf(AnalyzedPhoto(photoUri ?: "", recipe, matches)),
            selectedIndex = 0,
        )

        val currentPhoto: AnalyzedPhoto get() = photos.getOrElse(selectedIndex) { photos.first() }
        val recipe: PhotoRecipe get() = currentPhoto.recipe
        val matches: MatchResult get() = currentPhoto.matches
        val photoUri: String? get() = currentPhoto.uri.ifEmpty { null }
    }

    /** Each failure is its own answer, with its own remedy (P5). */
    data class Failed(val reason: PhotoReadFailure) : PhotoReaderStage
}

data class PhotoReaderUiState(
    val stage: PhotoReaderStage = PhotoReaderStage.Empty,
    val isAddingPhoto: Boolean = false,
    val addedPhotoUris: Set<String> = emptySet(),
) {
    // Backward compatibility helper
    val addedPhotoToRecipeId: String? get() = null
}

class PhotoReaderViewModel(
    private val repository: RecipeRepository,
    /**
     * Reads the chosen photo's bytes.
     *
     * Injected rather than reached for, so this class stays free of `android.net.Uri` and a
     * `ContentResolver` — which is what lets the stage machine be exercised without a device.
     */
    private val readPhoto: suspend (String) -> ByteArray?,
    private val savePhoto: (suspend (String) -> String?)? = null,
    private val listCameraPhotos: (suspend () -> List<CameraMediaObject>)? = null,
    private val fetchCameraThumbnail: (suspend (Int) -> ByteArray?)? = null,
    private val downloadCameraPhoto: (suspend (CameraMediaObject) -> String)? = null,
    private val clearCameraFiles: (() -> Unit)? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private var currentUris: List<String> = emptyList()
    private val thumbnailLoads = mutableSetOf<Int>()
    private val _state = MutableStateFlow(PhotoReaderUiState())
    val state: StateFlow<PhotoReaderUiState> = _state.asStateFlow()

    fun read(uri: String) = read(listOf(uri))

    fun read(uris: List<String>) {
        if (uris.isEmpty()) return
        if (currentUris == uris && _state.value.stage !is PhotoReaderStage.Empty) return
        currentUris = uris
        viewModelScope.launch {
            analyze(uris)
        }
    }

    private suspend fun analyze(uris: List<String>) {
        _state.value = PhotoReaderUiState(PhotoReaderStage.Reading)
        val library = repository.library.first { it.hasLoaded }.recipes
        val results = mutableListOf<AnalyzedPhoto>()
        var lastFailure: PhotoReadFailure? = null

        for (uri in uris) {
            val bytes = runCatching { readPhoto(uri) }.getOrNull()
            if (bytes == null) {
                lastFailure = PhotoReadFailure.UNREADABLE
                continue
            }
            when (val parsed = withContext(defaultDispatcher) { parseRecipeFromJpeg(bytes) }) {
                is PhotoReadResult.Failure -> {
                    lastFailure = parsed.reason
                }
                is PhotoReadResult.Success -> {
                    results += AnalyzedPhoto(
                        uri = uri,
                        recipe = parsed.recipe,
                        matches = findMatches(parsed.recipe, library),
                    )
                }
            }
        }

        if (results.isEmpty()) {
            fail(lastFailure ?: PhotoReadFailure.UNREADABLE)
        } else {
            _state.value = PhotoReaderUiState(
                PhotoReaderStage.Result(
                    photos = results,
                    selectedIndex = 0,
                ),
            )
        }
    }

    fun browseCamera() {
        val list = listCameraPhotos ?: return
        _state.value = PhotoReaderUiState(PhotoReaderStage.CameraLoading)
        viewModelScope.launch {
            runCatching { list() }
                .onFailure { error ->
                    _state.value = PhotoReaderUiState(
                        PhotoReaderStage.CameraFailed(error.message ?: "The camera could not be read."),
                    )
                }
                .onSuccess { photos ->
                    _state.value = PhotoReaderUiState(PhotoReaderStage.CameraBrowser(photos))
                }
        }
    }

    fun loadCameraThumbnail(handle: Int) {
        val load = fetchCameraThumbnail ?: return
        val browser = _state.value.stage as? PhotoReaderStage.CameraBrowser ?: return
        if (handle in browser.thumbnails || !thumbnailLoads.add(handle)) return

        viewModelScope.launch {
            try {
                val bytes = runCatching { load(handle) }.getOrNull() ?: return@launch
                _state.update { state ->
                    val current = state.stage as? PhotoReaderStage.CameraBrowser ?: return@update state
                    state.copy(stage = current.copy(thumbnails = current.thumbnails + (handle to bytes)))
                }
            } finally {
                thumbnailLoads -= handle
            }
        }
    }

    fun toggleCameraPhoto(handle: Int) {
        _state.update { state ->
            val browser = state.stage as? PhotoReaderStage.CameraBrowser ?: return@update state
            val selected = if (handle in browser.selectedHandles) {
                browser.selectedHandles - handle
            } else {
                browser.selectedHandles + handle
            }
            state.copy(stage = browser.copy(selectedHandles = selected))
        }
    }

    fun analyzeCameraSelection() {
        val browser = _state.value.stage as? PhotoReaderStage.CameraBrowser ?: return
        val download = downloadCameraPhoto ?: return
        val selected = browser.photos.filter { it.handle in browser.selectedHandles }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val uris = mutableListOf<String>()
            for ((index, photo) in selected.withIndex()) {
                _state.value = PhotoReaderUiState(
                    PhotoReaderStage.CameraDownloading(index, selected.size, photo.info.filename),
                )
                val uri = runCatching { download(photo) }.getOrElse { error ->
                    _state.value = PhotoReaderUiState(
                        PhotoReaderStage.CameraFailed(error.message ?: "The photo could not be downloaded."),
                    )
                    return@launch
                }
                uris += uri
            }
            currentUris = uris
            analyze(uris)
        }
    }

    fun selectPhoto(index: Int) {
        val currentStage = _state.value.stage as? PhotoReaderStage.Result ?: return
        if (index in currentStage.photos.indices && index != currentStage.selectedIndex) {
            _state.update {
                it.copy(stage = currentStage.copy(selectedIndex = index))
            }
        }
    }

    fun addPhotoToRecipe(recipeId: String) {
        val stage = _state.value.stage as? PhotoReaderStage.Result ?: return
        val uri = stage.currentPhoto.uri.ifEmpty { null } ?: return
        val save = savePhoto ?: return
        if (_state.value.isAddingPhoto || _state.value.addedPhotoUris.contains(uri)) return

        viewModelScope.launch {
            _state.update { it.copy(isAddingPhoto = true) }
            val imageName = withContext(defaultDispatcher) { save(uri) }
            if (imageName == null) {
                _state.update { it.copy(isAddingPhoto = false) }
                return@launch
            }

            val currentRecipe = repository.library.first { it.hasLoaded }.recipes.find { it.id == recipeId }
            if (currentRecipe != null && currentRecipe.images.size < dev.bondarenko.fujirecipes.core.store.ImageStore.MAX_IMAGES_PER_RECIPE) {
                val updatedImages = currentRecipe.images + imageName
                repository.update(recipeId, currentRecipe.copy(images = updatedImages).toJson())
                _state.update { it.copy(isAddingPhoto = false, addedPhotoUris = it.addedPhotoUris + uri) }
            } else {
                _state.update { it.copy(isAddingPhoto = false) }
            }
        }
    }

    fun reset() {
        currentUris = emptyList()
        thumbnailLoads.clear()
        clearCameraFiles?.invoke()
        _state.value = PhotoReaderUiState()
    }

    private fun fail(reason: PhotoReadFailure) {
        _state.value = PhotoReaderUiState(PhotoReaderStage.Failed(reason))
    }

    /**
     * The decoded settings as JSON, for the editor to open pre-filled.
     *
     * A route argument rather than a holder on `AppContainer`: it survives process death like
     * every other route argument, and it keeps the editor's inputs visible in the back stack
     * rather than hidden in a singleton.
     */
    fun prefillJson(): String? {
        val stage = _state.value.stage as? PhotoReaderStage.Result ?: return null

        return buildJsonObject {
            stage.recipe.settings.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, JsonPrimitive(value))
                    is Int -> put(key, JsonPrimitive(value))
                    is Double -> put(
                        key,
                        // Whole numbers go out as integers: the rest of the platform stores a
                        // tone of 2 as `2`, and a needless `2.0` is a needless difference.
                        if (value % 1.0 == 0.0) JsonPrimitive(value.toInt()) else JsonPrimitive(value),
                    )

                    is Boolean -> put(key, JsonPrimitive(value))
                    else -> Unit
                }
            }
        }.toString()
    }

    /**
     * A name to suggest, from what the photo actually says.
     *
     * The simulation and the body, because those are the two things a person would use to
     * describe the shot — and both come from the file rather than being invented.
     */
    fun suggestedName(): String {
        val stage = _state.value.stage as? PhotoReaderStage.Result ?: return ""
        val simulation = stage.recipe.filmSimulationLabel ?: return "Recipe from a photo"
        val body = stage.recipe.cameraModel

        return if (body != null) "$simulation ($body)" else simulation
    }

    companion object {
        fun factory(
            container: AppContainer,
            readPhoto: suspend (String) -> ByteArray?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PhotoReaderViewModel(
                    repository = container.recipeRepository,
                    readPhoto = readPhoto,
                    savePhoto = { uriString ->
                        if (uriString.startsWith("file:")) {
                            runCatching { File(URI(uriString)) }.getOrNull()?.let { file ->
                                container.imageStore.saveFromFile(file)
                            }
                        } else {
                            runCatching { android.net.Uri.parse(uriString) }.getOrNull()?.let { uri ->
                                container.imageStore.saveFromUri(uri)
                            }
                        }
                    },
                    listCameraPhotos = { container.cameraController.listCameraPhotos() },
                    fetchCameraThumbnail = container.cameraController::readCameraPhotoThumbnail,
                    downloadCameraPhoto = { media ->
                        container.cameraController.downloadCameraPhoto(media, container.cameraMediaCache)
                            .toURI().toString()
                    },
                    clearCameraFiles = container.cameraMediaCache::clear,
                )
            }
        }
    }
}
