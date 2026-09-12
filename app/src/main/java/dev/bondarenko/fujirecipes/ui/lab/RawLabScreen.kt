package dev.bondarenko.fujirecipes.ui.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawLabScreen(
    state: RawLabUiState,
    onChooseRaf: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    onApplyRecipe: () -> Unit,
    onStartFromDefaults: () -> Unit,
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

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.lab_title)) },
            actions = {
                if (lab.hasRaf) {
                    IconButton(onClick = onApplyRecipe) {
                        Icon(
                            imageVector = FujiIcons.BookmarkStacks,
                            contentDescription = stringResource(R.string.lab_action_apply_recipe),
                        )
                    }
                    IconButton(onClick = onDiscard) {
                        Icon(
                            imageVector = FujiIcons.Delete,
                            contentDescription = stringResource(R.string.lab_action_discard),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        val bodyModifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding())

        val importing = lab.importing
        val calibration = lab.calibration

        when {
            importing != null -> FujiCenteredLoading(
                label = stringResource(R.string.raw_importing),
                progress = importing.total?.takeIf { it > 0 }?.let { total ->
                    { importing.written.toFloat() / total.toFloat() }
                },
                modifier = bodyModifier,
            )

            calibration != null -> {
                FujiIconPanel(
                    icon = FujiIcons.Warning,
                    shape = MaterialShapes.Pill.toShape(),
                    title = stringResource(
                        R.string.raw_calibration_title,
                        calibration.cameraModel,
                    ),
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
            }

            !lab.hasRaf -> EmptyLab(
                onChooseRaf = onChooseRaf,
                onApplyRecipe = onApplyRecipe,
                onStartFromDefaults = onStartFromDefaults,
                appliedRecipeName = lab.appliedRecipeName,
                modifier = bodyModifier,
            )

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyLab(
    onChooseRaf: () -> Unit,
    onApplyRecipe: () -> Unit,
    onStartFromDefaults: () -> Unit,
    appliedRecipeName: String?,
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
        extra = {
            Text(
                text = appliedRecipeName
                    ?.let { stringResource(R.string.lab_starting_from_recipe, it) }
                    ?: stringResource(R.string.lab_starting_from_defaults),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onApplyRecipe) {
                    Text(stringResource(R.string.lab_action_apply_recipe))
                }
                TextButton(onClick = onStartFromDefaults) {
                    Text(stringResource(R.string.lab_action_from_defaults))
                }
            }
        },
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
    val fields = lab.fields(state.supportedFieldIds)

    Column(modifier = modifier) {
        PreviewPane(
            preview = lab.preview,
            isStale = lab.isPreviewStale,
            stage = lab.rendering,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .padding(horizontal = 16.dp),
        )

        LabActions(
            state = state,
            onRender = onRender,
            onAutoPreviewChange = onAutoPreviewChange,
            onConnect = onConnect,
            onSave = onSave,
            onChooseAnotherRaf = onChooseAnotherRaf,
        )

        HorizontalDivider()

        RawLabParameterPanel(
            settings = lab.settings,
            renderedFields = fields.rendered,
            storedOnlyFields = fields.storedOnly,
            onSettingChange = onSettingChange,
            modifier = Modifier.weight(0.55f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        )
    }
}

/**
 * The picture, and what is true about it right now.
 *
 * A stale preview is dimmed and badged rather than removed: it is still the most recent thing
 * the camera actually said, and blanking the pane on every edit would make the page flicker
 * between an answer and nothing.
 */
@Composable
private fun PreviewPane(
    preview: RawLabPreview?,
    isStale: Boolean,
    stage: RawDevelopmentStage?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (preview != null) {
            AsyncImage(
                model = preview.file,
                contentDescription = stringResource(R.string.raw_result_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .alpha(if (isStale || stage != null) 0.4f else 1f),
            )
        } else {
            Text(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LabActions(
    state: RawLabUiState,
    onRender: (RawRenderQuality) -> Unit,
    onAutoPreviewChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
) {
    val lab = state.lab
    val readiness = state.camera.readiness()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
            Text(
                text = lab.appliedRecipeName
                    ?.let {
                        if (lab.isDirty) {
                            stringResource(R.string.lab_recipe_edited, it)
                        } else {
                            it
                        }
                    }
                    ?: stringResource(R.string.lab_starting_from_defaults),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = readiness.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (readiness.canRender) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
        )

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
            if (!readiness.canRender) {
                FilledTonalButton(onClick = onConnect) {
                    Text(stringResource(R.string.camera_action_connect))
                }
            } else {
                Button(
                    onClick = { onRender(RawRenderQuality.PREVIEW) },
                    enabled = lab.canRender,
                ) {
                    Text(stringResource(R.string.lab_action_update_preview))
                }
                OutlinedButton(onClick = { onRender(RawRenderQuality.FULL) }, enabled = lab.canRender) {
                    Text(stringResource(R.string.lab_action_render_full))
                }
            }

            if (lab.canSaveJpeg) {
                TextButton(onClick = onSave) {
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

/** Whether the camera can be asked to render, and the one line that explains why not. */
private data class CameraReadiness(val canRender: Boolean, val message: String)

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

    is CameraState.Error -> CameraReadiness(canRender = false, message = message)

    else -> CameraReadiness(
        canRender = false,
        message = stringResource(R.string.raw_camera_not_ready_body),
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
