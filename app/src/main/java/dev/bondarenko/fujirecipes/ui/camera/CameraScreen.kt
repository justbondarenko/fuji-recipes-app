package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.SlotState
import dev.bondarenko.fujirecipes.camera.plan.SlotStatus
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.camera.ptp.responseName
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The camera's own screen — reached from the third toolbar item.
 *
 * When disconnected: Displays the status panel with connection guidance.
 * When connected: Immediately displays the green-accented camera header and a container card
 * showing the M3 shape-morphing loading indicator while reading slots, followed by a Bento grid
 * (arranged 2-2-2-1 with C7 centered) of the camera's custom slots.
 */
@Composable
fun CameraScreen(
    state: CameraState,
    isCameraAttached: Boolean,
    slots: List<SlotState> = slotStates(emptyList()),
    isLoadingSlots: Boolean = false,
    slotsError: String? = null,
    onRefreshSlots: () -> Unit = {},
    onSelectSlot: (Int) -> Unit = {},
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (state is CameraState.Connected) {
        CameraConnectedContent(
            state = state,
            slots = slots,
            isLoadingSlots = isLoadingSlots,
            slotsError = slotsError,
            onRefresh = onRefreshSlots,
            onSelectSlot = onSelectSlot,
            onDisconnect = onDisconnect,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    } else {
        CameraStatusContent(
            state = state,
            isCameraAttached = isCameraAttached,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

/**
 * Connected state content: Green-colorized header and Bento grid of camera slots inside a card.
 */
@Composable
fun CameraConnectedContent(
    state: CameraState.Connected,
    slots: List<SlotState>,
    isLoadingSlots: Boolean,
    slotsError: String?,
    onRefresh: () -> Unit,
    onSelectSlot: (Int) -> Unit = {},
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val greenAccent = if (dark) Color(0xFF6ABF69) else Color(0xFF2E7D32)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ─── 1. Header with Camera Name (Colorized in Green) ────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = CircleShape,
                    color = greenAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_photo_camera),
                            contentDescription = null,
                            tint = greenAccent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.identity.model.ifBlank { stringResource(R.string.camera_chip_connected) },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = greenAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.identity.label.ifBlank { stringResource(R.string.camera_writes_available) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.camera_action_disconnect))
            }
        }

        // ─── 2. Container/Card for Slots / Loading / Bento Grid ─────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.camera_slots_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!isLoadingSlots && slotsError == null && slots.isNotEmpty()) {
                            val occupiedCount = slots.count { it.status == SlotStatus.NAMED }
                            Text(
                                text = stringResource(R.string.camera_slots_count, occupiedCount, slots.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            enabled = !isLoadingSlots,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.write_slot_refresh),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                when {
                    isLoadingSlots -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                FujiLoadingIndicator(
                                    size = 44.dp,
                                    color = greenAccent,
                                )
                                Text(
                                    text = stringResource(R.string.camera_slots_reading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    slotsError != null -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = slotsError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Button(
                                    onClick = onRefresh,
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text(stringResource(R.string.camera_action_retry))
                                }
                            }
                        }
                    }

                    else -> {
                        CameraSlotsBentoGrid(
                            slots = slots,
                            greenAccent = greenAccent,
                            onSelectSlot = onSelectSlot,
                        )
                    }
                }
            }
        }

        // Additional information notes
        if (state.identity.writable) {
            Note(stringResource(R.string.camera_slot_note))
        } else if (state.identity.note != null) {
            Note(state.identity.note)
        }
    }
}

/**
 * Bento Grid for 7 custom slots arranged 2 - 2 - 2 - 1 (with C7 centered).
 */
