package dev.bondarenko.fujirecipes.ui.raw

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentResult
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.theme.icons.ImagesMode
import dev.bondarenko.fujirecipes.ui.theme.icons.Warning
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.core.share.ShareFile
import dev.bondarenko.fujirecipes.ui.common.FujiCenteredLoading
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawDevelopmentScreen(
    state: RawDevelopmentUiState,
    camera: CameraState,
    onBack: () -> Unit,
    onChooseRaf: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    onConnect: () -> Unit,
    onRender: () -> Unit,
    onRetry: () -> Unit,
    onSave: (File) -> Unit,
    onShareProfile: (String, ByteArray) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.raw_development_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FujiIcons.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        val panelModifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding())

        when (val step = state.step) {
            RawDevelopmentStep.Loading -> FujiCenteredLoading(modifier = panelModifier)

            RawDevelopmentStep.ChooseRaf -> FujiIconPanel(
                icon = FujiIcons.ImagesMode,
                shape = MaterialShapes.Pill.toShape(),
                title = stringResource(R.string.raw_choose_title),
                body = stringResource(R.string.raw_choose_body, state.recipe?.name.orEmpty()),
                actionLabel = stringResource(R.string.raw_action_choose),
                onAction = onChooseRaf,
                modifier = panelModifier,
            )

            is RawDevelopmentStep.Importing -> FujiCenteredLoading(
                label = stringResource(R.string.raw_importing),
                progress = step.total?.takeIf { it > 0 }?.let { total ->
                    { step.written.toFloat() / total.toFloat() }
                },
                modifier = panelModifier,
            )

            RawDevelopmentStep.Ready -> CameraReadiness(
                camera = camera,
                rafName = state.raf?.name.orEmpty(),
                onConnect = onConnect,
                onRender = onRender,
                onChooseAnotherRaf = onChooseAnotherRaf,
                modifier = panelModifier,
            )

            is RawDevelopmentStep.Running -> FujiCenteredLoading(
                label = stageLabel(step.stage),
                progress = step.stage.progress(),
                modifier = panelModifier,
            )

            is RawDevelopmentStep.CalibrationRequired -> FujiIconPanel(
                icon = FujiIcons.Warning,
                shape = MaterialShapes.Pill.toShape(),
                title = stringResource(R.string.raw_calibration_title, step.cameraModel),
                body = stringResource(R.string.raw_calibration_body, step.profile.size),
                actionLabel = stringResource(R.string.raw_action_share_profile),
                onAction = {
                    onShareProfile("${step.cameraModel.safeFilename()}-d185.bin", step.profile)
                },
                modifier = panelModifier,
                extra = { ChooseAnotherRafButton(onChooseAnotherRaf) },
            )

            is RawDevelopmentStep.Complete -> RawResultPanel(
                result = step.result,
                onSave = onSave,
                onChooseAnotherRaf = onChooseAnotherRaf,
                modifier = panelModifier,
            )

            is RawDevelopmentStep.Failed -> FujiIconPanel(
                icon = FujiIcons.Warning,
                shape = MaterialShapes.Pill.toShape(),
                title = stringResource(R.string.raw_failed_title),
                body = step.message,
                actionLabel = stringResource(R.string.raw_action_try_again),
                onAction = onRetry,
                modifier = panelModifier,
                extra = { ChooseAnotherRafButton(onChooseAnotherRaf) },
            )
        }
    }
}

/**
 * The rendered JPEG standing in for the panel's icon: the picture is what the page has to
 * say, so it takes the shape's place and the rest of the layout is unchanged.
 */
