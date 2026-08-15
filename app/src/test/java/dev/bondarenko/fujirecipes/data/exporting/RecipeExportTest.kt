package dev.bondarenko.fujirecipes.data.exporting

import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * FEAT-008 T-06 — a conformance suite, not a unit test.
 *
 * `recipe-format.spec.md` is **binding** and shared with the web client: a file written here
 * must import cleanly there. So the fixture below is the spec's own §3 example, pasted, and
 * the assertions are about bytes and key order rather than "it produced some JSON".
 *
 * A test that compared this implementation against itself would prove only that it is
 * consistent, which is not the promise being made.
 */
class RecipeExportTest {

    /** `recipe-format.spec.md` §3, verbatim. */
    private val specExample = """
        {
          "id": "3f2a5c18-6b4e-4f0a-9c21-8d7e5b3a1f60",
          "name": "Kodachrome 64",
          "notes": "Good in overcast light. Underexpose 1/3 in sun.",
          "rating": 5,
          "tags": ["street", "warm"],
          "sensorGeneration": "xtrans-v",
          "settings": {
            "filmSimulation": "classic-chrome",
            "dynamicRange": "dr400",
            "dRangePriority": "off",
            "highlightTone": 1.5,
            "shadowTone": 2,
            "color": 2,
            "sharpness": 1,
            "highIsoNR": -4,
            "clarity": 0,
            "grainEffect": "weak",
            "grainSize": "small",
            "colorChromeEffect": "strong",
            "colorChromeFxBlue": "weak",
            "whiteBalance": "color-temp",
            "colorTemperature": 5500,
            "wbShiftRed": 3,
            "wbShiftBlue": -2,
            "monochromaticColorWc": null,
            "monochromaticColorMg": null,
            "exposureCompensation": 0.667,
            "isoMin": 160,
            "isoMax": 6400
          },
          "createdAt": "2026-06-01T09:12:00.000Z",
          "updatedAt": "2026-07-20T18:44:00.000Z"
        }
    """.trimIndent()

    private fun recipeFrom(json: String): Recipe =
        Recipe.fromJson(Json.parseToJsonElement(json).jsonObject)

    private val kodachrome = recipeFrom(specExample)

    private val at = Instant.parse("2026-08-04T10:00:00.000Z")

    // ─── §3: the recipe object ──────────────────────────────────────────────

    @Test
    fun `the spec's own example exports with the spec's key order`() {
        val exported = toExportRecipe(kodachrome)

        assertContentEquals(
            listOf("id", "name", "notes", "rating", "tags", "settings", "createdAt", "updatedAt"),
            exported.keys.toList(),
        )
    }

    @Test
    fun `the spec's own settings order is preserved exactly`() {
        val settings = toExportRecipe(kodachrome)["settings"]!!.jsonObject

        assertContentEquals(SETTINGS_KEY_ORDER, settings.keys.toList())
    }

    @Test
    fun `every value survives unchanged`() {
        val exported = toExportRecipe(kodachrome)
        val settings = exported["settings"]!!.jsonObject

        assertEquals("Kodachrome 64", exported["name"]!!.jsonPrimitive.content)
        assertEquals(5, exported["rating"]!!.jsonPrimitive.content.toInt())
        assertEquals(listOf("street", "warm"), exported["tags"]!!.jsonArray.map { it.jsonPrimitive.content })
        // Not reformatted: 1.5 stays 1.5 and 0.667 stays 0.667, because the value came from
        // the server already validated and this build does not repair what it received.
        assertEquals("1.5", settings["highlightTone"]!!.jsonPrimitive.content)
        assertEquals("0.667", settings["exposureCompensation"]!!.jsonPrimitive.content)
        assertEquals("-4", settings["highIsoNR"]!!.jsonPrimitive.content)
    }

    /** §4: how "not applicable to this body" survives as different from "key absent". */
    @Test
    fun `null stays null`() {
        val settings = toExportRecipe(kodachrome)["settings"]!!.jsonObject

        assertEquals(JsonNull, settings["monochromaticColorWc"])
        assertTrue(settings.containsKey("monochromaticColorMg"))
    }

