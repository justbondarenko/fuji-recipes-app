package dev.bondarenko.fujirecipes.ui.raweditor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop

@Composable
internal fun RawCropDialog(
    bitmap: Bitmap, initial: RawCrop, onDismiss: () -> Unit, onApply: (RawCrop) -> Unit,
    imageAspect: Float = bitmap.width.toFloat() / bitmap.height,
) {
    var crop by remember(bitmap) { mutableStateOf(initial) }
    var ratio by remember(bitmap) { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentCrop by rememberUpdatedState(crop)
    val currentRatio by rememberUpdatedState(ratio)
    val aspect = bitmap.width.toFloat() / bitmap.height
    val description = stringResource(R.string.raw_editor_crop_hint)
    val labels = listOf(R.string.raw_editor_crop_left, R.string.raw_editor_crop_right,
        R.string.raw_editor_crop_up, R.string.raw_editor_crop_down,
        R.string.raw_editor_crop_smaller, R.string.raw_editor_crop_larger).map { stringResource(it) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.raw_editor_crop), style = MaterialTheme.typography.headlineSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
                Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    Canvas(Modifier.fillMaxSize().testTag("raw-crop-preview").onSizeChanged { canvasSize = it }
                        .semantics {
                            contentDescription = description
                            customActions = listOf(
                                CustomAccessibilityAction(labels[0]) { crop = crop.move(-.02f, 0f); true },
                                CustomAccessibilityAction(labels[1]) { crop = crop.move(.02f, 0f); true },
                                CustomAccessibilityAction(labels[2]) { crop = crop.move(0f, -.02f); true },
                                CustomAccessibilityAction(labels[3]) { crop = crop.move(0f, .02f); true },
                                CustomAccessibilityAction(labels[4]) { crop = scaleCrop(crop, .9f); true },
                                CustomAccessibilityAction(labels[5]) { crop = scaleCrop(crop, 1.1f); true },
                            )
                        }
                        .pointerInput(bitmap, canvasSize) {
                            val bounds = imageBounds(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), aspect)
                            var corner = -1
                            detectDragGestures(
                                onDragStart = { point ->
                                    val rect = cropBounds(currentCrop, bounds)
                                    val corners = listOf(rect.topLeft, rect.topRight, rect.bottomRight, rect.bottomLeft)
                                    corner = corners.indices.minBy { (point - corners[it]).getDistance() }
                                        .takeIf { (point - corners[it]).getDistance() <= 32.dp.toPx() }?.plus(1)
                                        ?: if (rect.contains(point)) 0 else -1
                                },
                                onDrag = { change, delta ->
                                    if (corner >= 0) {
                                        change.consume()
                                        val dx = delta.x / bounds.width
                                        val dy = delta.y / bounds.height
                                        crop = if (corner == 0) currentCrop.move(dx, dy) else currentCrop.resize(
                                            dx, dy, corner == 1 || corner == 4, corner <= 2,
                                            if (currentRatio == 0f) 0f else currentRatio / imageAspect,
                                        )
                                    }
                                },
                            )
                        }) {
                        val bounds = imageBounds(size, aspect)
                        val rect = cropBounds(crop, bounds)
                        val shade = Color.Black.copy(alpha = .6f)
                        drawRect(shade, bounds.topLeft, Size(bounds.width, rect.top - bounds.top))
                        drawRect(shade, Offset(bounds.left, rect.bottom), Size(bounds.width, bounds.bottom - rect.bottom))
                        drawRect(shade, Offset(bounds.left, rect.top), Size(rect.left - bounds.left, rect.height))
                        drawRect(shade, Offset(rect.right, rect.top), Size(bounds.right - rect.right, rect.height))
                        drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                        for (third in 1..2) {
                            val x = rect.left + rect.width * third / 3
                            val y = rect.top + rect.height * third / 3
                            drawLine(Color.White.copy(alpha = .5f), Offset(x, rect.top), Offset(x, rect.bottom))
                            drawLine(Color.White.copy(alpha = .5f), Offset(rect.left, y), Offset(rect.right, y))
                        }
                        for (point in listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)) drawCircle(Color.White, 6.dp.toPx(), point)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ratios = listOf(stringResource(R.string.raw_editor_crop_free) to 0f,
                        stringResource(R.string.raw_editor_crop_original) to imageAspect,
                        "1:1" to 1f, "3:2" to 1.5f, "2:3" to 2f / 3, "4:3" to 4f / 3,
                        "3:4" to .75f, "16:9" to 16f / 9, "9:16" to 9f / 16)
                    ratios.forEach { (name, value) ->
                        FilterChip(selected = ratio == value, onClick = {
                            ratio = value
                            if (value > 0) crop = crop.fitAspect(value, imageAspect)
                        }, label = { Text(name) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { crop = RawCrop(); ratio = 0f }) { Text(stringResource(R.string.raw_editor_reset)) }
                    Button(onClick = { onApply(crop) }) { Text(stringResource(R.string.raw_editor_crop_apply)) }
                }
            }
        }
    }
}

internal fun imageBounds(size: Size, aspect: Float): Rect {
    val width = minOf(size.width, size.height * aspect)
    val height = width / aspect
    return Rect((size.width - width) / 2, (size.height - height) / 2, (size.width + width) / 2, (size.height + height) / 2)
}

private fun cropBounds(crop: RawCrop, image: Rect) = Rect(image.left + crop.left * image.width,
    image.top + crop.top * image.height, image.left + crop.right * image.width, image.top + crop.bottom * image.height)

private fun scaleCrop(crop: RawCrop, factor: Float): RawCrop {
    val maximum = minOf(1 / crop.width, 1 / crop.height)
    val minimum = minOf(maximum, maxOf(.05f / crop.width, .05f / crop.height))
    val scale = factor.coerceIn(minimum, maximum)
    val width = crop.width * scale
    val height = crop.height * scale
    val left = ((crop.left + crop.right - width) / 2).coerceIn(0f, 1 - width)
    val top = ((crop.top + crop.bottom - height) / 2).coerceIn(0f, 1 - height)
    return RawCrop(left, top, left + width, top + height)
}
