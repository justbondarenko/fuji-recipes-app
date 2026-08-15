package dev.bondarenko.fujirecipes.data.photo

import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-009 T-08.
 *
 * Mirrors `fuji-recipes-book/tests/unit/recipe-matcher.spec.ts` @ `0c17106`.
 *
 * The claim under test is the one the screen makes: *this photo is "Kodachrome 64"*. Getting
 * it wrong either way is bad in its own direction — a false match names the wrong recipe with
 * confidence, and a missed one sends the photographer off to create a duplicate of something
 * they already have.
 */
class RecipeMatcherTest {

    private fun recipe(id: String, name: String, settings: String) = Recipe(
        id = id,
        name = name,
        settings = Json.parseToJsonElement(settings).jsonObject,
    )

    private val kodachrome = recipe(
        "a",
        "Kodachrome 64",
        """
        {
          "filmSimulation": "classic-chrome",
          "dynamicRange": "dr400",
          "highlightTone": 2,
          "shadowTone": 1,
          "sharpness": 1,
          "whiteBalance": "daylight"
        }
        """.trimIndent(),
    )

    private val acros = recipe(
        "b",
        "Acros Night",
        """{"filmSimulation":"acros","dynamicRange":"dr200","highlightTone":-1}""",
    )

    /** What the parser would hand over for a photo shot with Kodachrome 64. */
    private val photoOfKodachrome = PhotoRecipe(
        cameraModel = "X-T50",
        filmSimulationLabel = "Classic Chrome",
        settings = mapOf(
            "filmSimulation" to "classic-chrome",
            "dynamicRange" to "dr400",
            "highlightTone" to 2.0,
            "shadowTone" to 1.0,
            "sharpness" to 1,
            "whiteBalance" to "daylight",
        ),
    )

    // ─── The answer the feature exists to give ──────────────────────────────

    @Test
    fun `a photo shot with a recipe in the library matches it exactly`() {
        val result = findMatches(photoOfKodachrome, listOf(kodachrome, acros))

        assertEquals(1, result.exact.size)
        assertEquals("Kodachrome 64", result.exact.single().recipe.name)
        assertEquals(100, result.exact.single().percentage)
        assertEquals("Kodachrome 64", result.best?.recipe?.name)
    }

    /**
     * A tone decoded from a photo is a Double; the same tone from JSON may be an Int. Comparing
     * the boxed types would report a mismatch on a field that matches exactly.
     */
    @Test
    fun `numbers compare by value rather than by type`() {
        val result = findMatches(photoOfKodachrome, listOf(kodachrome))

        assertTrue(result.exact.single().mismatches.isEmpty())
    }

    // ─── Scored, not boolean ────────────────────────────────────────────────

    @Test
    fun `a near match names what differs, with both values`() {
        val photo = photoOfKodachrome.copy(
            settings = photoOfKodachrome.settings + ("whiteBalance" to "shade"),
        )

        val match = findMatches(photo, listOf(kodachrome)).best!!

        assertFalse(match.isExact)
        assertEquals(83, match.percentage)

        val difference = match.mismatches.single()
        assertEquals("whiteBalance", difference.fieldId)
        assertEquals("Shade", difference.photoValue)
        assertEquals("Daylight", difference.savedValue)
    }

    /** Scoring out of everything would punish a recipe for values the photo never mentioned. */
    @Test
    fun `only the fields the photo carried are compared`() {
        val sparse = PhotoRecipe(settings = mapOf("filmSimulation" to "classic-chrome"))

        val match = findMatches(sparse, listOf(kodachrome)).best!!

        assertEquals(1, match.compared)
        assertTrue(match.isExact)
    }

    @Test
    fun `a field the recipe does not carry is a mismatch, named as not set`() {
        // Six of seven match; the seventh is a clarity the saved recipe never had. Enough
        // fields agree for it to still be offered, which is the case worth reporting.
        val photo = photoOfKodachrome.copy(
            settings = photoOfKodachrome.settings + ("clarity" to 2),
        )

        val match = findMatches(photo, listOf(kodachrome)).best!!

        val difference = match.mismatches.single()
        assertEquals("clarity", difference.fieldId)
        assertEquals("+2", difference.photoValue)
        assertEquals("Not set", difference.savedValue)
    }

    // ─── Ordering and the floor ─────────────────────────────────────────────

    @Test
    fun `matches are ordered exact first, then by how close they are`() {
        val nearlyKodachrome = recipe(
            "c",
            "Nearly",
            """
            {
              "filmSimulation": "classic-chrome",
              "dynamicRange": "dr400",
              "highlightTone": 2,
              "shadowTone": 1,
              "sharpness": 4,
              "whiteBalance": "daylight"
            }
            """.trimIndent(),
        )

        val result = findMatches(photoOfKodachrome, listOf(acros, nearlyKodachrome, kodachrome))

        assertEquals("Kodachrome 64", result.best?.recipe?.name)
        assertEquals(listOf("Nearly"), result.similar.map { it.recipe.name })
    }

    @Test
    fun `a recipe below the threshold is not offered as a match`() {
        val result = findMatches(photoOfKodachrome, listOf(acros))

        assertTrue(result.exact.isEmpty())
        assertTrue(result.similar.isEmpty())
        assertNull(result.best, "a distant recipe was offered as a match")
        assertEquals(1, result.recipesChecked)
    }

    /** The floor is where a match stops being worth naming. Both sides of it. */
    @Test
    fun `the threshold decides what is offered`() {
        // Four of six — 67%, under the floor. Not offered at all.
        val tooFar = photoOfKodachrome.copy(
            settings = photoOfKodachrome.settings +
                mapOf("sharpness" to 9, "whiteBalance" to "shade"),
        )
        val distant = findMatches(tooFar, listOf(kodachrome))
        assertNull(distant.best)
        assertTrue(distant.similar.isEmpty())

        // Five of six — 83%, over it.
        val closeEnough = photoOfKodachrome.copy(
            settings = photoOfKodachrome.settings + ("whiteBalance" to "shade"),
        )
        assertEquals(83, findMatches(closeEnough, listOf(kodachrome)).best!!.percentage)
    }

    // ─── Nothing to compare against ─────────────────────────────────────────

    @Test
    fun `an empty library offers nothing but does not fail`() {
        val result = findMatches(photoOfKodachrome, emptyList())

        assertTrue(result.exact.isEmpty())
        assertNull(result.best)
        assertEquals(0, result.recipesChecked)
    }

    @Test
    fun `a photo that decoded nothing matches nothing`() {
        val result = findMatches(PhotoRecipe(), listOf(kodachrome))

        assertNull(result.best)
    }

    /** A field this build does not know cannot be compared, so it is not counted. */
    @Test
    fun `a setting with no field definition is left out of the score`() {
        val photo = PhotoRecipe(
            settings = mapOf("filmSimulation" to "classic-chrome", "somethingNewer" to 1),
        )

        val match = findMatches(photo, listOf(kodachrome)).best!!

        assertEquals(1, match.compared)
    }
}
