package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.SlotCaution
import dev.bondarenko.fujirecipes.camera.plan.SlotState
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.camera.usb.WriteOutcome

/**
 * The write sheet's stages — `PRD.md` §7.4.
 *
 * A type rather than a pile of booleans inside the composable, so the sequence is testable and
 * so "which stage am I on" has exactly one answer. One expanding sheet, not a stack of
 * dialogs: the user is doing one thing.
 */
sealed interface WriteStage {

    /** Stage 1. Skipped entirely when a camera is already connected. */
    data object Connect : WriteStage

    /**
     * Stage 2. Only when there is something to say: a refusal, or fields this body cannot
     * take. A compatible recipe never sees this stage.
     */
    data class Compatibility(val plan: WritePlan) : WriteStage

    /**
     * Stage 3. C1–C7, each showing what the camera says it currently holds.
     *
     * Read from the body when this stage is reached, never derived: a write is a destructive
     * act against a device, and the decision has to be made against the device's own answer at
     * the moment of choosing.
     */
    data object Picker : WriteStage

    /**
     * Stage 4. A second tap, on a slot with known contents.
     *
     * [caution] is why it is being asked — something named is about to be replaced, or the
     * camera would not say what is there. A slot the camera answered for with no name reaches
     * the write directly: confirming all seven slots of an untouched camera trains people to
     * tap through the dialog that exists to protect them.
     */
    data class Confirm(val slot: Int, val caution: SlotCaution) : WriteStage

    /** Stage 5. Non-dismissible; back is intercepted. */
    data class Progress(
        val slot: Int,
        val done: Int,
        val total: Int,
        val current: String,
    ) : WriteStage

    /** Stage 6, the good half. */
    data class Done(val outcome: WriteOutcome) : WriteStage

    /**
     * Stage 6, the other half.
     *
     * [warning] is the partial-slot sentence, present only when something was actually
     * written — saying "the slot may be half-written" about an untouched slot is a false
     * alarm.
     */
    data class Failed(
        val message: String,
        val warning: String? = null,
        val outcome: WriteOutcome? = null,
        /** The slot that was being written, so Retry starts the same write again. */
        val slot: Int? = null,
    ) : WriteStage
}

data class WriteUiState(
    val stage: WriteStage,
    val recipeName: String = "",
    /** True while the write is running: the sheet must not be dismissed. */
    val isWriting: Boolean = false,
    /** Set when the user pressed back mid-write and has not yet confirmed. */
    val confirmingCancel: Boolean = false,
    /** What the camera said each slot holds, C1 first. Always seven entries. */
    val slots: List<SlotState> = slotStates(emptyList(), loading = true),
    /** Set when the read failed as a whole, rather than one slot at a time. */
    val slotsError: String? = null,
    /**
     * The slot the picker is pointing at. Part of the write's own state rather than the
     * composable's, so it lives and dies with the write attempt — see [enteringPicker].
     */
    val selectedSlot: Int = FIRST_SLOT,
)

/**
 * The picker, as it must look every time it is reached.
 *
 * The selection is deliberately *not* carried over from an earlier write. A slot chosen for
 * the last recipe is a destructive default for this one: the button would offer to overwrite
 * C6 for a user who never asked about C6, and the only thing standing between them and a lost
 * recipe would be reading a number they have no reason to expect changed.
 */
fun WriteUiState.enteringPicker(): WriteUiState =
    copy(stage = WriteStage.Picker, selectedSlot = FIRST_SLOT)

/**
 * Where the sheet opens.
 *
 * Stage 1 is skipped when the camera is already connected, which is the ordinary case — the
 * app was launched by plugging the camera in, so asking it to connect again would be asking
 * about something already done.
 */
fun openingStage(camera: CameraState, plan: WritePlan): WriteStage = when {
    camera !is CameraState.Connected -> WriteStage.Connect
    plan.refusal != null || plan.dropped.isNotEmpty() -> WriteStage.Compatibility(plan)
    else -> WriteStage.Picker
}
