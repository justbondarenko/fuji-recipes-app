package dev.bondarenko.fujirecipes.ui.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.data.lab.RawLabPreview
import dev.bondarenko.fujirecipes.ui.common.FujiCenteredLoading
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.camera.CameraSheetButtonHost
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.Cable
import dev.bondarenko.fujirecipes.ui.theme.icons.Download
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.ImagesMode
import dev.bondarenko.fujirecipes.ui.theme.icons.Warning
import kotlinx.serialization.json.JsonElement

/**
 * The RAW lab — FEAT-016.
 *
 * A picture on top and its parameters underneath, because the whole point of the page is
 * watching one change the other. Stateless: every decision arrives in [state] and every
 * intent leaves through a callback, so the interesting logic stays in `RawLabState` where a
 * test can reach it.
 *
 * One header row carries the camera button too, so the shell draws no row of its own here.
 * While a RAF is loaded the shell hides its navigation bar as well: the lab takes the screen,
 * and back (the header's arrow or the system gesture) is how the session ends.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawLabScreen(
    state: RawLabUiState,
    onChooseRaf: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    onApplyRecipe: () -> Unit,
    onSettingChange: (String, JsonElement?) -> Unit,
    onRenderPreview: () -> Unit,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onShareProfile: (String, ByteArray) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val lab = state.lab

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LabHeader(
            // The filename *is* the title once there is one: the page is about that file, and
            // a separate chip for it cost a row of a screen that needs the height.
            title = lab.rafName.ifEmpty { stringResource(R.string.lab_title) },
            subtitle = lab.appliedRecipeName?.let { name ->
                if (lab.isDirty) stringResource(R.string.lab_recipe_edited, name) else name
            },
            showActions = lab.hasRaf,
            onBack = onBack,
            onApplyRecipe = onApplyRecipe,
        )

        val importing = lab.importing
        val calibration = lab.calibration
        val bodyModifier = Modifier.fillMaxSize()

        when {
            importing != null -> FujiCenteredLoading(
                label = stringResource(R.string.raw_importing),
                progress = importing.total?.takeIf { it > 0 }?.let { total ->
                    { importing.written.toFloat() / total.toFloat() }
                },
                modifier = bodyModifier,
            )

            calibration != null -> FujiIconPanel(
                icon = FujiIcons.Warning,
                shape = MaterialShapes.Pill.toShape(),
                title = stringResource(R.string.raw_calibration_title, calibration.cameraModel),
                body = stringResource(R.string.raw_calibration_body, calibration.profile.size),
                actionLabel = stringResource(R.string.raw_action_share_profile),
                onAction = {
                    onShareProfile(
                        "${calibration.cameraModel.safeFilename()}-d185.bin",
                        calibration.profile,
                    )
                },
                modifier = bodyModifier,
                extra = {
                    TextButton(onClick = onChooseAnotherRaf) {
                        Text(stringResource(R.string.raw_action_choose_another))
                    }
                },
            )

            !lab.hasRaf -> EmptyLab(onChooseRaf = onChooseRaf, modifier = bodyModifier)

            else -> LoadedLab(
                state = state,
                onSettingChange = onSettingChange,
                onRenderPreview = onRenderPreview,
                onConnect = onConnect,
                onSave = onSave,
                modifier = bodyModifier,
            )
        }
    }
}

/** The file being developed, the recipe behind it, the way out, and what acts on the session. */
@Composable
private fun LabHeader(
    title: String,
    subtitle: String?,
    showActions: Boolean,
    onBack: () -> Unit,
    onApplyRecipe: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (showActions) 4.dp else 20.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showActions) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = FujiIcons.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showActions) {
            // Round and tonal, matching the camera button beside it.
            FilledTonalIconButton(onClick = onApplyRecipe, shape = CircleShape) {
                Icon(
                    imageVector = FujiIcons.BookmarkStacks,
                    contentDescription = stringResource(R.string.lab_action_apply_recipe),
                )
            }
        }
        CameraSheetButtonHost()
    }
}

