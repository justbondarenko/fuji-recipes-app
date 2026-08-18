package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.slotStates

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

    var slotReadings by remember(state) { mutableStateOf<List<SlotNameReading>?>(null) }
    var isLoadingSlots by remember(state) { mutableStateOf(state is CameraState.Connected) }
    var slotsError by remember(state) { mutableStateOf<String?>(null) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    var selectedSlotForDetail by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state, refreshCounter) {
        if (state is CameraState.Connected) {
            isLoadingSlots = true
            slotsError = null
            runCatching { controller.readSlots() }
                .onSuccess { readings ->
                    slotReadings = readings
                    isLoadingSlots = false
                }
                .onFailure { error ->
                    slotsError = error.message ?: "The camera did not answer when asked what its slots hold."
                    isLoadingSlots = false
                }
        } else {
            slotReadings = null
            isLoadingSlots = false
            slotsError = null
            selectedSlotForDetail = null
        }
    }

    val slots = remember(slotReadings, isLoadingSlots) {
        if (slotReadings != null) {
            slotStates(slotReadings!!)
        } else {
            slotStates(emptyList(), loading = isLoadingSlots)
        }
    }

    CameraScreen(
        state = state,
        isCameraAttached = controller.isCameraAttached,
        slots = slots,
        isLoadingSlots = isLoadingSlots,
        slotsError = slotsError,
        onRefreshSlots = { refreshCounter++ },
        onSelectSlot = { slot -> selectedSlotForDetail = slot },
        onConnect = controller::connect,
        onDisconnect = controller::disconnect,
        contentPadding = contentPadding,
    )

    if (selectedSlotForDetail != null && state is CameraState.Connected) {
        SlotDetailBottomSheet(
            slotNumber = selectedSlotForDetail!!,
            cameraIdentity = (state as CameraState.Connected).identity,
            controller = controller,
            onDismiss = { selectedSlotForDetail = null },
        )
    }
}

@Composable
private fun cameraController() =
    (LocalContext.current.applicationContext as FujiRecipesApp).container.cameraController
