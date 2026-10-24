package dev.bondarenko.fujirecipes.ui.raweditor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.data.raweditor.MaskPoint
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.Colors
import dev.bondarenko.fujirecipes.ui.theme.icons.Contrast
import dev.bondarenko.fujirecipes.ui.theme.icons.Deblur
import dev.bondarenko.fujirecipes.ui.theme.icons.Details
import dev.bondarenko.fujirecipes.ui.theme.icons.Diamond
import dev.bondarenko.fujirecipes.ui.theme.icons.DiscoverTune
import dev.bondarenko.fujirecipes.ui.theme.icons.Exposure
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Grain
import dev.bondarenko.fujirecipes.ui.theme.icons.Palette
import dev.bondarenko.fujirecipes.ui.theme.icons.Refresh
import dev.bondarenko.fujirecipes.ui.theme.icons.ResetWrench
import dev.bondarenko.fujirecipes.ui.theme.icons.Save
import dev.bondarenko.fujirecipes.ui.theme.icons.StarShine
import dev.bondarenko.fujirecipes.ui.theme.icons.Tonality
import dev.bondarenko.fujirecipes.ui.theme.icons.Tonality2
import dev.bondarenko.fujirecipes.ui.theme.icons.TransitionDissolve
import dev.bondarenko.fujirecipes.ui.theme.icons.Tune
import dev.bondarenko.fujirecipes.ui.theme.icons.WbAuto
import kotlin.math.round

