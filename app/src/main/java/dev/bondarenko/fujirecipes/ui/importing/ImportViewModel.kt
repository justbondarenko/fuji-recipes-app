package dev.bondarenko.fujirecipes.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.data.importing.ImportRow
import dev.bondarenko.fujirecipes.data.importing.ImportSummary
import dev.bondarenko.fujirecipes.data.importing.importBody
import dev.bondarenko.fujirecipes.data.importing.reviewSlots
import dev.bondarenko.fujirecipes.data.importing.summarise
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The Import screen's state — FEAT-007 T-15.
 *
 * Reading the camera and writing the library are both local now, but they stay separate
 * stages for the reason they always were: a failed import must not cost the read that
 * preceded it, and re-reading seven slots over USB is not free.
 */
sealed interface ImportStage {

    /** No camera. The screen offers to connect rather than to read. */
    data object NeedsCamera : ImportStage

    /** A camera, and nothing read yet. */
    data object Ready : ImportStage

    data class Reading(val current: Int, val total: Int) : ImportStage

    /**
     * Rows may be empty, and that is a real answer rather than a failure: a camera with no
     * custom recipes saved. Kept distinct from [Ready] for exactly that reason — "nothing
     * found" and "nothing asked yet" look the same on screen unless the state says otherwise.
     */
    data object Review : ImportStage

    data object Importing : ImportStage

    data class Done(val imported: Int) : ImportStage

    /**
     * Something failed, and the review is kept.
     *
     * [retryable] is false for a read that produced nothing to keep — there is no review
     * behind it, so the action is to read again rather than to import again.
     */
    data class Failed(val message: String, val retryable: Boolean = true) : ImportStage
}

data class ImportUiState(
    val stage: ImportStage,
    val rows: List<ImportRow> = emptyList(),
    val cameraModel: String? = null,
) {
    val summary: ImportSummary get() = summarise(rows)
    val canImport: Boolean get() = rows.any { it.selected }
}

class ImportViewModel(
    private val repository: RecipeRepository,
    private val controller: CameraController,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState(stage = stageFor(controller.state.value)))
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    init {
        // The chip can connect from anywhere, so this screen follows the connection rather
        // than owning it — but only while it is still waiting for one.
        viewModelScope.launch {
            controller.state.collect { camera ->
                val stage = _state.value.stage
                // Only the two waiting stages follow the connection. A review that has been
                // read must survive the camera being unplugged — the rows are in memory, and
                // importing them touches nothing but this phone.
                val follows = stage is ImportStage.NeedsCamera || stage is ImportStage.Ready

                _state.value = _state.value.copy(
                    stage = if (follows) stageFor(camera) else stage,
                    cameraModel = (camera as? CameraState.Connected)?.identity?.model,
                )
            }
        }
    }

    fun connect() = controller.connect()

    /** Reads every slot, then reviews what came back against the library. */
    fun read() {
        val total = 7
        _state.value = _state.value.copy(stage = ImportStage.Reading(0, total))

        viewModelScope.launch {
            val slots = runCatching {
                controller.readSlotRecipes { current, count ->
                    _state.value = _state.value.copy(stage = ImportStage.Reading(current, count))
                }
            }.getOrElse { error ->
                _state.value = _state.value.copy(
                    stage = ImportStage.Failed(
                        error.message ?: "The camera stopped answering while its slots were " +
                            "being read.",
                        retryable = false,
                    ),
                )
                return@launch
            }

            val library = repository.library.first { it.hasLoaded }.recipes

            _state.value = _state.value.copy(
                stage = ImportStage.Review,
                rows = reviewSlots(slots, library),
            )
        }
    }

    fun toggle(slot: Int) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.map {
                if (it.slot == slot) it.copy(selected = !it.selected) else it
            },
        )
    }

    fun import() {
        val rows = _state.value.rows
        if (rows.none { it.selected }) return

        _state.value = _state.value.copy(stage = ImportStage.Importing)

        viewModelScope.launch {
            _state.value = when (val result = repository.importAll(importBody(rows))) {
                is LibraryResult.Success -> _state.value.copy(
                    stage = ImportStage.Done(result.value.imported),
                )

                is LibraryResult.Failure -> _state.value.copy(
                    // The review stays in `rows`, so Retry costs nothing and the choices
                    // survive — re-reading the camera to recover from a failed write would be
                    // punishing the wrong thing.
                    stage = ImportStage.Failed(messageFor(result.error)),
                )
            }
        }
    }

    /** After a failure: back to the review that is still held, to try the same import again. */
    fun backToReview() {
        _state.value = _state.value.copy(
            stage = if (_state.value.rows.isEmpty()) {
                stageFor(controller.state.value)
            } else {
                ImportStage.Review
            },
        )
    }

    private fun messageFor(error: LibraryError): String = when (error) {
        is LibraryError.Storage ->
            "Your library could not be saved to this phone, so nothing was imported" +
                (error.message?.let { ": $it" } ?: ".") +
                " What you read off the camera is still here to try again."

        is LibraryError.Unreadable ->
            "The library already on this phone could not be read, so nothing was written to " +
                "it. Importing on top of it would replace recipes that may still be " +
                "recoverable."

        is LibraryError.Invalid -> {
            val field = error.fields.firstOrNull()
            "A slot could not be saved as a recipe" +
                (field?.let { " — ${it.path}: ${it.message}" } ?: "") +
                ". Nothing was imported."
        }

        is LibraryError.NotFound ->
            "A recipe this import replaces is no longer in your library. Nothing was imported."
    }

    companion object {
        private fun stageFor(camera: CameraState): ImportStage =
            if (camera is CameraState.Connected) ImportStage.Ready else ImportStage.NeedsCamera

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ImportViewModel(
                    repository = container.recipeRepository,
                    controller = container.cameraController,
                )
            }
        }
    }
}