@Composable
private fun RawResultPanel(
    result: RawDevelopmentResult,
    onSave: (File) -> Unit,
    onChooseAnotherRaf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = result.jpeg,
            contentDescription = stringResource(R.string.raw_result_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
        Text(
            text = stringResource(R.string.raw_complete_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (result.width != null && result.height != null) {
                stringResource(
                    R.string.raw_complete_body_dimensions,
                    result.width,
                    result.height,
                    result.patch.appliedFields.size,
                    result.patch.preservedFields.size,
                )
            } else {
                stringResource(
                    R.string.raw_complete_body,
                    result.patch.appliedFields.size,
                    result.patch.preservedFields.size,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onSave(result.jpeg) },
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        ) {
            Text(stringResource(R.string.raw_action_save_jpeg))
        }
        ChooseAnotherRafButton(onChooseAnotherRaf)
    }
}

@Composable
private fun ChooseAnotherRafButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(stringResource(R.string.raw_action_choose_another))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CameraReadiness(
    camera: CameraState,
    rafName: String,
    onConnect: () -> Unit,
    onRender: () -> Unit,
    onChooseAnotherRaf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title: String
    val body: String
    var actionLabel: String? = null
    var action: (() -> Unit)? = null

    when (camera) {
        is CameraState.Connected -> {
            if (camera.usbMode == UsbMode.RAW_CONVERSION || camera.usbMode == UsbMode.UNREPORTED) {
                title = stringResource(R.string.raw_camera_ready, camera.identity.model)
                body = stringResource(R.string.raw_camera_ready_body)
                actionLabel = stringResource(R.string.raw_action_render)
                action = onRender
            } else {
                title = stringResource(R.string.raw_camera_not_ready)
                body = stringResource(R.string.raw_camera_wrong_mode_body)
            }
        }

        CameraState.Connecting -> {
            title = stringResource(R.string.camera_chip_connecting)
            body = stringResource(R.string.raw_camera_connecting_body)
        }

        is CameraState.Error -> {
            title = stringResource(R.string.raw_camera_not_ready)
            body = camera.message
            actionLabel = stringResource(R.string.camera_action_connect)
            action = onConnect
        }

        else -> {
            title = stringResource(R.string.raw_camera_not_ready)
            body = stringResource(R.string.raw_camera_not_ready_body)
            actionLabel = stringResource(R.string.camera_action_connect)
            action = onConnect
        }
    }

    FujiIconPanel(
        icon = FujiIcons.ImagesMode,
        shape = MaterialShapes.Pill.toShape(),
        title = title,
        body = body,
        actionLabel = actionLabel,
        onAction = action,
        modifier = modifier,
        extra = {
            Text(
                text = rafName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChooseAnotherRafButton(onChooseAnotherRaf)
        },
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

private fun RawDevelopmentStage.progress(): (() -> Float)? = when (this) {
    is RawDevelopmentStage.Uploading -> ({ written.toFloat() / total.coerceAtLeast(1).toFloat() })
    is RawDevelopmentStage.Downloading -> ({ written.toFloat() / total.coerceAtLeast(1).toFloat() })
    else -> null
}

@Composable
fun RawDevelopmentRouteContent(
    recipeId: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: RawDevelopmentViewModel = viewModel(
        factory = RawDevelopmentViewModel.factory(container, recipeId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val camera by container.cameraController.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val chooseRaf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.selectRaf(it.toString()) }
    }
    val saveJpeg = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { uri ->
        val jpeg = (state.step as? RawDevelopmentStep.Complete)?.result?.jpeg
        if (uri != null && jpeg != null) {
            scope.launch {
                val saved = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            jpeg.inputStream().use { input -> input.copyTo(output) }
                        } ?: error("The selected destination could not be opened.")
                    }
                }
                Toast.makeText(
                    context,
                    if (saved.isSuccess) R.string.raw_saved else R.string.raw_save_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    RawDevelopmentScreen(
        state = state,
        camera = camera,
        onBack = onBack,
        onChooseRaf = { chooseRaf.launch(arrayOf("*/*")) },
        onChooseAnotherRaf = viewModel::chooseAnotherRaf,
        onConnect = viewModel::connect,
        onRender = viewModel::render,
        onRetry = viewModel::retry,
        onSave = { jpeg -> saveJpeg.launch(jpeg.name) },
        onShareProfile = { filename, profile -> ShareFile.share(context, filename, profile) },
        contentPadding = contentPadding,
    )
}

private fun String.safeFilename(): String =
    replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "fuji-camera" }
