package dev.bondarenko.fujirecipes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.core.net.ApiResult
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.RecipeValidation
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class EditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val rating: Int = 0,
    val tags: List<String> = emptyList(),
    /** The parameter set as it currently stands, unknown keys included. */
    val settings: JsonObject = JsonObject(emptyMap()),
    val problems: List<RecipeValidation.Problem> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: ApiError? = null,
    val isDirty: Boolean = false,
    val notFound: Boolean = false,
) {
    /** What the applicability predicates need. Recomputed as the user edits. */
    val fieldContext: FieldContext
        get() = FieldContext(
            // No sensor generation anywhere in this app (FEAT-003 §8): the widest field set
            // is always assumed, so every parameter is offered. The predicates stay in the
            // table because FEAT-005 needs them — a connected camera reports its own.
            generation = SensorGeneration.XTRANS_V,
            filmSimulationId = settings.string("filmSimulation") ?: "provia",
            grainEffectOff = (settings.string("grainEffect") ?: "off") == "off",
            whiteBalanceId = settings.string("whiteBalance") ?: "auto",
        )

    fun problemFor(fieldId: String): String? =
        problems.firstOrNull { it.fieldId == fieldId }?.message
}

private fun JsonObject.string(key: String): String? =
    runCatching { (this[key] as? JsonPrimitive)?.content }.getOrNull()

/**
 * Create and edit — FEAT-003 T-06.
 *
 * **The working copy is a [JsonObject], not a typed settings class.** A typed class is a
 * list of the fields this build knows about, and editing through one would silently drop
 * whatever a newer web client had written. Holding the raw object and replacing individual
 * keys means an unknown setting is carried from load to save untouched
 * (`coding-standards.md` P2).
 */
class RecipeEditorViewModel(
    private val recipeId: String?,
    private val duplicateOf: String?,
    private val repository: RecipeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState(isLoading = true))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    /** What was loaded, so the PATCH can be a diff and dirtiness can be real. */
    private var original: Recipe? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val sourceId = recipeId ?: duplicateOf

        if (sourceId == null) {
            _state.value = EditorUiState(
                isLoading = false,
                isNew = true,
                settings = defaultSettings(),
            )
            return
        }

        val library = repository.library.first { it.hasLoaded }
        val recipe = library.recipes.firstOrNull { it.id == sourceId }

        if (recipe == null) {
            _state.value = EditorUiState(isLoading = false, notFound = true)
            return
        }

        val duplicating = recipeId == null
        original = if (duplicating) null else recipe

        _state.value = EditorUiState(
            isLoading = false,
            isNew = duplicating,
            // A copy that shares its name exactly is two identical rows in the list with no
            // way to tell which is which.
            name = if (duplicating) "${recipe.name} copy" else recipe.name,
            notes = recipe.notes,
            rating = recipe.rating,
            tags = recipe.tags,
            settings = recipe.settings,
            // A duplicate is unsaved work from the moment it opens; leaving it must ask.
            isDirty = duplicating,
        )
    }

    /** A new recipe starts at the documented defaults, not at zero. */
    private fun defaultSettings(): JsonObject = buildJsonObject {
        RecipeFields.all.forEach { field ->
            when (val default = field.defaultValue) {
                is String -> put(field.id, default)
                is Number -> put(field.id, default)
                else -> Unit // null default: the advisory bounds, legitimately unset
            }
        }
    }

    fun onNameChange(value: String) = edit { copy(name = value) }
    fun onNotesChange(value: String) = edit { copy(notes = value) }
    fun onRatingChange(value: Int) = edit { copy(rating = value) }
    fun onTagsChange(value: List<String>) = edit { copy(tags = value) }

    fun onSettingChange(fieldId: String, value: JsonElement?) = edit {
        val next = settings.toMutableMap()
        if (value == null) next.remove(fieldId) else next[fieldId] = value
        copy(settings = JsonObject(next))
    }

    private inline fun edit(change: EditorUiState.() -> EditorUiState) {
        _state.update { current ->
            // Clearing the previous failure on any edit: an error about a value the user has
            // since changed is worse than no error.
            current.change().copy(isDirty = true, saveError = null, problems = emptyList())
        }
    }

    fun save(onSaved: (String) -> Unit) {
        val current = _state.value

        val problems = RecipeValidation.validate(
            name = current.name,
            notes = current.notes,
            rating = current.rating,
            tags = current.tags,
            settings = current.settings,
            context = current.fieldContext,
        )
        if (problems.isNotEmpty()) {
            _state.update { it.copy(problems = problems) }
            return
        }

        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            val existing = original
            val result = if (existing == null) {
                repository.create(createBody(current))
            } else {
                val diff = diffAgainst(existing, current)
                // Nothing actually changed — a PATCH with an empty body would still bump
                // `updatedAt` and reorder the "recently updated" sort for no reason.
                if (diff.isEmpty()) {
                    ApiResult.Success(existing)
                } else {
                    repository.update(existing.id, diff)
                }
            }

            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false, isDirty = false) }
                    onSaved(result.value.id)
                }
                is ApiResult.Failure -> {
                    // Everything the user typed stays in state; only the flags move.
                    _state.update { it.copy(isSaving = false, saveError = result.error) }
                }
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = original?.id ?: return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false, isDirty = false) }
                    onDeleted()
                }
                is ApiResult.Failure ->
                    _state.update { it.copy(isSaving = false, saveError = result.error) }
            }
        }
    }

    companion object {
        /** The create body: what the user chose. The server assigns everything else. */
        internal fun createBody(state: EditorUiState): JsonObject = buildJsonObject {
            put("name", state.name.trim())
            put("notes", state.notes)
            put("rating", state.rating)
            put("tags", kotlinx.serialization.json.JsonArray(state.tags.map { JsonPrimitive(it) }))
            put("settings", state.settings)
        }

        /**
         * The PATCH body: **only** what changed.
         *
         * `settings` is compared as a whole and sent whole when it differs, because a partial
         * settings object would be merged by neither side unambiguously — but the object
         * sent is the one that was loaded with the user's changes applied on top, so keys
         * this build never displayed are still in it.
         */
        internal fun diffAgainst(original: Recipe, state: EditorUiState): JsonObject =
            buildJsonObject {
                if (state.name.trim() != original.name) put("name", state.name.trim())
                if (state.notes != original.notes) put("notes", state.notes)
                if (state.rating != original.rating) put("rating", state.rating)
                if (state.tags != original.tags) {
                    put(
                        "tags",
                        kotlinx.serialization.json.JsonArray(state.tags.map { JsonPrimitive(it) }),
                    )
                }
                if (state.settings != original.settings) put("settings", state.settings)
            }

        fun factory(
            container: AppContainer,
            recipeId: String?,
            duplicateOf: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecipeEditorViewModel(recipeId, duplicateOf, container.recipeRepository) as T
        }
    }
}
