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
) {
    val isEdited: Boolean get() = this != RawAdjustments()
}