    // ─── SF-008: what never leaves ──────────────────────────────────────────

    @Test
    fun `local bookkeeping and the dropped column are excluded`() {
        val recipe = recipeFrom(
            """
            {
              "id": "a", "name": "X", "settings": {},
              "sortKey": 1000.0, "lastWrittenSlot": 3,
              "lastWrittenAt": "2026-08-01T00:00:00.000Z",
              "sensorGeneration": "xtrans-v"
            }
            """.trimIndent(),
        )

        val exported = toExportRecipe(recipe)

        EXPORT_EXCLUDED.forEach { key ->
            assertFalse(exported.containsKey(key), "$key leaked into the file")
        }
    }

    @Test
    fun `the spec example's sensor generation does not survive export`() {
        assertFalse(toExportRecipe(kodachrome).containsKey("sensorGeneration"))
    }

    // ─── SF-017: nothing is lost ────────────────────────────────────────────

    @Test
    fun `an unknown top-level key survives, after the documented ones`() {
        val recipe = recipeFrom(
            """{"id":"a","name":"X","settings":{},"somethingNewer":"kept"}""",
        )

        val exported = toExportRecipe(recipe)

        assertEquals("kept", exported["somethingNewer"]!!.jsonPrimitive.content)
        assertTrue(
            exported.keys.indexOf("somethingNewer") > exported.keys.indexOf("settings"),
            "an unknown key came before a documented one",
        )
    }

    @Test
    fun `an unknown setting survives, after the documented ones`() {
        val recipe = recipeFrom(
            """{"id":"a","name":"X","settings":{"clarity":2,"newerField":"kept"}}""",
        )

        val settings = toExportRecipe(recipe)["settings"]!!.jsonObject

        assertEquals("kept", settings["newerField"]!!.jsonPrimitive.content)
        assertContentEquals(listOf("clarity", "newerField"), settings.keys.toList())
    }

    /** Defaults are filled on import. Inventing them here would put values in the file. */
    @Test
    fun `a setting the recipe never had is left out rather than defaulted`() {
        val recipe = recipeFrom("""{"id":"a","name":"X","settings":{"clarity":2}}""")

        val settings = toExportRecipe(recipe)["settings"]!!.jsonObject

        assertContentEquals(listOf("clarity"), settings.keys.toList())
    }

    @Test
    fun `settings that are not an object are passed through rather than replaced`() {
        val recipe = recipeFrom("""{"id":"a","name":"X","settings":{}}""")

        assertTrue(toExportRecipe(recipe)["settings"]!!.jsonObject.isEmpty())
    }

    // ─── §2: the envelope ───────────────────────────────────────────────────

    @Test
    fun `the envelope carries the format, the version and the moment`() {
        val envelope = buildEnvelope(listOf(kodachrome), at)

        assertContentEquals(
            listOf("format", "version", "exportedAt", "recipes"),
            envelope.keys.toList(),
        )
        assertEquals("fuji-recipe", envelope["format"]!!.jsonPrimitive.content)
        assertEquals("1", envelope["version"]!!.jsonPrimitive.content)
        assertEquals("2026-08-04T10:00:00.000Z", envelope["exportedAt"]!!.jsonPrimitive.content)
        assertEquals(1, envelope["recipes"]!!.jsonArray.size)
    }

    /** The format pins a key order so a diff of two backups is readable. */
    @Test
    fun `the file is indented two spaces and ends with a newline`() {
        val text = exportRecipes(listOf(kodachrome), at)

        assertTrue(text.endsWith("\n"), "no trailing newline")
        assertTrue(text.contains("\n  \"format\""), "not indented two spaces")
        assertFalse(text.startsWith("﻿"), "a BOM crept in")
    }

