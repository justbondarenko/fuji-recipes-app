package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.plan.WriteStep
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.responseName
import dev.bondarenko.fujirecipes.camera.ptp.stepPayload

/**
 * Runs one write plan against one session.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/write.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * The plan decided everything; this puts it on the wire and reports what happened. Sequential
 * by necessity — PTP holds one transaction at a time, so there is no faster shape available
 * even if the body would tolerate one.
 *
 * **Every step is read back.** "The camera said OK" and "the value is in the slot" are
 * different claims, and the read-back is what makes *written* mean written.
 */

/** What the caller is told as each step goes by. */
data class WriteProgress(
    /** Steps finished, including this one. */
    val done: Int,
    val total: Int,
    /** The step's own label, for "Writing <label>…". */
    val label: String,
)

enum class StepStatus {
    /** Written, and read back as the value we sent. */
    WRITTEN,

    /** The camera refused it. Non-fatal steps continue; fatal ones end the write. */
    REFUSED,

    /** Written and accepted, but it read back as something else. */
    MISMATCHED,

    /** Written, and the read-back could not be performed to check it. */
    UNVERIFIED,
}

data class StepOutcome(
    val id: String,
    val label: String,
    val property: Int,
    val status: StepStatus,
    /** Why, in words a person can act on (P5). Absent when written. */
    val detail: String? = null,
)

data class WriteOutcome(
    val slot: Int,
    /** Every step attempted, in the order attempted. */
    val steps: List<StepOutcome> = emptyList(),
    /** Steps that went in and verified. */
    val written: Int = 0,
    val total: Int = 0,
    /**
     * Anything the user should know that did not stop the write — a refused setting, a value
     * the body changed. Each is a whole sentence.
     */
    val warnings: List<String> = emptyList(),
    /**
     * Whether anything was written into the slot.
     *
     * The cancel and failure paths need this: a slot that was never touched is intact, and
     * saying "the slot may be half-written" about an untouched slot is a false alarm.
     */
    val slotTouched: Boolean = false,
)

enum class InterruptReason { CANCELLED, DISCONNECTED, REFUSED }

/** The write stopped early. Carries how far it got, for the partial-slot warning. */
class WriteInterrupted(
    val reason: InterruptReason,
    val outcome: WriteOutcome,
    message: String,
) : Exception(message)

/** How long to let the body settle after switching slots. */
const val SLOT_SETTLE_MS = 100L

/**
 * Runs one plan, in order, reporting progress and stopping when it must.
 *
 * **Cancellation is checked between steps, not inside them.** A `SetDevicePropValue` that has
 * been sent cannot be unsent, and abandoning a transaction half-read leaves the pipe answering
 * the previous question forever. So a cancel takes effect at the next step boundary, and the
 * outcome says how far it got.
 *
 * Blocking. Callers run it on `Dispatchers.IO`.
 */
