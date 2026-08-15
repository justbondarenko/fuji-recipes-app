package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.LAST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.PRESET_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.unpackPtpString

/**
 * Asks the camera what its custom slots hold.
 *
 * **Transcribed from** `fuji-recipes-book/camera/read-slot.ts` at commit `0c17106`
 * (`coding-standards.md` P3) — `readSlotNames` only. Reading a slot's full *settings* back is
 * the import path, which this client does not have.
 *
 * The sequence per slot is the write path's first two steps in reverse: select the slot with
 * `0xD18C`, let the body settle, read the name from `0xD18D`. The settle is the same
 * [SLOT_SETTLE_MS] the executor uses and for the same reason — the body switches its internal
 * property registers when the selector changes, and reading too early returns the previous
 * slot's name, which is worse than no name at all.
 *
 * Blocking. Callers run it on `Dispatchers.IO`.
 */
fun readSlotNames(
    session: PtpSession,
    sleep: (Long) -> Unit = { Thread.sleep(it) },
): List<SlotNameReading> = (FIRST_SLOT..LAST_SLOT).map { slot ->
    try {
        session.setPropertyBytes(PRESET_SLOT_PROPERTY, packU16(slot))
        sleep(SLOT_SETTLE_MS)
        val name = unpackPtpString(session.readPropertyBytes(PRESET_NAME_PROPERTY)).trim()
        SlotNameReading(slot = slot, name = name.ifBlank { null }, read = true)
    } catch (error: PtpError) {
        // This slot's own answer: the body refused to describe it. Marked unread and the rest
        // continue, because six known slots beat none.
        SlotNameReading(slot = slot, name = null, read = false)
    }
    // Anything that is not a PtpError — a timeout, a framing error — is the pipe itself
    // failing, and it propagates: every later reply would be answering an earlier question.
}
