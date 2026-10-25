package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bondarenko.fujirecipes.R
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

/** Keep horizontal control drags/flings from paging the surrounding editor. */
internal object RawControlScrollBoundary : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
}

@Composable
internal fun RawParameterWheel(
    param: RawParam,
    modifier: Modifier = Modifier,
    label: String = stringResource(param.labelRes),
) = key(param.labelRes, param.range) {
    val intervals = param.steps + 1
    val step = (param.range.endInclusive - param.range.start) / intervals
    fun index(value: Float) = ((value - param.range.start) / step).roundToInt().coerceIn(0, intervals)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = index(param.value))
    val currentParam by rememberUpdatedState(param)
    var lastWheelValue by remember { mutableFloatStateOf(param.value) }
    val primary = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = tickColor)
    val measurer = rememberTextMeasurer()
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale, param.decimals) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = param.decimals } }
    val resetLabel = stringResource(R.string.raw_editor_reset_parameter, label)

    fun setValue(value: Float) {
        val snapped = currentParam.snap(value)
        currentParam.onChange(snapped)
        list.requestScrollToItem(index(snapped))
    }

    // Preserve finger/fling motion; reposition only for external edits or disabling.
    LaunchedEffect(param.value, param.enabled) {
        if (!param.enabled || !list.isScrollInProgress || param.value != lastWheelValue) list.requestScrollToItem(index(param.value))
    }
    LaunchedEffect(list) {
        snapshotFlow {
            val info = list.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
        }.distinctUntilChanged().collect { selected ->
            if (selected != null && currentParam.enabled && list.isScrollInProgress) {
                val value = currentParam.snap(param.range.start + selected * step)
                if (value != currentParam.value) {
                    lastWheelValue = value
                    currentParam.onChange(value)
                }
            }
        }
    }
    BoxWithConstraints(modifier.height(56.dp).alpha(if (param.enabled) 1f else .38f)
        .testTag("raw-editor-wheel-${param.labelRes}")
        .semantics(mergeDescendants = true) {
            contentDescription = label
            stateDescription = param.formatValue(locale)
            progressBarRangeInfo = ProgressBarRangeInfo(param.value, param.range, param.steps)
            if (param.enabled) setProgress {
                if (!it.isFinite()) false else { setValue(it); true }
            } else disabled()
            customActions = if (param.resetEnabled) listOf(CustomAccessibilityAction(resetLabel) { setValue(currentParam.defaultValue); true }) else emptyList()
        }
        .onKeyEvent { event ->
            if (!param.enabled || event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.DirectionLeft -> { setValue(currentParam.value - step); true }
                Key.DirectionRight -> { setValue(currentParam.value + step); true }
                Key.MoveHome -> { setValue(param.range.start); true }
                Key.MoveEnd -> { setValue(param.range.endInclusive); true }
                else -> false
            }
        }.focusable(param.enabled)) {
        val tickWidth = 8.dp
        LazyRow(
            state = list,
            userScrollEnabled = param.enabled,
            flingBehavior = rememberSnapFlingBehavior(list, SnapPosition.Center),
            contentPadding = PaddingValues(horizontal = ((maxWidth - tickWidth) / 2).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize().nestedScroll(RawControlScrollBoundary).clearAndSetSemantics {},
        ) {
            items(intervals + 1) { tick ->
                val value = param.snap(param.range.start + tick * step)
                val major = (value / step).roundToInt() % 10 == 0 || tick == 0 || tick == intervals
                val text = if (major) measurer.measure(AnnotatedString(numbers.format(if (value == 0f) 0f else value * param.displayScale)), textStyle) else null
                Canvas(Modifier.width(tickWidth).fillMaxHeight()) {
                    val halfHeight = if (major) 10.dp.toPx() else 5.dp.toPx()
                    drawLine(tickColor.copy(alpha = if (major) .7f else .35f),
                        Offset(size.width / 2, 20.dp.toPx() - halfHeight), Offset(size.width / 2, 20.dp.toPx() + halfHeight),
                        1.dp.toPx(), StrokeCap.Round)
                    text?.let { drawText(it, topLeft = Offset((size.width - it.size.width) / 2, 36.dp.toPx())) }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawLine(primary, Offset(size.width / 2, 3.dp.toPx()), Offset(size.width / 2, 33.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
