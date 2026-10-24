package dev.bondarenko.fujirecipes.data.raweditor

import android.graphics.Bitmap

/** Calls are serialized by RawEditorViewModel; native handles never reach the UI. */
internal object NativeRaw {
    init { System.loadLibrary("fuji_raw") }

    external fun open(path: String, profile: ByteArray): Long
    external fun dimensions(handle: Long): IntArray
    external fun outputDimensions(handle: Long, adjustments: FloatArray, full: Boolean, draft: Boolean = false): IntArray
    external fun mapPoints(handle: Long, adjustments: FloatArray, points: FloatArray): FloatArray
    external fun maskOverlay(handle: Long, output: Bitmap, adjustments: FloatArray, index: Int, draft: Boolean = false)
    external fun render(
        handle: Long, output: Bitmap, adjustments: FloatArray, full: Boolean, draft: Boolean = false,
    )
    external fun close(handle: Long)
}
