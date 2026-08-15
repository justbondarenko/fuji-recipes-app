package dev.bondarenko.fujirecipes.ui.camera

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.plan.DroppedField
import dev.bondarenko.fujirecipes.camera.plan.SlotCaution
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.SlotState
import dev.bondarenko.fujirecipes.camera.plan.SlotStatus
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.slotCaution
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.camera.usb.WriteOutcome
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
import java.util.UUID

/**
 * The write flow — `PRD.md` §7.4.
 *
 * One `ModalBottomSheet` expanding through its stages rather than a stack of dialogs, because
 * the user is doing one thing. The stage machine is `WriteSheetState.kt`; this renders it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteSheetHost(recipeId: String, onDismiss: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container

    /**
     * One view model per *write attempt*, not per screen.
     *
     * The store owner here is the surrounding navigation entry, which outlives the sheet. With
     * the default key, the second write from the same screen reuses the first write's view
     * model — the recipe it was built for, the slot it ended on, the stage it finished in — so
     * opening the sheet for another recipe showed the previous write's outcome instead of a
     * picker. A key that changes each time the sheet opens makes every attempt start over.
     *
     * `rememberSaveable`, so a rotation mid-write returns to the write that is running rather
     * than starting a second one against a half-written slot.
     */
    val attemptKey = rememberSaveable { UUID.randomUUID().toString() }
    val viewModel: WriteViewModel = viewModel(
        key = attemptKey,
        factory = WriteViewModel.factory(container, recipeId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    /**
     * The confirmation you feel rather than read.
     *
     * `PRD.md` §7.4: you will often be looking at the camera, not the phone, when the write
     * lands. This matters more than it sounds.
     */
    LaunchedEffect(state.stage) {
        when (state.stage) {
            is WriteStage.Done -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            is WriteStage.Failed -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            else -> Unit
        }
    }

    ModalBottomSheet(
        // A write in progress is a slot being changed. Dismissing it would abandon the sheet,
        // not the write, and leave the user with no way back to the result.
        onDismissRequest = { if (!state.isWriting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        WriteSheetContent(
            state = state,
            onConnect = viewModel::connect,
            onAcceptCompatibility = viewModel::acceptCompatibility,
            onChooseSlot = viewModel::chooseSlot,
            onSelectSlot = viewModel::selectSlot,
            onRefreshSlots = viewModel::refreshSlots,
            onBackToPicker = viewModel::backToPicker,
            onConfirm = viewModel::confirm,
            onRetry = viewModel::retry,
            onRequestCancel = viewModel::requestCancel,
            onDismissCancel = viewModel::dismissCancel,
            onConfirmCancel = viewModel::confirmCancel,
            onDone = onDismiss,
        )
    }
}

@Composable
fun WriteSheetContent(
    state: WriteUiState,
    onConnect: () -> Unit,
    onAcceptCompatibility: () -> Unit,
    onChooseSlot: (Int) -> Unit,
    onSelectSlot: (Int) -> Unit,
    onRefreshSlots: () -> Unit,
    onBackToPicker: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onRequestCancel: () -> Unit,
    onDismissCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Back during a write asks first, for the same reason the sheet will not be dismissed.
    BackHandler(enabled = state.isWriting) { onRequestCancel() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val stage = state.stage) {
            WriteStage.Connect -> ConnectStage(state.recipeName, onConnect)

            is WriteStage.Compatibility -> CompatibilityStage(
                plan = stage.plan,
                onWriteAnyway = onAcceptCompatibility,
                onCancel = onDone,
            )

            WriteStage.Picker -> PickerStage(
                slots = state.slots,
                slotsError = state.slotsError,
                recipeName = state.recipeName,
                selectedSlot = state.selectedSlot,
                onSelectSlot = onSelectSlot,
                onConfirm = onConfirm,
                onRefresh = onRefreshSlots,
            )

            is WriteStage.Confirm -> ConfirmStage(
                slot = stage.slot,
                caution = stage.caution,
                recipeName = state.recipeName,
                onConfirm = { onConfirm(stage.slot) },
                onBack = onBackToPicker,
            )

            is WriteStage.Progress -> ProgressStage(stage)

            is WriteStage.Done -> DoneStage(stage.outcome, onDone)

            is WriteStage.Failed -> FailedStage(
                stage = stage,
                onRetry = { stage.slot?.let(onRetry) },
                onDone = onDone,
            )
        }
    }

    if (state.confirmingCancel) {
        AlertDialog(
            onDismissRequest = onDismissCancel,
            title = { Text(stringResource(R.string.write_cancel_title)) },
            text = { Text(stringResource(R.string.write_cancel_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmCancel) {
                    Text(stringResource(R.string.write_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancel) {
                    Text(stringResource(R.string.write_cancel_keep))
                }
            },
        )
    }
}

// ─── Stage 1 ────────────────────────────────────────────────────────────────

@Composable
private fun ConnectStage(recipeName: String, onConnect: () -> Unit) {
    StageHeader(stringResource(R.string.write_title))
    Body(stringResource(R.string.write_connect_body, recipeName))
    Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.camera_action_connect))
    }
}

// ─── Stage 2 ────────────────────────────────────────────────────────────────

@Composable
private fun CompatibilityStage(plan: WritePlan, onWriteAnyway: () -> Unit, onCancel: () -> Unit) {
    val refusal = plan.refusal

    if (refusal != null) {
        // WR-01. Blocking: there is no "write anyway" for a body with no slot registers.
        StageHeader(stringResource(R.string.write_refused_title))
        Panel(refusal, alert = true)
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_cancel))
        }
        return
    }

    StageHeader(stringResource(R.string.write_dropped_title))
    Body(stringResource(R.string.write_dropped_body))

    plan.dropped.forEach { DroppedRow(it) }
    plan.notes.forEach { Panel(it) }

    Button(onClick = onWriteAnyway, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.write_anyway))
    }
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_cancel))
    }
}

