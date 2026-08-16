package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.camera.ptp.responseName
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The camera's own screen — reached from the third toolbar item.
 *
 * Says what is connected, whether a write can be offered, and when it cannot, why. The two
 * reasons a write is unavailable are kept distinguishable here as well as in the model table:
 * a body without slot registers will never take one, which is not the same as a body this
 * build has not met.
 *
 * A destination rather than the bottom sheet it used to be. The status moved into the toolbar
 * when the second FAB went away, and a toolbar item that opens a sheet behaves unlike its
 * three neighbours, which all navigate.
 */
@Composable
fun CameraScreen(
    state: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // No heading of its own: the panel's title already names the state, and a fixed "Camera"
    // above it would say the same thing twice. No app bar or back arrow either — this is a
    // top-level destination, so nothing navigated here.
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

/**
 * The camera's state, in the same centred panel every other single-purpose page uses.
 *
 * Pentagon is this page's shape, and it is filled with the toolbar item's own colour for the
 * state — green connected, amber no-host, error red — so the thing you pressed and the thing
 * you arrived at agree. Same for the glyph: `cameraChipLook` drives both.
 *
 * The layout holds for every state. Only the words, the colour and the single action change;
 * a state with nothing to offer (no USB host, mid-connect, mid-write) simply has no button.
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
        // The nav item's state colour fills the shape; the glyph sits on it in the surface
        // colour so it stays legible against every tone.
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
            // The camera's own response code, when it gave one. "The camera said DeviceBusy"
            // tells the user to close the app holding the device; "connection failed" tells
            // them nothing (P5).
            (state as? CameraState.Error)?.ptpCode?.let { code ->
                Note(stringResource(R.string.camera_ptp_code, responseName(code)))
            }

            if (state is CameraState.Connected) {
                Note(
                    if (state.identity.writable) {
                        stringResource(R.string.camera_writes_available)
                    } else {
                        // Either "no custom slots" or "this build does not know this body" —
                        // the model table decides which, and they must not read the same.
                        state.identity.note.orEmpty()
                    },
                )
                if (state.identity.writable) Note(stringResource(R.string.camera_slot_note))
            }
        },
    )
}

private enum class ActionKind { CONNECT, DISCONNECT }

private data class CameraAction(val label: Int, val kind: ActionKind)

/**
 * The one thing this state offers, or nothing.
 *
 * No retry for a phone without USB host support: it will not start having it, and a button
 * that cannot work is a lie someone acts on. Nothing mid-write either — cancelling belongs to
 * the write sheet, which owns the operation and can warn about a partly written slot.
 */
private fun action(state: CameraState): CameraAction? = when (state) {
    CameraState.NoUsbHost, CameraState.Connecting -> null
    is CameraState.Writing -> null
    CameraState.Disconnected -> CameraAction(R.string.camera_action_connect, ActionKind.CONNECT)
    is CameraState.Error -> CameraAction(R.string.camera_action_retry, ActionKind.CONNECT)
    is CameraState.Connected ->
        CameraAction(R.string.camera_action_disconnect, ActionKind.DISCONNECT)
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
    // The body's own name, and its label right under it — never a fallback generation
    // presented as a fact.
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

@Preview(name = "Camera sheet — connected", showBackground = true)
@Preview(name = "Camera sheet — connected, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraStatusConnectedPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.Connected(CameraModels.identify("X100VI")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — unrecognised body", showBackground = true)
@Composable
private fun CameraStatusUnrecognisedPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.Connected(CameraModels.identify("X-T99")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — no slots", showBackground = true)
@Composable
private fun CameraStatusNoSlotsPreview() {
    FujiTheme {
        CameraStatusContent(
            state = CameraState.Connected(CameraModels.identify("X-T2")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — error", showBackground = true)
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

@Preview(name = "Camera sheet — disconnected", showBackground = true)
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

@Preview(name = "Camera sheet — no USB host", showBackground = true)
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
