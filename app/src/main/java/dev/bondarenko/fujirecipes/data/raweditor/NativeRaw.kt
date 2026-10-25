package dev.bondarenko.fujirecipes.data.raweditor

import android.graphics.Bitmap

/** Calls are serialized by RawEditorViewModel; native handles never reach the UI. */
internal object NativeRaw {
    init { System.loadLibrary("fuji_raw") }

    external fun open(path: String, profile: ByteArray): Long
    external fun setProfile(handle: Long, profile: ByteArray)
    /** Preview width/height, full width/height, embedded distortion applied (0/1). */
    external fun dimensions(handle: Long): IntArray
    external fun outputDimensions(handle: Long, adjustments: FloatArray, full: Boolean, draft: Boolean = false): IntArray
    external fun mapPoints(handle: Long, adjustments: FloatArray, points: FloatArray): FloatArray
    external fun maskOverlay(handle: Long, output: Bitmap, adjustments: FloatArray, index: Int, draft: Boolean = false)
    external fun render(
        handle: Long, output: Bitmap, adjustments: FloatArray, full: Boolean, draft: Boolean = false,
    )
    external fun renderDetail(handle: Long, output: Bitmap, adjustments: FloatArray, left: Int, top: Int, zoom: Float)
    external fun releaseDetail(handle: Long)
    external fun close(handle: Long)
}
