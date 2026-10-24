package dev.bondarenko.fujirecipes.ui.raweditor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
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
) {
    var showCamera by remember(state.name) { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current
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
                title = { Text(stringResource(R.string.raw_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FujiIcons.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.preview != null) TextButton(onClick = onOpen, enabled = !state.busy) {
                        Text(stringResource(R.string.raw_editor_open))
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
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(state.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.raw_editor_profile, state.simulation, state.fullWidth, state.fullHeight),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 700.dp
                    val preview: @Composable (Modifier) -> Unit = { modifier ->
                        Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
                            val bitmap = if (showCamera) state.cameraImage else state.preview
                            bitmap?.let {
                                Image(
                                    it.asImageBitmap(), stringResource(if (showCamera) R.string.raw_editor_camera_image else R.string.raw_editor_raw_image),
                                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                                )
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
                            TextButton(onClick = { showCamera = !showCamera }, enabled = !state.busy) {
                                Text(stringResource(if (showCamera) R.string.raw_editor_show_raw else R.string.raw_editor_show_camera))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(R.string.raw_editor_light, R.string.raw_editor_color, R.string.raw_editor_detail).forEachIndexed { index, label ->
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
                                            RawSlider(stringResource(R.string.raw_editor_radius), edits.sharpenRadius, .5f..3f, editable && edits.sharpening > 0, 1f, 1, " px") { onAdjust(edits.copy(sharpenRadius = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_threshold), edits.sharpenThreshold, 0f..1f, editable && edits.sharpening > 0) { onAdjust(edits.copy(sharpenThreshold = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_color_noise), edits.colorNoise, 0f..1f, editable) { onAdjust(edits.copy(colorNoise = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_luminance_noise), edits.luminanceNoise, 0f..1f, editable) { onAdjust(edits.copy(luminanceNoise = it)) }
                                            RawSlider(stringResource(R.string.raw_editor_noise_detail), edits.noiseDetail, 0f..1f, editable && (edits.colorNoise > 0 || edits.luminanceNoise > 0)) { onAdjust(edits.copy(noiseDetail = it)) }
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { onAdjust(RawAdjustments()) }, enabled = !state.busy && state.adjustments.isEdited,
                                ) { Text(stringResource(R.string.raw_editor_reset)) }
                                Button(onClick = onSave, enabled = !state.busy && !state.rendering) {
                                    Text(stringResource(R.string.raw_editor_save))
                                }
                            }
                            TextButton(onClick = { showInfo = true }) { Text(stringResource(R.string.raw_editor_profile_info)) }
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
    displayScale: Float = 100f, decimals: Int = 0, suffix: String = "", onChange: (Float) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            val sign = if (range.start < 0) "+" else ""
            val displayed = if (value == 0f) 0f else value
            Text(String.format(locale, "%$sign.${decimals}f$suffix", displayed * displayScale))
        }
        Slider(
            value = value, onValueChange = {
                val precision = displayScale * if (decimals == 1) 10f else 1f
                onChange(round(it * precision).toInt() / precision)
            }, valueRange = range, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
