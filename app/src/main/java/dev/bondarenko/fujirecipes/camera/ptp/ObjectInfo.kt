package dev.bondarenko.fujirecipes.camera.ptp

/** Standard ISO 15740 object constants used while browsing a camera card. */
object PtpObject {
    const val ALL_STORAGES = -1
    const val ALL_FORMATS = 0
    const val ROOT = 0
    const val FORMAT_ASSOCIATION = 0x3001
    const val FORMAT_JPEG = 0x3801
}

data class PtpObjectInfo(
    val storageId: Int,
    val format: Int,
    val protectionStatus: Int,
    val compressedSize: Long,
    val thumbFormat: Int,
    val thumbCompressedSize: Long,
    val thumbWidth: Long,
    val thumbHeight: Long,
    val imageWidth: Long,
    val imageHeight: Long,
    val imageBitDepth: Long,
    val parentObject: Int,
    val associationType: Int,
    val associationDescription: Long,
    val sequenceNumber: Long,
    val filename: String,
    val captureDate: String,
    val modificationDate: String,
    val keywords: String,
)

/** Parses the standard ObjectInfo dataset. Fuji's vendor objects use another layout. */
fun parseObjectInfo(bytes: ByteArray): PtpObjectInfo {
    val cursor = Cursor(bytes, "The object info dataset")
    return PtpObjectInfo(
        storageId = cursor.u32().toInt(),
        format = cursor.u16(),
        protectionStatus = cursor.u16(),
        compressedSize = cursor.u32(),
        thumbFormat = cursor.u16(),
        thumbCompressedSize = cursor.u32(),
        thumbWidth = cursor.u32(),
        thumbHeight = cursor.u32(),
        imageWidth = cursor.u32(),
        imageHeight = cursor.u32(),
        imageBitDepth = cursor.u32(),
        parentObject = cursor.u32().toInt(),
        associationType = cursor.u16(),
        associationDescription = cursor.u32(),
        sequenceNumber = cursor.u32(),
        filename = cursor.ptpString(),
        captureDate = cursor.ptpString(),
        modificationDate = cursor.ptpString(),
        keywords = cursor.ptpString(),
    )
}

/** A uint32 count followed by uint32 values, used by storage and object-handle responses. */
fun parseU32Array(bytes: ByteArray, what: String): List<Int> {
    val cursor = Cursor(bytes, what)
    val count = cursor.u32()
    if (count > Int.MAX_VALUE / 4 || count * 4 > cursor.remaining) {
        throw PtpFramingError("$what declared $count elements, but only ${cursor.remaining} bytes remain.")
    }
    return List(count.toInt()) { cursor.u32().toInt() }
}
