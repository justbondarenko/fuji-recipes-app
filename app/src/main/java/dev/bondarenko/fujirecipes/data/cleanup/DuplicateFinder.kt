package dev.bondarenko.fujirecipes.data.cleanup

import dev.bondarenko.fujirecipes.camera.plan.areConfigsEqual
import dev.bondarenko.fujirecipes.data.compare.RecipeComparison
import dev.bondarenko.fujirecipes.data.model.Recipe

/**
 * A single setting field difference between two similar recipes.
 */
data class FieldDifference(
    val fieldId: String,
    val label: String,
    val valueA: String,
    val valueB: String,
)

/**
 * A pair of recipes that share the same film simulation and differ in only 1 to 3 fields.
 */
data class SimilarRecipePair(
    val recipeA: Recipe,
    val recipeB: Recipe,
    val differences: List<FieldDifference>,
)

/**
 * A group of 2 or more recipes that are 100% identical in camera settings.
 */
data class ExactDuplicateGroup(
    val id: String,
    val recipes: List<Recipe>,
    val defaultKeepId: String,
)

/**
 * Aggregated scan outcome containing exact duplicates and highly similar recipes.
 */
data class CleanupScanResult(
    val exactGroups: List<ExactDuplicateGroup>,
    val similarPairs: List<SimilarRecipePair>,
    val totalRecipesScanned: Int,
) {
    val totalDuplicateRecipesCount: Int
        get() = exactGroups.sumOf { it.recipes.size - 1 }

    val isEmpty: Boolean
        get() = exactGroups.isEmpty() && similarPairs.isEmpty()
}

object DuplicateFinder {

    /**
     * Scans the provided list of recipes for:
     * 1. 100% match duplicates (clustered into [ExactDuplicateGroup]).
     * 2. Highly similar recipes (pairs with identical film simulation and 1 to 3 setting differences).
     */
    fun findDuplicates(recipes: List<Recipe>): CleanupScanResult {
        if (recipes.size < 2) {
            return CleanupScanResult(
                exactGroups = emptyList(),
                similarPairs = emptyList(),
                totalRecipesScanned = recipes.size,
            )
        }

        // ─── 1. Find 100% Match Duplicate Groups ─────────────────────────────
        val visitedForExact = BooleanArray(recipes.size)
        val exactGroups = mutableListOf<ExactDuplicateGroup>()
        // Map recipe id to group id for quick lookup
        val exactGroupMap = mutableMapOf<String, Int>()

        for (i in recipes.indices) {
            if (visitedForExact[i]) continue

            val groupMembers = mutableListOf(recipes[i])
            visitedForExact[i] = true

            for (j in (i + 1) until recipes.size) {
                if (!visitedForExact[j] && areConfigsEqual(recipes[i].settings, recipes[j].settings)) {
                    visitedForExact[j] = true
                    groupMembers += recipes[j]
                }
            }

            if (groupMembers.size > 1) {
                val groupId = "group_${exactGroups.size + 1}"
                // Choose default keep: highest rating, or newest updatedAt, or first
                val defaultKeep = groupMembers.maxWithOrNull(
                    compareBy<Recipe> { it.rating }
                        .thenBy { it.updatedAt }
                        .thenBy { it.createdAt },
                )?.id ?: groupMembers.first().id

                val group = ExactDuplicateGroup(
                    id = groupId,
                    recipes = groupMembers,
                    defaultKeepId = defaultKeep,
                )
                exactGroups += group

                val groupIdx = exactGroups.lastIndex
                groupMembers.forEach { exactGroupMap[it.id] = groupIdx }
            }
        }

        // ─── 2. Find Highly Similar Pairs (1–3 Differences, Same Film Sim) ───
        val similarPairs = mutableListOf<SimilarRecipePair>()
        val seenPairs = mutableSetOf<Pair<String, String>>()

        for (i in recipes.indices) {
            for (j in (i + 1) until recipes.size) {
                val recipeA = recipes[i]
                val recipeB = recipes[j]

                // If both belong to the exact same duplicate group, they are 100% matches, not similar
                val groupA = exactGroupMap[recipeA.id]
                val groupB = exactGroupMap[recipeB.id]
                if (groupA != null && groupA == groupB) continue

                // 1. Film simulation MUST match! ("not including film simulation")
                val simA = recipeA.filmSimulationId ?: "provia"
                val simB = recipeB.filmSimulationId ?: "provia"
                if (simA != simB) continue

                // 2. Compare face-to-face over all applicable fields
                val comp = RecipeComparison.compare(recipeA, recipeB)
                val nonMatchingRows = comp.groups
                    .flatMap { it.rows }
                    .filter { !it.isSame && it.fieldId != "filmSimulation" }

                // 3. Difference must be in 1, 2, or 3 fields only
                if (nonMatchingRows.size in 1..3) {
                    val pairKey = if (recipeA.id < recipeB.id) recipeA.id to recipeB.id else recipeB.id to recipeA.id
                    if (seenPairs.add(pairKey)) {
                        val differences = nonMatchingRows.map { row ->
                            FieldDifference(
                                fieldId = row.fieldId,
                                label = row.label,
                                valueA = row.valueA,
                                valueB = row.valueB,
                            )
                        }
                        similarPairs += SimilarRecipePair(
                            recipeA = recipeA,
                            recipeB = recipeB,
                            differences = differences,
                        )
                    }
                }
            }
        }

        return CleanupScanResult(
            exactGroups = exactGroups,
            similarPairs = similarPairs,
            totalRecipesScanned = recipes.size,
        )
    }
}
