package dev.bondarenko.fujirecipes.data.fields

import androidx.compose.ui.graphics.Color

/**
 * The film simulations — FEAT-001 T-01.
 *
 * **Transcribed, not invented.**
 *
 * | | |
 * |---|---|
 * | Source document | `fuji-recipes-book/specs/shared/field-definitions.md` §1 and §3, version 1 |
 * | Sibling transcription | `fuji-recipes-book/shared/recipe-fields.ts` (`FILM_SIMULATIONS`) |
 * | Read at commit | `b802b1dcebde6a4ee204311095b5b893bfed8997` |
 *
 * Two transcriptions of one document, never a transcription of a transcription
 * (`steering/architecture.md` §2). When the document changes, both move.
 *
 * The swatch is a real value rather than a fallback: each simulation renders as a clean,
 * fixed color swatch approximating its look, with a grey swatch fallback for unknown simulations.
 */
data class FilmSimulation(
    val id: String,
    val label: String,
    val monochrome: Boolean,
    val minGeneration: SensorGeneration,
    val swatch: Color,
)

/**
 * Sensor generations, ordered for compatibility comparisons — §1 of the field definitions.
 *
 * `ordinal` is the comparison order the document specifies
 * (`cmos` < `xtransIII` < `xtransIV` < `xtransV` = `gfx`), so declaration order here is
 * load-bearing. GFX is deliberately equal to X-Trans V rather than above it: the document
 * says to treat it as the X-Trans V field set until it is verified per body.
 *
 * Note this is **not** a stored column any more — D1 migration 0002 dropped it, and it
 * survives only inside `extra` on older rows (`steering/architecture.md` §7). It is here
 * because §3's minimum-generation column needs it, not because the list reads it.
 */
enum class SensorGeneration(val id: String, val label: String, val hasCustomSlots: Boolean) {
    CMOS("cmos", "Bayer CMOS", hasCustomSlots = false),
    XTRANS_III("xtrans-iii", "X-Trans III", hasCustomSlots = false),
    XTRANS_IV("xtrans-iv", "X-Trans IV", hasCustomSlots = true),
    XTRANS_V("xtrans-v", "X-Trans V", hasCustomSlots = true),
    GFX("gfx", "GFX", hasCustomSlots = true);

    companion object {
        fun fromId(id: String?): SensorGeneration? = entries.firstOrNull { it.id == id }
    }
}

object FilmSimulations {

    /** The unknown-simulation swatch, matching the web badge's `#9CA3AF` fallback. */
    val FallbackSwatch = Color(0xFF9CA3AF)

    val all: List<FilmSimulation> = listOf(
        FilmSimulation("provia", "PROVIA / Standard", false, SensorGeneration.XTRANS_III, Color(0xFF8C7B6B)),
        FilmSimulation("velvia", "Velvia / Vivid", false, SensorGeneration.XTRANS_III, Color(0xFFA83A2E)),
        FilmSimulation("astia", "ASTIA / Soft", false, SensorGeneration.XTRANS_III, Color(0xFFC08B7A)),
        FilmSimulation("classic-chrome", "Classic Chrome", false, SensorGeneration.XTRANS_III, Color(0xFF6E7A78)),
        FilmSimulation("pro-neg-hi", "PRO Neg. Hi", false, SensorGeneration.XTRANS_III, Color(0xFF9C8878)),
        FilmSimulation("pro-neg-std", "PRO Neg. Std", false, SensorGeneration.XTRANS_III, Color(0xFF9C8878)),
        FilmSimulation("classic-negative", "Classic Negative", false, SensorGeneration.XTRANS_IV, Color(0xFF7B6A5A)),
        FilmSimulation("nostalgic-negative", "Nostalgic Neg.", false, SensorGeneration.XTRANS_V, Color(0xFFA5703F)),
        FilmSimulation("reala-ace", "REALA ACE", false, SensorGeneration.XTRANS_V, Color(0xFF8A7150)),
        FilmSimulation("eterna", "ETERNA / Cinema", false, SensorGeneration.XTRANS_IV, Color(0xFF6B6659)),
        FilmSimulation("eterna-bleach-bypass", "ETERNA Bleach Bypass", false, SensorGeneration.XTRANS_IV, Color(0xFF6B6659)),
        FilmSimulation("acros", "ACROS", true, SensorGeneration.XTRANS_III, Color(0xFF5A5A5A)),
        FilmSimulation("acros-ye", "ACROS + Ye filter", true, SensorGeneration.XTRANS_III, Color(0xFF5A5A5A)),
        FilmSimulation("acros-r", "ACROS + R filter", true, SensorGeneration.XTRANS_III, Color(0xFF5A5A5A)),
        FilmSimulation("acros-g", "ACROS + G filter", true, SensorGeneration.XTRANS_III, Color(0xFF5A5A5A)),
        FilmSimulation("monochrome", "Monochrome", true, SensorGeneration.XTRANS_III, Color(0xFF7A7A7A)),
        FilmSimulation("monochrome-ye", "Monochrome + Ye", true, SensorGeneration.XTRANS_III, Color(0xFF7A7A7A)),
        FilmSimulation("monochrome-r", "Monochrome + R", true, SensorGeneration.XTRANS_III, Color(0xFF7A7A7A)),
        FilmSimulation("monochrome-g", "Monochrome + G", true, SensorGeneration.XTRANS_III, Color(0xFF7A7A7A)),
        FilmSimulation("sepia", "Sepia", true, SensorGeneration.XTRANS_III, Color(0xFF8B6F47)),
    )

    private val byId: Map<String, FilmSimulation> = all.associateBy { it.id }

    /**
     * **Null for an unknown id rather than a throw.**
     *
     * A stored recipe may hold a simulation this build has never heard of — a newer web
     * client can write one, and `data-model.md` §1 requires it to survive. A list that
     * crashed on such a recipe would be a worse failure than a card with a grey swatch, so
     * every caller is made to think about the absent case (`coding-standards.md` P2).
     */
    fun byId(id: String?): FilmSimulation? = id?.let { byId[it] }

    /** The label to show, falling back to the raw id so the row still says *something*. */
    fun labelFor(id: String?): String = byId(id)?.label ?: id.orEmpty()

    fun swatchFor(id: String?): Color = byId(id)?.swatch ?: FallbackSwatch
}
