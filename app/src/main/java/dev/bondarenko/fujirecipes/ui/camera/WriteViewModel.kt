package dev.bondarenko.fujirecipes.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.WriteResult
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.buildWritePlan
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The write sheet's state, and the one place a write is started from.
 *
 * Reads the recipe from the already-loaded library rather than fetching it: the whole library
 * is in memory (`architecture.md` §4), and a camera write must work with no signal
 * (`architecture.md` C4). A network call on this path would break the field case entirely.
 */
class WriteViewModel(
    private val recipeId: String,
    private val repository: RecipeRepository,
    private val controller: CameraController,
) : ViewModel() {

    private val _state = MutableStateFlow(WriteUiState(stage = WriteStage.Connect))
    val state: StateFlow<WriteUiState> = _state.asStateFlow()

    private var recipe: Recipe? = null
    private var cancelRequested = false

    val cameraState: StateFlow<CameraState> = controller.state

    init {
        viewModelScope.launch {
            val loaded = repository.library.first { it.hasLoaded }
                .recipes.firstOrNull { it.id == recipeId }

            recipe = loaded
            _state.value = WriteUiState(
                stage = openingStage(controller.state.value, planFor(FIRST_SLOT)),
                recipeName = loaded?.name.orEmpty(),
            )
        }
    }

    /**
     * The plan for a slot.
     *
     * Nothing but the slot step's own value depends on the slot, so the compatibility stage is
     * computed from the plan for C1 and the real one is rebuilt once a slot is chosen. Cheaper
     * than threading a provisional slot through the UI, and the plan is pure so building it
     * twice costs nothing.
     */
    private fun planFor(slot: Int): WritePlan {
        val loaded = recipe
            ?: return WritePlan(slot = slot, refusal = "That recipe is no longer in the library.")

        val camera = controller.state.value as? CameraState.Connected

        return buildWritePlan(
            slot = slot,
            generation = camera?.identity?.generation
            // Not connected yet: the plan is only used to decide whether the compatibility
            // stage is needed, and it is rebuilt once a body is known.
                ?: RecipeFields.generationOf(null),
            recipeName = loaded.name,
            settings = loaded.settings,
            recipeGeneration = null,
        )
    }

    /** Stage 1 → the camera connects, and the sheet moves on when it does. */
    fun connect() {
        controller.connect()
        viewModelScope.launch {
            controller.state.first { it is CameraState.Connected }
            _state.value = _state.value.copy(stage = openingStage(controller.state.value, planFor(FIRST_SLOT)))
        }
    }

    /** Stage 2 → "write anyway", having read what will be dropped. */
    fun acceptCompatibility() {
        _state.value = _state.value.copy(stage = WriteStage.Picker)
    }

    /** Stage 3 → a slot is chosen; stage 4 asks for a second tap. */
    fun chooseSlot(slot: Int) {
        _state.value = _state.value.copy(stage = WriteStage.Confirm(slot))
    }

    fun backToPicker() {
        _state.value = _state.value.copy(stage = WriteStage.Picker)
    }

    /** Stage 4 → the write runs. */
    fun confirm(slot: Int) {
        val plan = planFor(slot)
        cancelRequested = false

        _state.value = _state.value.copy(
            stage = WriteStage.Progress(slot, 0, plan.total, ""),
            isWriting = true,
        )

        viewModelScope.launch {
            // The chip's own progress comes from the controller's state; this mirrors it into
            // the sheet so the two cannot disagree about the count.
            val mirror = launch {
                controller.state.collect { camera ->
                    if (camera is CameraState.Writing) {
                        _state.value = _state.value.copy(
                            stage = WriteStage.Progress(
                                camera.slot,
                                camera.done,
                                camera.total,
                                camera.current,
                            ),
                        )
                    }
                }
            }

            val result = controller.write(plan) { cancelRequested }
            mirror.cancel()

            _state.value = _state.value.copy(
                stage = when (result) {
                    is WriteResult.Success -> WriteStage.Done(result.outcome)
                    is WriteResult.Failure -> WriteStage.Failed(
                        message = result.message,
                        warning = result.warning,
                        outcome = result.outcome,
                        slot = slot,
                    )
                },
                isWriting = false,
                confirmingCancel = false,
            )
        }
    }

    /** Back during a write asks first — the slot is being changed as we speak. */
    fun requestCancel() {
        _state.value = _state.value.copy(confirmingCancel = true)
    }

    fun dismissCancel() {
        _state.value = _state.value.copy(confirmingCancel = false)
    }

    /** Takes effect at the next step boundary; a sent property cannot be unsent. */
    fun confirmCancel() {
        cancelRequested = true
        _state.value = _state.value.copy(confirmingCancel = false)
    }

    /** Failure → try the same slot again from the start. */
    fun retry(slot: Int) = confirm(slot)

    companion object {
        fun factory(container: AppContainer, recipeId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    WriteViewModel(
                        recipeId = recipeId,
                        repository = container.recipeRepository,
                        controller = container.cameraController,
                    )
                }
            }
    }
}
