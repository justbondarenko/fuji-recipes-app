package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bondarenko.fujirecipes.FujiRecipesApp

/**
 * The chip and its sheet, wired to the controller.
 *
 * The wiring lives here rather than in `AppShell` or `MainActivity` so the shell keeps knowing
 * nothing about the camera, and so `CameraChip` and `CameraSheetContent` stay state-and-lambda
 * composables that previews and UI tests can render (`coding-standards.md`, Compose
 * conventions).
 *
 * There is no ViewModel: `CameraController` already outlives every screen and already exposes
 * a `StateFlow`. Wrapping it in one would add a scope that is shorter than the thing it holds,
 * which is the wrong way round.
 */
@Composable
fun CameraChipHost() {
    val controller = (LocalContext.current.applicationContext as FujiRecipesApp)
        .container
        .cameraController

    val state by controller.state.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }

    CameraChip(state = state, onClick = { sheetOpen = true })

    if (sheetOpen) {
        CameraSheet(
            state = state,
            isCameraAttached = controller.isCameraAttached,
            onConnect = controller::connect,
            onDisconnect = {
                controller.disconnect()
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}
