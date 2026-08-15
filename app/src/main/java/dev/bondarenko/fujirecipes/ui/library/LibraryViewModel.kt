package dev.bondarenko.fujirecipes.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.settings.ViewPreferences
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.library.LibraryFilters
import dev.bondarenko.fujirecipes.data.library.LibraryView
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.data.library.StoredLibraryView
import dev.bondarenko.fujirecipes.data.library.selectRecipes
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the list screen draws. One state object, per `coding-standards.md`. */
data class LibraryUiState(
    val visible: List<RecipeCardModel> = emptyList(),
    val totalCount: Int = 0,
    val search: String = "",
    val filters: LibraryFilters = LibraryFilters(),
    val sort: SortId = SortId.Default,
    val hasLoaded: Boolean = false,
    val error: LibraryError? = null,
    val lastUpdatedAt: String? = null,
    /** Every tag in the library, for the filter surface. */
    val availableTags: List<String> = emptyList(),
    /** Every simulation actually in use, so the filter never offers an empty result. */
    val availableSimulations: List<String> = emptyList(),
) {
    /** Narrowed by search or filters — the difference between "n of m" and "n". */
    val isNarrowed: Boolean get() = search.isNotBlank() || !filters.isEmpty

    val isEmptyLibrary: Boolean get() = hasLoaded && totalCount == 0 && error == null

    val hasNoMatches: Boolean get() = hasLoaded && totalCount > 0 && visible.isEmpty()

    /**
     * An error that has taken the screen, as opposed to one shown over a usable library.
     *
     * With recipes on screen a failed save is reported without taking the library away;
     * with nothing on screen — a library file that would not parse — it takes the screen.
     */
    val isBlockingError: Boolean get() = error != null && totalCount == 0
}

class LibraryViewModel(
    private val repository: RecipeRepository,
    private val preferences: ViewPreferences,
) : ViewModel() {

    /**
     * Search lives here and nowhere else.
     *
     * Never persisted, on purpose: it is a way of getting to one recipe, not a setting, and
     * a phone reopened tomorrow showing three of forty recipes because of a word typed
     * yesterday reads as a library that lost things.
     */
    private val search = MutableStateFlow("")

    val state: StateFlow<LibraryUiState> =
        combine(repository.library, preferences.view, search) { library, stored, query ->
            val view = LibraryView(query, stored.filters, stored.sort)
            val visible = selectRecipes(library.recipes, view)

            LibraryUiState(
                visible = visible.map { recipe ->
                    RecipeCardModel(
                        id = recipe.id,
                        name = recipe.name,
                        filmSimulationId = recipe.filmSimulationId,
                        rating = recipe.rating,
                        tags = recipe.tags,
                    )
                },
                totalCount = library.recipes.size,
                search = query,
                filters = stored.filters,
                sort = stored.sort,
                hasLoaded = library.hasLoaded,
                error = library.error,
                lastUpdatedAt = library.lastUpdatedAt,
                availableTags = library.recipes.flatMap { it.tags }.distinct().sorted(),
                availableSimulations = library.recipes
                    .mapNotNull { it.filmSimulationId }
                    .distinct()
                    // In the canonical order of the field definitions, not alphabetical:
                    // the picker should read like the camera's own menu.
                    .sortedBy { id -> FilmSimulations.all.indexOfFirst { it.id == id } },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    init {
        retry()
    }

    /**
     * Read the library off the device.
     *
     * Called once on arrival, and again from the error panel — the only two moments there is
     * anything to re-read, now that every change to the library republishes on its own.
     */
    fun retry() {
        viewModelScope.launch { repository.load() }
    }

    fun onSearchChange(query: String) {
        search.update { query }
    }

    fun onSortChange(sort: SortId) {
        viewModelScope.launch { preferences.saveSort(sort) }
    }

    fun onFiltersChange(filters: LibraryFilters) {
        viewModelScope.launch {
            preferences.save(StoredLibraryView(filters, state.value.sort))
        }
    }

    /**
     * Delete straight from the list — FEAT-003's delete, reached by a swipe action.
     *
     * No confirmation here on purpose: the row has to be deliberately slid open first, which
     * is already a two-step gesture, and the library is rewritten whole — so a failure simply
     * leaves the recipe where it was.
     */
    fun onDeleteRecipe(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Clears both, which is what the "no matches" panel offers. */
    fun onClearSearchAndFilters() {
        search.update { "" }
        onFiltersChange(LibraryFilters())
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(
                        repository = container.recipeRepository,
                        preferences = container.viewPreferences,
                    ) as T
            }
    }
}