@Composable
private fun DroppedRow(dropped: DroppedField) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dropped.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = dropped.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// ─── Stage 3 ────────────────────────────────────────────────────────────────

@Composable
private fun PickerStage(
    slots: List<SlotState>,
    slotsError: String?,
    recipeName: String,
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.write_pick_slot),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.write_slot_refresh),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    slotsError?.let { Panel(it, alert = true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        slots.forEachIndexed { index, slot ->
            SegmentedSlotItem(
                state = slot,
                isSelected = slot.slot == selectedSlot,
                index = index,
                count = slots.size,
                onClick = { onSelectSlot(slot.slot) },
            )
        }
    }

    val selectedSlotState = slots.firstOrNull { it.slot == selectedSlot }
    val caution = selectedSlotState?.let { slotCaution(it) }

    if (caution != null) {
        Panel(
            text = when (caution) {
                is SlotCaution.Named -> stringResource(
                    R.string.write_confirm_named,
                    selectedSlot,
                    caution.recipeName,
                    recipeName,
                )
                is SlotCaution.Unknown -> stringResource(R.string.write_confirm_unknown, selectedSlot)
            },
            alert = true,
        )
    }

    Button(
        onClick = { onConfirm(selectedSlot) },
        enabled = selectedSlotState != null && selectedSlotState.status != SlotStatus.READING,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.write_confirm_action, selectedSlot))
    }
}

/**
 * One slot in the M3 single-select list, segmented variant.
 *
 * The stepped corner radii that mark first / middle / last are
 * `ListItemDefaults.segmentedShapes`, and the selected-vs-not container and text roles are
 * `segmentedColors` — both were hand-computed here before.
 *
 * The `C1`…`C7` badge stays custom: it is the camera's own label for the slot, and no list
 * component supplies one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SegmentedSlotItem(
    state: SlotState,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        selected = isSelected,
        enabled = state.status != SlotStatus.READING,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "C${state.slot}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        trailingContent = {
            if (state.status == SlotStatus.READING) {
                Text(
                    text = stringResource(R.string.write_slot_reading),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    ) {
        Text(
            text = state.name ?: stringResource(state.status.labelRes()),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The status in the app's words. In `strings.xml` because everything user-facing is. */
