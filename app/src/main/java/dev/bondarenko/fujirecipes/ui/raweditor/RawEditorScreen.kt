package dev.bondarenko.fujirecipes.ui.raweditor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import kotlin.math.round

@Composable
fun RawEditorRouteContent(onBack: () -> Unit) {
    val model: RawEditorViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
        onCrop = model::prepareCrop,
        onDismissCrop = model::dismissCrop,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawEditorScreen(
    state: RawEditorState,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onAdjust: (RawAdjustments) -> Unit,
    onSave: () -> Unit,
    onCrop: () -> Unit = {},
    onDismissCrop: () -> Unit = {},
) {
    var showCamera by remember(state.name, state.busy, state.cameraImage) { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableIntStateOf(0) }
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                onClick = { onAdjust(RawAdjustments()) },
                                enabled = !state.busy && state.adjustments.isEdited,
                            ) { Text(stringResource(R.string.raw_editor_reset)) }
                            Button(
                                onClick = onSave,
                                enabled = !state.busy && !state.rendering,
                            ) { Text(stringResource(R.string.raw_editor_save)) }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                if (state.busy || state.rendering) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.busy && state.preview == null) Text(
                stringResource(R.string.raw_editor_processing),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            if (state.preview == null) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.raw_editor_intro), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.raw_editor_intro_body), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onOpen, enabled = !state.busy) { Text(stringResource(R.string.raw_editor_open)) }
                    Text(stringResource(R.string.raw_editor_profile_note), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showInfo = true }) { Text(stringResource(R.string.raw_editor_profile_info)) }
                }
            } else {
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 700.dp
                    val preview: @Composable (Modifier) -> Unit = { modifier ->
                        val compareLabel = stringResource(if (showCamera) R.string.raw_editor_show_raw else R.string.raw_editor_show_camera)
                        Box(
                            modifier.background(Color.Black).testTag("raw-editor-preview")
                                .pointerInput(state.cameraImage, state.busy) {
                                    detectTapGestures(onPress = {
                                        if (!state.busy && state.cameraImage != null) {
                                            showCamera = true
                                            try { tryAwaitRelease() } finally { showCamera = false }
                                        }
                                    })
                                }
                                .semantics(mergeDescendants = true) {
                                    customActions = if (!state.busy && state.cameraImage != null) listOf(
                                        CustomAccessibilityAction(compareLabel) { showCamera = !showCamera; true },
                                    ) else emptyList()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val bitmap = if (showCamera) state.cameraImage else state.preview
                            bitmap?.let {
                                Image(
                                    it.asImageBitmap(), stringResource(if (showCamera) R.string.raw_editor_camera_image else R.string.raw_editor_raw_image),
                                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                                )
                                if (panel == 5 && !showCamera) Canvas(Modifier.fillMaxSize().testTag("raw-editor-optics-grid")) {
                                    val bounds = imageBounds(size, it.width.toFloat() / it.height)
                                    for (line in 1..3) {
                                        val x = bounds.left + bounds.width * line / 4
                                        val y = bounds.top + bounds.height * line / 4
                                        drawLine(Color.White.copy(alpha = .35f), Offset(x, bounds.top), Offset(x, bounds.bottom))
                                        drawLine(Color.White.copy(alpha = .35f), Offset(bounds.left, y), Offset(bounds.right, y))
                                    }
                                }
                            }
                            if (showCamera || state.busy) Surface(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                color = Color.Black.copy(alpha = .8f), contentColor = Color.White,
                            ) {
                                Text(
                                    stringResource(if (state.busy) R.string.raw_editor_processing else R.string.raw_editor_comparison_note),
                                    Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    val controls: @Composable (Modifier) -> Unit = { modifier ->
                        val edits = state.adjustments
                        val editable = !state.busy && !showCamera
                        Column(
                            modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(R.string.raw_editor_light, R.string.raw_editor_color, R.string.raw_editor_detail,
                                    R.string.raw_editor_effects, R.string.raw_editor_framing, R.string.raw_editor_optics).forEachIndexed { index, label ->
                                    FilterChip(selected = panel == index, onClick = { panel = index }, label = { Text(stringResource(label)) })
                                }
                            }
                            key(panel) {
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                    when (panel) {
                                        0 -> {
                                            RawSlider(stringResource(R.string.raw_editor_exposure), edits.exposure, -3f..3f, editable, 1f, 1, " EV") { onAdjust(edits.copy(exposure = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_shadows), edits.shadows, -1f..1f, editable) { onAdjust(edits.copy(shadows = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_highlights), edits.highlights, -1f..1f, editable) { onAdjust(edits.copy(highlights = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_contrast), edits.contrast, -1f..1f, editable) { onAdjust(edits.copy(contrast = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_whites), edits.whites, -1f..1f, editable) { onAdjust(edits.copy(whites = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_blacks), edits.blacks, -1f..1f, editable) { onAdjust(edits.copy(blacks = it)) }
                                        }
                                        1 -> {
                                            Text(stringResource(R.string.raw_editor_wb_relative), style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { onAdjust(edits.copy(temperature = 0f, tint = 0f)) }, enabled = editable) {
                                                Text(stringResource(R.string.raw_editor_as_shot))
                                            }
                                            RawSlider(stringResource(R.string.raw_editor_temperature), edits.temperature, -1f..1f, editable) { onAdjust(edits.copy(temperature = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_tint), edits.tint, -1f..1f, editable) { onAdjust(edits.copy(tint = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_saturation), edits.saturation, -1f..1f, editable) { onAdjust(edits.copy(saturation = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_vibrance), edits.vibrance, -1f..1f, editable) { onAdjust(edits.copy(vibrance = it)) }
                                        }
                                        2 -> {
                                            RawSlider(stringResource(R.string.raw_editor_sharpening), edits.sharpening, 0f..1f, editable) { onAdjust(edits.copy(sharpening = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_radius), edits.sharpenRadius, .5f..3f, editable && edits.sharpening > 0, 1f, 1, " px", defaultValue = 1f, resetEnabled = editable) { onAdjust(edits.copy(sharpenRadius = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_threshold), edits.sharpenThreshold, 0f..1f, editable && edits.sharpening > 0, resetEnabled = editable) { onAdjust(edits.copy(sharpenThreshold = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_color_noise), edits.colorNoise, 0f..1f, editable) { onAdjust(edits.copy(colorNoise = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_luminance_noise), edits.luminanceNoise, 0f..1f, editable) { onAdjust(edits.copy(luminanceNoise = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_noise_detail), edits.noiseDetail, 0f..1f, editable && (edits.colorNoise > 0 || edits.luminanceNoise > 0), defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(noiseDetail = it)) }
                                        }
                                        3 -> {
                                            RawSlider(stringResource(R.string.raw_editor_clarity), edits.clarity, -1f..1f, editable) { onAdjust(edits.copy(clarity = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_texture), edits.texture, -1f..1f, editable) { onAdjust(edits.copy(texture = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_dehaze), edits.dehaze, -1f..1f, editable) { onAdjust(edits.copy(dehaze = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_vignette), edits.vignette, -1f..1f, editable) { onAdjust(edits.copy(vignette = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_vignette_midpoint), edits.vignetteMidpoint, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteMidpoint = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_vignette_roundness), edits.vignetteRoundness, -1f..1f, editable && edits.vignette != 0f, resetEnabled = editable) { onAdjust(edits.copy(vignetteRoundness = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_vignette_feather), edits.vignetteFeather, 0f..1f, editable && edits.vignette != 0f, defaultValue = .5f, resetEnabled = editable) { onAdjust(edits.copy(vignetteFeather = it)) }
                                        }
                                        4 -> {
                                            Button(onClick = onCrop, enabled = editable && !state.rendering) { Text(stringResource(R.string.raw_editor_crop)) }
                                            TextButton(onClick = { onAdjust(edits.copy(crop = RawCrop())) }, enabled = editable && edits.crop != RawCrop()) {
                                                Text(stringResource(R.string.raw_editor_crop_reset))
                                            }
                                            RawSlider(stringResource(R.string.raw_editor_rotation), edits.quarterTurns.toFloat(), 0f..3f, editable,
                                                displayScale = 90f, suffix = "°", steps = 2) {
                                                onAdjust(edits.rotated(it.toInt()))
                                            }
                                            RawSlider(stringResource(R.string.raw_editor_straighten), edits.straighten, -15f..15f, editable,
                                                displayScale = 1f, decimals = 1, suffix = "°") { onAdjust(edits.copy(straighten = it)) }
                                            Text(stringResource(R.string.raw_editor_straighten_note), style = MaterialTheme.typography.bodySmall)
                                        }
                                        5 -> {
                                            Text(stringResource(R.string.raw_editor_optics_note), style = MaterialTheme.typography.bodySmall)
                                            RawSlider(stringResource(R.string.raw_editor_distortion), edits.distortion, -1f..1f, editable) { onAdjust(edits.copy(distortion = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_perspective_vertical), edits.perspectiveVertical, -1f..1f, editable) { onAdjust(edits.copy(perspectiveVertical = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_perspective_horizontal), edits.perspectiveHorizontal, -1f..1f, editable) { onAdjust(edits.copy(perspectiveHorizontal = it)) }
                                            Text(stringResource(R.string.raw_editor_chromatic_note), style = MaterialTheme.typography.bodySmall)
                                            RawSlider(stringResource(R.string.raw_editor_chromatic_red), edits.chromaticRed, -1f..1f, editable) { onAdjust(edits.copy(chromaticRed = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_chromatic_blue), edits.chromaticBlue, -1f..1f, editable) { onAdjust(edits.copy(chromaticBlue = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_defringe_purple), edits.defringePurple, 0f..1f, editable) { onAdjust(edits.copy(defringePurple = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_defringe_green), edits.defringeGreen, 0f..1f, editable) { onAdjust(edits.copy(defringeGreen = it)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (wide) Row(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(1f).fillMaxSize())
                        controls(Modifier.widthIn(max = 340.dp).fillMaxSize())
                    } else Column(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(1f).fillMaxWidth())
                        controls(Modifier.fillMaxWidth().heightIn(max = this@BoxWithConstraints.maxHeight * .58f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RawSlider(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean,
    displayScale: Float = 100f, decimals: Int = 0, suffix: String = "",
    defaultValue: Float = 0f, resetEnabled: Boolean = enabled, steps: Int = 0, onChange: (Float) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val currentOnChange by rememberUpdatedState(onChange)
    val resetLabel = stringResource(R.string.raw_editor_reset_parameter, label)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .pointerInput(resetEnabled, defaultValue) {
                        if (resetEnabled) detectTapGestures(onDoubleTap = { currentOnChange(defaultValue) })
                    }
                    .semantics {
                        customActions = if (resetEnabled) listOf(
                            CustomAccessibilityAction(resetLabel) { currentOnChange(defaultValue); true },
                        ) else emptyList()
                    }.wrapContentHeight(),
            )
            val sign = if (range.start < 0) "+" else ""
            val displayed = if (value == 0f) 0f else value
            Text(String.format(locale, "%$sign.${decimals}f$suffix", displayed * displayScale))
        }
        Slider(
            value = value, onValueChange = {
                val precision = displayScale * if (decimals == 1) 10f else 1f
                onChange(round(it * precision).toInt() / precision)
            }, valueRange = range, enabled = enabled, steps = steps,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
