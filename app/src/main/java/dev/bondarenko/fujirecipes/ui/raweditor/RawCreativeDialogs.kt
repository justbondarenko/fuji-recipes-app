package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.*
import dev.bondarenko.fujirecipes.ui.theme.icons.*
import kotlin.math.abs

@Composable
private fun CreativeDialog(
    title: String, help: String, onDismiss: () -> Unit, preview: @Composable (Modifier) -> Unit,
    controls: @Composable () -> Unit,
) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.safeDrawingPadding().padding(12.dp)) {
                val wide = maxWidth > maxHeight
                val controlHeight = maxHeight * .65f
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        RawEditorHelp(help, Modifier.testTag("raw-mask-help"))
                        TextButton(onDismiss) { Text(stringResource(R.string.raw_editor_done)) }
                    }
                    if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        preview(Modifier.weight(1f).fillMaxHeight())
                        Column(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) { controls() }
                    } else {
                        preview(Modifier.fillMaxWidth().weight(1f))
                        Column(Modifier.fillMaxWidth().heightIn(max = controlHeight).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) { controls() }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreativeParameters(params: List<RawParam>) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, params.lastIndex)
    val active = params[index]
    Text("${stringResource(active.labelRes)}  ${active.formatValue(LocalConfiguration.current.locales[0])}",
        style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth().height(48.dp).wrapContentHeight(Alignment.CenterVertically).padding(horizontal = 16.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    RawParameterWheel(active, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        params.forEachIndexed { i, param -> RawParamCircle(param, i == index, { selected = i }) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RawToneCurveControls(
    state: RawEditorState, onAdjust: (RawAdjustments) -> Unit, channel: Int, onChannel: (Int) -> Unit,
    enabled: Boolean, modifier: Modifier,
) {
    val curve = state.adjustments.curves.channels[channel]
    val labels = listOf(R.string.raw_editor_master, R.string.raw_editor_red, R.string.raw_editor_green, R.string.raw_editor_blue)
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp).testTag("raw-curve-channels").selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            labels.forEachIndexed { index, labelRes ->
                val label = stringResource(labelRes)
                ToggleButton(
                    checked = channel == index,
                    onCheckedChange = { onChannel(index) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).semantics { contentDescription = label; role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
        }
        Row(Modifier.fillMaxWidth().height(48.dp).testTag("raw-curve-actions"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            RawEditorHelp(stringResource(R.string.raw_editor_curve_hint), Modifier.testTag("raw-curve-help"))
            OutlinedButton({ onAdjust(state.adjustments.copy(curves = state.adjustments.curves.updated(channel, RawCurve()))) },
                enabled = enabled && curve != RawCurve()) {
                Icon(FujiIcons.Refresh, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.raw_editor_reset_curve))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun RawToneCurveGraph(state: RawEditorState, onAdjust: (RawAdjustments) -> Unit, channel: Int, modifier: Modifier, enabled: Boolean) {
    var selected by rememberSaveable(state.name, channel) { mutableIntStateOf(0) }
    val curves = state.adjustments.curves
    val curve = curves.channels[channel]
    val index = selected.coerceIn(0, curve.points.lastIndex)
    val point = curve.points[index]
    val editable = enabled && !state.busy
    var pressedOnCurve by remember(state.name, channel, editable) { mutableStateOf(false) }
    var draggingPoint by remember(state.name, channel, editable) { mutableStateOf(false) }
    val interacting = pressedOnCurve || draggingPoint
    val change: (RawCurve) -> Unit = { onAdjust(state.adjustments.copy(curves = curves.updated(channel, it))) }
    val currentCurve by rememberUpdatedState(curve)
    val currentChange by rememberUpdatedState(change)
    val grid = Color.White.copy(alpha = if (interacting) 0f else .3f)
    val colors = listOf(Color.LightGray, Color(0xffef6c6c), Color(0xff66bb6a), Color(0xff64a6f0))
    val description = stringResource(R.string.raw_editor_curve_hint)
    val actionLabels = listOf(R.string.raw_editor_previous_point, R.string.raw_editor_next_point,
        R.string.raw_editor_add_point, R.string.raw_editor_remove_point, R.string.raw_editor_reset_curve,
        R.string.raw_editor_curve_left, R.string.raw_editor_curve_right,
        R.string.raw_editor_curve_up, R.string.raw_editor_curve_down).map { stringResource(it) }
    fun removePoint(i: Int) {
        if (i in 1 until currentCurve.points.lastIndex) {
            currentChange(RawCurve(currentCurve.points.filterIndexed { n,_ -> n != i }))
            selected = (i-1).coerceAtLeast(0)
        } else currentChange(currentCurve.moved(i,currentCurve.points[i].x,currentCurve.points[i].x))
    }
    Canvas(modifier.background(Color.Black.copy(alpha = if (interacting) 0f else .08f)).border(1.dp, grid).testTag("raw-tone-curve").semantics {
        contentDescription = description
        stateDescription = "${index+1}/${curve.points.size}: ${(point.x*100).toInt()}%, ${(point.y*100).toInt()}%"
        if (editable) customActions = buildList {
            if (index > 0) add(CustomAccessibilityAction(actionLabels[0]) { selected = index-1; true })
            if (index < curve.points.lastIndex) add(CustomAccessibilityAction(actionLabels[1]) { selected = index+1; true })
            if (curve.points.size < 16) add(CustomAccessibilityAction(actionLabels[2]) {
                val gap = curve.points.zipWithNext().maxBy { (a,b) -> b.x-a.x }
                val x = (gap.first.x+gap.second.x)/2
                val next = (curve.points + CurvePoint(x,curve.sample(x))).sortedBy { it.x }
                selected = next.indexOfFirst { it.x == x }; change(RawCurve(next)); true
            })
            if (index in 1 until curve.points.lastIndex) add(CustomAccessibilityAction(actionLabels[3]) { removePoint(index); true })
            add(CustomAccessibilityAction(actionLabels[4]) { change(RawCurve()); selected = 0; true })
            if (index in 1 until curve.points.lastIndex) {
                add(CustomAccessibilityAction(actionLabels[5]) { change(curve.moved(index,point.x-.02f,point.y)); true })
                add(CustomAccessibilityAction(actionLabels[6]) { change(curve.moved(index,point.x+.02f,point.y)); true })
            }
            add(CustomAccessibilityAction(actionLabels[7]) { change(curve.moved(index,point.x,point.y+.02f)); true })
            add(CustomAccessibilityAction(actionLabels[8]) { change(curve.moved(index,point.x,point.y-.02f)); true })
        }
    }.pointerInput(state.name, channel, editable) {
        if (!editable) return@pointerInput
        fun nearest(position: Offset): Int? = currentCurve.points.indices.minBy { i ->
            val p = currentCurve.points[i]; Offset(p.x*size.width-position.x,(1-p.y)*size.height-position.y).getDistance()
        }.takeIf { i ->
            val p = currentCurve.points[i]; Offset(p.x*size.width-position.x,(1-p.y)*size.height-position.y).getDistance() <= 24.dp.toPx()
        }
        detectTapGestures(onPress = { position ->
            val x = (position.x / size.width).coerceIn(0f, 1f)
            pressedOnCurve = nearest(position) != null || abs(position.y - (1 - currentCurve.sample(x)) * size.height) <= 24.dp.toPx()
            try { tryAwaitRelease() } finally { pressedOnCurve = false }
        }, onTap = { position ->
            val existing = nearest(position)
            if (existing != null) selected = existing
            else {
                val x = (position.x/size.width).coerceIn(0f,1f)
                val y = currentCurve.sample(x)
                if (abs(position.y-(1-y)*size.height) <= 24.dp.toPx() && currentCurve.points.size < 16 && currentCurve.points.all { abs(it.x-x) >= .01f }) {
                    val next = (currentCurve.points + CurvePoint(x,y)).sortedBy { it.x }
                    selected = next.indexOfFirst { it.x == x }; currentChange(RawCurve(next))
                }
            }
        }, onDoubleTap = { position -> nearest(position)?.let { removePoint(it) } })
    }.pointerInput(state.name, channel, editable) {
        if (!editable) return@pointerInput
        var dragged = -1
        detectDragGestures(orientationLock = null, onDragStart = { down, _, _ ->
            val position = down.position
            dragged = currentCurve.points.indices.minBy { i ->
                val p = currentCurve.points[i]; Offset(p.x*size.width-position.x,(1-p.y)*size.height-position.y).getDistance()
            }.takeIf { i ->
                val p = currentCurve.points[i]; Offset(p.x*size.width-position.x,(1-p.y)*size.height-position.y).getDistance() <= 24.dp.toPx()
            } ?: -1
            draggingPoint = dragged >= 0
            if (dragged >= 0) selected = dragged
        }, onDragEnd = {
            draggingPoint = false
        }, onDragCancel = {
            draggingPoint = false
        }, onDrag = { pointer,_ ->
            if (dragged in currentCurve.points.indices) {
                pointer.consume()
                currentChange(currentCurve.moved(dragged,pointer.position.x/size.width,1-pointer.position.y/size.height))
            }
        })
    }) {
        for (i in 0..4) {
            val t = i/4f
            drawLine(grid,Offset(t*size.width,0f),Offset(t*size.width,size.height))
            drawLine(grid,Offset(0f,t*size.height),Offset(size.width,t*size.height))
        }
        drawLine(grid,Offset(0f,size.height),Offset(size.width,0f))
        val path = Path()
        for (i in 0..256) {
            val x = i/256f; val y = (1-curve.sample(x))*size.height
            if (i == 0) path.moveTo(0f,y) else path.lineTo(x*size.width,y)
        }
        drawPath(path,colors[channel],style = Stroke(2.dp.toPx()))
        curve.points.forEachIndexed { i,p -> drawCircle(colors[channel],if (i == index) 7.dp.toPx() else 4.dp.toPx(),Offset(p.x*size.width,(1-p.y)*size.height)) }
    }
}

@Composable
internal fun RawMaskDialog(
    state: RawEditorState, onAdjust: (RawAdjustments) -> Unit, onSelect: (Int) -> Unit,
    onDraw: (List<MaskPoint>, Float, Boolean) -> Unit, onDismiss: () -> Unit,
) {
    val edits = state.adjustments
    val masks = edits.masks
    val index = state.activeMask.coerceIn(0, masks.lastIndex.coerceAtLeast(0))
    val mask = masks.getOrNull(index)
    val editable = !state.busy
    var brushRadius by rememberSaveable { mutableFloatStateOf(.05f) }
    var erase by rememberSaveable { mutableStateOf(false) }
    var overlay by rememberSaveable { mutableStateOf(true) }
    var shapeControls by rememberSaveable { mutableStateOf(false) }
    var path by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val update: (RawMask) -> Unit = { next -> onAdjust(edits.copy(masks = masks.toMutableList().also { it[index] = next })) }
    val currentDraw by rememberUpdatedState(onDraw)
    val currentRadius by rememberUpdatedState(brushRadius)
    val currentErase by rememberUpdatedState(erase)
    val primary = MaterialTheme.colorScheme.primary
    val hint = stringResource(when (mask?.kind) {
        RawMaskKind.BRUSH -> R.string.raw_editor_brush_hint
        RawMaskKind.LINEAR -> R.string.raw_editor_linear_hint
        else -> R.string.raw_editor_radial_hint
    })
    CreativeDialog(stringResource(R.string.raw_editor_masks), if (mask == null) stringResource(R.string.raw_editor_mask_add_hint) else hint, onDismiss, preview = { modifier ->
                Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
                    Image(state.preview!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    if (overlay) state.maskOverlay?.let {
                        Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(primary))
                    }
                    if (mask != null) Canvas(Modifier.fillMaxSize().testTag("raw-mask-preview").semantics { contentDescription = hint }
                        .pointerInput(index, mask.kind, editable, state.rendering) {
                            if (!editable || state.rendering) return@pointerInput
                            val bounds = imageBounds(Size(size.width.toFloat(),size.height.toFloat()), state.preview.width.toFloat()/state.preview.height)
                            fun normalized(p: Offset) = MaskPoint(((p.x-bounds.left)/bounds.width).coerceIn(0f,1f), ((p.y-bounds.top)/bounds.height).coerceIn(0f,1f))
                            fun commit() {
                                if (path.isNotEmpty()) {
                                    // ponytail: cap each gesture at 256 evenly spaced samples; split unusually long strokes into gestures.
                                    val sampled = if (path.size <= 256) path else List(256) { path[it*(path.lastIndex)/255] }
                                    currentDraw(sampled.map(::normalized),currentRadius,currentErase)
                                }
                                path = emptyList()
                            }
                            detectDragGestures(orientationLock = null, onDragStart = { down, _, _ ->
                                    val p = down.position
                                    path = if (bounds.contains(p)) listOf(p) else emptyList()
                                },
                                onDrag = { change,_ ->
                                    if (path.isNotEmpty()) {
                                        change.consume()
                                        // Bound temporary feedback as well as the saved stroke.
                                        path = (if (path.size >= 512) path.filterIndexed { i,_ -> i % 2 == 0 } else path) + change.position.coerceIn(bounds)
                                    }
                                }, onDragEnd = { commit() }, onDragCancel = { path = emptyList() })
                        }.pointerInput(index, mask.kind, editable, state.rendering) {
                            if (!editable || state.rendering || mask.kind != RawMaskKind.BRUSH) return@pointerInput
                            val bounds = imageBounds(Size(size.width.toFloat(),size.height.toFloat()), state.preview.width.toFloat()/state.preview.height)
                            detectTapGestures { p -> if (bounds.contains(p)) currentDraw(listOf(MaskPoint((p.x-bounds.left)/bounds.width,(p.y-bounds.top)/bounds.height)),currentRadius,currentErase) }
                        }) {
                        if (path.isNotEmpty()) {
                            if (mask.kind == RawMaskKind.BRUSH) {
                                val bounds = imageBounds(size,state.preview.width.toFloat()/state.preview.height)
                                val radius = brushRadius*minOf(bounds.width,bounds.height)
                                path.forEach { drawCircle(primary.copy(alpha=.35f),radius,it) }
                            } else {
                                drawLine(primary,path.first(),path.last(),3.dp.toPx())
                                drawCircle(primary,5.dp.toPx(),path.first()); drawCircle(primary,5.dp.toPx(),path.last())
                            }
                        }
                    }
                    state.error?.let { Snackbar(Modifier.align(Alignment.BottomCenter).padding(8.dp)) { Text(it) } }
                }
    }) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    masks.forEachIndexed { i,m -> FilterChip(index == i, { onSelect(i) }, { Text("${i+1} · ${stringResource(maskLabel(m.kind))}") }, enabled = editable) }
                    RawMaskKind.entries.forEach { kind ->
                        TextButton({
                            onSelect(masks.size)
                            onAdjust(edits.copy(masks = masks + RawMask(kind)))
                        }, enabled = editable && masks.size < 8) { Text("+ ${stringResource(maskLabel(kind))}") }
                    }
                }
                Box(Modifier.height(4.dp).fillMaxWidth()) { if (state.rendering) LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (mask != null) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(mask.enabled, { update(mask.copy(enabled = !mask.enabled)) }, { Text(stringResource(R.string.raw_editor_mask_enabled)) }, enabled = editable)
                        FilterChip(mask.inverted, { update(mask.copy(inverted = !mask.inverted)) }, { Text(stringResource(R.string.raw_editor_mask_invert)) }, enabled = editable)
                        FilterChip(overlay, { overlay = !overlay }, { Text(stringResource(R.string.raw_editor_mask_overlay)) })
                        if (mask.kind == RawMaskKind.BRUSH) {
                            FilterChip(erase, { erase = !erase }, { Text(stringResource(R.string.raw_editor_mask_erase)) })
                            TextButton({ update(mask.copy(strokes = mask.strokes.dropLast(1))) }, enabled = editable && mask.strokes.isNotEmpty()) { Text(stringResource(R.string.raw_editor_mask_undo)) }
                            // Accessible alternative to drawing a stroke with a touchscreen.
                            TextButton({ update(mask.copy(strokes = mask.strokes + RawBrushStroke(listOf(MaskPoint(.5f,.5f)),brushRadius,erase))) },
                                enabled = editable && mask.strokes.size < 64 && masks.sumOf { m -> m.strokes.sumOf { it.points.size } } < 4096) { Text(stringResource(R.string.raw_editor_mask_center_dab)) }
                        }
                        TextButton({ onSelect((index-1).coerceAtLeast(0)); onAdjust(edits.copy(masks = masks.filterIndexed { i,_ -> i != index })) }, enabled = editable) { Text(stringResource(R.string.raw_editor_mask_delete)) }
                    }
                    Row {
                        FilterChip(!shapeControls, { shapeControls = false }, { Text(stringResource(R.string.raw_editor_mask_adjustments)) })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(shapeControls, { shapeControls = true }, { Text(stringResource(R.string.raw_editor_mask_shape)) })
                    }
                    if (shapeControls) CreativeParameters(buildList {
                        add(RawParam(R.string.raw_editor_mask_opacity,FujiIcons.Contrast,mask.opacity,0f..1f,editable,defaultValue=1f) { update(mask.copy(opacity=it)) })
                        add(RawParam(R.string.raw_editor_mask_feather,FujiIcons.Deblur,mask.feather,0f..1f,editable,defaultValue=.5f) { update(mask.copy(feather=it)) })
                        if (mask.kind == RawMaskKind.BRUSH) add(RawParam(R.string.raw_editor_mask_brush_size,FujiIcons.Grain,brushRadius,.005f.. .5f,editable,decimals=1,defaultValue=.05f) { brushRadius=it })
                        else {
                            add(RawParam(R.string.raw_editor_mask_start_x,FujiIcons.Tune,mask.start.x,0f..1f,editable,defaultValue=.5f) { update(mask.copy(start=mask.start.copy(x=it))) })
                            add(RawParam(R.string.raw_editor_mask_start_y,FujiIcons.Tune,mask.start.y,0f..1f,editable,defaultValue=.5f) { update(mask.copy(start=mask.start.copy(y=it))) })
                            add(RawParam(R.string.raw_editor_mask_end_x,FujiIcons.Tune,mask.end.x,0f..1f,editable,defaultValue=.8f) { update(mask.copy(end=mask.end.copy(x=it))) })
                            add(RawParam(R.string.raw_editor_mask_end_y,FujiIcons.Tune,mask.end.y,0f..1f,editable,defaultValue=.75f) { update(mask.copy(end=mask.end.copy(y=it))) })
                        }
                    }) else {
                        val a = mask.adjustments
                        CreativeParameters(listOf(
                            RawParam(R.string.raw_editor_exposure,FujiIcons.Exposure,a.exposure,-3f..3f,editable,1f,1," EV") { update(mask.copy(adjustments=a.copy(exposure=it))) },
                            RawParam(R.string.raw_editor_shadows,FujiIcons.Tonality,a.shadows,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(shadows=it))) },
                            RawParam(R.string.raw_editor_highlights,FujiIcons.StarShine,a.highlights,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(highlights=it))) },
                            RawParam(R.string.raw_editor_temperature,FujiIcons.WbAuto,a.temperature,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(temperature=it))) },
                            RawParam(R.string.raw_editor_tint,FujiIcons.DiscoverTune,a.tint,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(tint=it))) },
                            RawParam(R.string.raw_editor_contrast,FujiIcons.Contrast,a.contrast,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(contrast=it))) },
                            RawParam(R.string.raw_editor_saturation,FujiIcons.Palette,a.saturation,-1f..1f,editable) { update(mask.copy(adjustments=a.copy(saturation=it))) },
                        ))
                    }
                }
    }
}

private fun maskLabel(kind: RawMaskKind) = when(kind) {
    RawMaskKind.BRUSH -> R.string.raw_editor_mask_brush
    RawMaskKind.LINEAR -> R.string.raw_editor_mask_linear
    RawMaskKind.RADIAL -> R.string.raw_editor_mask_radial
}
private fun Offset.coerceIn(rect: androidx.compose.ui.geometry.Rect) = Offset(x.coerceIn(rect.left,rect.right),y.coerceIn(rect.top,rect.bottom))
