package dev.bondarenko.fujirecipes.data.raweditor

import kotlin.math.abs
import kotlin.math.min

data class CurvePoint(val x: Float, val y: Float)

data class RawCurve(val points: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))) {
    init {
        require(points.size in 2..16 && points.first().x == 0f && points.last().x == 1f)
        require(points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
        require(points.zipWithNext().all { (a, b) -> b.x - a.x >= .00999f })
    }
    val isIdentity get() = points.all { it.x == it.y }

    fun moved(index: Int, x: Float, y: Float): RawCurve {
        val position = when (index) {
            0 -> 0f
            points.lastIndex -> 1f
            else -> {
                val low = points[index - 1].x + .01f
                val high = points[index + 1].x - .01f
                if (low > high) points[index].x else x.coerceIn(low, high)
            }
        }
        return RawCurve(points.toMutableList().also { it[index] = CurvePoint(position, y.coerceIn(0f, 1f)) })
    }

    /** Conservative Hermite tangents keep every segment within its two control values. */
    fun sample(input: Float): Float {
        if (isIdentity) return input.coerceIn(0f, 1f)
        val x = input.coerceIn(0f, 1f)
        val i = points.indexOfLast { it.x <= x }.coerceAtMost(points.lastIndex - 1)
        fun slope(index: Int): Float = (points[index + 1].y - points[index].y) / (points[index + 1].x - points[index].x)
        fun tangent(index: Int): Float {
            if (index == 0) return slope(0)
            if (index == points.lastIndex) return slope(index - 1)
            val a = slope(index - 1); val b = slope(index)
            return if (a * b <= 0) 0f else kotlin.math.sign(a) * min(abs(a), abs(b))
        }
        val a = points[i]; val b = points[i + 1]
        val h = b.x - a.x; val t = (x - a.x) / h
        return ((2*t*t*t - 3*t*t + 1)*a.y + (t*t*t - 2*t*t + t)*h*tangent(i) +
            (-2*t*t*t + 3*t*t)*b.y + (t*t*t - t*t)*h*tangent(i + 1)).coerceIn(0f, 1f)
    }
}

data class RawCurves(
    val master: RawCurve = RawCurve(), val red: RawCurve = RawCurve(),
    val green: RawCurve = RawCurve(), val blue: RawCurve = RawCurve(),
) {
    val channels get() = listOf(master, red, green, blue)
    val isEdited get() = channels.any { !it.isIdentity }
    fun updated(channel: Int, curve: RawCurve): RawCurves = when (channel) {
        0 -> copy(master = curve); 1 -> copy(red = curve); 2 -> copy(green = curve); 3 -> copy(blue = curve)
        else -> error("Invalid curve channel")
    }
}

enum class RawMaskKind { BRUSH, LINEAR, RADIAL }
data class MaskPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) }
}
data class RawBrushStroke(val points: List<MaskPoint>, val radius: Float, val erase: Boolean = false) {
    init { require(points.size in 1..256 && radius.isFinite() && radius in .0005f..1f) }
}
data class RawLocalAdjustments(
    val exposure: Float = 0f, val shadows: Float = 0f, val highlights: Float = 0f,
    val temperature: Float = 0f, val tint: Float = 0f, val contrast: Float = 0f, val saturation: Float = 0f,
) {
    fun values() = listOf(exposure, shadows, highlights, temperature, tint, contrast, saturation)
    init {
        require(values().all(Float::isFinite) && exposure in -3f..3f && values().drop(1).all { it in -1f..1f })
    }
}
data class RawMask(
    val kind: RawMaskKind,
    val start: MaskPoint = MaskPoint(.5f, .5f), val end: MaskPoint = MaskPoint(.8f, .75f),
    val feather: Float = .5f, val opacity: Float = 1f, val inverted: Boolean = false, val enabled: Boolean = true,
    val adjustments: RawLocalAdjustments = RawLocalAdjustments(), val strokes: List<RawBrushStroke> = emptyList(),
) {
    init { require(feather.isFinite() && opacity.isFinite() && feather in 0f..1f && opacity in 0f..1f && strokes.size <= 64) }
}

/** Bounded float payload is shared with JNI and SavedStateHandle, including older fixed arrays. */
internal fun RawAdjustments.creativeValues(): List<Float> = buildList {
    add(1f) // Creative payload version.
    curves.channels.forEach { curve ->
        add(curve.points.size.toFloat())
        curve.points.forEach { add(it.x); add(it.y) }
    }
    require(masks.size <= 8 && masks.sumOf { m -> m.strokes.sumOf { it.points.size } } <= 4096)
    add(masks.size.toFloat())
    masks.forEach { mask ->
        add(mask.kind.ordinal.toFloat()); add(if (mask.enabled) 1f else 0f); add(if (mask.inverted) 1f else 0f)
        add(mask.opacity); add(mask.feather)
        add(mask.start.x); add(mask.start.y); add(mask.end.x); add(mask.end.y)
        addAll(mask.adjustments.values()); add(mask.strokes.size.toFloat())
        mask.strokes.forEach { stroke ->
            add(stroke.radius); add(if (stroke.erase) 1f else 0f); add(stroke.points.size.toFloat())
            stroke.points.forEach { add(it.x); add(it.y) }
        }
    }
}

internal class RawCreativeReader(private val values: FloatArray, private var position: Int = 39) {
    init { require(values.size <= 16384 && values.all(Float::isFinite)) }
    private fun next(): Float { require(position < values.size); return values[position++] }
    private fun count(maximum: Int): Int = next().let { require(it >= 0 && it <= maximum && it == it.toInt().toFloat()); it.toInt() }
    private fun flag(): Boolean = count(1) == 1
    private fun point() = MaskPoint(next(), next())
    fun read(base: RawAdjustments): RawAdjustments {
        require(next() == 1f)
        val curves = List(4) { RawCurve(List(count(16)) { CurvePoint(next(), next()) }) }
        var totalPoints = 0
        val masks = List(count(8)) {
            val kind = RawMaskKind.entries[count(2)]
            val enabled = flag(); val inverted = flag(); val opacity = next(); val feather = next()
            val start = point(); val end = point()
            val local = RawLocalAdjustments(next(), next(), next(), next(), next(), next(), next())
            val strokes = List(count(64)) {
                val radius = next(); val erase = flag(); val count = count(256)
                totalPoints += count; require(totalPoints <= 4096)
                RawBrushStroke(List(count) { point() }, radius, erase)
            }
            RawMask(kind, start, end, feather, opacity, inverted, enabled, local, strokes)
        }
        require(position == values.size)
        return base.copy(curves = RawCurves(curves[0], curves[1], curves[2], curves[3]), masks = masks)
    }
}
