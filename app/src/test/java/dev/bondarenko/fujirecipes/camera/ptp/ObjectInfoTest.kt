package dev.bondarenko.fujirecipes.camera.ptp

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ObjectInfoTest {
    @Test
    fun `parses the standard object info layout`() {
        val info = parseObjectInfo(objectInfo(filename = "DSCF0042.JPG", size = 0xf0000000L))

        assertEquals(0x00010001, info.storageId)
        assertEquals(PtpObject.FORMAT_JPEG, info.format)
        assertEquals(0xf0000000L, info.compressedSize)
        assertEquals(6240L, info.imageWidth)
        assertEquals("DSCF0042.JPG", info.filename)
        assertEquals("20260909T123456", info.captureDate)
    }

    @Test
    fun `rejects a handle count larger than the remaining data`() {
        assertFailsWith<PtpFramingError> {
            parseU32Array(packU32(2) + packU32(7), "Handles")
        }
    }

    companion object {
        fun objectInfo(
            filename: String,
            size: Long,
            format: Int = PtpObject.FORMAT_JPEG,
            captureDate: String = "20260909T123456",
        ): ByteArray {
            val out = ByteArrayOutputStream()
            out.write(packU32(0x00010001))
            out.write(packU16(format))
            out.write(packU16(0))
            out.write(packU32(size.toInt()))
            out.write(packU16(PtpObject.FORMAT_JPEG))
            out.write(packU32(16_000))
            out.write(packU32(320))
            out.write(packU32(240))
            out.write(packU32(6240))
            out.write(packU32(4160))
            out.write(packU32(24))
            out.write(packU32(0))
            out.write(packU16(0))
            out.write(packU32(0))
            out.write(packU32(0))
            out.write(packPtpString(filename))
            out.write(packPtpString(captureDate))
            out.write(packPtpString(captureDate))
            out.write(packPtpString(""))
            return out.toByteArray()
        }
    }
}