@Composable
private fun CameraSlotsBentoGrid(
    slots: List<SlotState>,
    greenAccent: Color,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (slots.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: C1 & C2
        if (slots.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoSlotTile(state = slots[0], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[0].slot) }, modifier = Modifier.weight(1f))
                BentoSlotTile(state = slots[1], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[1].slot) }, modifier = Modifier.weight(1f))
            }
        }

        // Row 2: C3 & C4
        if (slots.size >= 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoSlotTile(state = slots[2], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[2].slot) }, modifier = Modifier.weight(1f))
                BentoSlotTile(state = slots[3], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[3].slot) }, modifier = Modifier.weight(1f))
            }
        }

        // Row 3: C5 & C6
        if (slots.size >= 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoSlotTile(state = slots[4], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[4].slot) }, modifier = Modifier.weight(1f))
                BentoSlotTile(state = slots[5], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[5].slot) }, modifier = Modifier.weight(1f))
            }
        }

        // Row 4: C7 (centered)
        if (slots.size >= 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                BentoSlotTile(state = slots[6], greenAccent = greenAccent, onSelectSlot = { onSelectSlot(slots[6].slot) }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

/**
 * Individual tile in the Bento grid for a single camera slot.
 */
@Composable
private fun BentoSlotTile(
    state: SlotState,
    greenAccent: Color,
    onSelectSlot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onSelectSlot,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (state.occupied) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (state.occupied) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (state.occupied) {
                        greenAccent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (state.occupied) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "C${state.slot}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (state.occupied) {
                    Surface(
                        shape = CircleShape,
                        color = greenAccent,
                        modifier = Modifier.size(6.dp),
                    ) {}
                }
            }

            Text(
                text = when (state.status) {
                    SlotStatus.NAMED -> state.name.orEmpty()
                    SlotStatus.UNNAMED -> stringResource(R.string.write_slot_unnamed)
                    SlotStatus.UNREADABLE -> stringResource(R.string.write_slot_unreadable)
                    SlotStatus.UNKNOWN -> stringResource(R.string.write_slot_unknown)
                    SlotStatus.READING -> stringResource(R.string.write_slot_reading)
                },
                style = if (state.occupied) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (state.occupied) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    state.occupied -> MaterialTheme.colorScheme.onSurface
                    state.status == SlotStatus.UNREADABLE -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The camera's state, in the same centred panel every other single-purpose page uses.
 * Used for disconnected, error, connecting, and no-usb states.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CameraStatusContent(
    state: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = cameraChipLook(state)
    val accent = look.tone.accent()
    val action = action(state)

    FujiIconPanel(
        icon = look.icon.painter(),
        shape = MaterialShapes.Pentagon.toShape(),
        title = title(state),
        body = body(state, isCameraAttached),
        containerColor = accent,
        contentColor = MaterialTheme.colorScheme.surface,
        actionLabel = action?.label?.let { stringResource(it) },
        onAction = when (action?.kind) {
            ActionKind.CONNECT -> onConnect
            ActionKind.DISCONNECT -> onDisconnect
            null -> null
        },
        actionIsPrimary = action?.kind != ActionKind.DISCONNECT,
        modifier = modifier,
        extra = {
            (state as? CameraState.Error)?.ptpCode?.let { code ->
                Note(stringResource(R.string.camera_ptp_code, responseName(code)))
            }
        },
    )
}

private enum class ActionKind { CONNECT, DISCONNECT }

private data class CameraAction(val label: Int, val kind: ActionKind)

private fun action(state: CameraState): CameraAction? = when (state) {
    CameraState.NoUsbHost, CameraState.Connecting -> null
    is CameraState.Writing -> null
    CameraState.Disconnected -> CameraAction(R.string.camera_action_connect, ActionKind.CONNECT)
    is CameraState.Error -> CameraAction(R.string.camera_action_retry, ActionKind.CONNECT)
    is CameraState.Connected -> CameraAction(R.string.camera_action_disconnect, ActionKind.DISCONNECT)
}

@Composable
private fun Note(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun title(state: CameraState): String = when (state) {
    CameraState.NoUsbHost -> stringResource(R.string.camera_state_no_usb_title)
    CameraState.Disconnected -> stringResource(R.string.camera_state_disconnected_title)
    CameraState.Connecting -> stringResource(R.string.camera_state_connecting_title)
    is CameraState.Connected -> state.identity.model.ifBlank { stringResource(R.string.camera_sheet_title) }
    is CameraState.Writing -> stringResource(R.string.camera_chip_writing)
    is CameraState.Error -> stringResource(R.string.camera_state_error_title)
}

@Composable
private fun body(state: CameraState, isCameraAttached: Boolean): String? = when (state) {
    CameraState.NoUsbHost -> stringResource(R.string.camera_state_no_usb_body)
    CameraState.Disconnected -> if (isCameraAttached) {
        stringResource(R.string.camera_state_attached_body)
    } else {
        stringResource(R.string.camera_state_disconnected_body)
    }
    CameraState.Connecting -> stringResource(R.string.camera_state_connecting_body)
    is CameraState.Connected -> state.identity.label
    is CameraState.Writing -> "C${state.slot} · ${state.done} / ${state.total} · ${state.current}"
    is CameraState.Error -> state.message
}

// ─── Previews ───────────────────────────────────────────────────────────────

private val previewConnectedSlots = slotStates(
    listOf(
        SlotNameReading(1, "Kodachrome 64", read = true),
        SlotNameReading(2, null, read = true),
        SlotNameReading(3, "Acros Night", read = true),
        SlotNameReading(4, null, read = true),
        SlotNameReading(5, "Portra 400", read = true),
        SlotNameReading(6, "CineStill 800T", read = true),
        SlotNameReading(7, null, read = true),
    ),
)

@Preview(name = "Connected — Loaded Bento Grid", showBackground = true)
@Preview(name = "Connected — Loaded Bento Grid (Dark)", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraConnectedLoadedPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraConnectedContent(
                state = CameraState.Connected(CameraModels.identify("X100VI")),
                slots = previewConnectedSlots,
                isLoadingSlots = false,
                slotsError = null,
                onRefresh = {},
                onDisconnect = {},
            )
        }
    }
}

@Preview(name = "Connected — Loading M3 Indicator", showBackground = true)
@Composable
private fun CameraConnectedLoadingPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraConnectedContent(
                state = CameraState.Connected(CameraModels.identify("X100VI")),
                slots = slotStates(emptyList(), loading = true),
                isLoadingSlots = true,
                slotsError = null,
                onRefresh = {},
                onDisconnect = {},
            )
        }
    }
}

