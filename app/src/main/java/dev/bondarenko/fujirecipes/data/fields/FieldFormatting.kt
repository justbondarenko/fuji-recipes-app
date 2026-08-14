package dev.bondarenko.fujirecipes.data.fields

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How a stored value is written down — FEAT-002 T-04.
 *
 * Implements `fuji-recipes-book/specs/shared/field-definitions.md` §5, the display rules.
 * Pure, no Compose, no Android: §5 is a list of testable statements and this keeps it that
 * way (`coding-standards.md` P7).
 *
 * The rules exist because a parameter set is read at a glance, in the field, and small
 * inconsistencies cost real time — a bare `2` and a `+2` are different claims about whether
 * something was deliberately raised.
 */
object FieldFormatting {

    /** One row of the view: a label and a value, and whether the value is a default. */
    data class Row(
        val fieldId: String,
        val label: String,
        val value: String,
        val isDefault: Boolean,
        val advisory: Boolean,
    )

    /**
     * A formatted value, or null when §5 says the row is omitted.
     *
     * **An absent value falls back to the field's default rather than dropping the row.**
     * `data-model.md` §2 says a well-formed recipe stores every applicable key, but a recipe
     * written by an older client predates whatever was added since — and a parameter the
     * camera will apply at its default belongs on screen, at reduced emphasis, not missing.
     * Dropping it would make an old recipe look like it had fewer settings than it has.
     *
     * Null is then left to mean one thing: an advisory field that was never set (`isoMin`,
     * `isoMax`), whose default is itself null. "No minimum ISO" is not a value worth a row.
     */
    fun format(field: RecipeField, raw: Any?, generation: SensorGeneration): String? {
        val value = raw ?: field.defaultValue ?: return null

        return when (field) {
            is EnumFieldDef -> field.labelFor(value as? String).ifEmpty { null }
            is NumberField -> formatNumber(field, value, generation)
        }
    }

    private fun formatNumber(
        field: NumberField,
        raw: Any?,
        generation: SensorGeneration,
    ): String? {
        val number = (raw as? Number)?.toDouble() ?: return null

        // §5: `5500K`, no space. The one field with a unit.
        if (field.id == "colorTemperature") return "${number.roundToInt()}K"

        // ISO bounds are magnitudes, not offsets: "+3200" would be nonsense.
        if (field.id == "isoMin" || field.id == "isoMax") return number.roundToInt().toString()

        return signed(number, halfSteps = field.stepFor(generation) < 1.0)
    }

    /**
     * §5: explicit sign for non-zero (`+2`, `-1`), bare `0` for zero; one decimal on half
     * steps.
     *
     * Zero is bare because `+0` claims a deliberate nudge that did not happen — and zero is
     * the default for every signed parameter in the table, so it is the value most often on
     * screen.
     */
    fun signed(value: Double, halfSteps: Boolean): String {
        val magnitude = if (halfSteps && abs(value % 1.0) > 0.001) {
            // One decimal place, so +1.5 reads as a half step rather than as 1.500001.
            String.format("%.1f", abs(value))
        } else {
            abs(value).roundToInt().toString()
        }

        return when {
            value > 0 -> "+$magnitude"
            value < 0 -> "-$magnitude"
            else -> magnitude
        }
    }

    /**
     * §5: WB shift is `R +3 / B -2` as **one combined row**.
     *
     * Two stored fields, one displayed row, so it cannot fall out of the generic renderer —
     * which is the whole reason it has its own function rather than a special case buried in
     * a `when`.
     */
    const val WB_SHIFT_ROW_ID = "wbShift"

    fun wbShiftRow(settings: JsonObject): Row? {
        val red = settings.number("wbShiftRed") ?: 0.0
        val blue = settings.number("wbShiftBlue") ?: 0.0

        return Row(
            fieldId = WB_SHIFT_ROW_ID,
            label = "WB shift",
            value = "R ${signed(red, halfSteps = false)} / B ${signed(blue, halfSteps = false)}",
            isDefault = red == 0.0 && blue == 0.0,
            advisory = false,
        )
    }

    /**
     * Whether a stored value equals the field's default.
     *
     * Drives §5's reduced-emphasis rule and the "changed only" filter. An **absent** value
     * counts as the default: a recipe written before a field existed did not choose
     * anything, and showing it as a deliberate change would be a lie about the recipe.
     */
    fun isDefault(field: RecipeField, raw: Any?): Boolean {
        val default = field.defaultValue
        if (raw == null) return true
        if (default == null) return false

        return when {
            raw is Number && default is Number ->
                abs(raw.toDouble() - default.toDouble()) < 0.0001
            else -> raw.toString() == default.toString()
        }
    }

    /**
     * Every row the view should draw, in group order, for one recipe's settings.
     *
     * The WB shift pair is collapsed into its combined row and its two source fields are
     * dropped, so nothing is shown twice.
     */
    fun rowsFor(settings: JsonObject, context: FieldContext): List<Row> {
        val rows = mutableListOf<Row>()

        for (field in RecipeFields.applicable(context)) {
            if (field.id == "wbShiftRed") {
                wbShiftRow(settings)?.let { rows += it }
                continue
            }
            if (field.id == "wbShiftBlue") continue

            val raw = settings.value(field.id)
            val text = format(field, raw, context.generation) ?: continue

            rows += Row(
                fieldId = field.id,
                label = field.label,
                value = text,
                isDefault = isDefault(field, raw),
                advisory = field.advisory,
            )
        }

        return rows
    }

    /** A JSON value as the plain Kotlin type the field table talks in. */
    private fun JsonObject.value(key: String): Any? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (primitive.isString) return primitive.content
        primitive.booleanOrNull?.let { return it }
        primitive.doubleOrNull?.let { return it }
        return null
    }

    private fun JsonObject.number(key: String): Double? =
        runCatching { this[key]?.jsonPrimitive?.doubleOrNull }.getOrNull()
}
