package dev.bondarenko.fujirecipes.camera.raw

import dev.bondarenko.fujirecipes.camera.CameraModels

/**
 * Which recipe fields a connected body's RAW engine will actually act on.
 *
 * The lab exists to let someone change a setting and watch the picture change, so a control
 * that silently does nothing is worse than one that is absent. This answers that question for
 * the parameter panel, and it answers it from [NATIVE_625_FIELD_IDS] — the same table
 * `patchRawDevelopmentProfile` writes from — rather than from a second list that would drift
 * away from it on the first added field.
 *
 * A model here is one with a **verified** adapter. The profile still validates its own shape
 * at render time, so an X-T50 that returns an unexpected `0xD185` blob is refused even though
 * this reports its field set: this is what the adapter can do, not a promise about the blob.
 */
fun rawSupportedFieldIds(cameraModel: String?): Set<String> =
    when (cameraModel?.let(CameraModels::normaliseModel)) {
        "X100VI", "XT50" -> NATIVE_625_FIELD_IDS
        else -> emptySet()
    }

/** Whether this build carries a RAW recipe mapping for the model at all. */
fun isRawDevelopmentCalibrated(cameraModel: String?): Boolean =
    rawSupportedFieldIds(cameraModel).isNotEmpty()

/**
 * The field set to edit against when no camera is attached.
 *
 * Editing is allowed with nothing plugged in — only rendering needs a body — and the panel
 * still has to decide which band each control sits in. The verified native set is the honest
 * guess: it is what every calibrated body in this build supports.
 */
val DEFAULT_RAW_SUPPORTED_FIELD_IDS: Set<String> = NATIVE_625_FIELD_IDS
