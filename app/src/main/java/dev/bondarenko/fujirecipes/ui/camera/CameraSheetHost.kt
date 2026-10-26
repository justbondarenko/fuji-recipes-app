package dev.bondarenko.fujirecipes.ui.camera

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.bondarenko.fujirecipes.camera.CameraTransport
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bondarenko.fujirecipes.BuildConfig
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.renderCameraReport
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.share.ShareFile
import dev.bondarenko.fujirecipes.ui.common.FujiModalSideSheet
import dev.bondarenko.fujirecipes.ui.theme.icons.Close
import dev.bondarenko.fujirecipes.ui.theme.icons.Bluetooth
import dev.bondarenko.fujirecipes.ui.theme.icons.Usb
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The button, the side sheet it opens, and the screen inside it, wired to the controller.
 *
 * The wiring lives here rather than in `AppShell` or `MainActivity` so the shell keeps knowing
 * nothing about the camera, and so `CameraSheetButton` and `CameraStatusContent` stay
 * state-and-lambda composables that previews and UI tests can render
 * (`coding-standards.md`, Compose conventions).
 *
 * There is no ViewModel: `CameraController` already outlives every screen and already exposes
 * a `StateFlow`. Wrapping it in one would add a scope that is shorter than the thing it holds,
 * which is the wrong way round.
 */
@Composable
fun CameraSheetButtonHost(modifier: Modifier = Modifier) {
    val state by cameraController().state.collectAsStateWithLifecycle()
    val open = LocalCameraSheetOpener.current

    CameraSheetButton(state = state, onClick = open, modifier = modifier)
}

/**
 * The camera side sheet: the screen below, behind a title row with a way out.
 *
 * A sheet rather than a destination in the navigation bar, so the bar's places stay for
 * features and the camera can be reached from any of them.
 */
@Composable
fun CameraSheetHost(visible: Boolean, onDismiss: () -> Unit) {
    val controller = cameraController()
    val state by controller.state.collectAsStateWithLifecycle()
    val transport by controller.transport.collectAsStateWithLifecycle()
    FujiModalSideSheet(visible = visible, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxSize()) {
            CameraSheetHeader(state, transport, controller::selectTransport, onDismiss)

            // Rendered only while the sheet is up: the slot read talks to the camera, and it
            // has no business running behind a closed sheet.
            if (visible) {
                CameraSheetContent(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CameraSheetHeader(
    state: CameraState,
    transport: CameraTransport,
    onSelectTransport: (CameraTransport) -> Unit,
    onDismiss: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.camera_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(FujiIcons.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                CameraTransport.entries.forEachIndexed { index, mode ->
                    ToggleButton(
                        checked = transport == mode,
                        onCheckedChange = { onSelectTransport(mode) },
                        enabled = state != CameraState.Connecting && state !is CameraState.Writing,
                        shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                            else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (mode == CameraTransport.USB) FujiIcons.Usb else FujiIcons.Bluetooth,
                            contentDescription = stringResource(if (mode == CameraTransport.USB) R.string.camera_transport_usb else R.string.camera_transport_bluetooth),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraSheetContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val controller = cameraController()
    val state by controller.state.collectAsStateWithLifecycle()
    val transport by controller.transport.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (controller.transport.value == CameraTransport.BLUETOOTH) controller.connect()
    }

    var slotReadings by remember(state) { mutableStateOf<List<SlotNameReading>?>(null) }
    var isLoadingSlots by remember(state) { mutableStateOf(state is CameraState.Connected) }
    var slotsError by remember(state) { mutableStateOf<String?>(null) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    var selectedSlotForDetail by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state, refreshCounter) {
        val connected = state as? CameraState.Connected
        if (connected != null && !connected.usbMode.isKnownWrongMode) {
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

    // ─── The diagnostic report ──────────────────────────────────────────────
    //
    // Held here rather than in a ViewModel for the same reason the rest of this screen is:
    // `CameraController` already outlives every screen. Resetting on `state` releases the
    // button the moment the cable is pulled.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isReportRunning by remember(state) { mutableStateOf(false) }

    CameraScreen(
        modifier = modifier,
        state = state,
        isCameraAttached = controller.isCameraAttached,
        transport = transport,
        slots = slots,
        isLoadingSlots = isLoadingSlots,
        slotsError = slotsError,
        onRefreshSlots = { refreshCounter++ },
        onSelectSlot = { slot -> selectedSlotForDetail = slot },
        onConnect = {
            if (transport == CameraTransport.BLUETOOTH && Build.VERSION.SDK_INT >= 31 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else controller.connect()
        },
        onDisconnect = controller::disconnect,
        contentPadding = contentPadding,
        isReportRunning = isReportRunning,
        onShareReport = {
            scope.launch {
                isReportRunning = true
                // Nothing on screen reports the outcome any more, so a camera that stops
                // answering partway through must not take the app down with it.
                runCatching {
                    val report = controller.readReport(
                        appVersion = BuildConfig.VERSION_NAME,
                        capturedAt = timestamp(),
                    )

                    ShareFile.share(
                        context = context,
                        filename = "fuji-camera-report-${fileStamp()}.txt",
                        text = renderCameraReport(report),
                    )
                }
                isReportRunning = false
            }
        },
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

/** UTC, because a report read by someone else should not be in the reader's guess at a zone. */
private fun timestamp(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

/** Local time, because this one ends up in a filename the owner sorts by. */
private fun fileStamp(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
