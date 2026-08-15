package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.ptp.responseName
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * What the camera chip opens — `FEAT-005` 01-functional §20.
 *
 * Says what is connected, whether a write can be offered, and when it cannot, why. The two
 * reasons a write is unavailable are kept distinguishable here as well as in the model table:
 * a body without slot registers will never take one, which is not the same as a body this
 * build has not met.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSheet(
    state: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        CameraSheetContent(
            state = state,
            isCameraAttached = isCameraAttached,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
        )
    }
}

@Composable
fun CameraSheetContent(
    state: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                text = title(state),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        body(state, isCameraAttached)?.let { body ->
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The camera's own response code, when it gave one. "The camera said DeviceBusy" tells
        // the user to close the app holding the device; "connection failed" tells them
        // nothing (P5).
        (state as? CameraState.Error)?.ptpCode?.let { code ->
            Note(stringResource(R.string.camera_ptp_code, responseName(code)))
        }

        if (state is CameraState.Connected) {
            Note(
                if (state.identity.writable) {
                    stringResource(R.string.camera_writes_available)
                } else {
                    // Either "no custom slots" or "this build does not know this body" — the
                    // model table decides which, and they must not read the same.
                    state.identity.note.orEmpty()
                },
            )
            if (state.identity.writable) Note(stringResource(R.string.camera_slot_note))
        }

        Actions(state = state, onConnect = onConnect, onDisconnect = onDisconnect)
    }
}

@Composable
private fun Actions(
    state: CameraState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    when (state) {
        // No retry: this phone will not start having USB host support, and a button that
        // cannot work is a lie someone acts on.
        CameraState.NoUsbHost, CameraState.Connecting -> Unit

        CameraState.Disconnected -> Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.camera_action_connect))
        }

        is CameraState.Error -> Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.camera_action_retry))
        }

        is CameraState.Connected -> OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.camera_action_disconnect))
        }

        // Nothing is offered mid-write. Cancelling belongs to the write sheet, which owns the
        // operation and can warn about a partly written slot.
        is CameraState.Writing -> Unit
    }
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
private fun CameraSheetConnectedPreview() {
    FujiTheme {
        CameraSheetContent(
            state = CameraState.Connected(CameraModels.identify("X100VI")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — unrecognised body", showBackground = true)
@Composable
private fun CameraSheetUnrecognisedPreview() {
    FujiTheme {
        CameraSheetContent(
            state = CameraState.Connected(CameraModels.identify("X-T99")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — no slots", showBackground = true)
@Composable
private fun CameraSheetNoSlotsPreview() {
    FujiTheme {
        CameraSheetContent(
            state = CameraState.Connected(CameraModels.identify("X-T2")),
            isCameraAttached = true,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — error", showBackground = true)
@Composable
private fun CameraSheetErrorPreview() {
    FujiTheme {
        CameraSheetContent(
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
private fun CameraSheetDisconnectedPreview() {
    FujiTheme {
        CameraSheetContent(
            state = CameraState.Disconnected,
            isCameraAttached = false,
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(name = "Camera sheet — no USB host", showBackground = true)
@Composable
private fun CameraSheetNoUsbPreview() {
    FujiTheme {
        CameraSheetContent(
            state = CameraState.NoUsbHost,
            isCameraAttached = false,
            onConnect = {},
            onDisconnect = {},
        )
    }
}
