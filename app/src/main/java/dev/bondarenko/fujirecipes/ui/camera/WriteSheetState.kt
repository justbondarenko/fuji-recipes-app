package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
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

    /** Stage 3. C1–C7. Contents read "Unknown" — this build does not read slots back. */
    data object Picker : WriteStage

    /** Stage 4. A second tap on the chosen slot. */
    data class Confirm(val slot: Int) : WriteStage

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
)

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
