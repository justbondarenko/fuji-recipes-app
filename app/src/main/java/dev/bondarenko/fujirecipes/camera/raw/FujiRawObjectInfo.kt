package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.camera.ptp.packPtpString
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.packU32
import java.io.ByteArrayOutputStream

const val FUJI_RAW_OBJECT_FORMAT = 0xf802
const val FUJI_RAW_UPLOAD_NAME = "FUP_FILE.dat"

/** Fuji's vendor SendObjectInfo payload for a host-provided RAF. */
fun fujiRawObjectInfo(length: Long): ByteArray {
    require(length in 1..0xffffffffL) { "A RAF upload must fit in PTP's uint32 object size." }
    val out = ByteArrayOutputStream()
    out.write(packU32(0)) // StorageID
    out.write(packU16(FUJI_RAW_OBJECT_FORMAT))
    out.write(packU16(0)) // ProtectionStatus
    out.write(packU32(length.toInt()))
    out.write(packU16(0)) // ThumbFormat
    repeat(3) { out.write(packU32(0)) } // Thumb size and dimensions
    repeat(4) { out.write(packU32(0)) } // Image dimensions/depth and parent
    out.write(packU16(0)) // AssociationType
    repeat(2) { out.write(packU32(0)) } // AssociationDesc and SequenceNumber
    out.write(packPtpString(FUJI_RAW_UPLOAD_NAME))
    repeat(3) { out.write(packPtpString("")) }
    return out.toByteArray()
}
