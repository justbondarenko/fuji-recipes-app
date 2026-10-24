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
) {
    val isEdited: Boolean get() = this != RawAdjustments()

    /** Wire order shared with Adjustments::read in raw_color.h; also the saved-session shape. */
    fun nativeValues(): FloatArray = floatArrayOf(
        exposure, shadows, highlights, temperature, tint, contrast, whites, blacks,
        saturation, vibrance, sharpening, sharpenRadius, sharpenThreshold,
        colorNoise, luminanceNoise, noiseDetail,
    )

    companion object {
        fun fromValues(p: FloatArray): RawAdjustments {
            require(p.size == 16)
            return RawAdjustments(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                p[8], p[9], p[10], p[11], p[12], p[13], p[14], p[15])
        }
    }
}
