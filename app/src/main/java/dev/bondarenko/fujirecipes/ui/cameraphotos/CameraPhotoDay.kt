package dev.bondarenko.fujirecipes.ui.cameraphotos

import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import java.time.LocalDate

internal data class CameraPhotoDay(val date: LocalDate?, val files: List<CameraMediaObject>)

/** Keep the card's ordering within each day; put undated files after the newest-first days. */
internal fun groupCameraFilesByDay(files: List<CameraMediaObject>): List<CameraPhotoDay> =
    files.groupBy { cameraCaptureDay(it.info.captureDate) }
        .map { (date, files) -> CameraPhotoDay(date, files) }
        .sortedWith(compareByDescending { it.date })