@Composable
fun RawEditorRouteContent(
    initialUri: String? = null,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onActiveSessionChange: (Boolean) -> Unit = {},
) {
    val model: RawEditorViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.preview != null) {
        onActiveSessionChange(state.preview != null)
    }
    LaunchedEffect(initialUri) {
        if (initialUri != null && state.preview == null && !state.busy) {
            model.open(android.net.Uri.parse(initialUri))
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model.open(it) }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg"), model::save)
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val leave: (() -> Unit) -> Unit = { action ->
        if (state.hasUnsavedChanges) confirmAction = action else action()
    }
    BackHandler { leave(onBack) }
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            model.clearMessage()
        }
    }
    RawEditorScreen(
        state = state,
        onBack = { leave(onBack) },
        onOpen = { leave { open.launch(arrayOf("*/*")) } },
        onAdjust = model::adjust,
        onSave = {
            model.prepareExport()
            save.launch("${state.name.substringBeforeLast('.')}-edited.jpg")
        },
        onMasks = model::editMasks,
        onSelectMask = model::selectMask,
        onDrawMask = model::drawMask,
        onCrop = model::prepareCrop,
        onDismissCrop = model::dismissCrop,
        onViewport = model::detailViewport,
        contentPadding = contentPadding,
    )
    if (confirmAction != null) {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(stringResource(R.string.raw_editor_discard_title)) },
            text = { Text(stringResource(R.string.raw_editor_discard_body)) },
            confirmButton = {
                TextButton(onClick = { val action = confirmAction; confirmAction = null; action?.invoke() }) {
                    Text(stringResource(R.string.raw_editor_discard))
                }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text(stringResource(R.string.raw_editor_keep)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawEditorScreen(
    state: RawEditorState,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onAdjust: (RawAdjustments) -> Unit,
    onSave: () -> Unit,
    onMasks: (Boolean) -> Unit = {},
    onSelectMask: (Int) -> Unit = {},
    onDrawMask: (List<MaskPoint>, Float, Boolean) -> Unit = { _, _, _ -> },
    onCrop: () -> Unit = {},
    onDismissCrop: () -> Unit = {},
    onViewport: (RawViewport?) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var showCamera by remember(state.name, state.busy, state.cameraImage) { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableIntStateOf(0) }
    var selectedParam by rememberSaveable(panel) { mutableIntStateOf(0) }
    if (state.maskEditorOpen && state.preview != null) RawMaskDialog(state, onAdjust, onSelectMask, onDrawMask, { onMasks(false) })
    val uriHandler = LocalUriHandler.current
    state.cropPreview?.let { bitmap ->
        RawCropDialog(bitmap, state.adjustments.crop, onDismissCrop, onApply = { crop ->
            onAdjust(state.adjustments.copy(crop = crop))
            onDismissCrop()
        }, imageAspect = state.cropImageAspect ?: bitmap.width.toFloat() / bitmap.height)
    }
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text(stringResource(R.string.raw_editor_profile_info)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.raw_editor_profile_note))
                Text(stringResource(R.string.raw_editor_attribution))
                TextButton(onClick = { uriHandler.openUri("https://github.com/abpy/FujifilmCameraProfiles") }) {
                    Text(stringResource(R.string.raw_editor_profile_source))
                }
                TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/by-nc-sa/4.0/") }) {
                    Text("CC BY-NC-SA 4.0")
                }
                TextButton(onClick = { uriHandler.openUri("https://www.libraw.org/") }) { Text("LibRaw · CDDL 1.0") }
            }
        },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_back)) } },
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.preview != null && state.name.isNotBlank()) state.name else stringResource(R.string.raw_editor_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FujiIcons.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.preview != null) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { onAdjust(RawAdjustments()) },
                                enabled = !state.busy && state.adjustments.isEdited,
                            ) {
                                Icon(FujiIcons.ResetWrench, stringResource(R.string.raw_editor_reset))
                            }
                            Button(
                                onClick = onSave,
                                enabled = !state.busy && !state.rendering,
                            ) {
                                Icon(
                                    imageVector = FujiIcons.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.raw_editor_save))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                if (state.busy || state.rendering || state.detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.busy && state.preview == null) Text(
                stringResource(R.string.raw_editor_processing),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            if (state.preview == null) {
                FujiIconPanel(
                    icon = FujiIcons.Tune,
                    shape = MaterialShapes.Pill.toShape(),
                    title = stringResource(R.string.raw_editor_title),
                    body = stringResource(R.string.raw_editor_intro_body),
                    actionLabel = stringResource(R.string.raw_editor_open),
                    onAction = onOpen,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = contentPadding.calculateBottomPadding()),
                    extra = {
                        TextButton(onClick = { showInfo = true }) {
                            Text(stringResource(R.string.raw_editor_profile_info))
                        }
                    },
                )
            } else {
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 700.dp
                    val preview: @Composable (Modifier) -> Unit = { modifier ->
                        RawEditorPreview(state, panel == 5, showCamera, { showCamera = it }, onViewport, modifier)
                    }
                    val controls: @Composable (Modifier) -> Unit = { modifier ->
                        val edits = state.adjustments
                        val editable = !state.busy && !showCamera
                        val params = when (panel) {
                            0 -> listOf(
                                RawParam(R.string.raw_editor_exposure, FujiIcons.Exposure, edits.exposure, -3f..3f, editable, 1f, 1, " EV") { onAdjust(edits.copy(exposure = it)) },
                                RawParam(R.string.raw_editor_shadows, FujiIcons.Tonality, edits.shadows, -1f..1f, editable) { onAdjust(edits.copy(shadows = it)) },
                                RawParam(R.string.raw_editor_highlights, FujiIcons.StarShine, edits.highlights, -1f..1f, editable) { onAdjust(edits.copy(highlights = it)) },
                                RawParam(R.string.raw_editor_contrast, FujiIcons.Contrast, edits.contrast, -1f..1f, editable) { onAdjust(edits.copy(contrast = it)) },
                                RawParam(R.string.raw_editor_whites, FujiIcons.Tonality2, edits.whites, -1f..1f, editable) { onAdjust(edits.copy(whites = it)) },
                                RawParam(R.string.raw_editor_blacks, FujiIcons.Tonality, edits.blacks, -1f..1f, editable) { onAdjust(edits.copy(blacks = it)) },
                            )
                            1 -> listOf(
                                RawParam(R.string.raw_editor_temperature, FujiIcons.WbAuto, edits.temperature, -1f..1f, editable) { onAdjust(edits.copy(temperature = it)) },
                                RawParam(R.string.raw_editor_tint, FujiIcons.DiscoverTune, edits.tint, -1f..1f, editable) { onAdjust(edits.copy(tint = it)) },
                                RawParam(R.string.raw_editor_saturation, FujiIcons.Palette, edits.saturation, -1f..1f, editable) { onAdjust(edits.copy(saturation = it)) },
                                RawParam(R.string.raw_editor_vibrance, FujiIcons.Colors, edits.vibrance, -1f..1f, editable) { onAdjust(edits.copy(vibrance = it)) },
                            )
                            2 -> listOf(
                                RawParam(R.string.raw_editor_sharpening, FujiIcons.Details, edits.sharpening, 0f..1f, editable) { onAdjust(edits.copy(sharpening = it)) },
                                RawParam(R.string.raw_editor_radius, FujiIcons.Tune, edits.sharpenRadius, .5f..3f, editable && edits.sharpening > 0, 1f, 1, " px", defaultValue = 1f, resetEnabled = editable) { onAdjust(edits.copy(sharpenRadius = it)) },
                                RawParam(R.string.raw_editor_threshold, FujiIcons.Contrast, edits.sharpenThreshold, 0f..1f, editable && edits.sharpening > 0, resetEnabled = editable) { onAdjust(edits.copy(sharpenThreshold = it)) },
                                RawParam(R.string.raw_editor_color_noise, FujiIcons.Deblur, edits.colorNoise, 0f..1f, editable) { onAdjust(edits.copy(colorNoise = it)) },
                                RawParam(R.string.raw_editor_luminance_noise, FujiIcons.Grain, edits.luminanceNoise, 0f..1f, editable) { onAdjust(edits.copy(luminanceNoise = it)) },
                                RawParam(R.string.raw_editor_noise_detail, FujiIcons.Diamond, edits.noiseDetail, 0f..1f, editable && (edits.colorNoise > 0 || edits.luminanceNoise > 0), defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(noiseDetail = it)) },
                            )
                            3 -> listOf(
                                RawParam(R.string.raw_editor_grain_amount, FujiIcons.Grain, edits.grainAmount, 0f..1f, editable) { onAdjust(edits.copy(grainAmount = it)) },
                                RawParam(R.string.raw_editor_grain_size, FujiIcons.Details, edits.grainSize, .5f..10f, editable && edits.grainAmount > 0, 1f, 1, " px", defaultValue = 3f, resetEnabled = editable) { onAdjust(edits.copy(grainSize = it)) },
                                RawParam(R.string.raw_editor_grain_roughness, FujiIcons.Deblur, edits.grainRoughness, 0f..1f, editable && edits.grainAmount > 0, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(grainRoughness = it)) },
                                RawParam(R.string.raw_editor_clarity, FujiIcons.Diamond, edits.clarity, -1f..1f, editable) { onAdjust(edits.copy(clarity = it)) },
                                RawParam(R.string.raw_editor_texture, FujiIcons.Grain, edits.texture, -1f..1f, editable) { onAdjust(edits.copy(texture = it)) },
                                RawParam(R.string.raw_editor_dehaze, FujiIcons.TransitionDissolve, edits.dehaze, -1f..1f, editable) { onAdjust(edits.copy(dehaze = it)) },
                                RawParam(R.string.raw_editor_vignette, FujiIcons.Tonality, edits.vignette, -1f..1f, editable) { onAdjust(edits.copy(vignette = it)) },
                                RawParam(R.string.raw_editor_vignette_midpoint, FujiIcons.Tune, edits.vignetteMidpoint, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteMidpoint = it)) },
                                RawParam(R.string.raw_editor_vignette_roundness, FujiIcons.Contrast, edits.vignetteRoundness, -1f..1f, editable && edits.vignette != 0f, resetEnabled = editable) { onAdjust(edits.copy(vignetteRoundness = it)) },
                                RawParam(R.string.raw_editor_vignette_feather, FujiIcons.Deblur, edits.vignetteFeather, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteFeather = it)) },
                            )
                            4 -> listOf(
                                RawParam(R.string.raw_editor_rotation, FujiIcons.Refresh, edits.quarterTurns.toFloat(), 0f..3f, editable, displayScale = 90f, suffix = "°", steps = 2) { onAdjust(edits.rotated(it.toInt())) },
                                RawParam(R.string.raw_editor_straighten, FujiIcons.Tune, edits.straighten, -15f..15f, editable, displayScale = 1f, decimals = 1, suffix = "°") { onAdjust(edits.copy(straighten = it)) },
                            )
                            5 -> listOf(
                                RawParam(R.string.raw_editor_distortion, FujiIcons.Tonality2, edits.distortion, -1f..1f, editable) { onAdjust(edits.copy(distortion = it)) },
                                RawParam(R.string.raw_editor_perspective_vertical, FujiIcons.Tune, edits.perspectiveVertical, -1f..1f, editable) { onAdjust(edits.copy(perspectiveVertical = it)) },
                                RawParam(R.string.raw_editor_perspective_horizontal, FujiIcons.DiscoverTune, edits.perspectiveHorizontal, -1f..1f, editable) { onAdjust(edits.copy(perspectiveHorizontal = it)) },
                                RawParam(R.string.raw_editor_chromatic_red, FujiIcons.Colors, edits.chromaticRed, -1f..1f, editable) { onAdjust(edits.copy(chromaticRed = it)) },
                                RawParam(R.string.raw_editor_chromatic_blue, FujiIcons.Palette, edits.chromaticBlue, -1f..1f, editable) { onAdjust(edits.copy(chromaticBlue = it)) },
                                RawParam(R.string.raw_editor_defringe_purple, FujiIcons.StarShine, edits.defringePurple, 0f..1f, editable) { onAdjust(edits.copy(defringePurple = it)) },
                                RawParam(R.string.raw_editor_defringe_green, FujiIcons.Deblur, edits.defringeGreen, 0f..1f, editable) { onAdjust(edits.copy(defringeGreen = it)) },
                            )
                            else -> emptyList()
                        }
                        val activeIndex = selectedParam.coerceIn(0, params.lastIndex.coerceAtLeast(0))
                        val active = params.getOrNull(activeIndex)
                        val locale = LocalConfiguration.current.locales[0]

                        Column(
                            modifier = modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (panel == 5) Text(
                                stringResource(if (state.lensCorrectionApplied) R.string.raw_editor_optics_note else R.string.raw_editor_optics_unavailable),
                                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (panel == 6) RawToneCurveEditor(state, onAdjust, compact = wide, enabled = editable)

                            // 1. Active Parameter Name + Value
                            if (active != null) Text(
                                text = "${stringResource(active.labelRes)}  ${active.formatValue(locale)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // 2. Tick-Mark Dial for Active Parameter (placed above icons)
                            if (active != null) RawTickDial(
                                param = active,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            )

                            // 3. Circular Icon Carousel + Contextual Action Pills
                            if (panel != 6) Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (panel == 7) {
                                    FilledTonalButton(onClick = { onMasks(true) }, enabled = editable && !state.rendering) { Text(stringResource(R.string.raw_editor_edit_masks, edits.masks.size)) }
                                }
                                if (panel == 1) {
                                    FilledTonalButton(
                                        onClick = { onAdjust(edits.copy(temperature = 0f, tint = 0f)) },
                                        enabled = editable,
                                    ) { Text(stringResource(R.string.raw_editor_as_shot)) }
                                }
                                if (panel == 4) {
                                    FilledTonalButton(
                                        onClick = onCrop,
                                        enabled = editable && !state.rendering,
                                    ) { Text(stringResource(R.string.raw_editor_crop)) }
                                    if (edits.crop != RawCrop()) {
                                        TextButton(
                                            onClick = { onAdjust(edits.copy(crop = RawCrop())) },
                                            enabled = editable,
                                        ) { Text(stringResource(R.string.raw_editor_crop_reset)) }
                                    }
                                }
                                params.forEachIndexed { index, param ->
                                    RawParamCircle(
                                        param = param,
                                        selected = index == activeIndex,
                                        onSelect = { selectedParam = index },
                                    )
                                }
                            }

                            // 4. Secondary Category Tabs at Bottom
                            SecondaryScrollableTabRow(
                                selectedTabIndex = panel,
                                edgePadding = 16.dp,
                            ) {
                                listOf(
                                    R.string.raw_editor_light, R.string.raw_editor_color, R.string.raw_editor_detail,
                                    R.string.raw_editor_effects, R.string.raw_editor_framing, R.string.raw_editor_optics,
                                    R.string.raw_editor_curves, R.string.raw_editor_masks,
                                ).forEachIndexed { index, label ->
                                    Tab(
                                        selected = panel == index,
                                        onClick = { panel = index },
                                        text = { Text(stringResource(label)) },
                                    )
                                }
                            }
                        }
                    }
                    if (wide) Row(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(1f).fillMaxSize())
                        Box(Modifier.widthIn(max = 360.dp).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                            controls(Modifier.fillMaxWidth().wrapContentHeight())
                        }
                    } else Column(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(1f).fillMaxWidth())
                        controls(Modifier.fillMaxWidth().wrapContentHeight())
                    }
                }
            }
        }
    }
}