@Preview(name = "Connected — All Occupied", showBackground = true)
@Composable
private fun CameraConnectedAllOccupiedPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraConnectedContent(
                state = CameraState.Connected(CameraModels.identify("X-T5")),
                slots = slotStates((1..7).map { SlotNameReading(it, "Recipe $it", read = true) }),
                isLoadingSlots = false,
                slotsError = null,
                onRefresh = {},
                onDisconnect = {},
            )
        }
    }
}

@Preview(name = "Connected — All Empty", showBackground = true)
@Composable
private fun CameraConnectedAllEmptyPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraConnectedContent(
                state = CameraState.Connected(CameraModels.identify("X-H2S")),
                slots = slotStates((1..7).map { SlotNameReading(it, null, read = true) }),
                isLoadingSlots = false,
                slotsError = null,
                onRefresh = {},
                onDisconnect = {},
            )
        }
    }
}

@Preview(name = "Camera — Error", showBackground = true)
@Composable
private fun CameraStatusErrorPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.Error(
                "The camera could not be claimed. Another app is probably holding it.",
                ptpCode = 0x2019,
            ),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera — Disconnected", showBackground = true)
@Composable
private fun CameraStatusDisconnectedPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.Disconnected,
            isCameraAttached = false,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera — No USB host", showBackground = true)
@Composable
private fun CameraStatusNoUsbPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.NoUsbHost,
            isCameraAttached = false,
            onConnect = {},
            onDisconnect = {},
        )
    }
}
