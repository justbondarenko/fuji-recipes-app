package dev.bondarenko.fujirecipes.ui.raweditor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Label
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
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
import dev.bondarenko.fujirecipes.ui.theme.icons.Details
import dev.bondarenko.fujirecipes.ui.theme.icons.Diamond
import dev.bondarenko.fujirecipes.ui.theme.icons.Exposure
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.RawOn
import dev.bondarenko.fujirecipes.ui.theme.icons.Grain
import dev.bondarenko.fujirecipes.ui.theme.icons.Palette
import dev.bondarenko.fujirecipes.ui.theme.icons.ResetWrench
import dev.bondarenko.fujirecipes.ui.theme.icons.Save
import dev.bondarenko.fujirecipes.ui.theme.icons.Tonality
import dev.bondarenko.fujirecipes.ui.theme.icons.Tonality2
import dev.bondarenko.fujirecipes.ui.theme.icons.Tune
import dev.bondarenko.fujirecipes.ui.theme.icons.Thermometer
import dev.bondarenko.fujirecipes.ui.theme.icons.WaterDrop
import dev.bondarenko.fujirecipes.ui.theme.icons.BlurOff
import dev.bondarenko.fujirecipes.ui.theme.icons.DiamondShine
import dev.bondarenko.fujirecipes.ui.theme.icons.Texture
import dev.bondarenko.fujirecipes.ui.theme.icons.Dehaze
import dev.bondarenko.fujirecipes.ui.theme.icons.Vignette2
import dev.bondarenko.fujirecipes.ui.theme.icons.Vrpano
import dev.bondarenko.fujirecipes.ui.theme.icons.TiltArrowUp
import dev.bondarenko.fujirecipes.ui.theme.icons.FilterBAndW
import dev.bondarenko.fujirecipes.ui.theme.icons.FormatColorFill
import dev.bondarenko.fujirecipes.ui.theme.icons.UnfoldMore
import dev.bondarenko.fujirecipes.ui.theme.icons.PhotoCamera
import kotlinx.coroutines.launch
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
        onCropAdjust = model::adjustCrop,
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
    onCropAdjust: (RawAdjustments) -> Unit = {},
    onDismissCrop: () -> Unit = {},
    onViewport: (RawViewport?) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var showCamera by remember(state.name, state.busy, state.cameraImage) { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val categoryLabels = listOf(
        R.string.raw_editor_light, R.string.raw_editor_color, R.string.raw_editor_detail,
        R.string.raw_editor_curves, R.string.raw_editor_effects, R.string.raw_editor_optics,
        R.string.raw_editor_masks,
    )
    val pagerState = rememberPagerState { categoryLabels.size }
    val panel = pagerState.currentPage
    val scope = rememberCoroutineScope()
    val optionScrollBoundary = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                Offset(available.x, 0f)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(available.x, 0f)
        }
    }
    if (state.maskEditorOpen && state.preview != null) RawMaskDialog(state, onAdjust, onSelectMask, onDrawMask, { onMasks(false) })
    val uriHandler = LocalUriHandler.current
    state.cropPreview?.let { bitmap ->
        RawCropDialog(bitmap, state.cropAdjustments ?: state.adjustments,
            onDismiss = onDismissCrop,
            onApply = { edits -> onDismissCrop(); onAdjust(edits) },
            onAdjust = onCropAdjust,
            imageAspect = state.cropImageAspect ?: bitmap.width.toFloat() / bitmap.height,
            rendering = state.cropRendering, error = state.error,
        )
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
                    icon = FujiIcons.RawOn,
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
                    val maxCurveSize = if (wide) maxWidth else maxHeight * .4f
                    val preview: @Composable (Modifier) -> Unit = { modifier ->
                        RawEditorPreview(state, panel == 5, showCamera, { showCamera = it }, onViewport, modifier, onCrop)
                    }
                    val controls: @Composable (Modifier) -> Unit = { modifier ->
                        val categories: @Composable () -> Unit = {
                            SecondaryScrollableTabRow(
                                selectedTabIndex = panel,
                                modifier = Modifier.testTag("raw-editor-groups"),
                                edgePadding = 16.dp,
                            ) {
                                categoryLabels.forEachIndexed { index, label ->
                                    Tab(
                                        selected = panel == index,
                                        enabled = !state.busy,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        text = { Text(stringResource(label)) },
                                    )
                                }
                            }
                        }
                        Column(
                            modifier.padding(top = 8.dp).testTag("raw-editor-controls"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (wide) categories()
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = !state.busy,
                                verticalAlignment = Alignment.Top,
                                modifier = (if (wide) Modifier.weight(1f) else Modifier)
                                    .fillMaxWidth().testTag("raw-editor-pager"),
                            ) { panel ->
                                var selectedParam by rememberSaveable { mutableIntStateOf(0) }
                                val edits = state.adjustments
                                val editable = !state.busy && !showCamera
                                val params = when (panel) {
                                    0 -> listOf(
                                        RawParam(R.string.raw_editor_exposure, FujiIcons.Exposure, edits.exposure, -3f..3f, editable, 1f, 1, " EV") { onAdjust(edits.copy(exposure = it)) },
                                        RawParam(R.string.raw_editor_contrast, FujiIcons.Contrast, edits.contrast, -1f..1f, editable) { onAdjust(edits.copy(contrast = it)) },
                                        RawParam(R.string.raw_editor_shadows, FujiIcons.Tonality, edits.shadows, -1f..1f, editable) { onAdjust(edits.copy(shadows = it)) },
                                        RawParam(R.string.raw_editor_highlights, FujiIcons.Tonality2, edits.highlights, -1f..1f, editable) { onAdjust(edits.copy(highlights = it)) },
                                    )
                                    1 -> listOf(
                                        RawParam(R.string.raw_editor_temperature, FujiIcons.Thermometer, edits.temperature, -1f..1f, editable) { onAdjust(edits.copy(temperature = it)) },
                                        RawParam(R.string.raw_editor_tint, FujiIcons.WaterDrop, edits.tint, -1f..1f, editable) { onAdjust(edits.copy(tint = it)) },
                                        RawParam(R.string.raw_editor_saturation, FujiIcons.Palette, edits.saturation, -1f..1f, editable) { onAdjust(edits.copy(saturation = it)) },
                                        RawParam(R.string.raw_editor_vibrance, FujiIcons.Colors, edits.vibrance, -1f..1f, editable) { onAdjust(edits.copy(vibrance = it)) },
                                    )
                                    2 -> listOf(
                                        RawParam(R.string.raw_editor_sharpening, FujiIcons.Details, edits.sharpening, 0f..1f, editable) { onAdjust(edits.copy(sharpening = it)) },
                                        RawParam(R.string.raw_editor_color_noise, FujiIcons.BlurOff, edits.colorNoise, 0f..1f, editable) { onAdjust(edits.copy(colorNoise = it)) },
                                        RawParam(R.string.raw_editor_noise_detail, FujiIcons.DiamondShine, edits.noiseDetail, 0f..1f, editable && (edits.colorNoise > 0 || edits.luminanceNoise > 0), defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(noiseDetail = it)) },
                                    )
                                    4 -> listOf(
                                        RawParam(R.string.raw_editor_grain_amount, FujiIcons.Grain, edits.grainAmount, 0f..1f, editable) { onAdjust(edits.copy(grainAmount = it)) },
                                        RawParam(R.string.raw_editor_clarity, FujiIcons.Diamond, edits.clarity, -1f..1f, editable) { onAdjust(edits.copy(clarity = it)) },
                                        RawParam(R.string.raw_editor_texture, FujiIcons.Texture, edits.texture, -1f..1f, editable) { onAdjust(edits.copy(texture = it)) },
                                        RawParam(R.string.raw_editor_dehaze, FujiIcons.Dehaze, edits.dehaze, -1f..1f, editable) { onAdjust(edits.copy(dehaze = it)) },
                                        RawParam(R.string.raw_editor_vignette, FujiIcons.Vignette2, edits.vignette, -1f..1f, editable) { onAdjust(edits.copy(vignette = it)) },
                                    )
                                    5 -> listOf(
                                        RawParam(R.string.raw_editor_distortion, FujiIcons.Vrpano, edits.distortion, -1f..1f, editable) { onAdjust(edits.copy(distortion = it)) },
                                        RawParam(R.string.raw_editor_perspective_vertical, FujiIcons.TiltArrowUp, edits.perspectiveVertical, -1f..1f, editable) { onAdjust(edits.copy(perspectiveVertical = it)) },
                                        RawParam(R.string.raw_editor_defringe_green, FujiIcons.FilterBAndW, edits.defringeGreen, 0f..1f, editable) { onAdjust(edits.copy(defringeGreen = it)) },
                                        RawParam(R.string.raw_editor_chromatic_red, FujiIcons.FormatColorFill, edits.chromaticRed, -1f..1f, editable) { onAdjust(edits.copy(chromaticRed = it)) },
                                    )
                                    else -> emptyList()
                                }
                                val related = when (panel) {
                                    0 -> mapOf(
                                        R.string.raw_editor_shadows to listOf(
                                            RawParam(R.string.raw_editor_blacks, FujiIcons.Tonality, edits.blacks, -1f..1f, editable) { onAdjust(edits.copy(blacks = it)) },
                                        ),
                                        R.string.raw_editor_highlights to listOf(
                                            RawParam(R.string.raw_editor_whites, FujiIcons.Tonality2, edits.whites, -1f..1f, editable) { onAdjust(edits.copy(whites = it)) },
                                        ),
                                    )
                                    2 -> mapOf(
                                        R.string.raw_editor_sharpening to listOf(
                                            RawParam(R.string.raw_editor_radius, FujiIcons.Tune, edits.sharpenRadius, .5f..3f, editable && edits.sharpening > 0, 1f, 1, " px", defaultValue = 1f, resetEnabled = editable) { onAdjust(edits.copy(sharpenRadius = it)) },
                                        ),
                                        R.string.raw_editor_color_noise to listOf(
                                            RawParam(R.string.raw_editor_luminance_noise, FujiIcons.Grain, edits.luminanceNoise, 0f..1f, editable) { onAdjust(edits.copy(luminanceNoise = it)) },
                                        ),
                                    )
                                    4 -> mapOf(
                                        R.string.raw_editor_grain_amount to listOf(
                                            RawParam(R.string.raw_editor_grain_size, FujiIcons.Details, edits.grainSize, .5f..10f, editable && edits.grainAmount > 0, 1f, 1, " px", defaultValue = 3f, resetEnabled = editable) { onAdjust(edits.copy(grainSize = it)) },
                                            RawParam(R.string.raw_editor_grain_roughness, FujiIcons.BlurOff, edits.grainRoughness, 0f..1f, editable && edits.grainAmount > 0, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(grainRoughness = it)) },
                                        ),
                                        R.string.raw_editor_vignette to listOf(
                                            RawParam(R.string.raw_editor_vignette_midpoint, FujiIcons.Tune, edits.vignetteMidpoint, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteMidpoint = it)) },
                                            RawParam(R.string.raw_editor_vignette_roundness, FujiIcons.Contrast, edits.vignetteRoundness, -1f..1f, editable && edits.vignette != 0f, resetEnabled = editable) { onAdjust(edits.copy(vignetteRoundness = it)) },
                                            RawParam(R.string.raw_editor_vignette_feather, FujiIcons.BlurOff, edits.vignetteFeather, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteFeather = it)) },
                                        ),
                                    )
                                    5 -> mapOf(
                                        R.string.raw_editor_perspective_vertical to listOf(
                                            RawParam(R.string.raw_editor_perspective_horizontal, FujiIcons.TiltArrowUp, edits.perspectiveHorizontal, -1f..1f, editable) { onAdjust(edits.copy(perspectiveHorizontal = it)) },
                                        ),
                                        R.string.raw_editor_defringe_green to listOf(
                                            RawParam(R.string.raw_editor_defringe_purple, FujiIcons.FilterBAndW, edits.defringePurple, 0f..1f, editable) { onAdjust(edits.copy(defringePurple = it)) },
                                        ),
                                        R.string.raw_editor_chromatic_red to listOf(
                                            RawParam(R.string.raw_editor_chromatic_blue, FujiIcons.Palette, edits.chromaticBlue, -1f..1f, editable) { onAdjust(edits.copy(chromaticBlue = it)) },
                                        ),
                                    )
                                    else -> emptyMap()
                                }
                                val optionLabels = mapOf(
                                    R.string.raw_editor_grain_amount to R.string.raw_editor_grain,
                                    R.string.raw_editor_color_noise to R.string.raw_editor_noise_reduction,
                                    R.string.raw_editor_perspective_vertical to R.string.raw_editor_perspective,
                                    R.string.raw_editor_defringe_green to R.string.raw_editor_defringe,
                                    R.string.raw_editor_chromatic_red to R.string.raw_editor_color_alignment,
                                )
                                val activeIndex = selectedParam.coerceIn(0, params.lastIndex.coerceAtLeast(0))
                                val active = params.getOrNull(activeIndex)
                                val activeControls = listOfNotNull(active) + related[active?.labelRes].orEmpty()
                                var selectedControl by rememberSaveable(panel, active?.labelRes) { mutableIntStateOf(0) }
                                var selectorOpen by remember(panel, active?.labelRes) { mutableStateOf(false) }
                                val useSelector = activeControls.size >= 3
                                val visibleControls = if (useSelector) listOf(activeControls[selectedControl.coerceIn(activeControls.indices)]) else activeControls
                                val locale = LocalConfiguration.current.locales[0]
                                val listScroll = rememberScrollState()

                                Box(Modifier.testTag("raw-editor-page-$panel")
                                    .then(if (panel == pagerState.currentPage) Modifier else Modifier.clearAndSetSemantics {})) {
                                    Column(
                                        modifier = if (wide) Modifier.fillMaxSize()
                                            .verticalScroll(listScroll)
                                            .testTag("raw-editor-slider-list").padding(vertical = 12.dp) else Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        if (panel == 5) Text(
                                            stringResource(if (state.lensCorrectionApplied) R.string.raw_editor_optics_note else R.string.raw_editor_optics_unavailable),
                                            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (panel == 3) RawToneCurveEditor(state, onAdjust, maxGraphSize = maxCurveSize, enabled = editable)

                                        if (wide) {
                                            if (panel == 1) FilledTonalButton(
                                                onClick = { onAdjust(edits.copy(temperature = 0f, tint = 0f)) }, enabled = editable,
                                                modifier = Modifier.align(Alignment.Start).padding(horizontal = 20.dp),
                                            ) {
                                                Icon(FujiIcons.PhotoCamera, null, Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.raw_editor_as_shot))
                                            }
                                            params.forEach { option ->
                                                val optionLabel = stringResource(optionLabels[option.labelRes] ?: option.labelRes)
                                                (listOf(option) + related[option.labelRes].orEmpty()).forEach { control ->
                                                    key(control.labelRes) {
                                                        val label = stringResource(if (control.labelRes == R.string.raw_editor_vignette) R.string.raw_editor_amount else control.labelRes)
                                                        val title = if (label == optionLabel) label else "$optionLabel · $label"
                                                        val currentControl by rememberUpdatedState(control)
                                                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                                Icon(option.icon, null, Modifier.size(22.dp).testTag("raw-editor-icon-${control.labelRes}"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                Text(title, Modifier.weight(1f).testTag("raw-editor-parameter-${control.labelRes}")
                                                                    .pointerInput(control.resetEnabled, control.defaultValue) {
                                                                        detectTapGestures(onDoubleTap = { if (currentControl.resetEnabled) currentControl.onChange(currentControl.defaultValue) })
                                                                    }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                                Text(control.formatValue(locale), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            RawParameterSlider(control, Modifier.fillMaxWidth(), title)
                                                        }
                                                    }
                                                }
                                            }
                                        } else visibleControls.forEach { control ->
                                            key(control.labelRes) {
                                                val label = stringResource(if (useSelector && control.labelRes == R.string.raw_editor_vignette) R.string.raw_editor_amount else control.labelRes)
                                                val currentControl by rememberUpdatedState(control)
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Box {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (useSelector) IconButton(
                                                                onClick = { selectorOpen = true },
                                                                modifier = Modifier.testTag("raw-editor-parameter-selector"),
                                                            ) { Icon(FujiIcons.UnfoldMore, stringResource(R.string.raw_editor_select_parameter)) }
                                                            Text(
                                                                text = "$label  ${control.formatValue(locale)}",
                                                                modifier = Modifier.testTag("raw-editor-parameter-${control.labelRes}").pointerInput(control.resetEnabled, control.defaultValue, useSelector) {
                                                                    detectTapGestures(
                                                                        onTap = { if (useSelector) selectorOpen = true },
                                                                        onDoubleTap = { if (currentControl.resetEnabled) currentControl.onChange(currentControl.defaultValue) },
                                                                    )
                                                                },
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                            )
                                                        }
                                                        DropdownMenu(expanded = useSelector && selectorOpen, onDismissRequest = { selectorOpen = false }) {
                                                            activeControls.forEachIndexed { index, parameter ->
                                                                DropdownMenuItem(
                                                                    text = { Text(stringResource(if (parameter.labelRes == R.string.raw_editor_vignette) R.string.raw_editor_amount else parameter.labelRes)) },
                                                                    onClick = { selectedControl = index; selectorOpen = false },
                                                                )
                                                            }
                                                        }
                                                    }
                                                    RawParameterSlider(control, Modifier.fillMaxWidth().padding(horizontal = 20.dp), label)
                                                }
                                            }
                                        }

                                        // Compact option carousel; Masks retains its entry action in both layouts.
                                        if (panel != 3 && (!wide || panel == 6)) Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .nestedScroll(optionScrollBoundary)
                                                .horizontalScroll(rememberScrollState())
                                                .testTag("raw-editor-options")
                                                .padding(horizontal = 16.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (panel == 6) {
                                                FilledTonalButton(onClick = { onMasks(true) }, enabled = editable && !state.rendering) { Text(stringResource(R.string.raw_editor_edit_masks, edits.masks.size)) }
                                            }
                                            if (panel == 1) {
                                                Column(Modifier.widthIn(min = 56.dp, max = 76.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = { onAdjust(edits.copy(temperature = 0f, tint = 0f)) }, enabled = editable,
                                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)) {
                                                        Icon(FujiIcons.PhotoCamera, stringResource(R.string.raw_editor_as_shot))
                                                    }
                                                    Text(stringResource(R.string.raw_editor_as_shot), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            params.forEachIndexed { index, param ->
                                                RawParamCircle(
                                                    param = param,
                                                    selected = index == activeIndex,
                                                    related = related[param.labelRes].orEmpty(),
                                                    optionLabelRes = optionLabels[param.labelRes] ?: param.labelRes,
                                                    onSelect = { selectedParam = index },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (!wide) categories()
                        }
                    }
                    if (wide) Row(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(1f).fillMaxSize())
                        controls(Modifier.width(360.dp).fillMaxHeight())
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
    val onChange: (Float) -> Unit,
) {
    val isEdited: Boolean get() = kotlin.math.abs(value - defaultValue) > 0.0005f

    private val precision: Float get() = displayScale * if (decimals == 1) 10f else 1f
    val steps: Int get() = (round((range.endInclusive - range.start) * precision).toInt() - 1).coerceAtLeast(0)

    fun snap(raw: Float): Float =
        (round(raw.coerceIn(range.start, range.endInclusive) * precision) / precision)
            .coerceIn(range.start, range.endInclusive)

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
    related: List<RawParam> = emptyList(),
    optionLabelRes: Int = param.labelRes,
) {
    val label = stringResource(optionLabelRes)
    val description = stringResource(R.string.raw_editor_parameter_controls, label)
    val edited = param.isEdited || related.any { it.isEdited }
    val resetLabel = stringResource(R.string.raw_editor_reset_parameter, label)
    val currentParam by rememberUpdatedState(param)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        edited -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        edited -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .widthIn(min = 56.dp, max = 76.dp)
            .alpha(if (param.enabled || param.resetEnabled) 1f else 0.45f)
            .pointerInput(param.resetEnabled, param.defaultValue, related.isNotEmpty()) {
                detectTapGestures(
                    onPress = { if (tryAwaitRelease()) currentOnSelect() },
                    onDoubleTap = if (related.isEmpty()) ({
                        currentOnSelect()
                        if (currentParam.resetEnabled) currentParam.onChange(currentParam.defaultValue)
                    }) else null,
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                onClick { currentOnSelect(); true }
                customActions = if (related.isEmpty() && param.resetEnabled) listOf(
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
            if (edited) {
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RawParameterSlider(
    param: RawParam,
    modifier: Modifier = Modifier,
    label: String = stringResource(param.labelRes),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val locale = LocalConfiguration.current.locales[0]
    val currentParam by rememberUpdatedState(param)
    val resetLabel = stringResource(R.string.raw_editor_reset_parameter, label)
    Slider(
        value = param.value,
        onValueChange = { currentParam.onChange(currentParam.snap(it)) },
        valueRange = param.range,
        steps = param.steps,
        enabled = param.enabled,
        interactionSource = interactionSource,
        modifier = modifier.testTag("raw-editor-slider-${param.labelRes}").semantics {
            contentDescription = label
            customActions = if (param.resetEnabled) listOf(
                CustomAccessibilityAction(resetLabel) { currentParam.onChange(currentParam.defaultValue); true },
            ) else emptyList()
        },
        thumb = {
            Label(
                label = {
                    PlainTooltip(Modifier.testTag("raw-editor-slider-value")) { Text(param.formatValue(locale)) }
                },
                interactionSource = interactionSource,
            ) {
                SliderDefaults.Thumb(interactionSource, enabled = param.enabled)
            }
        },
        track = { state ->
            if (param.range.start < 0f && param.range.endInclusive > 0f) {
                SliderDefaults.CenteredTrack(state, enabled = param.enabled)
            } else {
                SliderDefaults.Track(state, enabled = param.enabled)
            }
        },
    )
}