fun executeWritePlan(
    session: PtpSession,
    plan: WritePlan,
    onProgress: (WriteProgress) -> Unit = {},
    isCancelled: () -> Boolean = { false },
    sleep: (Long) -> Unit = { Thread.sleep(it) },
): WriteOutcome {
    val steps = mutableListOf<StepOutcome>()
    val warnings = mutableListOf<String>()
    var written = 0
    var slotTouched = false

    fun outcome() = WriteOutcome(
        slot = plan.slot,
        steps = steps.toList(),
        written = written,
        total = plan.total,
        warnings = warnings.toList(),
        slotTouched = slotTouched,
    )

    fun record(step: WriteStep, status: StepStatus, detail: String? = null) {
        steps += StepOutcome(step.id, step.label, step.property, status, detail)
        if (status == StepStatus.WRITTEN) written += 1
    }

    plan.steps.forEachIndexed { index, step ->
        if (isCancelled()) {
            throw WriteInterrupted(
                InterruptReason.CANCELLED,
                outcome(),
                "Stopped before writing ${step.label}, at your request.",
            )
        }

        val payload = stepPayload(step)

        try {
            session.setPropertyBytes(step.property, payload)
        } catch (error: PtpError) {
            record(step, StepStatus.REFUSED, refusalDetail(error))

            if (step.fatal) {
                throw WriteInterrupted(
                    InterruptReason.REFUSED,
                    outcome(),
                    "The camera refused ${step.label} — ${refusalDetail(error)}. " +
                        "Nothing further was written.",
                )
            }

            // A refused setting costs one setting. Abandoning a whole recipe over one
            // property the body will not take would be worse than writing the rest.
            warnings += "${step.label} was refused — ${refusalDetail(error)} — so the slot " +
                "keeps whatever it had for it."
            onProgress(WriteProgress(index + 1, plan.total, step.label))
            return@forEachIndexed
        } catch (error: Exception) {
            // Not a refusal — the pipe itself is broken. An unplug lands here. The remaining
            // writes are abandoned: every later reply would be answering an earlier question,
            // and a "successful" write built on that would be a lie.
            record(step, StepStatus.REFUSED, error.message)
            throw WriteInterrupted(
                InterruptReason.DISCONNECTED,
                outcome(),
                "The connection failed while writing ${step.label}: ${error.message}",
            )
        }

        slotTouched = true

        /**
         * The settle, after the slot selector only.
         *
         * Keyed on the step's id rather than on "the first step": the plan's order is
         * `field-definitions.md` §6's, and hard-coding position 0 would silently stop waiting
         * if that order ever changed.
         */
        if (step.id == "slot") sleep(SLOT_SETTLE_MS)

        // Read back and check. A verification failure never stops the write — the value is
        // already in the camera, and the useful response is to say so, not to abandon the
        // remaining settings that are still fine.
        try {
            val read = session.readPropertyBytes(step.property)
            if (sameBytes(payload, read)) {
                record(step, StepStatus.WRITTEN)
            } else {
                record(
                    step,
                    StepStatus.MISMATCHED,
                    "sent ${payload.asHex()}, read back ${read.asHex()}",
                )
                warnings += "${step.label} was accepted but read back changed, so the camera " +
                    "may have adjusted it."
            }
        } catch (error: PtpError) {
            // The body took the value and will not read it back. Some bodies refuse the
            // read for every property in this block, and that is not a reason to call the
            // write a failure. Recorded as unverified, which is the honest word.
            record(step, StepStatus.UNVERIFIED, refusalDetail(error))
        } catch (error: Exception) {
            record(step, StepStatus.UNVERIFIED, error.message)
            throw WriteInterrupted(
                InterruptReason.DISCONNECTED,
                outcome(),
                "The connection failed after writing ${step.label}: ${error.message}",
            )
        }

        onProgress(WriteProgress(index + 1, plan.total, step.label))
    }

    if (isCancelled()) {
        throw WriteInterrupted(
            InterruptReason.CANCELLED,
            outcome(),
            "Stopped after the last write, at your request.",
        )
    }

    return outcome()
}

/**
 * A [PtpError]'s own words, with the code the user can quote (P5).
 *
 * The numeric code matters as much as the name: a search for "0x2019" finds the answer, and
 * "Device Busy" alone reads like an opinion.
 */
private fun refusalDetail(error: PtpError): String =
    "the camera answered ${responseName(error.code)} " +
        "(0x${error.code.toString(16).uppercase()})"

/**
 * Does the value read back match what we sent?
 *
 * Compared as **bytes**, against the same payload the write used, so nothing here needs to
 * know how a field is encoded. A short read is a mismatch: a body that answers two bytes to a
 * four-byte property has not confirmed anything. A read that is *longer* is not — a body may
 * answer a two-byte property with a padded buffer, and the bytes we care about are the ones we
 * wrote.
 */
/**
 * Bytes as the wire carries them, little-endian, for a mismatch the user has to read on a
 * phone with a camera hanging off it.
 *
 * Hex rather than decimal, and the value alongside it: the codes in `CameraEncoding.kt` are
 * written in hex, so `0x2000` is greppable against the table and `0, 32` is not. This is the
 * only place in the app that shows a raw value, and it exists for exactly one job — telling
 * you what the camera did with something you sent it.
 */
private fun ByteArray.asHex(): String {
    val hex = joinToString(" ") { "%02X".format(it) }
    if (size != 2) return hex

    val value = (this[0].toInt() and 0xff) or ((this[1].toInt() and 0xff) shl 8)
    return "$hex (0x%04X)".format(value)
}

private fun sameBytes(sent: ByteArray, read: ByteArray): Boolean {
    if (read.size < sent.size) return false
    return sent.indices.all { sent[it] == read[it] }
}

/**
 * The sentence to show when a write did not finish.
 *
 * Here rather than in the sheet because it is a statement about the *device* — what the slot
 * now holds — and it has to be the same whether the write was cancelled, refused or unplugged.
 * The one thing it must never do is guess: this app cannot read a slot back as a whole, so it
 * says what was written and stops there.
 */
fun partialWriteWarning(outcome: WriteOutcome): String? {
    if (!outcome.slotTouched) return null

    // "properties", not "settings": two of the steps are the slot selector and the name,
    // which are not settings.
    return "Slot C${outcome.slot} was partly written: ${outcome.written} of ${outcome.total} " +
        "properties went in before this stopped. The rest are whatever the slot held before, " +
        "so it is neither the old recipe nor the new one. Writing it again from the start " +
        "fixes it."
}
