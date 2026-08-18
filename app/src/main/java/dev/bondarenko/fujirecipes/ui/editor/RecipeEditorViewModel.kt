package dev.bondarenko.fujirecipes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class EditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val rating: Int = 0,
    val tags: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val isProcessingImage: Boolean = false,
    /** The parameter set as it currently stands, unknown keys included. */
    val settings: JsonObject = JsonObject(emptyMap()),
    val problems: List<RecipeValidation.Problem> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: LibraryError? = null,
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
    private val imageStore: dev.bondarenko.fujirecipes.core.store.ImageStore,
    /** Settings decoded from a photo (FEAT-009), as JSON. Only ever set on a create. */
    private val prefill: String? = null,
    private val prefillName: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState(isLoading = true))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    /** What was loaded, so the PATCH can be a diff and dirtiness can be real. */
    private var original: Recipe? = null
    private var processingJob: kotlinx.coroutines.Job? = null
    private val stagedNewImages = mutableSetOf<String>()
    private var isSaved = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val sourceId = recipeId ?: duplicateOf

        if (sourceId == null) {
            // A create can start from a photo. The decoded settings go **over** the defaults
            // rather than replacing them, so a field the photo did not carry keeps its
            // default and the form is complete either way.
            val decoded = prefill
                ?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }

            _state.value = EditorUiState(
                isLoading = false,
                isNew = true,
                name = prefillName.orEmpty(),
                settings = if (decoded == null) {
                    defaultSettings()
                } else {
                    JsonObject(defaultSettings() + decoded)
                },
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
            name = if (duplicating) "${recipe.name} copy" else recipe.name,
            notes = recipe.notes,
            rating = recipe.rating,
            tags = recipe.tags,
            images = recipe.images,
            settings = recipe.settings,
            isDirty = duplicating,
        )
    }

    /** A new recipe starts at the documented defaults, not at zero. */
    private fun defaultSettings(): JsonObject = buildJsonObject {
        RecipeFields.all.forEach { field ->
            when (val default = field.defaultValue) {
                is String -> put(field.id, default)
                is Number -> put(field.id, default)
                else -> Unit
            }
        }
    }

    fun onNameChange(value: String) = edit { copy(name = value) }
    fun onNotesChange(value: String) = edit { copy(notes = value) }
    fun onRatingChange(value: Int) = edit { copy(rating = value) }
    fun onTagsChange(value: List<String>) = edit { copy(tags = value) }

    fun onAddImages(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        val current = _state.value.images
        val remainingSlots = dev.bondarenko.fujirecipes.core.store.ImageStore.MAX_IMAGES_PER_RECIPE - current.size
        if (remainingSlots <= 0) return
        val toAdd = uris.take(remainingSlots)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _state.update { it.copy(isProcessingImage = true) }
            val savedNames = toAdd.mapNotNull { uri -> imageStore.saveFromUri(uri) }
            stagedNewImages.addAll(savedNames)
            _state.update { currentUi ->
                val updatedImages = (currentUi.images + savedNames).take(
                    dev.bondarenko.fujirecipes.core.store.ImageStore.MAX_IMAGES_PER_RECIPE,
                )
                currentUi.copy(
                    images = updatedImages,
                    isDirty = true,
                    isProcessingImage = false,
                )
            }
        }
    }

    fun onRemoveImage(fileName: String) {
        val originalImages = original?.images ?: emptyList()
        if (stagedNewImages.contains(fileName) && !originalImages.contains(fileName)) {
            imageStore.delete(fileName)
            stagedNewImages.remove(fileName)
        }
        edit {
            copy(images = images.filterNot { it == fileName })
        }
    }

    fun onSettingChange(fieldId: String, value: JsonElement?) = edit {
        val next = settings.toMutableMap()
        if (value == null) next.remove(fieldId) else next[fieldId] = value
        copy(settings = JsonObject(next))
    }

    private inline fun edit(change: EditorUiState.() -> EditorUiState) {
        _state.update { current ->
            current.change().copy(isDirty = true, saveError = null, problems = emptyList())
        }
    }

    fun save(onSaved: (String) -> Unit) {
        val current = _state.value
        if (current.isProcessingImage) return

        val problems = RecipeValidation.validate(
            name = current.name,
            notes = current.notes,
            rating = current.rating,
            tags = current.tags,
            settings = current.settings,
            context = current.fieldContext,
        ) + (RecipeValidation.validateImages(current.images)?.let { listOf(it) } ?: emptyList())
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
                if (diff.isEmpty()) {
                    LibraryResult.Success(existing)
                } else {
                    repository.update(existing.id, diff)
                }
            }

            when (result) {
                is LibraryResult.Success -> {
                    isSaved = true
                    _state.update { it.copy(isSaving = false, isDirty = false) }
                    onSaved(result.value.id)
                }
                is LibraryResult.Failure -> {
                    _state.update { it.copy(isSaving = false, saveError = result.error) }
                }
            }
        }
    }

    fun cancel() {
        cancelProcessingAndCleanStagedImages()
    }

    private fun cancelProcessingAndCleanStagedImages() {
        processingJob?.cancel()
        if (!isSaved) {
            val originalImages = original?.images?.toSet() ?: emptySet()
            val toDelete = stagedNewImages - originalImages
            toDelete.forEach { imageStore.delete(it) }
            stagedNewImages.clear()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelProcessingAndCleanStagedImages()
    }

    fun delete(onDeleted: () -> Unit) {
        val id = original?.id ?: return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            when (val result = repository.delete(id)) {
                is LibraryResult.Success -> {
                    _state.update { it.copy(isSaving = false, isDirty = false) }
                    onDeleted()
                }
                is LibraryResult.Failure ->
                    _state.update { it.copy(isSaving = false, saveError = result.error) }
            }
        }
    }

    companion object {
        /** The create body: what the user chose. The repository assigns everything else. */
        internal fun createBody(state: EditorUiState): JsonObject = buildJsonObject {
            put("name", state.name.trim())
            put("notes", state.notes)
            put("rating", state.rating)
            put("tags", kotlinx.serialization.json.JsonArray(state.tags.map { JsonPrimitive(it) }))
            if (state.images.isNotEmpty()) {
                put("images", kotlinx.serialization.json.JsonArray(state.images.map { JsonPrimitive(it) }))
            }
            put("settings", state.settings)
        }

        /**
         * The update body: **only** what changed.
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
                if (state.images != original.images) {
                    put(
                        "images",
                        kotlinx.serialization.json.JsonArray(state.images.map { JsonPrimitive(it) }),
                    )
                }
                if (state.settings != original.settings) put("settings", state.settings)
            }

        fun factory(
            container: AppContainer,
            recipeId: String?,
            duplicateOf: String?,
            prefill: String? = null,
            prefillName: String? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RecipeEditorViewModel(
                    recipeId,
                    duplicateOf,
                    container.recipeRepository,
                    container.imageStore,
                    prefill,
                    prefillName,
                ) as T
        }
    }
}
