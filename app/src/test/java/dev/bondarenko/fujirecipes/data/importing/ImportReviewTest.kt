package dev.bondarenko.fujirecipes.data.importing

import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-007 T-11.
 *
 * Mirrors `fuji-recipes-book/tests/unit/import-review.spec.ts` @ `0c17106`, minus its
 * id-conflict cases — nothing read off a camera carries an id, so that status cannot arise.
 *
 * The claim under test is the one the screen makes to the photographer: this slot is new, or
 * you already have it. A false negative fills the library with copies of itself; a false
 * positive hides a recipe they wanted.
 */
class ImportReviewTest {

    private val classicChrome = mapOf<String, Any?>(
        "filmSimulation" to "classic-chrome",
        "dynamicRange" to "dr400",
        "highlightTone" to 1.5,
        "sharpness" to 0,
    )

    private val velvia = classicChrome + ("filmSimulation" to "velvia")

    private fun slot(number: Int, name: String, settings: Map<String, Any?> = classicChrome) =
        SlotRecipe(number, name, settings.filterValues { it != null }.mapValues { it.value!! })

    private fun recipe(id: String, name: String, settings: Map<String, Any?> = classicChrome) =
        Recipe(id = id, name = name, settings = settings.toJsonObject())

    // ─── Statuses ───────────────────────────────────────────────────────────

    @Test
    fun `a slot the library does not have is new and selected`() {
        val rows = reviewSlots(listOf(slot(1, "Kodachrome 64")), emptyList())

        assertEquals(ImportStatus.NEW, rows.single().status)
        assertTrue(rows.single().selected)
        assertNull(rows.single().existingName)
    }

    @Test
    fun `a slot matching a library recipe is a duplicate, deselected, and names the match`() {
        val rows = reviewSlots(
            listOf(slot(3, "1970s Summer")),
            listOf(recipe("a", "Summer 70s")),
        )

        val row = rows.single()
        assertEquals(ImportStatus.CONFIG_DUPLICATE, row.status)
        assertEquals("Summer 70s", row.existingName)
        assertFalse(row.selected, "a duplicate must not be imported by default")
    }

    /** The match is on settings. The name is not part of a camera configuration. */
    @Test
    fun `the duplicate match ignores the name entirely`() {
        val rows = reviewSlots(
            listOf(slot(1, "Completely Different Name")),
            listOf(recipe("a", "Kodachrome 64")),
        )

        assertEquals(ImportStatus.CONFIG_DUPLICATE, rows.single().status)
    }

    @Test
    fun `a difference in any applicable setting makes it new`() {
        val rows = reviewSlots(
            // A different name too, or this would be reported as a name warning instead —
            // which is a different fact about the same row.
            listOf(slot(1, "Sharper", classicChrome + ("sharpness" to 3))),
            listOf(recipe("a", "Kodachrome 64")),
        )

        assertEquals(ImportStatus.NEW, rows.single().status)
    }

    /**
     * SF-011's reasoning, and it survives here: names are not unique, so a name collision
     * with different settings is a warning rather than a refusal.
     */
    @Test
    fun `a shared name with different settings is a warning, and still selected`() {
        val rows = reviewSlots(
            listOf(slot(1, "Kodachrome 64", velvia)),
            listOf(recipe("a", "Kodachrome 64")),
        )

        val row = rows.single()
        assertEquals(ImportStatus.NAME_WARNING, row.status)
        assertEquals("Kodachrome 64", row.existingName)
        assertTrue(row.selected)
    }

    /** A duplicate is a duplicate whatever it is called — the settings decide first. */
    @Test
    fun `a slot that both matches settings and shares a name is reported as a duplicate`() {
        val rows = reviewSlots(
            listOf(slot(1, "Kodachrome 64")),
            listOf(recipe("a", "Kodachrome 64")),
        )

        assertEquals(ImportStatus.CONFIG_DUPLICATE, rows.single().status)
    }

    // ─── Within one read ────────────────────────────────────────────────────

