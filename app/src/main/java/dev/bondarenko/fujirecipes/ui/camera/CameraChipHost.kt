package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bondarenko.fujirecipes.FujiRecipesApp

/**
 * The navigation bar item and the screen it opens, wired to the controller.
 *
 * The wiring lives here rather than in `AppShell` or `MainActivity` so the shell keeps knowing
 * nothing about the camera, and so `CameraToolbarItem` and `CameraStatusContent` stay
 * state-and-lambda composables that previews and UI tests can render
 * (`coding-standards.md`, Compose conventions).
 *
 * There is no ViewModel: `CameraController` already outlives every screen and already exposes
 * a `StateFlow`. Wrapping it in one would add a scope that is shorter than the thing it holds,
 * which is the wrong way round.
 */
@Composable
fun RowScope.CameraToolbarItemHost(selected: Boolean, onClick: () -> Unit) {
    val state by cameraController().state.collectAsStateWithLifecycle()

    CameraToolbarItem(state = state, selected = selected, onClick = onClick)
}

@Composable
fun CameraRouteContent(contentPadding: PaddingValues) {
    val controller = cameraController()
    val state by controller.state.collectAsStateWithLifecycle()

    CameraScreen(
        state = state,
        isCameraAttached = controller.isCameraAttached,
        onConnect = controller::connect,
        // Stays put: this is a destination, not a sheet, and the disconnected state is worth
        // showing — it carries the Connect action.
        onDisconnect = controller::disconnect,
        contentPadding = contentPadding,
    )
}

@Composable
private fun cameraController() =
    (LocalContext.current.applicationContext as FujiRecipesApp).container.cameraController
