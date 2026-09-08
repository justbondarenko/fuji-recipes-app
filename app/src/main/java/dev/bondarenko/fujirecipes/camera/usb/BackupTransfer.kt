package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.BACKUP_OBJECT_HANDLE
import dev.bondarenko.fujirecipes.camera.plan.MIN_BACKUP_BYTES
import dev.bondarenko.fujirecipes.camera.plan.backupSizeProblem
import dev.bondarenko.fujirecipes.camera.plan.sendObjectInfoDataset
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.responseName

/**
 * Reading the camera's settings blob off it, and writing one back.
 *
 * **Transcribed from** `petabyt/libfuji` `lib/fuji_usb.c` (`fujiusb_download_backup`,
 * `fujiusb_restore_backup`). Two operations each, in the order that project observed working;
 * see `camera/plan/CameraBackup.kt` for the dataset layout and the guards.
 *
 * Blocking. Callers run both on `Dispatchers.IO`.
 */

/** A backup operation that could not be carried out, in words naming the remedy (P5). */
class BackupError(message: String, val ptpCode: Int? = null) : Exception(message)

/**
 * Reads the whole settings blob off the body.
 *
 * `GetObjectInfo` first, and its refusal is the clean signal that this body has no backup
 * object to give — which almost always means the wrong USB mode, and is a different sentence
 * to the user than a transfer that broke halfway. Its *contents* are ignored on purpose: the
 * ISO and Fuji `ObjectInfo` layouts diverge two fields in, and the payload's own length is the
 * only size that cannot be misparsed.
 */
fun downloadBackup(session: PtpSession): ByteArray {
    try {
        session.getObjectInfo(BACKUP_OBJECT_HANDLE)
    } catch (error: PtpError) {
        throw BackupError(
            "The camera has no settings backup to give. Check that its USB mode is " +
                "USB RAW CONV./BACKUP RESTORE, then try again.",
            ptpCode = error.code,
        )
    }

    val bytes = try {
        session.getObject(BACKUP_OBJECT_HANDLE)
    } catch (error: PtpError) {
        throw BackupError(
            "The camera refused to send its settings: ${responseName(error.code)}.",
            ptpCode = error.code,
        )
    }

    // A body that answers both operations and hands back nothing usable is worse than one that
    // refuses: the file would be written, named after the camera, and be junk on the day it was
    // needed.
    if (bytes.size < MIN_BACKUP_BYTES) {
        throw BackupError(
            "The camera sent ${bytes.size} bytes, too few to be a settings backup. Nothing " +
                "was saved.",
        )
    }

    return bytes
}

/**
 * Writes a settings blob back to the body.
 *
 * The size is checked here as well as in the UI. This is the last point before the camera's
 * whole configuration is replaced, and the check that matters is the one nearest the wire —
 * a caller that skipped its own guard should not be able to reach `SendObject`.
 *
 * No read-back: unlike a slot write there is nothing to read back, because the object is
 * write-only in this direction. The camera's `OK` on `SendObject` is the whole of the
 * acknowledgement available, which is worth knowing when reading the result.
 */
fun restoreBackup(session: PtpSession, bytes: ByteArray) {
    backupSizeProblem(bytes.size)?.let { throw BackupError(it) }

    try {
        session.sendObjectInfo(sendObjectInfoDataset(bytes.size))
    } catch (error: PtpError) {
        throw BackupError(
            "The camera would not accept a settings restore: ${responseName(error.code)}. " +
                "Nothing was written.",
            ptpCode = error.code,
        )
    }

    try {
        session.sendObject(bytes)
    } catch (error: PtpError) {
        // Past SendObjectInfo the camera is expecting a payload, so this one cannot promise
        // that nothing changed — and saying so is the difference between a user who power
        // cycles and one who assumes it is fine.
        throw BackupError(
            "The settings transfer failed partway: ${responseName(error.code)}. Check the " +
                "camera's settings before relying on them.",
            ptpCode = error.code,
        )
    }
}
