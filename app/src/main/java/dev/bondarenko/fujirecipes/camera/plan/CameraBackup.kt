package dev.bondarenko.fujirecipes.camera.plan

import dev.bondarenko.fujirecipes.camera.CameraModels

/**
 * The settings backup: the other half of *USB RAW CONV./**BACKUP RESTORE***.
 *
 * **Transcribed from** `petabyt/libfuji` `lib/fuji_usb.c` — `fujiusb_download_backup` and
 * `fujiusb_restore_backup`. In that USB mode the body exposes exactly one object at handle 0,
 * and it is the whole of the camera's settings: `GetObjectInfo` then `GetObject` reads it,
 * `SendObjectInfo` then `SendObject` writes one back. This is the same operation the camera's
 * own menu performs to and from a card; the app is simply the other end of the cable.
 *
 * **Restoring is the only destructive thing this app can do.** Writing a recipe touches one
 * custom slot and leaves the rest of the body alone. Restoring replaces every setting the
 * camera holds. So the guards below are the point of this file, and they are deliberately not
 * clever: a size the body could not have produced is refused outright, and the model the file
 * came from is carried in its *name* so a restore across bodies can be shown to the user before
 * it happens rather than discovered after.
 *
 * **Why the name and not a header.** Wrapping the blob in an envelope of this app's own design
 * would make the file unreadable by `libfuji`, by Fudge, and by anything else that speaks this
 * protocol — and the blob's own format is Fujifilm's, not ours to extend. A filename is weak
 * evidence and is treated as weak evidence: it informs a confirmation, it does not authorise
 * one.
 *
 * Pure: no `android.*` (P4), enforced by `CameraPurityTest`.
 */

/** The one object a body exposes in this USB mode. */
const val BACKUP_OBJECT_HANDLE = 0

/**
 * `libfuji`'s own ceiling, and the check it makes before sending: *"Backup file seems to be too
 * big, is the path correct?"*. A settings blob is tens of kilobytes; anything larger is a file
 * the user did not mean to pick.
 */
const val MAX_BACKUP_BYTES = 100_000

/** Below this there is no plausible settings blob, only a truncated or empty file. */
const val MIN_BACKUP_BYTES = 64

/**
 * The `SendObjectInfo` dataset a restore announces itself with.
 *
 * A fixed 1088 bytes of which the first twelve carry anything: storage id 0, object format
 * `0x5000`, protection 0, then the size. The remainder is zero. That is what the captures show
 * X RAW Studio sending and what `libfuji` reproduces — **not** an ISO 15740 `ObjectInfo`, whose
 * fields after `protection` are laid out differently, and not the 208-byte Fuji `ObjectInfo`
 * used by the wireless gallery either. Three layouts share a prefix and diverge; this is the
 * one this operation takes.
 */
const val SEND_OBJECT_INFO_DATASET_BYTES = 1088

private const val BACKUP_OBJECT_FORMAT = 0x5000

fun sendObjectInfoDataset(size: Int): ByteArray {
    require(size in MIN_BACKUP_BYTES..MAX_BACKUP_BYTES) {
        "A backup is $MIN_BACKUP_BYTES..$MAX_BACKUP_BYTES bytes; got $size."
    }

    val dataset = ByteArray(SEND_OBJECT_INFO_DATASET_BYTES)

    fun putU16(at: Int, value: Int) {
        dataset[at] = (value and 0xff).toByte()
        dataset[at + 1] = ((value ushr 8) and 0xff).toByte()
    }

    fun putU32(at: Int, value: Int) {
        putU16(at, value and 0xffff)
        putU16(at + 2, (value ushr 16) and 0xffff)
    }

    putU32(0, 0)                          // storage id
    putU16(4, BACKUP_OBJECT_FORMAT)       // object format
    putU16(6, 0)                          // protection status
    putU32(8, size)                       // compressed size

    return dataset
}

// ─── The file on the phone ──────────────────────────────────────────────────

private const val FILENAME_PREFIX = "fuji-backup-"
private const val FILENAME_SUFFIX = ".bin"

/**
 * `fuji-backup-X-T50-20260908-2117.bin`.
 *
 * [stamp] is supplied by the caller — the pure layer has no clock — and is expected as
 * `yyyyMMdd-HHmm`, which is what [modelFromBackupFilename] knows how to skip back over.
 */
fun backupFilename(model: String, stamp: String): String {
    val safe = model.trim()
        .map { if (it.isLetterOrDigit() || it == '-') it else '-' }
        .joinToString("")
        .trim('-')
        .ifEmpty { "camera" }

    return "$FILENAME_PREFIX$safe-$stamp$FILENAME_SUFFIX"
}

private val FILENAME_PATTERN =
    Regex("^" + Regex.escape(FILENAME_PREFIX) + "(.+)-\\d{8}-\\d{4}" + Regex.escape(FILENAME_SUFFIX) + "$")

/**
 * The model a file's name claims it came from, or null.
 *
 * Null for any name this app did not write — a renamed file, a backup taken with another tool,
 * one pulled off a card. That is a normal case and not a failure: it means the restore
 * confirmation cannot name a source body, so it says so.
 */
fun modelFromBackupFilename(filename: String): String? =
    FILENAME_PATTERN.find(filename.trim())?.groupValues?.get(1)

/** How a chosen file compares to the body it is about to be written to. */
enum class BackupMatch {
    /** The name says the same body this app is connected to. */
    SAME_MODEL,

    /** The name says a different body. Restoring anyway is the user's call, and a loud one. */
    DIFFERENT_MODEL,

    /** The name says nothing — not written by this app, or renamed since. */
    UNKNOWN_MODEL,
}

/**
 * Whether a file's name matches the connected body.
 *
 * Compared through [CameraModels.normaliseModel], so `X-T50` and `XT50` are the same body —
 * a camera's own model string and a marketing name do not agree on punctuation, and a
 * confirmation that cried wolf over a hyphen would train people to ignore it.
 */
fun backupMatch(filename: String, connectedModel: String): BackupMatch {
    val claimed = modelFromBackupFilename(filename) ?: return BackupMatch.UNKNOWN_MODEL

    return if (CameraModels.normaliseModel(claimed) == CameraModels.normaliseModel(connectedModel)) {
        BackupMatch.SAME_MODEL
    } else {
        BackupMatch.DIFFERENT_MODEL
    }
}

/** Why a chosen file cannot be sent at all, or null when it can. */
fun backupSizeProblem(size: Int): String? = when {
    size < MIN_BACKUP_BYTES ->
        "That file is $size bytes, too small to be a camera backup. It is probably empty or truncated."

    size > MAX_BACKUP_BYTES ->
        "That file is $size bytes. A camera backup is well under $MAX_BACKUP_BYTES, so this is " +
            "almost certainly the wrong file."

    else -> null
}