internal data class RawParam(
    val labelRes: Int,
    val icon: ImageVector,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val enabled: Boolean,
    val displayScale: Float = 100f,
    val decimals: Int = 0,
    val suffix: String = "",
    val defaultValue: Float = 0f,
    val resetEnabled: Boolean = enabled,
    val steps: Int = 0,
    val onChange: (Float) -> Unit,
) {
    val isEdited: Boolean get() = kotlin.math.abs(value - defaultValue) > 0.0005f

    fun snap(raw: Float): Float {
        val clamped = raw.coerceIn(range.start, range.endInclusive)
        if (steps > 0) {
            val stepCount = steps + 1
            val stepSize = (range.endInclusive - range.start) / stepCount
            return range.start + round((clamped - range.start) / stepSize) * stepSize
        }
        val precision = displayScale * if (decimals == 1) 10f else 1f
        return round(clamped * precision).toInt() / precision
    }

    fun formatValue(locale: java.util.Locale): String {
        val sign = if (range.start < 0) "+" else ""
        val displayed = if (value == 0f) 0f else value
        return String.format(locale, "%$sign.${decimals}f$suffix", displayed * displayScale)
    }
}

@Composable
internal fun RawParamCircle(
    param: RawParam,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val label = stringResource(param.labelRes)
    val resetLabel = stringResource(R.string.raw_editor_reset_parameter, label)
    val currentParam by rememberUpdatedState(param)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        param.isEdited -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        param.isEdited -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .widthIn(min = 56.dp, max = 76.dp)
            .alpha(if (param.enabled || param.resetEnabled) 1f else 0.45f)
            .pointerInput(param.resetEnabled, param.defaultValue) {
                detectTapGestures(
                    onTap = { currentOnSelect() },
                    onDoubleTap = {
                        currentOnSelect()
                        if (currentParam.resetEnabled) currentParam.onChange(currentParam.defaultValue)
                    },
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = label
                onClick { currentOnSelect(); true }
                progressBarRangeInfo = ProgressBarRangeInfo(param.value, param.range, param.steps)
                if (param.enabled) {
                    setProgress { target ->
                        currentOnSelect()
                        currentParam.onChange(currentParam.snap(target))
                        true
                    }
                }
                customActions = if (param.resetEnabled) listOf(
                    CustomAccessibilityAction(resetLabel) {
                        currentOnSelect()
                        currentParam.onChange(currentParam.defaultValue)
                        true
                    },
                ) else emptyList()
            },
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = param.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            if (param.isEdited) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (selected) contentColor else MaterialTheme.colorScheme.primary),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun RawTickDial(
    param: RawParam,
    modifier: Modifier = Modifier,
) {
    val currentParam by rememberUpdatedState(param)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .height(36.dp)
            .alpha(if (param.enabled) 1f else 0.38f)
            .pointerInput(param.enabled, param.range, param.defaultValue) {
                if (!param.enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = { currentParam.onChange(currentParam.defaultValue) },
                    onTap = { offset ->
                        val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val raw = currentParam.range.start + fraction * (currentParam.range.endInclusive - currentParam.range.start)
                        currentParam.onChange(currentParam.snap(raw))
                    },
                )
            }
            .pointerInput(param.enabled, param.range) {
                if (!param.enabled) return@pointerInput
                var dragValue = currentParam.value
                detectHorizontalDragGestures(
                    onDragStart = { dragValue = currentParam.value },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val span = currentParam.range.endInclusive - currentParam.range.start
                        val delta = (dragAmount / size.width.coerceAtLeast(1)) * span
                        dragValue = (dragValue + delta).coerceIn(currentParam.range.start, currentParam.range.endInclusive)
                        currentParam.onChange(currentParam.snap(dragValue))
                    },
                )
            },
    ) {
        val tickCount = 41
        val width = size.width
        val centerY = size.height / 2f
        val span = (param.range.endInclusive - param.range.start).coerceAtLeast(0.0001f)
        val valueFraction = ((param.value - param.range.start) / span).coerceIn(0f, 1f)
        val defaultFraction = ((param.defaultValue - param.range.start) / span).coerceIn(0f, 1f)

        for (i in 0 until tickCount) {
            val f = i.toFloat() / (tickCount - 1)
            val x = f * width
            val isDefaultTick = kotlin.math.abs(f - defaultFraction) < (0.5f / (tickCount - 1))
            val isMajor = i % 5 == 0
            val halfH = when {
                isDefaultTick -> 10.dp.toPx()
                isMajor -> 7.dp.toPx()
                else -> 4.5.dp.toPx()
            }
            val inActiveSpan = (f >= minOf(defaultFraction, valueFraction) - 0.001f) &&
                (f <= maxOf(defaultFraction, valueFraction) + 0.001f) && param.isEdited
            drawLine(
                color = if (inActiveSpan) primaryColor.copy(alpha = 0.85f) else tickColor.copy(alpha = if (isMajor) 0.55f else 0.3f),
                start = Offset(x, centerY - halfH),
                end = Offset(x, centerY + halfH),
                strokeWidth = if (isDefaultTick) 2.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val markerX = valueFraction * width
        drawLine(
            color = primaryColor,
            start = Offset(markerX, centerY - 13.dp.toPx()),
            end = Offset(markerX, centerY + 13.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
