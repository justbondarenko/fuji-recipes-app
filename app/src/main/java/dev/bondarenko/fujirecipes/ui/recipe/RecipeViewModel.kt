package dev.bondarenko.fujirecipes.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.settings.RecipeViewMode
import dev.bondarenko.fujirecipes.core.settings.UiPreferences
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.exporting.exportRecipe
import dev.bondarenko.fujirecipes.data.exporting.recipeExportFilename
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    /** A failed in-place rating or tag save. The screen still shows the stored values. */
    val saveError: LibraryError? = null,
    val viewMode: RecipeViewMode = RecipeViewMode.GRID,
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
    val images: List<String> = emptyList(),
)

/**
 * FEAT-002 T-07.
 *
 * **Reads from the already-loaded library.** The whole library is in memory
 * (`architecture.md` §4), so there is nothing for this screen to go and get.
 */
class RecipeViewModel(
    private val recipeId: String,
    private val repository: RecipeRepository,
    private val uiPreferences: UiPreferences,
) : ViewModel() {

    private val changedOnly = MutableStateFlow(false)
    private val saveError = MutableStateFlow<LibraryError?>(null)

    val state: StateFlow<RecipeViewUiState> =
        combine(repository.library, changedOnly, saveError, uiPreferences.preferences) { library, filterToChanged, error, uiPrefs ->
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
                saveError = error,
                viewMode = uiPrefs.recipeViewMode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeViewUiState(),
        )

    fun onChangedOnlyChange(enabled: Boolean) = changedOnly.update { enabled }

    /**
     * Rating and tags, saved in place — FEAT-003 T-14.
     *
     * The only two parameters editable from the reading screen, and deliberately so: they
     * are what gets adjusted while looking at a photo, and sending someone into a form to
     * add one tag is friction with no purpose. Everything else stays read-only here.
     */
    fun onRatingChange(rating: Int) = patch(buildJsonObject { put("rating", rating) })

    fun onTagsChange(tags: List<String>) =
        patch(buildJsonObject { put("tags", JsonArray(tags.map { JsonPrimitive(it) })) })

    private fun patch(body: JsonObject) {
        viewModelScope.launch {
            // No optimistic update: the repository republishes the library on success, and
            // on failure the screen keeps showing what is actually stored rather than a
            // change that did not happen.
            when (val result = repository.update(recipeId, body)) {
                is LibraryResult.Success -> saveError.update { null }
                is LibraryResult.Failure -> saveError.update { result.error }
            }
        }
    }

    fun onDismissError() = saveError.update { null }

    /**
     * Builds this recipe as a file — FEAT-008 T-14.
     *
     * A **bare recipe object, not an envelope**: SF-005 has import accept exactly that shape,
     * and it makes the file readable at a glance, which is the whole point of exporting one
     * rather than the library.
     *
     * Here rather than in the composable because it needs the stored [Recipe], and the screen
     * only ever sees the header projection.
     */
    fun buildExport(onReady: (filename: String, content: String) -> Unit) {
        viewModelScope.launch {
            val recipe = repository.library.first { it.hasLoaded }
                .recipes.firstOrNull { it.id == recipeId } ?: return@launch

            onReady(recipeExportFilename(recipe), exportRecipe(recipe))
        }
    }

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
            images = recipe.images,
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
                    RecipeViewModel(
                        recipeId = recipeId,
                        repository = container.recipeRepository,
                        uiPreferences = container.uiPreferences,
                    ) as T
            }
    }
}
