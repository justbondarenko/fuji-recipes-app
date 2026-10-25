package dev.bondarenko.fujirecipes.data.raweditor

import dev.bondarenko.fujirecipes.data.photo.MAX_PHOTO_BYTES
import java.io.File
import java.io.RandomAccessFile

/** The embedded camera JPEG supplies metadata and comparison, never the editable pixels. */
fun readRafPreview(file: File): ByteArray = RandomAccessFile(file, "r").use { input ->
    require(input.length() >= 108) { "The RAF header is incomplete." }
    val signature = ByteArray(16).also(input::readFully)
    require(signature.contentEquals("FUJIFILMCCD-RAW ".toByteArray(Charsets.US_ASCII))) {
        "Choose a Fujifilm RAF file."
    }
    input.seek(84)
    val offset = input.readInt().toLong() and 0xffffffffL
    val length = input.readInt().toLong() and 0xffffffffL
    require(offset >= 108 && length in 2..MAX_PHOTO_BYTES && offset + length <= input.length()) {
        "The RAF has no readable camera preview."
    }
    input.seek(offset)
    ByteArray(length.toInt()).also {
        input.readFully(it)
        require(it[0] == 0xff.toByte() && it[1] == 0xd8.toByte()) { "The RAF preview is not a JPEG." }
    }
}

/** Bundled profile IDs match the existing MakerNote parser's filmSimulation field. */
val rawColorProfileIds = setOf(
    "provia", "velvia", "astia", "classic-chrome", "pro-neg-hi", "pro-neg-std",
    "classic-negative", "nostalgic-negative", "reala-ace", "eterna", "eterna-bleach-bypass",
)

data class RawAdjustments(
    val exposure: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val contrast: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val sharpening: Float = 0f,
    val sharpenRadius: Float = 1f,
    val sharpenThreshold: Float = 0f,
    val colorNoise: Float = 0f,
    val luminanceNoise: Float = 0f,
    val noiseDetail: Float = .5f,
    val clarity: Float = 0f,
    val texture: Float = 0f,
    val dehaze: Float = 0f,
    val crop: RawCrop = RawCrop(),
    val quarterTurns: Int = 0,
    val straighten: Float = 0f,
    val vignette: Float = 0f,
    val vignetteMidpoint: Float = .5f,
    val vignetteRoundness: Float = 0f,
    val vignetteFeather: Float = .5f,
    val distortion: Float = 0f,
    val perspectiveVertical: Float = 0f,
    val perspectiveHorizontal: Float = 0f,
    val chromaticRed: Float = 0f,
    val chromaticBlue: Float = 0f,
    val defringePurple: Float = 0f,
    val defringeGreen: Float = 0f,
    val grainAmount: Float = 0f,
    val grainSize: Float = 3f,
    val grainRoughness: Float = .5f,
    val curves: RawCurves = RawCurves(),
    val masks: List<RawMask> = emptyList(),
    /** Null keeps the recorded simulation. Stored separately from the native adjustment vector. */
    val filmSimulation: String? = null,
) {
    init { require(filmSimulation == null || filmSimulation in rawColorProfileIds) { "Unsupported RAW color profile." } }

    val isEdited: Boolean get() = this != RawAdjustments()

    fun rotated(turns: Int): RawAdjustments {
        require(turns in 0..3)
        var nextCrop = crop
        var vertical = perspectiveVertical
        var horizontal = perspectiveHorizontal
        repeat((turns - quarterTurns + 4) % 4) {
            nextCrop = nextCrop.rotateClockwise()
            val previousHorizontal = horizontal
            horizontal = if (vertical == 0f) 0f else -vertical
            vertical = previousHorizontal
        }
        return copy(quarterTurns = turns, crop = nextCrop, perspectiveVertical = vertical, perspectiveHorizontal = horizontal)
    }

    /** Wire order shared with Adjustments::read in raw_color.h; numeric saved-session data. */
    fun nativeValues(): FloatArray = floatArrayOf(
        exposure, shadows, highlights, temperature, tint, contrast, whites, blacks,
        saturation, vibrance, sharpening, sharpenRadius, sharpenThreshold,
        colorNoise, luminanceNoise, noiseDetail,
        clarity, texture, dehaze, crop.left, crop.top, crop.right, crop.bottom,
        quarterTurns.toFloat(), straighten, vignette, vignetteMidpoint, vignetteRoundness, vignetteFeather,
        distortion, perspectiveVertical, perspectiveHorizontal, chromaticRed, chromaticBlue, defringePurple, defringeGreen,
        grainAmount, grainSize, grainRoughness,
    ) + creativeValues().toFloatArray()

    companion object {
        fun fromValues(p: FloatArray): RawAdjustments {
            require(p.size == 16 || p.size == 29 || p.size == 36 || p.size >= 61)
            // Upgrade older sessions without changing their edits or enabling corrections.
            if (p.size <= 36) return fromValues(RawAdjustments().nativeValues().also { p.copyInto(it) })
            require(p[36] in 0f..1f && p[37] in .5f..10f && p[38] in 0f..1f)
            val base = RawAdjustments(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                p[8], p[9], p[10], p[11], p[12], p[13], p[14], p[15], p[16], p[17], p[18],
                RawCrop(p[19], p[20], p[21], p[22]), p[23].toInt(), p[24], p[25], p[26], p[27], p[28],
                p[29], p[30], p[31], p[32], p[33], p[34], p[35], p[36], p[37], p[38])
            return RawCreativeReader(p).read(base)
        }
    }
}

