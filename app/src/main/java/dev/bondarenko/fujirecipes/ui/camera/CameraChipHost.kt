package dev.bondarenko.fujirecipes.ui.camera

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bondarenko.fujirecipes.BuildConfig
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.backupFilename
import dev.bondarenko.fujirecipes.camera.plan.backupMatch
import dev.bondarenko.fujirecipes.camera.plan.backupSizeProblem
import dev.bondarenko.fujirecipes.camera.plan.modelFromBackupFilename
import dev.bondarenko.fujirecipes.camera.plan.renderCameraReport
import dev.bondarenko.fujirecipes.camera.plan.slotStates
import dev.bondarenko.fujirecipes.core.share.ShareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // The chosen file's bytes live outside `CameraToolsState` on purpose: the state is compared
    // on every recomposition and a data class holding a 40 kB ByteArray would compare it by
    // identity, which is both wasteful and a trap for anyone who later relies on equality.
    var pendingBytes by remember(state) { mutableStateOf<ByteArray?>(null) }

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

    // ─── Camera tools ───────────────────────────────────────────────────────
    //
    // Held here rather than in a ViewModel for the same reason the rest of this screen is:
    // `CameraController` already outlives every screen. Resetting on `state` clears a stale
    // "backed up 42 kB" the moment the cable is pulled, which would otherwise sit there
    // describing a camera that is gone.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tools by remember(state) { mutableStateOf(CameraToolsState()) }

    val connectedModel = (state as? CameraState.Connected)?.identity?.model.orEmpty()

    val reportProgressFormat = stringResource(R.string.camera_tools_report_progress)
    val reportDoneFormat = stringResource(R.string.camera_tools_report_done)
    val backupProgress = stringResource(R.string.camera_tools_backup_progress)
    val backupDoneFormat = stringResource(R.string.camera_tools_backup_done)
    val restoreProgress = stringResource(R.string.camera_tools_restore_progress)
    val restoreDone = stringResource(R.string.camera_tools_restore_done)
    val unreadable = stringResource(R.string.camera_tools_file_unreadable)

    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            val chosen = withContext(Dispatchers.IO) { readChosenFile(context, uri) }

            tools = when {
                chosen == null -> tools.copy(message = unreadable, messageIsError = true)

                else -> {
                    val problem = backupSizeProblem(chosen.bytes.size)
                    if (problem != null) {
                        tools.copy(message = problem, messageIsError = true)
                    } else {
                        pendingBytes = chosen.bytes
                        tools.copy(
                            message = null,
                            pendingRestore = PendingRestore(
                                filename = chosen.name,
                                sizeBytes = chosen.bytes.size,
                                match = backupMatch(chosen.name, connectedModel),
                                claimedModel = modelFromBackupFilename(chosen.name),
                                connectedModel = connectedModel,
                            ),
                        )
                    }
                }
            }
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
        tools = tools,
        onShareReport = {
            scope.launch {
                tools = CameraToolsState(running = CameraTask.REPORT)
                tools = runCameraTask {
                    val report = controller.readReport(
                        appVersion = BuildConfig.VERSION_NAME,
                        capturedAt = timestamp(),
                        // The report runs on Dispatchers.IO, so its progress arrives on a
                        // background thread. Hopped back onto the composition's scope rather
                        // than written from there: snapshot state tolerates a cross-thread
                        // write, but the value only becomes visible when that thread's
                        // snapshot is applied, which is not a thing to make a progress
                        // indicator depend on.
                        onProgress = { done, total ->
                            scope.launch {
                                tools = tools.copy(
                                    progress = String.format(reportProgressFormat, done, total),
                                )
                            }
                        },
                    )

                    ShareFile.share(
                        context = context,
                        filename = "fuji-camera-report-${fileStamp()}.txt",
                        text = renderCameraReport(report),
                    )

                    String.format(reportDoneFormat, report.properties.size)
                }
            }
        },
        onBackUp = {
            scope.launch {
                tools = CameraToolsState(running = CameraTask.BACK_UP, progress = backupProgress)
                tools = runCameraTask {
                    val bytes = controller.downloadSettingsBackup()

                    ShareFile.share(
                        context = context,
                        filename = backupFilename(connectedModel, fileStamp()),
                        content = bytes,
                    )

                    String.format(backupDoneFormat, formatSize(bytes.size))
                }
            }
        },
        onChooseRestoreFile = {
            // Every MIME, because a `.bin` is served as whatever the provider feels like and a
            // file the user cannot choose is a feature that does not work.
            restorePicker.launch(arrayOf("*/*"))
        },
    )

    tools.pendingRestore?.let { pending ->
        RestoreConfirmDialog(
            pending = pending,
            onDismiss = {
                pendingBytes = null
                tools = tools.copy(pendingRestore = null)
            },
            onConfirm = {
                val bytes = pendingBytes
                pendingBytes = null
                tools = CameraToolsState(
                    running = CameraTask.RESTORE,
                    progress = restoreProgress,
                )

                scope.launch {
                    tools = runCameraTask {
                        controller.restoreSettingsBackup(requireNotNull(bytes))
                        restoreDone
                    }
                }
            },
        )
    }

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

// ─── Camera-tool plumbing ───────────────────────────────────────────────────

/** A file the user picked, with the name the provider shows for it. */
private data class ChosenBackup(val name: String, val bytes: ByteArray)

/**
 * Runs one camera tool and turns whatever happened into the next state.
 *
 * Every one of these ends the same way — cleared progress, one sentence, and a flag saying
 * whether that sentence is bad news — so the shape lives here once rather than in each of the
 * three call sites. A [BackupError] and a dropped cable both arrive as exceptions carrying a
 * message written for a person (P5), so the message is used as-is; anything with no message at
 * all falls back to naming its type, which is still better than an empty box.
 */
private suspend fun runCameraTask(block: suspend () -> String): CameraToolsState = try {
    CameraToolsState(message = block(), messageIsError = false)
} catch (error: Exception) {
    CameraToolsState(
        message = error.message?.takeIf { it.isNotBlank() }
            ?: "The camera operation failed (${error::class.simpleName}).",
        messageIsError = true,
    )
}

/** Reads a picked document, or null if it could not be opened. */
private fun readChosenFile(context: Context, uri: Uri): ChosenBackup? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return@runCatching null

    ChosenBackup(name = displayName(context, uri), bytes = bytes)
}.getOrNull()

/** The name the provider shows for a document, falling back to the last path segment. */
private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')

/** UTC, because a report read by someone else should not be in the reader's guess at a zone. */
private fun timestamp(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

/** Local time, because this one ends up in a filename the owner sorts by. */
private fun fileStamp(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))

private fun formatSize(bytes: Int): String =
    if (bytes < 1024) "$bytes bytes" else "${bytes / 1024} kB"
