package dev.bondarenko.fujirecipes.ui.raw

import android.content.ContentResolver
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentResult
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.camera.usb.RawProfileCalibrationRequired
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.store.RawDevelopmentCache
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RawDevelopmentStep {
    data object Loading : RawDevelopmentStep
    data object ChooseRaf : RawDevelopmentStep
    data class Importing(val written: Long, val total: Long?) : RawDevelopmentStep
    data object Ready : RawDevelopmentStep
    data class Running(val stage: RawDevelopmentStage) : RawDevelopmentStep
    data class CalibrationRequired(
        val cameraModel: String,
        val profile: ByteArray,
        val message: String,
    ) : RawDevelopmentStep
    data class Complete(val result: RawDevelopmentResult) : RawDevelopmentStep
    data class Failed(val message: String) : RawDevelopmentStep
}

data class RawDevelopmentUiState(
    val recipe: Recipe? = null,
    val raf: File? = null,
    val step: RawDevelopmentStep = RawDevelopmentStep.Loading,
)

class RawDevelopmentViewModel(
    private val recipeId: String,
    private val repository: RecipeRepository,
    private val controller: CameraController,
    private val cache: RawDevelopmentCache,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(RawDevelopmentUiState())
    val state: StateFlow<RawDevelopmentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recipe = repository.library.first { it.hasLoaded }.recipes
                .firstOrNull { it.id == recipeId }
            _state.value = if (recipe == null) {
                RawDevelopmentUiState(
                    step = RawDevelopmentStep.Failed("This recipe is no longer in your library."),
                )
            } else {
                RawDevelopmentUiState(recipe = recipe, step = RawDevelopmentStep.ChooseRaf)
            }
        }
    }

    fun connect() = controller.connect()

    fun selectRaf(uriText: String) {
        val recipe = _state.value.recipe ?: return
        viewModelScope.launch {
            val uri = uriText.toUri()
            val metadata = readMetadata(uri)
            _state.value = RawDevelopmentUiState(
                recipe = recipe,
                step = RawDevelopmentStep.Importing(0, metadata.length),
            )
            val raf = runCatching {
                withContext(Dispatchers.IO) {
                    val input = contentResolver.openInputStream(uri)
                        ?: error("The selected file could not be opened.")
                    input.use {
                        cache.importRaf(metadata.name, it, metadata.length) { written, total ->
                            _state.value = _state.value.copy(
                                step = RawDevelopmentStep.Importing(written, total),
                            )
                        }
                    }
                }
            }.getOrElse { error ->
                _state.value = RawDevelopmentUiState(
                    recipe = recipe,
                    step = RawDevelopmentStep.Failed(
                        error.message ?: "The selected RAF could not be read.",
                    ),
                )
                return@launch
            }
            _state.value = RawDevelopmentUiState(
                recipe = recipe,
                raf = raf,
                step = RawDevelopmentStep.Ready,
            )
        }
    }

    fun render() {
        val current = _state.value
        val recipe = current.recipe ?: return
        val raf = current.raf ?: return
        if (current.step is RawDevelopmentStep.Running) return

        viewModelScope.launch {
            val output = cache.outputFile(raf)
            try {
                val result = controller.developRaw(raf, recipe, output) { stage ->
                    _state.value = _state.value.copy(step = RawDevelopmentStep.Running(stage))
                }
                _state.value = _state.value.copy(step = RawDevelopmentStep.Complete(result))
            } catch (error: RawProfileCalibrationRequired) {
                _state.value = _state.value.copy(
                    step = RawDevelopmentStep.CalibrationRequired(
                        cameraModel = error.cameraModel,
                        profile = error.profile,
                        message = error.message
                            ?: "This camera model needs a verified RAW profile mapping.",
                    ),
                )
            } catch (error: Exception) {
                output.delete()
                _state.value = _state.value.copy(
                    step = RawDevelopmentStep.Failed(
                        error.message ?: "RAW development stopped before a JPEG was returned.",
                    ),
                )
            }
        }
    }

    fun chooseAnotherRaf() {
        val recipe = _state.value.recipe ?: return
        cache.clear()
        _state.value = RawDevelopmentUiState(recipe = recipe, step = RawDevelopmentStep.ChooseRaf)
    }

    fun retry() {
        _state.value = _state.value.copy(
            step = if (_state.value.raf != null) RawDevelopmentStep.Ready
            else RawDevelopmentStep.ChooseRaf,
        )
    }

    private fun readMetadata(uri: android.net.Uri): RafMetadata {
        var name = "selected.raf"
        var length: Long? = null
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
                    name = cursor.getString(it) ?: name
                }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let {
                    if (!cursor.isNull(it)) length = cursor.getLong(it).takeIf { size -> size >= 0 }
                }
            }
        }
        return RafMetadata(name, length)
    }

    private data class RafMetadata(val name: String, val length: Long?)

    companion object {
        fun factory(container: AppContainer, recipeId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    RawDevelopmentViewModel(
                        recipeId = recipeId,
                        repository = container.recipeRepository,
                        controller = container.cameraController,
                        cache = container.rawDevelopmentCache,
                        contentResolver = container.applicationContext.contentResolver,
                    )
                }
            }
    }
}