    @Test
    fun `two identical slots do not both import, and the second names the first`() {
        val rows = reviewSlots(
            listOf(slot(1, "First Copy"), slot(4, "Second Copy")),
            emptyList(),
        )

        assertEquals(ImportStatus.NEW, rows[0].status)
        assertTrue(rows[0].selected)

        assertEquals(ImportStatus.CONFIG_DUPLICATE, rows[1].status)
        assertEquals("First Copy", rows[1].existingName)
        assertFalse(rows[1].selected)
    }

    @Test
    fun `rows come back in slot order`() {
        val rows = reviewSlots(
            listOf(slot(2, "B", velvia), slot(5, "E", classicChrome)),
            emptyList(),
        )

        assertEquals(listOf(2, 5), rows.map { it.slot })
    }

    // ─── Summary ────────────────────────────────────────────────────────────

    @Test
    fun `the summary counts each status and what will be imported`() {
        val rows = reviewSlots(
            listOf(
                slot(1, "Brand New", velvia),
                slot(2, "Whatever It Is Called"),
                slot(3, "Held", classicChrome + ("clarity" to 4)),
            ),
            listOf(recipe("a", "Held")),
        )

        val summary = summarise(rows)

        assertEquals(3, summary.total)
        assertEquals(1, summary.new)
        assertEquals(1, summary.duplicates)
        assertEquals(1, summary.warnings)
        // The duplicate is the one left out.
        assertEquals(2, summary.selected)
    }

    /** Worth saying rather than implying: an empty-looking screen needs a reason. */
    @Test
    fun `a camera holding only recipes already in the library says nothing is new`() {
        val rows = reviewSlots(listOf(slot(1, "A"), slot(2, "B")), listOf(recipe("a", "A")))

        assertTrue(summarise(rows).nothingNew)
    }

    @Test
    fun `an empty read is not the same as nothing new`() {
        assertFalse(summarise(emptyList()).nothingNew)
    }

    // ─── The request body ───────────────────────────────────────────────────

    @Test
    fun `only the selected rows are sent`() {
        val rows = reviewSlots(
            listOf(slot(1, "New One", velvia), slot(2, "Already Held")),
            listOf(recipe("a", "Held")),
        )

        val recipes = importBody(rows)["recipes"]!!.jsonArray

        assertEquals(1, recipes.size)
        assertEquals("New One", recipes[0].jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a duplicate the photographer selected is sent`() {
        val rows = reviewSlots(listOf(slot(2, "Already Held")), listOf(recipe("a", "Held")))
            .map { it.copy(selected = true) }

        assertEquals(1, importBody(rows)["recipes"]!!.jsonArray.size)
    }

    /** P2: the server assigns ids, so the phone must not send one. */
    @Test
    fun `no recipe in the body carries an id`() {
        val rows = reviewSlots(listOf(slot(1, "New One")), emptyList())

        val recipe = importBody(rows)["recipes"]!!.jsonArray.single().jsonObject

        assertFalse(recipe.containsKey("id"))
        assertEquals(setOf("name", "settings"), recipe.keys)
    }

    /** With no ids nothing can collide, so there is nothing to resolve. */
    @Test
    fun `the resolutions map is always empty`() {
        val rows = reviewSlots(listOf(slot(1, "New One")), emptyList())

        assertTrue(importBody(rows)["resolutions"]!!.jsonObject.isEmpty())
    }

    @Test
    fun `settings survive the round trip into the request body`() {
        val rows = reviewSlots(listOf(slot(1, "New One")), emptyList())

        val settings = importBody(rows)["recipes"]!!.jsonArray.single()
            .jsonObject["settings"]!!.jsonObject

        assertEquals("classic-chrome", settings["filmSimulation"]!!.jsonPrimitive.content)
        assertEquals("1.5", settings["highlightTone"]!!.jsonPrimitive.content)
        // Whole numbers go out as integers, matching what the rest of the platform stores.
        assertEquals("0", settings["sharpness"]!!.jsonPrimitive.content)
    }

    @Test
    fun `nothing selected sends no recipes`() {
        val rows = reviewSlots(listOf(slot(1, "A")), listOf(recipe("a", "A")))

        assertTrue(importBody(rows)["recipes"]!!.jsonArray.isEmpty())
    }
}
