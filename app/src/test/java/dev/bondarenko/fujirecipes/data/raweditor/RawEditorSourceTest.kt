package dev.bondarenko.fujirecipes.data.raweditor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.file.Files

class RawEditorSourceTest {
    @Test
    fun readsOnlyTheBoundedCameraJpegAndRejectsInvalidRanges() {
        val file = Files.createTempFile("raf-preview-", ".raf").toFile()
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 0xff.toByte(), 0xd9.toByte())
        val bytes = ByteArray(148 + jpeg.size)
        "FUJIFILMCCD-RAW ".toByteArray().copyInto(bytes)
        val header = ByteBuffer.wrap(bytes)
        header.putInt(84, 148)
        header.putInt(88, jpeg.size)
        jpeg.copyInto(bytes, 148)
        try {
            file.writeBytes(bytes)
            assertArrayEquals(jpeg, readRafPreview(file))
            header.putInt(88, Int.MAX_VALUE)
            file.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
            header.putInt(88, jpeg.size)
            header.putInt(84, -1)
            file.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
            file.writeBytes(byteArrayOf(1))
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
        } finally { file.delete() }
    }
}
