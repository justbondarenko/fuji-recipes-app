package dev.bondarenko.fujirecipes.ui.cameraphotos

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaType
import dev.bondarenko.fujirecipes.camera.usb.mediaType
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.theme.icons.CameraRoll
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowDownwardAlt
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.LinkedCamera
import dev.bondarenko.fujirecipes.ui.theme.icons.Refresh
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CameraPhotosScreen(
    state: CameraPhotosUiState,
    cameraState: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onFilter: (CameraFileFilter) -> Unit,
    onToggleFile: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onLoadThumbnail: (Int) -> Unit,
    onChooseFolder: () -> Unit,
    onClearNotice: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (cameraState !is CameraState.Connected) {
        CameraPhotosConnectionState(
            state = cameraState,
            isCameraAttached = isCameraAttached,
            onConnect = onConnect,
            modifier = modifier.fillMaxSize().padding(contentPadding),
        )
        return
    }

    val actionsEnabled = !state.isLoading && state.download == null
    Box(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.camera_photos_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.camera_photos_count, state.files.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh, enabled = actionsEnabled) {
                Icon(FujiIcons.Refresh, contentDescription = stringResource(R.string.camera_photos_refresh))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            CameraFileFilter.entries.forEachIndexed { index, filter ->
                ToggleButton(
                    checked = state.filter == filter,
                    onCheckedChange = { onFilter(filter) },
                    enabled = actionsEnabled,
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        CameraFileFilter.entries.lastIndex ->
                            ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (filter) {
                            CameraFileFilter.ALL -> stringResource(R.string.camera_photos_filter_all)
                            CameraFileFilter.JPEG -> stringResource(R.string.camera_photos_jpeg)
                            CameraFileFilter.RAW -> stringResource(R.string.camera_photos_raw)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onSelectAll,
                enabled = actionsEnabled && state.filteredFiles.isNotEmpty() &&
                    state.filteredFiles.any { it.handle !in state.selectedHandles },
            ) { Text(stringResource(R.string.camera_photos_select_all)) }
            TextButton(
                onClick = onClearSelection,
                enabled = actionsEnabled && state.selectedHandles.isNotEmpty(),
            ) { Text(stringResource(R.string.camera_photos_clear)) }
            Text(
                text = stringResource(R.string.camera_photos_selected_count, state.selectedHandles.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = if (state.scanTotal > 0) {
                        stringResource(R.string.camera_photos_scanning_progress, state.scanCurrent, state.scanTotal)
                    } else {
                        stringResource(R.string.camera_photos_scanning)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val notice = state.error ?: state.message
        if (notice != null) {
            Surface(
                color = if (state.error != null) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(notice, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onClearNotice) { Text(stringResource(R.string.action_dismiss)) }
                }
            }
        }

        when {
            state.isLoading && state.files.isEmpty() -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.hasScanned && state.files.isEmpty() -> FujiIconPanel(
                icon = FujiIcons.CameraRoll,
                shape = MaterialShapes.Pill.toShape(),
                title = stringResource(R.string.camera_photos_empty_title),
                body = stringResource(R.string.camera_photos_empty_body),
                actionLabel = stringResource(R.string.camera_photos_refresh),
                onAction = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            )

            state.hasScanned && state.filteredFiles.isEmpty() -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.camera_photos_filter_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(state.filteredFiles, key = { _, file -> file.handle }) { index, file ->
                    LaunchedEffect(file.handle, state.thumbnails[file.handle]) {
                        if (state.thumbnails[file.handle] == null) onLoadThumbnail(file.handle)
                    }
                    CameraFileRow(
                        file = file,
                        thumbnail = state.thumbnails[file.handle],
                        selected = file.handle in state.selectedHandles,
                        enabled = actionsEnabled,
                        index = index,
                        count = state.filteredFiles.size,
                        onToggle = { onToggleFile(file.handle) },
                    )
                }
            }
        }

        state.download?.let { download ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val progress = if (download.totalBytes > 0) {
                    (download.written.toFloat() / download.totalBytes).coerceIn(0f, 1f)
                } else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "camera download progress",
                )
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.camera_photos_downloading,
                        download.currentFile,
                        download.totalFiles,
                        download.filename,
                        formatBytes(download.written),
                        formatBytes(download.totalBytes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        }

        if (actionsEnabled && state.selectedHandles.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onChooseFolder,
                icon = {
                    Icon(
                        imageVector = FujiIcons.ArrowDownwardAlt,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(stringResource(R.string.camera_photos_download_selected, state.selectedHandles.size))
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun CameraFileRow(
    file: CameraMediaObject,
    thumbnail: ByteArray?,
    selected: Boolean,
    enabled: Boolean,
    index: Int,
    count: Int,
    onToggle: () -> Unit,
) {
    ListItem(
        onClick = onToggle,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = file.info.filename,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(FujiIcons.CameraRoll, contentDescription = null)
                }
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = formatCameraCaptureDate(file.info.captureDate)
                        ?: stringResource(R.string.photo_camera_date_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${fileTypeLabel(file)} · ${formatBytes(file.info.compressedSize)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
        },
    ) {
        Text(
            text = file.info.filename,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CameraPhotosConnectionState(
    state: CameraState,
    isCameraAttached: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (state) {
        CameraState.NoUsbHost -> stringResource(R.string.camera_state_no_usb_title)
        CameraState.Connecting -> stringResource(R.string.camera_state_connecting_title)
        is CameraState.Error -> stringResource(R.string.camera_state_error_title)
        is CameraState.Writing -> stringResource(R.string.camera_chip_writing)
        else -> stringResource(R.string.camera_photos_connect_title)
    }
    val body = when (state) {
        CameraState.NoUsbHost -> stringResource(R.string.camera_state_no_usb_body)
        CameraState.Connecting -> stringResource(R.string.camera_state_connecting_body)
        is CameraState.Error -> state.message
        is CameraState.Writing -> stringResource(R.string.camera_photos_busy)
        else -> if (isCameraAttached) stringResource(R.string.camera_photos_attached_body)
            else stringResource(R.string.camera_photos_connect_body)
    }
    FujiIconPanel(
        icon = FujiIcons.LinkedCamera,
        shape = MaterialShapes.Pill.toShape(),
        title = title,
        body = body,
        actionLabel = when (state) {
            CameraState.Disconnected -> stringResource(R.string.camera_action_connect)
            is CameraState.Error -> stringResource(R.string.camera_action_retry)
            else -> null
        },
        onAction = when (state) {
            CameraState.Disconnected, is CameraState.Error -> onConnect
            else -> null
        },
        modifier = modifier.padding(24.dp),
    )
}

@Composable
private fun fileTypeLabel(file: CameraMediaObject): String = when (file.mediaType) {
    CameraMediaType.RAW -> stringResource(R.string.camera_photos_raw)
    else -> stringResource(R.string.camera_photos_jpeg)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
fun CameraPhotosRouteContent(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: CameraPhotosViewModel = viewModel(factory = CameraPhotosViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraState by container.cameraController.state.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.downloadSelected(uri.toString())
        }
    }

    LaunchedEffect(cameraState is CameraState.Connected) {
        if (cameraState is CameraState.Connected) viewModel.refresh()
    }

    CameraPhotosScreen(
        state = state,
        cameraState = cameraState,
        isCameraAttached = container.cameraController.isCameraAttached,
        onConnect = container.cameraController::connect,
        onRefresh = viewModel::refresh,
        onFilter = viewModel::setFilter,
        onToggleFile = viewModel::toggleFile,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onLoadThumbnail = viewModel::loadThumbnail,
        onChooseFolder = { folderPicker.launch(null) },
        onClearNotice = viewModel::clearNotice,
        contentPadding = contentPadding,
    )
}