    @Test
    fun `a single recipe exports bare, with no envelope`() {
        val text = exportRecipe(kodachrome)
        val parsed = Json.parseToJsonElement(text).jsonObject

        assertFalse(parsed.containsKey("format"))
        assertFalse(parsed.containsKey("recipes"))
        assertEquals("Kodachrome 64", parsed["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `only the recipes given are in the file`() {
        val second = recipeFrom("""{"id":"b","name":"Second","settings":{}}""")

        val envelope = buildEnvelope(listOf(kodachrome, second), at)

        assertEquals(2, envelope["recipes"]!!.jsonArray.size)
    }

    // ─── SF-018: filenames ──────────────────────────────────────────────────

    @Test
    fun `a library export is named by its UTC date`() {
        // 00:30 UTC on the 5th is still the 4th in a western timezone. The format is UTC
        // everywhere else, and two clients disagreeing by a day looks like a missing backup.
        val justAfterMidnight = Instant.parse("2026-08-05T00:30:00.000Z")

        assertEquals(
            "fuji-recipes-2026-08-05.json",
            libraryExportFilename(ExportKind.JSON, justAfterMidnight),
        )
        assertEquals(
            "fuji-recipes-2026-08-05.zip",
            libraryExportFilename(ExportKind.ZIP, justAfterMidnight),
        )
    }

    @Test
    fun `a single recipe is named after itself`() {
        assertEquals("kodachrome-64.json", recipeExportFilename(kodachrome))
    }

    @Test
    fun `punctuation collapses to single dashes and the ends are trimmed`() {
        assertEquals("portra-400-warm-soft", slugify("Portra 400 — warm / soft"))
        assertEquals("acros", slugify("  ACROS!  "))
    }

    /** A recipe named "Cinéma" should not become "cin-ma". */
    @Test
    fun `letters outside ASCII are kept`() {
        assertEquals("cinéma", slugify("Cinéma"))
    }

    @Test
    fun `a name that slugifies to nothing falls back to the id, not to a generic name`() {
        val recipe = recipeFrom("""{"id":"3f2a5c18-6b4e","name":"!!!","settings":{}}""")

        assertEquals("recipe-3f2a5c18.json", recipeExportFilename(recipe))
    }

    // ─── The archive ────────────────────────────────────────────────────────

    @Test
    fun `an archive holds one bare recipe per entry`() {
        val second = recipeFrom("""{"id":"b","name":"Acros Night","settings":{}}""")

        val entries = archiveEntries(listOf(kodachrome, second))

        assertContentEquals(listOf("kodachrome-64.json", "acros-night.json"), entries.keys.toList())
        assertFalse(entries.values.first().contains("\"format\""))
    }

    /**
     * The case that would silently lose a recipe: names are not unique, and a map keyed by
     * name would hold one entry where there were two.
     */
    @Test
    fun `two recipes with one name both survive, the later one suffixed`() {
        val first = recipeFrom("""{"id":"a","name":"Portra 400","settings":{"clarity":1}}""")
        val second = recipeFrom("""{"id":"b","name":"Portra 400","settings":{"clarity":2}}""")
        val third = recipeFrom("""{"id":"c","name":"Portra 400","settings":{"clarity":3}}""")

        val entries = archiveEntries(listOf(first, second, third))

        assertEquals(3, entries.size)
        assertContentEquals(
            listOf("portra-400.json", "portra-400-2.json", "portra-400-3.json"),
            entries.keys.toList(),
        )
        // And they are different recipes, not the same one three times.
        assertTrue(entries["portra-400.json"]!!.contains("\"clarity\": 1"))
        assertTrue(entries["portra-400-3.json"]!!.contains("\"clarity\": 3"))
    }

    // ─── The key order is not allowed to drift from the field source ────────

    /**
     * Membership, not order. A new settings field must not be silently left out of the
     * serialisation order — and if one ever is, the unknown-key pass still exports it, so this
     * is a warning about drift rather than a data-loss guard.
     */
    @Test
    fun `every settings field the app knows appears in the key order`() {
        RecipeFields.all.forEach { field ->
            assertTrue(
                SETTINGS_KEY_ORDER.contains(field.id),
                "${field.id} is a field but has no place in the export order",
            )
        }
    }

    @Test
    fun `the key order carries nothing twice`() {
        assertEquals(SETTINGS_KEY_ORDER.size, SETTINGS_KEY_ORDER.toSet().size)
        assertEquals(RECIPE_KEY_ORDER.size, RECIPE_KEY_ORDER.toSet().size)
    }
}