/**
 * Before anything else, a file.
 *
 * One call to action and no second one: a recipe cannot be applied to nothing, and offering
 * that choice here only asks a question whose answer does not matter yet. The recipe button
 * appears with the picture, once there is something for it to act on.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyLab(
    onChooseRaf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FujiIconPanel(
        icon = FujiIcons.ImagesMode,
        shape = MaterialShapes.Pill.toShape(),
        title = stringResource(R.string.lab_empty_title),
        body = stringResource(R.string.lab_empty_body),
        actionLabel = stringResource(R.string.raw_action_choose),
        onAction = onChooseRaf,
        modifier = modifier,
    )
}

@Composable
private fun LoadedLab(
    state: RawLabUiState,
    onSettingChange: (String, JsonElement?) -> Unit,
    onRenderPreview: () -> Unit,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lab = state.lab
    val fields = lab.renderedFields(state.supportedFieldIds)
    val readiness = state.camera.readiness()

    // Wider than tall — landscape, or a wide window — puts the picture beside its parameters
    // instead of above them: 60% of the width for the photo, 40% for the controls.
    BoxWithConstraints(modifier = modifier) {
        val preview: @Composable (Modifier) -> Unit = { paneModifier ->
            PreviewPane(
                preview = lab.preview,
                isStale = lab.isPreviewStale,
                stage = lab.rendering,
                renderingForSave = state.isRenderingForSave,
                readiness = readiness,
                onConnect = onConnect,
                modifier = paneModifier,
            )
        }
        val parameters: @Composable (Modifier) -> Unit = { panelModifier ->
            RawLabParameterPanel(
                settings = lab.settings,
                fields = fields,
                onSettingChange = onSettingChange,
                modifier = panelModifier,
                // Room under the last field for the save button to float over nothing.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            )
        }

        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                preview(
                    Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
                VerticalDivider()
                Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                    LabActions(
                        state = state,
                        readiness = readiness,
                        onRenderPreview = onRenderPreview,
                    )
                    parameters(Modifier.weight(1f))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // 95% of the width, not a 16dp gutter each side: the picture is the point of the
                // page, and on a phone those two gutters are the difference between judging a
                // film simulation and squinting at it.
                // The bottom gap used to come from the auto-render row; with that row usually
                // hidden, the picture would otherwise sit right on the divider.
                preview(
                    Modifier
                        .fillMaxWidth(0.95f)
                        .weight(0.45f)
                        .padding(bottom = 12.dp),
                )

                LabActions(
                    state = state,
                    readiness = readiness,
                    onRenderPreview = onRenderPreview,
                )

                HorizontalDivider()

                parameters(Modifier.weight(0.55f))
            }
        }

        // Save does one thing: write the full-resolution JPEG, rendering it first if needed.
        FloatingActionButton(
            onClick = onSave,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = FujiIcons.Download,
                contentDescription = stringResource(R.string.lab_action_save_jpeg),
            )
        }
    }
}

/**
 * The picture, or — while there isn't one — what is standing between you and it.
 *
 * No camera means no render, so the space the picture will occupy is where the camera's state
 * belongs; a warning wedged between the filename and the controls both crowded them and left
 * this area empty. Once a picture exists it keeps the space even if the cable goes: it is
 * still the most recent true answer, badged rather than thrown away.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewPane(
    preview: RawLabPreview?,
    isStale: Boolean,
    stage: RawDevelopmentStage?,
    renderingForSave: Boolean,
    readiness: CameraReadiness,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            preview != null -> ZoomablePreview(
                preview = preview,
                modifier = Modifier.fillMaxSize(),
            )

            stage != null -> Unit

            !readiness.canRender -> CameraNotReady(readiness = readiness, onConnect = onConnect)

            else -> Text(
                text = stringResource(R.string.lab_no_preview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (stage != null && renderingForSave) {
            // A full-resolution render for Save is long and ends in the file picker, so it gets
            // the whole pane rather than the re-render's corner indicator.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                ContainedLoadingIndicator()
                Text(
                    text = stringResource(R.string.lab_rendering_full),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        } else if (stage != null && preview != null) {
            // A re-render leaves the picture alone: a small indicator in its corner rather than
            // a veil over the thing you are judging.
            ContainedLoadingIndicator(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp),
            )
        } else if (stage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stageLabel(stage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                stage.progress()?.let { fraction ->
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                }
            }
        } else if (preview != null && isStale) {
            LabBadge(stringResource(R.string.lab_preview_stale), Modifier.align(Alignment.TopEnd))
        } else if (preview != null && !preview.isFullResolution) {
            LabBadge(stringResource(R.string.lab_preview_quality), Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * Pinch to zoom, drag to pan, double-tap to go back.
 *
 * Grain and sharpness are the reasons to render at all and neither survives being fitted into
 * a phone-sized pane, so the picture has to open up in place. It zooms inside its own bounds
 * rather than into a separate viewer: the parameters stay under your thumb, which is the point
 * of putting them on the same screen.
 *
 * Zoom resets when a new render lands — that picture is a different answer, and inheriting the
 * last one's corner would hide what changed.
 */