@StringRes
private fun SlotStatus.labelRes(): Int = when (this) {
    SlotStatus.READING -> R.string.write_slot_reading
    // Never reached: a named slot renders its name.
    SlotStatus.NAMED -> R.string.write_slot_name
    SlotStatus.UNNAMED -> R.string.write_slot_unnamed
    SlotStatus.UNREADABLE -> R.string.write_slot_unreadable
    SlotStatus.UNKNOWN -> R.string.write_slot_unknown
}

// ─── Stage 4 ────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmStage(
    slot: Int,
    caution: SlotCaution,
    recipeName: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    StageHeader(stringResource(R.string.write_confirm_title, slot))

    // The confirmation names what is being lost. "Are you sure?" is a question nobody can
    // answer; "C3 currently holds Kodachrome 64" is one they can.
    Panel(
        when (caution) {
            is SlotCaution.Named -> stringResource(
                R.string.write_confirm_named,
                slot,
                caution.recipeName,
                recipeName,
            )

            is SlotCaution.Unknown -> stringResource(R.string.write_confirm_unknown, slot)
        },
        alert = true,
    )

    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.write_confirm_action, slot))
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_back))
    }
}

// ─── Stage 5 ────────────────────────────────────────────────────────────────

@Composable
private fun ProgressStage(stage: WriteStage.Progress) {
    StageHeader(stringResource(R.string.write_progress_title, stage.slot))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stage.current,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.write_progress_count, stage.done, stage.total),
            // Tabular figures, so the count does not jitter as it climbs (`PRD.md` §5.3).
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = TabularFigures,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    // `LinearWavyProgressIndicator` — the wave being the visual signal that something is
    // genuinely happening on the wire — is internal at material3 1.4.0 (`tech-stack.md` §6).
    val fraction = if (stage.total == 0) 0f else stage.done.toFloat() / stage.total
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

// ─── Stage 6 ────────────────────────────────────────────────────────────────

@Composable
private fun DoneStage(outcome: WriteOutcome, onDone: () -> Unit) {
    StageHeader(stringResource(R.string.write_done_title, outcome.slot))
    Body(stringResource(R.string.write_done_body, outcome.written, outcome.total))

    // A refused setting or a value the body adjusted did not stop the write, and the user
    // should still know about it.
    outcome.warnings.forEach { Panel(it) }

    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_done))
    }
}

@Composable
private fun FailedStage(stage: WriteStage.Failed, onRetry: () -> Unit, onDone: () -> Unit) {
    StageHeader(stringResource(R.string.write_failed_title))
    Panel(stage.message, alert = true)

    // Only when something actually landed. Saying "the slot may be half-written" about an
    // untouched slot is a false alarm.
    stage.warning?.let { Panel(it, alert = true) }

    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.camera_action_retry))
    }
    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_done))
    }
}

// ─── Pieces ─────────────────────────────────────────────────────────────────

@Composable
private fun StageHeader(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Panel(text: String, alert: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (alert) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (alert) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────
//
// `coding-standards.md`: a preview for every state a spec names, including the error and
// refusal ones. A preview of only the happy path is how the failure stage ships broken — and
// this one is unreachable without a camera refusing a property mid-write.

private val previewSlots = slotStates(
    listOf(
        SlotNameReading(1, "Kodachrome 64", read = true),
        SlotNameReading(2, null, read = true),
        SlotNameReading(3, "Acros Night", read = true),
        SlotNameReading(4, null, read = true),
        SlotNameReading(5, null, read = false),
        SlotNameReading(6, "Portra 2", read = true),
        SlotNameReading(7, null, read = true),
    ),
)

private fun previewState(stage: WriteStage, writing: Boolean = false) = WriteUiState(
    stage = stage,
    recipeName = "Kodachrome 64",
    isWriting = writing,
    slots = previewSlots,
)

@Composable
private fun PreviewSheet(state: WriteUiState) {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            WriteSheetContent(
                state = state,
                onConnect = {}, onAcceptCompatibility = {}, onChooseSlot = {},
                onSelectSlot = {}, onRefreshSlots = {},
                onBackToPicker = {}, onConfirm = {}, onRetry = {}, onRequestCancel = {},
                onDismissCancel = {}, onConfirmCancel = {}, onDone = {},
            )
        }
    }
}

