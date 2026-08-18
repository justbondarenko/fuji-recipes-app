package dev.bondarenko.fujirecipes.data.importing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipesCollectionImportTest {

    private val collectionDir = File("../recipes-collection")

    @Test
    fun `all individual and combined json files in recipes-collection parse and validate cleanly`() {
        if (!collectionDir.exists()) return
        val jsonFiles = collectionDir.listFiles { _, name -> name.endsWith(".json") }
        assertTrue(jsonFiles != null && jsonFiles.isNotEmpty(), "Found no JSON files in recipes-collection")

        println("Testing ${jsonFiles.size} JSON files...")

        for (file in jsonFiles) {
            val text = file.readText()
            val parsed = readJsonFile(text, file.name)
            assertTrue(parsed.recipes.isNotEmpty(), "${file.name} had no recipes parsed")

            val rows = reviewFile(parsed.recipes, emptyList())
            for (row in rows) {
                if (!row.importable || row.errors.isNotEmpty()) {
                    println("FAILED FILE: ${file.name}, recipe: '${row.name}', status: ${row.status}, errors: ${row.errors.map { "${it.fieldId}: ${it.message}" }}")
                }
                assertTrue(
                    row.importable,
                    "Recipe '${row.name}' in ${file.name} is not importable (${row.status}): ${row.errors.map { "${it.fieldId}: ${it.message}" }}",
                )
                assertTrue(row.errors.isEmpty(), "Recipe '${row.name}' in ${file.name} has errors: ${row.errors}")
            }
        }
    }
}
