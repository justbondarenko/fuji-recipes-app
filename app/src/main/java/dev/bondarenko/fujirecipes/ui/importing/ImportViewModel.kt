package dev.bondarenko.fujirecipes.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.core.net.ApiResult
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
 * Reading is local and works with no signal; importing is a server write and does not
 * (`architecture.md` §4). Those two halves are deliberately separate stages, so a failed
 * import never costs the read that preceded it.
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
                // read must survive the camera being unplugged — the rows are in memory and
                // importing them needs the server, not the body.
                val follows = stage is ImportStage.NeedsCamera || stage is ImportStage.Ready

                _state.value = _state.value.copy(
                    stage = if (follows) stageFor(camera) else stage,
                    cameraModel = (camera as? CameraState.Connected)?.identity?.model,
                )
            }
        }
    }

    fun connect() = controller.connect()

    /**
     * Reads every slot, then reviews what came back against the library.
     *
     * The library comes from the snapshot when there is no signal, which is the point: the
     * duplicate check is as useful offline as on, because it compares against recipes the
     * phone is already holding.
     */
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
                is ApiResult.Success -> _state.value.copy(
                    stage = ImportStage.Done(result.value.imported),
                )

                is ApiResult.Failure -> _state.value.copy(
                    // The review stays in `rows`, so Retry costs nothing and the choices
                    // survive — re-reading the camera to recover from a server error would be
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

    private fun messageFor(error: ApiError): String = when (error) {
        // The distinction the whole screen turns on: the camera is local and the library is
        // not. Reading needed no signal; saving does.
        is ApiError.Network ->
            "Saving needs a connection — reading the camera did not. Nothing was imported, " +
                "and your choices are still here to try again."

        is ApiError.Forbidden ->
            "The server refused the app's credentials, so nothing was imported. Check the " +
                "connection settings."

        is ApiError.ValidationFailed -> {
            val field = error.fields.firstOrNull()
            "The server refused a recipe" +
                (field?.let { " — ${it.path}: ${it.message}" } ?: "") +
                ". Nothing was imported."
        }

        is ApiError.StorageUnavailable ->
            "The library is unreachable right now, so nothing was imported. Try again shortly."

        else -> "The import failed, so nothing was written: ${error::class.simpleName}"
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
