package dev.bondarenko.fujirecipes.ui.raw

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val step = state.step) {
                RawDevelopmentStep.Loading -> item {
                    FujiCenteredLoading(modifier = Modifier.fillParentMaxSize())
                }

                RawDevelopmentStep.ChooseRaf -> item {
                    LibraryPanel(
                        title = stringResource(R.string.raw_choose_title),
                        body = stringResource(R.string.raw_choose_body, state.recipe?.name.orEmpty()),
                        primaryLabel = stringResource(R.string.raw_action_choose),
                        onPrimary = onChooseRaf,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                is RawDevelopmentStep.Importing -> item {
                    FujiCenteredLoading(
                        label = stringResource(R.string.raw_importing),
                        progress = step.total?.takeIf { it > 0 }?.let { total ->
                            { step.written.toFloat() / total.toFloat() }
                        },
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                RawDevelopmentStep.Ready -> {
                    item { RafSummary(state) }
                    item {
                        CameraReadiness(
                            camera = camera,
                            onConnect = onConnect,
                            onRender = onRender,
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = onChooseAnotherRaf,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.raw_action_choose_another))
                        }
                    }
                }

                is RawDevelopmentStep.Running -> item {
                    val progress = step.stage.progress()
                    FujiCenteredLoading(
                        label = stageLabel(step.stage),
                        progress = progress,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                is RawDevelopmentStep.CalibrationRequired -> item {
                    LibraryPanel(
                        title = stringResource(R.string.raw_calibration_title, step.cameraModel),
                        body = stringResource(R.string.raw_calibration_body, step.profile.size),
                        primaryLabel = stringResource(R.string.raw_action_share_profile),
                        onPrimary = {
                            onShareProfile(
                                "${step.cameraModel.safeFilename()}-d185.bin",
                                step.profile,
                            )
                        },
                        secondaryLabel = stringResource(R.string.raw_action_choose_another),
                        onSecondary = onChooseAnotherRaf,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                is RawDevelopmentStep.Complete -> {
                    item {
                        AsyncImage(
                            model = step.result.jpeg,
                            contentDescription = stringResource(R.string.raw_result_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                        )
                    }
                    item {
                        LibraryPanel(
                            title = stringResource(R.string.raw_complete_title),
                            body = if (step.result.width != null && step.result.height != null) {
                                stringResource(
                                    R.string.raw_complete_body_dimensions,
                                    step.result.width,
                                    step.result.height,
                                    step.result.patch.appliedFields.size,
                                    step.result.patch.preservedFields.size,
                                )
                            } else {
                                stringResource(
                                    R.string.raw_complete_body,
                                    step.result.patch.appliedFields.size,
                                    step.result.patch.preservedFields.size,
                                )
                            },
                            primaryLabel = stringResource(R.string.raw_action_save_jpeg),
                            onPrimary = { onSave(step.result.jpeg) },
                            secondaryLabel = stringResource(R.string.raw_action_choose_another),
                            onSecondary = onChooseAnotherRaf,
                        )
                    }
                }

                is RawDevelopmentStep.Failed -> item {
                    LibraryPanel(
                        title = stringResource(R.string.raw_failed_title),
                        body = step.message,
                        primaryLabel = stringResource(R.string.raw_action_try_again),
                        onPrimary = onRetry,
                        secondaryLabel = stringResource(R.string.raw_action_choose_another),
                        onSecondary = onChooseAnotherRaf,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RafSummary(state: RawDevelopmentUiState) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(state.recipe?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text(
                state.raf?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CameraReadiness(
    camera: CameraState,
    onConnect: () -> Unit,
    onRender: () -> Unit,
) {
    when (camera) {
        is CameraState.Connected -> {
            if (camera.usbMode == UsbMode.RAW_CONVERSION || camera.usbMode == UsbMode.UNREPORTED) {
                LibraryPanel(
                    title = stringResource(R.string.raw_camera_ready, camera.identity.model),
                    body = stringResource(R.string.raw_camera_ready_body),
                    primaryLabel = stringResource(R.string.raw_action_render),
                    onPrimary = onRender,
                )
            } else {
                LibraryPanel(
                    title = stringResource(R.string.raw_camera_not_ready),
                    body = stringResource(R.string.raw_camera_wrong_mode_body),
                )
            }
        }

        CameraState.Connecting -> LibraryPanel(
            title = stringResource(R.string.camera_chip_connecting),
            body = stringResource(R.string.raw_camera_connecting_body),
        )

        is CameraState.Error -> LibraryPanel(
            title = stringResource(R.string.raw_camera_not_ready),
            body = camera.message,
            primaryLabel = stringResource(R.string.camera_action_connect),
            onPrimary = onConnect,
        )

        else -> LibraryPanel(
            title = stringResource(R.string.raw_camera_not_ready),
            body = stringResource(R.string.raw_camera_not_ready_body),
            primaryLabel = stringResource(R.string.camera_action_connect),
            onPrimary = onConnect,
        )
    }
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