/** Normalized bounds in the rotated, straightened image; independent of preview size. */
data class RawCrop(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    init {
        require(listOf(left, top, right, bottom).all(Float::isFinite) && left >= 0 && top >= 0 && right <= 1 && bottom <= 1 &&
            right - left >= .04999f && bottom - top >= .04999f) { "Invalid crop bounds." }
    }
    val width get() = right - left
    val height get() = bottom - top

    fun move(dx: Float, dy: Float): RawCrop {
        val x = dx.coerceIn(-left, 1 - right)
        val y = dy.coerceIn(-top, 1 - bottom)
        return RawCrop(left + x, top + y, right + x, bottom + y)
    }

    fun fitAspect(ratio: Float, imageAspect: Float): RawCrop {
        require(ratio.isFinite() && ratio > 0 && imageAspect.isFinite() && imageAspect > 0)
        val normalized = ratio / imageAspect
        require(normalized in .05f..20f) { "Aspect ratio is too narrow for this image." }
        val w = minOf(width, height * normalized)
        val h = w / normalized
        if (w < .05f || h < .05f) return RawCrop().fitAspect(ratio, imageAspect)
        return RawCrop((left + right - w) / 2, (top + bottom - h) / 2,
            (left + right + w) / 2, (top + bottom + h) / 2)
    }

    fun resize(dx: Float, dy: Float, fromLeft: Boolean, fromTop: Boolean, normalizedAspect: Float = 0f): RawCrop {
        val maxWidth = if (fromLeft) right else 1 - left
        val maxHeight = if (fromTop) bottom else 1 - top
        var w = (width + if (fromLeft) -dx else dx).coerceIn(minOf(.05f, maxWidth), maxWidth)
        var h = (height + if (fromTop) -dy else dy).coerceIn(minOf(.05f, maxHeight), maxHeight)
        if (normalizedAspect > 0) {
            val proposed = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy) * normalizedAspect) w else h * normalizedAspect
            val minimum = maxOf(.05f, .05f * normalizedAspect)
            val maximum = minOf(maxWidth, maxHeight * normalizedAspect)
            if (maximum < minimum) return this
            w = proposed.coerceIn(minimum, maximum)
            h = w / normalizedAspect
        }
        return RawCrop(if (fromLeft) right - w else left, if (fromTop) bottom - h else top,
            if (fromLeft) right else left + w, if (fromTop) bottom else top + h)
    }

    fun rotateClockwise() = RawCrop(1 - bottom, left, 1 - top, right)
}