@Preview(name = "Write — connect", showBackground = true)
@Composable
private fun WriteConnectPreview() = PreviewSheet(previewState(WriteStage.Connect))

@Preview(name = "Write — refused (WR-01)", showBackground = true)
@Composable
private fun WriteRefusedPreview() = PreviewSheet(
    previewState(
        WriteStage.Compatibility(
            WritePlan(
                slot = 1,
                refusal = "X-Trans III has no custom slots, so a recipe cannot be written to it.",
            ),
        ),
    ),
)

@Preview(name = "Write — dropped fields", showBackground = true)
@Preview(name = "Write — dropped fields, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun WriteDroppedPreview() = PreviewSheet(
    previewState(
        WriteStage.Compatibility(
            WritePlan(
                slot = 1,
                dropped = listOf(
                    DroppedField(
                        "filmSimulation",
                        "Film simulation",
                        "X-Trans IV has no Nostalgic Neg. simulation, so the slot will keep " +
                            "the simulation it already has",
                    ),
                    DroppedField(
                        "monochromaticColorWc",
                        "Warm / cool",
                        "X-Trans IV has no monochromatic colour",
                    ),
                ),
            ),
        ),
    ),
)

@Preview(name = "Write — slot picker", showBackground = true)
@Composable
private fun WritePickerPreview() = PreviewSheet(previewState(WriteStage.Picker))

@Preview(name = "Write — confirm", showBackground = true)
@Composable
private fun WriteConfirmPreview() = PreviewSheet(
    previewState(WriteStage.Confirm(3, SlotCaution.Named(3, "Acros Night"))),
)

/** The slot the camera would not describe — a different warning, deliberately. */
@Preview(name = "Write — confirm, unreadable slot", showBackground = true)
@Composable
private fun WriteConfirmUnknownPreview() = PreviewSheet(
    previewState(WriteStage.Confirm(5, SlotCaution.Unknown(5))),
)

@Preview(name = "Write — picker, still reading", showBackground = true)
@Composable
private fun WritePickerReadingPreview() = PreviewSheet(
    previewState(WriteStage.Picker).copy(slots = slotStates(emptyList(), loading = true)),
)

@Preview(name = "Write — progress", showBackground = true)
@Composable
private fun WriteProgressPreview() = PreviewSheet(
    previewState(WriteStage.Progress(slot = 3, done = 7, total = 17, current = "Clarity"), true),
)

@Preview(name = "Write — done", showBackground = true)
@Composable
private fun WriteDonePreview() = PreviewSheet(
    previewState(
        WriteStage.Done(
            WriteOutcome(
                slot = 3,
                written = 16,
                total = 17,
                warnings = listOf(
                    "Clarity was refused — the camera answered InvalidDevicePropValue " +
                        "(0x200C) — so the slot keeps whatever it had for it.",
                ),
                slotTouched = true,
            ),
        ),
    ),
)

@Preview(name = "Write — failed", showBackground = true)
@Composable
private fun WriteFailedPreview() = PreviewSheet(
    previewState(
        WriteStage.Failed(
            message = "The connection failed while writing Sharpness: the camera did not " +
                "answer within 5000ms.",
            warning = "Slot C3 was partly written: 9 of 17 properties went in before this " +
                "stopped. The rest are whatever the slot held before, so it is neither the " +
                "old recipe nor the new one. Writing it again from the start fixes it.",
            slot = 3,
        ),
    ),
)