@Composable
private fun ZoomablePreview(
    preview: RawLabPreview,
    modifier: Modifier = Modifier,
) {
    var scale by remember(preview.file.path) { mutableFloatStateOf(1f) }
    var offset by remember(preview.file.path) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    /** Keeps the picture from being dragged off its own pane. */
    fun clamp(candidate: Offset, at: Float): Offset {
        val maxX = (size.width * (at - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (size.height * (at - 1f) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(MinZoom, MaxZoom)
        scale = next
        offset = if (next <= 1f) Offset.Zero else clamp(offset + panChange, next)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clipToBounds()
            .onSizeChanged { size = it }
            .transformable(transform)
            .pointerInput(preview.file.path) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DoubleTapZoom
                            // Zoom towards the point that was tapped rather than the middle.
                            val centre = Offset(size.width / 2f, size.height / 2f)
                            offset = clamp((centre - tap) * (DoubleTapZoom - 1f), DoubleTapZoom)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = preview.file,
            contentDescription = stringResource(R.string.raw_result_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

/**
 * What the camera is doing, in the space the picture will take.
 *
 * Connect is offered but not demanded: plugging the cable in raises the attach intent and the
 * app opens the session itself (`PRD.md` §8.2), so the button is there for the case where the
 * app was already running and the body was not noticed — which the copy says plainly rather
 * than instructing everyone to press it.
 */
@Composable
private fun CameraNotReady(
    readiness: CameraReadiness,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = FujiIcons.Cable,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = readiness.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (readiness.offersConnect) {
            FilledTonalButton(onClick = onConnect) {
                Text(stringResource(R.string.camera_action_connect))
            }
        }
    }
}

@Composable
private fun LabBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Only there when something went wrong: renders happen by themselves after every change, so
 * the retry and the error are all this row has to say.
 */
@Composable
private fun LabActions(
    state: RawLabUiState,
    readiness: CameraReadiness,
    onRenderPreview: () -> Unit,
) {
    val lab = state.lab
    val showRetry = !lab.autoPreview || lab.error != null

    AnimatedVisibility(visible = showRetry) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lab.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onRenderPreview, enabled = lab.canRender && readiness.canRender) {
                Text(stringResource(R.string.lab_action_update_preview))
            }
        }
    }
}

/**
 * Whether the camera can be asked to render, what to say when it cannot, and whether pressing
 * Connect could help — it cannot while a session is already opening.
 */
private data class CameraReadiness(
    val canRender: Boolean,
    val message: String,
    val offersConnect: Boolean = false,
)

@Composable
private fun CameraState.readiness(): CameraReadiness = when (this) {
    is CameraState.Connected ->
        if (usbMode == UsbMode.RAW_CONVERSION || usbMode == UsbMode.UNREPORTED) {
            CameraReadiness(
                canRender = true,
                message = stringResource(R.string.raw_camera_ready, identity.model),
            )
        } else {
            CameraReadiness(
                canRender = false,
                message = stringResource(R.string.raw_camera_wrong_mode_body),
            )
        }

    CameraState.Connecting -> CameraReadiness(
        canRender = false,
        message = stringResource(R.string.raw_camera_connecting_body),
    )

    CameraState.NoUsbHost -> CameraReadiness(
        canRender = false,
        message = stringResource(R.string.camera_state_no_usb_body),
    )

    // A slot write has the connection. Connect would not help; waiting will.
    is CameraState.Writing -> CameraReadiness(
        canRender = false,
        message = stringResource(R.string.lab_camera_busy),
    )

    is CameraState.Error -> CameraReadiness(
        canRender = false,
        message = message,
        offersConnect = true,
    )

    else -> CameraReadiness(
        canRender = false,
        message = stringResource(R.string.lab_camera_waiting),
        offersConnect = true,
    )
}

@Composable
private fun stageLabel(stage: RawDevelopmentStage): String = when (stage) {
    RawDevelopmentStage.Preparing -> stringResource(R.string.raw_stage_preparing)
    is RawDevelopmentStage.Uploading -> stringResource(R.string.raw_stage_uploading)
    RawDevelopmentStage.ApplyingRecipe -> stringResource(R.string.raw_stage_applying)
    RawDevelopmentStage.Processing -> stringResource(R.string.raw_stage_processing)
    is RawDevelopmentStage.Downloading -> stringResource(R.string.raw_stage_downloading)
    RawDevelopmentStage.CleaningUp -> stringResource(R.string.raw_stage_cleaning)
}

private fun RawDevelopmentStage.progress(): Float? = when (this) {
    is RawDevelopmentStage.Uploading ->
        if (total > 0) (written.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null

    is RawDevelopmentStage.Downloading ->
        if (total > 0) (written.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null

    else -> null
}

/** Below 1 the picture would sit inside its own pane; above 6 it is one grain cluster. */
private const val MinZoom = 1f
private const val MaxZoom = 6f
private const val DoubleTapZoom = 2.5f

internal fun String.safeFilename(): String =
    replace(Regex("[^A-Za-z0-9_-]"), "-").trim('-').ifEmpty { "camera" }
