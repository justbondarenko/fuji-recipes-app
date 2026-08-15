package dev.bondarenko.fujirecipes.ui.recipe

/**
 * Formats a recipe and its settings into a clean, human-readable plain text document
 * suitable for sharing or copying to the clipboard.
 */
object RecipeTextFormatter {

    fun format(recipe: RecipeHeader, groups: List<SettingsGroup>): String = buildString {
        appendLine(recipe.name)
        appendLine("Film simulation: ${recipe.filmSimulationLabel}")

        if (recipe.rating > 0) {
            val stars = "★".repeat(recipe.rating) + "☆".repeat((5 - recipe.rating).coerceAtLeast(0))
            appendLine("Rating: $stars (${recipe.rating}/5)")
        }

        if (recipe.tags.isNotEmpty()) {
            appendLine("Tags: ${recipe.tags.joinToString(", ")}")
        }

        groups.forEach { group ->
            if (group.rows.isNotEmpty()) {
                appendLine()
                appendLine(group.group.label)
                group.rows.forEach { row ->
                    appendLine("• ${row.label}: ${row.value}")
                }
            }
        }

        if (recipe.notes.isNotBlank()) {
            appendLine()
            appendLine("Notes:")
            appendLine(recipe.notes.trim())
        }
    }.trim()
}
