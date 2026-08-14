package dev.bondarenko.fujirecipes.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One group of rows, as the screen draws it. */
data class SettingsGroup(
    val group: FieldGroup,
    val rows: List<FieldFormatting.Row>,
)

data class RecipeViewUiState(
    val isLoading: Boolean = true,
    /** Null once loaded means the id is not in the library — the not-found state. */
    val recipe: RecipeHeader? = null,
    val groups: List<SettingsGroup> = emptyList(),
    val changedOnly: Boolean = false,
    /** True when `changedOnly` is on and the recipe is entirely at its defaults. */
    val nothingChanged: Boolean = false,
) {
    val isNotFound: Boolean get() = !isLoading && recipe == null
}

/** What the header draws — the identity fields, which live on the recipe, not in settings. */
data class RecipeHeader(
    val id: String,
    val name: String,
    val filmSimulationId: String?,
    val filmSimulationLabel: String,
    val rating: Int,
    val tags: List<String>,
    val notes: String,
)

/**
 * FEAT-002 T-07.
 *
 * **Reads from the already-loaded library, not from `GET /api/recipes/:id`.** The whole
 * library is in memory (`architecture.md` §4), so a per-recipe fetch would add a network
 * dependency to a screen that has every byte it needs — and would fail offline for data the
 * app is holding.
 */
class RecipeViewModel(
    private val recipeId: String,
    repository: RecipeRepository,
) : ViewModel() {

    private val changedOnly = MutableStateFlow(false)

    val state: StateFlow<RecipeViewUiState> =
        combine(repository.library, changedOnly) { library, filterToChanged ->
            if (!library.hasLoaded) return@combine RecipeViewUiState(isLoading = true)

            val recipe = library.recipes.firstOrNull { it.id == recipeId }
                ?: return@combine RecipeViewUiState(isLoading = false, recipe = null)

            val context = contextFor(recipe)
            val allRows = FieldFormatting.rowsFor(recipe.settings, context)
            val rows = if (filterToChanged) allRows.filterNot { it.isDefault } else allRows

            RecipeViewUiState(
                isLoading = false,
                recipe = headerFor(recipe),
                groups = groupRows(rows),
                changedOnly = filterToChanged,
                // Distinguished from "no rows" so the screen can say *why* it is empty.
                nothingChanged = filterToChanged && rows.isEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeViewUiState(),
        )

    fun onChangedOnlyChange(enabled: Boolean) = changedOnly.update { enabled }

    companion object {
        /**
         * The context the applicability predicates need.
         *
         * `grainEffect` and `whiteBalance` are read out of the recipe because two of §4's
         * rules depend on another field's *value* rather than on the body.
         */
        private fun contextFor(recipe: Recipe) = FieldContext(
            generation = RecipeFields.generationOf(recipe.storedString("sensorGeneration")),
            filmSimulationId = recipe.filmSimulationId,
            grainEffectOff = recipe.settingString("grainEffect").let { it == null || it == "off" },
            whiteBalanceId = recipe.settingString("whiteBalance") ?: "auto",
        )

        private fun headerFor(recipe: Recipe) = RecipeHeader(
            id = recipe.id,
            name = recipe.name,
            filmSimulationId = recipe.filmSimulationId,
            filmSimulationLabel = FilmSimulations.labelFor(recipe.filmSimulationId),
            rating = recipe.rating,
            tags = recipe.tags,
            notes = recipe.notes,
        )

        /** Rows into groups, preserving the document's group order. */
        private fun groupRows(rows: List<FieldFormatting.Row>): List<SettingsGroup> =
            rows.groupBy { row ->
                RecipeFields.byId(row.fieldId)?.group
                    // The combined WB shift row has no field of its own.
                    ?: FieldGroup.WHITE_BALANCE
            }.toList()
                .sortedBy { (group, _) -> group.ordinal }
                .map { (group, groupRows) -> SettingsGroup(group, groupRows) }

        /** A top-level key that only older recipes carry, via `extra`. */
        private fun Recipe.storedString(key: String): String? =
            runCatching { extra[key]?.jsonPrimitive?.content }.getOrNull()

        private fun Recipe.settingString(key: String): String? =
            runCatching { (settings as JsonObject)[key]?.jsonPrimitive?.content }.getOrNull()

        fun factory(container: AppContainer, recipeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RecipeViewModel(recipeId, container.recipeRepository) as T
            }
    }
}
