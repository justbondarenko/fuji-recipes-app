package dev.bondarenko.fujirecipes.ui.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.photo.MatchResult
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.PhotoReadResult
import dev.bondarenko.fujirecipes.data.photo.PhotoRecipe
import dev.bondarenko.fujirecipes.data.photo.findMatches
import dev.bondarenko.fujirecipes.data.photo.parseRecipeFromJpeg
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * What the Read screen is showing — FEAT-009 T-09.
 *
 * Everything here is local: the photo is read from the device and decoded on it. Nothing is
 * uploaded, which is both a privacy property worth having and what makes the screen work with
 * no signal.
 */
sealed interface PhotoReaderStage {

    /** Nothing chosen yet. */
    data object Empty : PhotoReaderStage

    data object Reading : PhotoReaderStage

    data class Result(
        val recipe: PhotoRecipe,
        val matches: MatchResult,
    ) : PhotoReaderStage

    /** Each failure is its own answer, with its own remedy (P5). */
    data class Failed(val reason: PhotoReadFailure) : PhotoReaderStage
}

data class PhotoReaderUiState(val stage: PhotoReaderStage = PhotoReaderStage.Empty)

class PhotoReaderViewModel(
    private val repository: RecipeRepository,
    /**
     * Reads the chosen photo's bytes.
     *
     * Injected rather than reached for, so this class stays free of `android.net.Uri` and a
     * `ContentResolver` — which is what lets the stage machine be exercised without a device.
     */
    private val readPhoto: suspend (String) -> ByteArray?,
) : ViewModel() {

    private val _state = MutableStateFlow(PhotoReaderUiState())
    val state: StateFlow<PhotoReaderUiState> = _state.asStateFlow()

    fun read(uri: String) {
        _state.value = PhotoReaderUiState(PhotoReaderStage.Reading)

        viewModelScope.launch {
            val bytes = runCatching { readPhoto(uri) }.getOrNull()
                ?: return@launch fail(PhotoReadFailure.UNREADABLE)

            // Off the main thread: a 20 MB JPEG is a 20 MB scan, and the signature hunt walks
            // up to half a megabyte of it.
            when (val parsed = withContext(Dispatchers.Default) { parseRecipeFromJpeg(bytes) }) {
                is PhotoReadResult.Failure -> fail(parsed.reason)

                is PhotoReadResult.Success -> {
                    // The stored library, so matching works for the same reason the
                    // decoding does.
                    val library = repository.library.first { it.hasLoaded }.recipes

                    _state.value = PhotoReaderUiState(
                        PhotoReaderStage.Result(
                            recipe = parsed.recipe,
                            matches = findMatches(parsed.recipe, library),
                        ),
                    )
                }
            }
        }
    }

    fun reset() {
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
            initializer { PhotoReaderViewModel(container.recipeRepository, readPhoto) }
        }
    }
}
