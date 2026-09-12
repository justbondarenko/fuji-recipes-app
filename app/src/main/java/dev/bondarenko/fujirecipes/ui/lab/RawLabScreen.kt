package dev.bondarenko.fujirecipes.ui.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.camera.usb.RawRenderQuality
import dev.bondarenko.fujirecipes.data.lab.RawLabPreview
import dev.bondarenko.fujirecipes.ui.common.FujiCenteredLoading
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.Cable
import dev.bondarenko.fujirecipes.ui.theme.icons.Delete
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
 * No `TopAppBar` of its own. This is a bottom-bar destination, and the shell already draws a
 * row above it for the camera button; a second bar underneath that one put this page's title
 * and the camera control on the same line, crowded against each other. A plain header row
 * inside the content is what the other top-level pages do.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawLabScreen(
    state: RawLabUiState,
    onChooseRaf: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    onApplyRecipe: () -> Unit,
    onSettingChange: (String, JsonElement?) -> Unit,
    onRender: (RawRenderQuality) -> Unit,
    onAutoPreviewChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
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
            showActions = lab.hasRaf,
            onApplyRecipe = onApplyRecipe,
            onDiscard = onDiscard,
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
                onRender = onRender,
                onAutoPreviewChange = onAutoPreviewChange,
                onConnect = onConnect,
                onSave = onSave,
                onChooseAnotherRaf = onChooseAnotherRaf,
                modifier = bodyModifier,
            )
        }
    }
}

/** The page's name, and the two things that act on the whole session. */
@Composable
private fun LabHeader(
    showActions: Boolean,
    onApplyRecipe: () -> Unit,
    onDiscard: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.lab_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        if (showActions) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Tonal rather than filled: the picture is what this page is for, and a
                // primary-coloured button beside the title outshouted it.
                FilledTonalButton(
                    onClick = onApplyRecipe,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = FujiIcons.BookmarkStacks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lab_action_apply_recipe))
                }
                IconButton(onClick = onDiscard) {
                    Icon(
                        imageVector = FujiIcons.Delete,
                        contentDescription = stringResource(R.string.lab_action_discard),
                    )
                }
            }
        }
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
    onRender: (RawRenderQuality) -> Unit,
    onAutoPreviewChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lab = state.lab
    val fields = lab.renderedFields(state.supportedFieldIds)
    val readiness = state.camera.readiness()

    Column(modifier = modifier) {
        PreviewPane(
            preview = lab.preview,
            isStale = lab.isPreviewStale,
            stage = lab.rendering,
            readiness = readiness,
            onConnect = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .padding(horizontal = 16.dp),
        )

        LabActions(
            state = state,
            readiness = readiness,
            onRender = onRender,
            onAutoPreviewChange = onAutoPreviewChange,
            onSave = onSave,
            onChooseAnotherRaf = onChooseAnotherRaf,
        )

        HorizontalDivider()

        RawLabParameterPanel(
            settings = lab.settings,
            fields = fields,
            onSettingChange = onSettingChange,
            modifier = Modifier.weight(0.55f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        )
    }
}

/**
 * The picture, or — while there isn't one — what is standing between you and it.
 *
 * No camera means no render, so the space the picture will occupy is where the camera's state
 * belongs; a warning wedged between the filename and the controls both crowded them and left
 * this area empty. Once a picture exists it keeps the space even if the cable goes: it is
 * still the most recent true answer, dimmed and badged rather than thrown away.
 */
@Composable
private fun PreviewPane(
    preview: RawLabPreview?,
    isStale: Boolean,
    stage: RawDevelopmentStage?,
    readiness: CameraReadiness,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            preview != null -> AsyncImage(
                model = preview.file,
                contentDescription = stringResource(R.string.raw_result_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .alpha(if (isStale || stage != null) 0.4f else 1f),
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

        if (stage != null) {
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

@Composable
private fun LabActions(
    state: RawLabUiState,
    readiness: CameraReadiness,
    onRender: (RawRenderQuality) -> Unit,
    onAutoPreviewChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
) {
    val lab = state.lab
    val canRender = lab.canRender && readiness.canRender

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = onChooseAnotherRaf,
                label = {
                    Text(
                        text = lab.rafName.ifEmpty { stringResource(R.string.lab_no_raf) },
                        maxLines = 1,
                    )
                },
            )
            // Only when a recipe is behind these settings. "Starting from the defaults" beside
            // the filename said nothing the absence of a recipe name did not already say.
            lab.appliedRecipeName?.let { name ->
                Text(
                    text = if (lab.isDirty) {
                        stringResource(R.string.lab_recipe_edited, name)
                    } else {
                        name
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(visible = lab.error != null) {
            Text(
                text = lab.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onRender(RawRenderQuality.PREVIEW) }, enabled = canRender) {
                Text(stringResource(R.string.lab_action_update_preview))
            }
            OutlinedButton(onClick = { onRender(RawRenderQuality.FULL) }, enabled = canRender) {
                Text(stringResource(R.string.lab_action_render_full))
            }
            if (lab.canSaveJpeg) {
                TextButton(
                    onClick = onSave,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text(stringResource(R.string.raw_action_save_jpeg))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = lab.autoPreview,
                onCheckedChange = onAutoPreviewChange,
                enabled = readiness.canRender,
            )
            Text(
                text = stringResource(R.string.lab_auto_preview),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

internal fun String.safeFilename(): String =
    replace(Regex("[^A-Za-z0-9_-]"), "-").trim('-').ifEmpty { "camera" }
