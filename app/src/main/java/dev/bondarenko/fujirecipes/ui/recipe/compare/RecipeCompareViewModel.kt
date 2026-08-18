package dev.bondarenko.fujirecipes.ui.recipe.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.compare.RecipeComparison
import dev.bondarenko.fujirecipes.data.compare.RecipeComparisonGroup
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import dev.bondarenko.fujirecipes.ui.recipe.RecipeHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Lightweight representation of a library recipe candidate for the comparison selector.
 */
data class RecipeCandidate(
    val id: String,
    val name: String,
    val filmSimulationLabel: String,
    val rating: Int,
    val tags: List<String> = emptyList(),
)

data class RecipeCompareUiState(
    val isLoading: Boolean = true,
    val baseRecipe: RecipeHeader? = null,
    val targetRecipe: RecipeHeader? = null,
    val availableCandidates: List<RecipeCandidate> = emptyList(),
    val groups: List<RecipeComparisonGroup> = emptyList(),
    val differencesOnly: Boolean = false,
    val totalDifferences: Int = 0,
    val totalFields: Int = 0,
    val hasNoCandidates: Boolean = false,
) {
    val isNotFound: Boolean get() = !isLoading && baseRecipe == null
    val isSelectingTarget: Boolean get() = !isLoading && baseRecipe != null && targetRecipe == null && !hasNoCandidates
    val allMatch: Boolean get() = totalDifferences == 0 && totalFields > 0
}

class RecipeCompareViewModel(
    private val baseRecipeId: String,
    private val repository: RecipeRepository,
    initialTargetRecipeId: String? = null,
) : ViewModel() {

    private val selectedTargetRecipeId = MutableStateFlow(initialTargetRecipeId)
    private val differencesOnly = MutableStateFlow(false)

    val state: StateFlow<RecipeCompareUiState> = combine(
        repository.library,
        selectedTargetRecipeId,
        differencesOnly,
    ) { library, targetId, showDiffOnly ->
        if (!library.hasLoaded) return@combine RecipeCompareUiState(isLoading = true)

        val base = library.recipes.firstOrNull { it.id == baseRecipeId }
            ?: return@combine RecipeCompareUiState(isLoading = false, baseRecipe = null)

        val candidates = library.recipes
            .filter { it.id != baseRecipeId }
            .map {
                RecipeCandidate(
                    id = it.id,
                    name = it.name,
                    filmSimulationLabel = FilmSimulations.labelFor(it.filmSimulationId),
                    rating = it.rating,
                    tags = it.tags,
                )
            }

        if (candidates.isEmpty()) {
            return@combine RecipeCompareUiState(
                isLoading = false,
                baseRecipe = headerFor(base),
                hasNoCandidates = true,
            )
        }

        // If targetId is null, don't preselect — prompt user to choose what to compare with
        if (targetId == null) {
            return@combine RecipeCompareUiState(
                isLoading = false,
                baseRecipe = headerFor(base),
                targetRecipe = null,
                availableCandidates = candidates,
                hasNoCandidates = false,
            )
        }

        val target = library.recipes.firstOrNull { it.id == targetId }
        if (target == null) {
            return@combine RecipeCompareUiState(
                isLoading = false,
                baseRecipe = headerFor(base),
                targetRecipe = null,
                availableCandidates = candidates,
                hasNoCandidates = false,
            )
        }

        val comparison = RecipeComparison.compare(base, target)

        val displayedGroups = if (showDiffOnly) {
            comparison.groups.mapNotNull { group ->
                val diffRows = group.rows.filterNot { it.isSame }
                if (diffRows.isNotEmpty()) group.copy(rows = diffRows) else null
            }
        } else {
            comparison.groups
        }

        RecipeCompareUiState(
            isLoading = false,
            baseRecipe = headerFor(base),
            targetRecipe = headerFor(target),
            availableCandidates = candidates,
            groups = displayedGroups,
            differencesOnly = showDiffOnly,
            totalDifferences = comparison.differencesCount,
            totalFields = comparison.totalFields,
            hasNoCandidates = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecipeCompareUiState(),
    )

    fun onSelectTargetRecipe(id: String) = selectedTargetRecipeId.update { id }

    fun onClearTargetRecipe() = selectedTargetRecipeId.update { null }

    fun onDifferencesOnlyChange(enabled: Boolean) = differencesOnly.update { enabled }

    companion object {
        private fun headerFor(recipe: Recipe) = RecipeHeader(
            id = recipe.id,
            name = recipe.name,
            filmSimulationId = recipe.filmSimulationId,
            filmSimulationLabel = FilmSimulations.labelFor(recipe.filmSimulationId),
            rating = recipe.rating,
            tags = recipe.tags,
            notes = recipe.notes,
            images = recipe.images,
        )

        fun factory(
            container: AppContainer,
            baseRecipeId: String,
            initialTargetRecipeId: String? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecipeCompareViewModel(
                    baseRecipeId = baseRecipeId,
                    repository = container.recipeRepository,
                    initialTargetRecipeId = initialTargetRecipeId,
                ) as T
        }
    }
}
