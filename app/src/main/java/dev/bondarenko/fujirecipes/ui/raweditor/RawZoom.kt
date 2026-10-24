package dev.bondarenko.fujirecipes.ui.raweditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Coordinates in the scaled full-resolution output; 1f means one image pixel per screen pixel. */
data class RawViewport(val left: Int, val top: Int, val width: Int, val height: Int, val scale: Float) {
    init {
        require(left >= 0 && top >= 0 && width in 1..4096 && height in 1..4096 && scale.isFinite() && scale > 0 && scale <= 1)
    }
}

internal fun RawAdjustments.frameKey() = listOf(crop, quarterTurns, straighten, distortion,
    perspectiveVertical, perspectiveHorizontal, chromaticRed, chromaticBlue)

internal data class RawZoom(val scale: Float, val offset: Offset = Offset.Zero) {
    fun clamped(image: IntSize, view: IntSize): RawZoom {
        val fit = minOf(1f, view.width.toFloat() / image.width, view.height.toFloat() / image.height)
        val next = scale.coerceIn(fit, 1f)
        val x = (((image.width * next).roundToInt() - view.width) / 2f).coerceAtLeast(0f)
        val y = (((image.height * next).roundToInt() - view.height) / 2f).coerceAtLeast(0f)
        return RawZoom(next, Offset(offset.x.coerceIn(-x, x), offset.y.coerceIn(-y, y)))
    }

    fun transform(image: IntSize, view: IntSize, centroid: Offset, pan: Offset, factor: Float): RawZoom {
        val next = copy(scale = scale * factor).clamped(image, view).scale
        val anchor = centroid - Offset(view.width / 2f, view.height / 2f)
        return RawZoom(next, anchor - (anchor - offset) * (next / scale) + pan).clamped(image, view)
    }

    fun origin(image: IntSize, view: IntSize) = Offset(
        (view.width - (image.width * scale).roundToInt()) / 2f + offset.x,
        (view.height - (image.height * scale).roundToInt()) / 2f + offset.y,
    )

    fun viewport(image: IntSize, view: IntSize): RawViewport {
        val origin = origin(image, view)
        val left = floor(-origin.x).toInt().coerceAtLeast(0)
        val top = floor(-origin.y).toInt().coerceAtLeast(0)
        val width = (image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (image.height * scale).roundToInt().coerceAtLeast(1)
        return RawViewport(left, top,
            minOf(4096, width - left, ceil(view.width - origin.x).toInt() - left).coerceAtLeast(1),
            minOf(4096, height - top, ceil(view.height - origin.y).toInt() - top).coerceAtLeast(1), scale)
    }
}
