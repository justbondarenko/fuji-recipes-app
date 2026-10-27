package dev.bondarenko.fujirecipes.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.CameraTransport
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackup
import dev.bondarenko.fujirecipes.camera.WriteResult
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.buildWritePlan
import dev.bondarenko.fujirecipes.camera.plan.SlotStatus
import dev.bondarenko.fujirecipes.camera.plan.slotCaution
import dev.bondarenko.fujirecipes.camera.plan.slotStates
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
 * is in memory (`architecture.md` §4), and a camera write touches nothing but the phone and
 * the body on the other end of the cable (`architecture.md` C4).
 */
class WriteViewModel(
    private val recipeId: String,
    private val repository: RecipeRepository,
    private val controller: CameraController,
) : ViewModel() {

    private val _state = MutableStateFlow(WriteUiState(stage = WriteStage.Connect))
    val state: StateFlow<WriteUiState> = _state.asStateFlow()

    private var recipe: Recipe? = null
    @Volatile private var cancelRequested = false

    val cameraState: StateFlow<CameraState> = controller.state

    init {
        viewModelScope.launch {
            val loaded = repository.library.first { it.hasLoaded }
                .recipes.firstOrNull { it.id == recipeId }

            recipe = loaded
            controller.refreshBluetoothRecovery()
            val stage = openingStage(controller.state.value, openingPlan())
            _state.value = WriteUiState(stage = stage, recipeName = loaded?.name.orEmpty())
            if (requiresRecovery()) return@launch

            // The ordinary case opens straight on the picker, so the read starts here rather
            // than only on the compatibility stage's way out.
            if (stage == WriteStage.Picker) enterPicker()
        }
    }

    /**
     * Arrive at the slot picker: nothing chosen for the user, and a fresh read from the body.
     *
     * Both halves matter every single time this stage is reached. `WriteUiState.enteringPicker`
     * says why the selection resets; [refreshSlots] says why the names are re-read.
     */
    private fun enterPicker() {
        if (requiresRecovery()) return
        _state.value = _state.value.enteringPicker()
        refreshSlots()
    }

    private fun requiresRecovery(): Boolean {
        if (controller.transport.value != CameraTransport.BLUETOOTH || controller.recovery.value == null) return false
        _state.value = _state.value.copy(stage = WriteStage.RecoveryRequired())
        return true
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
        if (camera?.transport == CameraTransport.BLUETOOTH) {
            val refusal = runCatching {
                require(slot in 1..7) { "Choose a Bluetooth slot from C1–C7." }
                BluetoothBackup.validateRecipe(loaded.name, loaded.settings)
            }.exceptionOrNull()?.message
            return WritePlan(slot = slot, refusal = refusal)
        }

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

    private fun openingPlan() = planFor(FIRST_SLOT)

    private fun isBluetooth() = (controller.state.value as? CameraState.Connected)?.transport == CameraTransport.BLUETOOTH

    /** Stage 1 → the camera connects, and the sheet moves on when it does. */
    fun connect() {
        if (requiresRecovery()) return
        _state.value = _state.value.copy(connectionError = null)
        viewModelScope.launch {
            controller.connect()?.join()
            val camera = controller.state.first { it !is CameraState.Connecting }
            if (camera !is CameraState.Connected) {
                _state.value = _state.value.copy(connectionError = (camera as? CameraState.Error)?.message
                    ?: "The camera is not connected. Wake it and try again.")
                return@launch
            }
            val stage = openingStage(camera, openingPlan())
            _state.value = _state.value.copy(stage = stage)
            if (stage == WriteStage.Picker) enterPicker()
        }
    }

    /** Stage 2 → "write anyway", having read what will be dropped. */
    fun acceptCompatibility() {
        enterPicker()
    }

    /**
     * Asks the camera what its slots hold.
     *
     * Run every time the picker is reached rather than cached, because the answer can change
     * without this app: a recipe set by hand on the body, or written from the web client. A
     * stale name here is worse than no name, since it is a wrong fact rather than a missing
     * one.
     */
    fun refreshSlots() {
        _state.value = _state.value.copy(
            slots = slotStates(emptyList(), loading = true),
            slotsError = null,
            selectedSlot = null,
        )

        viewModelScope.launch {
            runCatching { controller.readSlots() }
                .onSuccess { readings ->
                    val newSlots = slotStates(readings)
                    val firstEmptySlot = newSlots.firstOrNull { it.status == SlotStatus.UNNAMED }?.slot
                    _state.value = _state.value.copy(
                        slots = newSlots,
                        selectedSlot = firstEmptySlot,
                    )
                }
                .onFailure { error ->
                    // The read failed as a whole. Seven slots showing "Unknown" is the honest
                    // rendering, and the message says why they are unknown.
                    _state.value = _state.value.copy(
                        slots = slotStates(emptyList()),
                        slotsError = error.message
                            ?: "The camera did not answer when asked what its slots hold.",
                        selectedSlot = null,
                    )
                }
        }
    }

    /**
     * Stage 3 → a slot is chosen.
     *
     * A slot with something known in it gets stage 4's second tap. A slot the camera answered
     * for with no name goes straight to the write — there is nothing identifiable to lose, and
     * a confirmation in front of all seven slots of an untouched camera is one nobody reads.
     */
    fun chooseSlot(slot: Int) {
        val state = _state.value.slots.firstOrNull { it.slot == slot }

        // A slot still being read is not a choice anyone can make yet, and it must not fall
        // through to the no-caution branch — `slotCaution` answers null for it, which would
        // start a write with no confirmation. The picker also disables the row; this is the
        // half that has to hold, because the other half is a rendering decision.
        if (state == null || state.status == SlotStatus.READING) return

        val caution = slotCaution(state)

        if (caution == null) {
            confirm(slot)
        } else {
            _state.value = _state.value.copy(stage = WriteStage.Confirm(slot, caution))
        }
    }

    fun backToPicker() {
        enterPicker()
    }

    /**
     * The picker's selection — which slot the write button is offering.
     *
     * A slot still being read cannot be chosen: `slotCaution` answers null for it, so a write
     * aimed at one would start with no warning about what it is replacing.
     */
    fun selectSlot(slot: Int) {
        val state = _state.value.slots.firstOrNull { it.slot == slot } ?: return
        if (state.status == SlotStatus.READING) return

        _state.value = _state.value.copy(selectedSlot = slot)
    }

    /** Stage 4 → the write runs. */
    fun confirm(slot: Int) {
        if (_state.value.isWriting) return
        if (requiresRecovery()) return
        // The same guard as [selectSlot], on the path that actually sends bytes. The picker
        // disables the button too; this is the half that has to hold, because the other half
        // is a rendering decision.
        val slotState = _state.value.slots.firstOrNull { it.slot == slot }
        if (slotState?.status == SlotStatus.READING) return

        val plan = planFor(slot)
        if (plan.refusal != null) {
            _state.value = _state.value.copy(stage = WriteStage.Compatibility(plan))
            return
        }
        cancelRequested = false

        _state.value = _state.value.copy(
            stage = WriteStage.Progress(slot, 0, plan.total, ""),
            isWriting = true,
            canCancel = true,
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
                            canCancel = camera.cancellable,
                            confirmingCancel = if (camera.cancellable) _state.value.confirmingCancel else false,
                        )
                    }
                }
            }

            val loaded = recipe
            val result = if (isBluetooth() && loaded != null) {
                controller.writeBluetoothRecipe(slot, loaded.name, loaded.settings) { cancelRequested }
            } else controller.write(plan) { cancelRequested }
            mirror.cancel()

            _state.value = _state.value.copy(
                stage = when (result) {
                    is WriteResult.Success -> WriteStage.Done(result.outcome)
                    is WriteResult.Failure -> if (result.recoveryRequired) WriteStage.RecoveryRequired(result.message)
                    else WriteStage.Failed(
                        message = result.message,
                        warning = result.warning,
                        outcome = result.outcome,
                        slot = slot,
                        retryable = result.retryable,
                    )
                },
                isWriting = false,
                confirmingCancel = false,
            )
        }
    }

    /** Back during a write asks first — the slot is being changed as we speak. */
    fun requestCancel() {
        if (!(_state.value.canCancel && (controller.state.value as? CameraState.Writing)?.cancellable != false)) return
        _state.value = _state.value.copy(confirmingCancel = true)
    }

    fun dismissCancel() {
        _state.value = _state.value.copy(confirmingCancel = false)
    }

    /** Takes effect at the next step boundary; a sent property cannot be unsent. */
    fun confirmCancel() {
        if ((controller.state.value as? CameraState.Writing)?.cancellable == false) return
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
