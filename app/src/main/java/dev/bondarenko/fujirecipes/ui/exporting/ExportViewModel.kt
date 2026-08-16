package dev.bondarenko.fujirecipes.ui.exporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.data.exporting.ExportKind
import dev.bondarenko.fujirecipes.data.exporting.archiveEntries
import dev.bondarenko.fujirecipes.data.exporting.exportRecipes
import dev.bondarenko.fujirecipes.data.exporting.libraryExportFilename
import dev.bondarenko.fujirecipes.data.exporting.zipOf
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What the export screen is showing — FEAT-008 T-11.
 *
 * The library is already in memory (`architecture.md` §4) and the file is built on the
 * phone, which is what makes a backup something you can take in the field rather than only
 * at home. With no server behind the app, this is also the only way a library leaves it.
 */
data class ExportUiState(
    val recipes: List<Recipe> = emptyList(),
    /** Ids that will be exported. */
    val selected: Set<String> = emptySet(),
    val kind: ExportKind = ExportKind.JSON,
    val hasLoaded: Boolean = false,
    /** Set when the file could not be written. Nothing left the app. */
    val error: String? = null,
) {
    val total: Int get() = recipes.size
    val isEmptyLibrary: Boolean get() = hasLoaded && recipes.isEmpty()
    val canExport: Boolean get() = selected.isNotEmpty()
    val allSelected: Boolean get() = recipes.isNotEmpty() && selected.size == recipes.size

    /** The file the action will produce, named on the button so it is not a surprise. */
    val filename: String get() = libraryExportFilename(kind)
}

/** What a completed export hands to the share sheet. */
data class ExportFile(val filename: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is ExportFile && filename == other.filename && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * filename.hashCode() + bytes.contentHashCode()
}

class ExportViewModel(repository: RecipeRepository) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.library.collect { library ->
                if (!library.hasLoaded) return@collect

                _state.value = _state.value.copy(
                    recipes = library.recipes,
                    // Everything selected on arrival. A backup is the common reason to be
                    // here, and a screen that opens with nothing selected asks the user to do
                    // work to reach the obvious result. Only on the first load — a refresh
                    // must not undo a selection someone is in the middle of making.
                    selected = if (_state.value.hasLoaded) {
                        _state.value.selected.intersect(library.recipes.map { it.id }.toSet())
                    } else {
                        library.recipes.map { it.id }.toSet()
                    },
                    hasLoaded = true,
                )
            }
        }
    }

    fun toggle(id: String) {
        val selected = _state.value.selected
        _state.value = _state.value.copy(
            selected = if (id in selected) selected - id else selected + id,
        )
    }

    fun selectAll() {
        _state.value = _state.value.copy(selected = _state.value.recipes.map { it.id }.toSet())
    }

    fun selectNone() {
        _state.value = _state.value.copy(selected = emptySet())
    }

    fun chooseKind(kind: ExportKind) {
        _state.value = _state.value.copy(kind = kind)
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Builds the file. Returns null when there is nothing selected.
     *
     * The moment is stamped once here and used for both the envelope and the filename, so a
     * file called `fuji-recipes-2026-08-15.json` cannot carry an `exportedAt` from the day
     * before across a midnight boundary.
     */
    fun buildFile(): ExportFile? {
        val state = _state.value
        val chosen = state.recipes.filter { it.id in state.selected }
        if (chosen.isEmpty()) return null

        val at = Instant.now()
        val filename = libraryExportFilename(state.kind, at)

        return when (state.kind) {
            ExportKind.JSON -> ExportFile(
                filename,
                exportRecipes(chosen, at).toByteArray(Charsets.UTF_8),
            )

            ExportKind.ZIP -> ExportFile(filename, zipOf(archiveEntries(chosen)))
        }
    }

    fun onShareFailed(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExportViewModel(container.recipeRepository) }
        }
    }
}
