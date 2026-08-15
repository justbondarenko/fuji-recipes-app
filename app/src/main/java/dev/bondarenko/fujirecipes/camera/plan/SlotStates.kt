package dev.bondarenko.fujirecipes.camera.plan

/**
 * What each of C1 to C7 holds *right now*.
 *
 * **Transcribed from** `fuji-recipes-book/src/utils/slots.ts` at commit `0c17106`
 * (`coding-standards.md` P3), minus its label strings — those live in `strings.xml` here, and
 * `ui/camera/SlotLabels.kt` maps a status to one.
 *
 * **The camera is the only source.** The sibling repo tried deriving slot contents from
 * `lastWrittenSlot` / `lastWrittenAt` on the recipes the app already held — a model of the
 * camera built from this app's own past writes. That model is wrong the moment anything else
 * touches the body: a recipe set by hand on the camera, one written by the manufacturer's
 * software, a slot overwritten from another device. It also survives long after the fact, so
 * the picker confidently named a recipe that had not been in that slot for weeks.
 *
 * This client has an even shorter road to the same conclusion: it does not keep slot
 * bookkeeping at all (`roadmap.md` §3), so there is nothing to derive from. A write is a
 * destructive act against a device, and the decision has to be made against the device's own
 * answer at the moment of choosing.
 *
 * Pure: no `android.*` (P4).
 */

/**
 * What the camera said about one slot when it was asked.
 *
 * Three outcomes, not two, and the third is the point: a slot the camera **answered for with
 * no name** and a slot the camera **would not answer for** are different facts, and collapsing
 * them into "no name" turns a failed read into a claim about an empty slot.
 */
data class SlotNameReading(
    val slot: Int,
    /** The name the camera reported, or `null` when it reported a blank one. */
    val name: String?,
    /** False when the camera refused the read, so nothing here is known about the slot. */
    val read: Boolean,
)

/**
 * The states are kept apart deliberately — "the camera says nothing is named here" and "the
 * camera would not tell us" lead to different warnings, and merging them is how someone writes
 * over a recipe after being shown an empty-looking slot.
 */
enum class SlotStatus {
    /** A read is in flight; nothing is known yet. */
    READING,

    /** The camera reported a name. */
    NAMED,

    /** The camera answered and reported no name. */
    UNNAMED,

    /** The camera refused the read for this slot. */
    UNREADABLE,

    /** Nothing has been read — no camera, or the read failed as a whole. */
    UNKNOWN,
}

data class SlotState(
    val slot: Int,
    val status: SlotStatus,
    /** The name the camera reported, or `null` for every other status. */
    val name: String?,
) {
    /**
     * Whether the camera named something here. Not "is there anything in this slot" — the app
     * cannot answer that, and [status] carries the distinction that matters.
     */
    val occupied: Boolean get() = status == SlotStatus.NAMED
}

/**
 * One entry per slot, C1 first, describing what the camera said in this read.
 *
 * A pure function, so the picker composable has no logic to test: this is unit-tested and the
 * composable renders what it returns.
 */
fun slotStates(readings: List<SlotNameReading>, loading: Boolean = false): List<SlotState> {
    val bySlot = readings.associateBy { it.slot }

    return (FIRST_SLOT..LAST_SLOT).map { slot ->
        val reading = bySlot[slot]

        // Loading wins over a leftover reading: a name shown during a re-read is a name from
        // before the re-read.
        val status = when {
            loading -> SlotStatus.READING
            reading == null -> SlotStatus.UNKNOWN
            !reading.read -> SlotStatus.UNREADABLE
            !reading.name.isNullOrBlank() -> SlotStatus.NAMED
            else -> SlotStatus.UNNAMED
        }

        SlotState(
            slot = slot,
            status = status,
            name = if (status == SlotStatus.NAMED) reading?.name else null,
        )
    }
}

/**
 * Whether choosing this slot needs an acknowledgement before the write runs, and why.
 *
 * `PRD.md` §7.4's stage 4 is "a second tap on a slot with **known contents**", and this is what
 * decides that. `null` means the camera answered and there is nothing identifiable to lose.
 *
 * A slot the camera answered for with **no name** takes no acknowledgement. The body was asked
 * and reported nothing to identify, and putting a confirmation in front of all seven slots of
 * an untouched camera trains people to tap through it without reading — which costs exactly
 * the recipe the confirmation exists to protect.
 */
sealed interface SlotCaution {
    val slot: Int

    /** The plain overwrite case: something is in there and the camera named it. */
    data class Named(override val slot: Int, val recipeName: String) : SlotCaution

    /**
     * The camera would not describe this slot. The app has no idea what is in it, and saying
     * nothing would let a single tap replace a recipe nobody knew was there.
     */
    data class Unknown(override val slot: Int) : SlotCaution
}

fun slotCaution(state: SlotState?): SlotCaution? = when {
    state == null -> null
    state.status == SlotStatus.NAMED && state.name != null ->
        SlotCaution.Named(state.slot, state.name)

    state.status == SlotStatus.UNREADABLE || state.status == SlotStatus.UNKNOWN ->
        SlotCaution.Unknown(state.slot)

    else -> null
}
