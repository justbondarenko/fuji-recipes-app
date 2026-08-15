package dev.bondarenko.fujirecipes.camera.ptp

import dev.bondarenko.fujirecipes.camera.plan.StepKind
import dev.bondarenko.fujirecipes.camera.plan.WriteStep

/**
 * A plan step as the bytes that go to the camera.
 *
 * **Transcribed from** `fuji-recipes-book/camera/usb/payload.ts` at commit `0c17106`
 * (`coding-standards.md` P3).
 *
 * The last translation before the wire: `buildWritePlan` decides *what* value each property
 * gets, and this decides how those two bytes are laid out. Small on purpose — every decision
 * worth making was already made upstream, and the only thing that can go wrong here is width
 * or signedness.
 *
 * **Why it lives here rather than in `camera/plan/`.** Packing needs `PtpFraming`, and
 * `camera/plan/` must stay reachable without the byte layer for P4 to mean anything. So the
 * value→code half lives with the field knowledge and the code→bytes half lives with the other
 * byte code, and the boundary is the thing P4 asks for rather than a filing preference.
 */

/**
 * The payload for one step.
 *
 * Throws rather than guessing. A numeric step whose value is not a number, or a name step
 * whose value is not a string, means the plan and this function disagree about a step's kind —
 * and a zero written into a property because a guard was missing is exactly the failure this
 * project keeps refusing to allow.
 */
fun stepPayload(step: WriteStep): ByteArray {
    if (step.kind == StepKind.STRING) {
        val text = step.value as? String
            ?: throw PtpFramingError(
                "Step “${step.id}” is a string property but carries " +
                    "${step.value::class.simpleName}.",
            )
        return packPtpString(text)
    }

    val number = (step.value as? Number)?.toInt()
        ?: throw PtpFramingError(
            "Step “${step.id}” is a numeric property but carries ${step.value}.",
        )

    // `packI16` clamps and `packU16` masks, both by design — but a value out of range here is
    // an upstream bug, not something to silently reshape, so it is refused first.
    val range = if (step.kind == StepKind.I16) -32768..32767 else 0..0xffff
    if (number !in range) {
        throw PtpFramingError(
            "Step “${step.id}” has value $number, outside ${range.first}..${range.last} for a " +
                "${step.kind} property.",
        )
    }

    return if (step.kind == StepKind.I16) packI16(number) else packU16(number)
}
