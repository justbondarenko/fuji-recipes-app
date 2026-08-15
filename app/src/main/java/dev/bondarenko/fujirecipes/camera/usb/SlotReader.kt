package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.FIELD_PROPERTIES
import dev.bondarenko.fujirecipes.camera.plan.FIRST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.LAST_SLOT
import dev.bondarenko.fujirecipes.camera.plan.PRESET_NAME_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.PRESET_SLOT_PROPERTY
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.StepKind
import dev.bondarenko.fujirecipes.camera.plan.decodeValue
import dev.bondarenko.fujirecipes.camera.plan.wireKind
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.packU16
import dev.bondarenko.fujirecipes.camera.ptp.unpackI16
import dev.bondarenko.fujirecipes.camera.ptp.unpackPtpString
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16

/**
 * Asks the camera what its custom slots hold.
 *
 * **Transcribed from** `fuji-recipes-book/camera/read-slot.ts` at commit `0c17106`
 * (`coding-standards.md` P3). Two readers, for the two questions this app asks:
 *
 * - [readSlotNames] — names only, for the write sheet's slot picker (FEAT-006). Seven round
 *   trips, because all the picker needs is what it is about to overwrite.
 * - [readSlotRecipes] — the whole slot decoded into a recipe, for import (FEAT-007).
 *
 * The sequence per slot is the write path's first steps in reverse: select the slot with
 * `0xD18C`, let the body settle, read from there. The settle is the same [SLOT_SETTLE_MS] the
 * executor uses and for the same reason — the body switches its internal property registers
 * when the selector changes, and reading too early returns the *previous* slot's values, which
 * is worse than none at all because it is a wrong answer rather than a missing one.
 *
 * Blocking. Callers run both on `Dispatchers.IO`.
 */

/**
 * One custom slot, decoded into something the library could hold.
 *
 * No id: `coding-standards.md` P2 forbids the phone inventing one the server would assign, and
 * `POST /api/import` assigns it. The web client generates a UUID here only to satisfy its own
 * client-side schema check.
 */
data class SlotRecipe(
    val slot: Int,
    /** The camera's own name for the slot, or `Slot C3` when it has none. */
    val name: String,
    /** Field id to decoded value, for every property this build could read and name. */
    val settings: Map<String, Any>,
)

/**
 * Reads every custom slot as a recipe.
 *
 * Transcribed from `read-slot.ts`'s `readSlot` / `readAllSlots`, minus their `ParsedRecipe`
 * wrapper — that is the web client's file-import shape and nothing here needs it.
 *
 * Two things worth keeping when changing this:
 *
 * - **Properties are read once each, cached by code.** `grainEffect` and `grainSize` are both
 *   `0xD195`; reading per *field* would ask the camera for the same property twice and take
 *   twice as long over a link that is already slow.
 * - **A slot that decodes to nothing is dropped.** An unconfigured slot is not an empty
 *   recipe, and importing it as one would put a shell in the library with a body's default
 *   name on it.
 */
fun readSlotRecipes(
    session: PtpSession,
    onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    sleep: (Long) -> Unit = { Thread.sleep(it) },
): List<SlotRecipe> {
    val total = LAST_SLOT - FIRST_SLOT + 1

    return (FIRST_SLOT..LAST_SLOT).mapNotNull { slot ->
        onProgress(slot - FIRST_SLOT + 1, total)

        try {
            session.setPropertyBytes(PRESET_SLOT_PROPERTY, packU16(slot))
            sleep(SLOT_SETTLE_MS)
            readOneSlot(session, slot)
        } catch (error: PtpError) {
            // This slot's own answer. The rest continue: six readable slots beat none.
            null
        }
        // Anything that is not a PtpError — a timeout, a framing error — is the pipe itself
        // failing and propagates, because every later reply would answer an earlier question.
    }
}

private fun readOneSlot(session: PtpSession, slot: Int): SlotRecipe? {
    val name = runCatching {
        unpackPtpString(session.readPropertyBytes(PRESET_NAME_PROPERTY)).trim()
    }.getOrDefault("")

    val settings = mutableMapOf<String, Any>()
    val readByCode = mutableMapOf<Int, ByteArray?>()

    FIELD_PROPERTIES.forEach { (fieldId, mapping) ->
        val code = mapping.code ?: return@forEach

        // `containsKey` rather than `getOrPut`: the latter re-invokes its lambda when the
        // stored value is null, so a property the body *refused* would be asked for again by
        // every other field sharing its code. The refusal has to be cached too.
        if (!readByCode.containsKey(code)) {
            readByCode[code] = try {
                session.readPropertyBytes(code)
            } catch (error: PtpError) {
                // The body will not report this property. One field lost, not the slot.
                null
            }
        }

        val bytes = readByCode[code] ?: return@forEach

        if (bytes.size < 2) return@forEach

        // The width the plan would have *written* is the width to read it back at — a tone is
        // signed and a lookup is a bit pattern, and reading `0x8000` as a signed value gives
        // -32768, which matches nothing in any table.
        val raw = if (wireKind(mapping.packing) == StepKind.I16) {
            unpackI16(bytes)
        } else {
            unpackU16(bytes)
        }

        // A code this build cannot name is omitted rather than guessed at.
        decodeValue(fieldId, raw)?.let { settings[fieldId] = it }
    }

    if (settings.isEmpty()) return null

    return SlotRecipe(
        slot = slot,
        // A camera slot need not be named, and a nameless recipe is worse than a plainly
        // labelled one.
        name = name.ifBlank { "Slot C$slot" },
        settings = settings,
    )
}

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
