package dev.bondarenko.fujirecipes.ui.lab

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.raw.DEFAULT_RAW_SUPPORTED_FIELD_IDS
import dev.bondarenko.fujirecipes.camera.raw.rawSupportedFieldIds
import dev.bondarenko.fujirecipes.camera.usb.RawProfileCalibrationRequired
import dev.bondarenko.fujirecipes.camera.usb.RawRenderQuality
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.store.RawDevelopmentCache
import dev.bondarenko.fujirecipes.data.lab.RawLabCalibration
import dev.bondarenko.fujirecipes.data.lab.RawLabState
import dev.bondarenko.fujirecipes.data.lab.RawLabWorkspace
import dev.bondarenko.fujirecipes.data.model.Recipe
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The lab's state plus what only the ViewModel can know: the camera, and the library. */
data class RawLabUiState(
    val lab: RawLabState = RawLabState(),
    val camera: CameraState = CameraState.Disconnected,
    val recipes: List<Recipe> = emptyList(),
    val isLibraryLoaded: Boolean = false,
    /** The connected body's RAW field set, or the verified default when nothing is attached. */
    val supportedFieldIds: Set<String> = DEFAULT_RAW_SUPPORTED_FIELD_IDS,
    val isSaving: Boolean = false,
    val message: String? = null,
)

/**
 * The lab — FEAT-016.
 *
 * Holds almost nothing itself. The work in progress lives in [RawLabWorkspace] above the nav
 * graph, so switching to another bottom-bar page and back does not throw away a RAF the camera
 * is already holding; this owns the *jobs* — importing, rendering, saving — and the rule for
 * when an automatic render may start.
 */
class RawLabViewModel(
    private val workspace: RawLabWorkspace,
    private val repository: RecipeRepository,
    private val controller: CameraController,
    private val cache: RawDevelopmentCache,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())

    val state: StateFlow<RawLabUiState> = combine(
        workspace.state,
        controller.state,
        repository.library,
        transient,
    ) { lab, camera, library, extra ->
        RawLabUiState(
            lab = lab,
            camera = camera,
            recipes = library.recipes,
            isLibraryLoaded = library.hasLoaded,
            supportedFieldIds = camera.supportedFieldIds(),
            isSaving = extra.isSaving,
            message = extra.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RawLabUiState())

    private var renderJob: Job? = null

    init {
        viewModelScope.launch { repository.load() }
        watchForAutomaticRenders()
    }

    /**
     * Automatic preview, for people who would rather not press a button after every nudge.
     *
     * Debounced rather than immediate: a stepper held down would otherwise queue a camera
     * round trip per press. Only the settled value renders, and only one render runs at a
     * time — the check after each render picks up anything that changed while it ran, so the
     * newest settings win and the intermediate ones are dropped rather than queued.
     */
    @OptIn(FlowPreview::class)
    private fun watchForAutomaticRenders() {
        viewModelScope.launch {
            workspace.state
                .map { it.settings }
                .distinctUntilChanged()
                .debounce(AUTO_PREVIEW_DELAY_MS)
                .collect { maybeAutoRender() }
        }
    }

    private fun maybeAutoRender() {
        if (workspace.current.shouldAutoRender()) render(RawRenderQuality.PREVIEW)
    }

    fun connect() = controller.connect()

    // ─── Choosing what to develop ───────────────────────────────────────────

    fun selectRaf(uriText: String) {
        viewModelScope.launch {
            val uri = uriText.toUri()
            val metadata = readMetadata(uri)
            workspace.update { it.importing(metadata.length) }
            val imported = runCatching {
                withContext(Dispatchers.IO) {
                    val input = contentResolver.openInputStream(uri)
                        ?: error("The selected file could not be opened.")
                    input.use {
                        // Reported in steps, not per buffer: the cache calls back every 8 KB,
                        // which for a 40 MB RAF is several thousand recompositions of a bar
                        // that moves a pixel.
                        var reported = 0L
                        cache.importRaf(metadata.name, it, metadata.length) { written, total ->
                            if (written - reported >= IMPORT_PROGRESS_STEP_BYTES || written == total) {
                                reported = written
                                workspace.update { state -> state.importProgress(written, total) }
                            }
                        }
                    }
                }
            }.getOrElse { error ->
                workspace.update {
                    it.copy(importing = null)
                        .withError(error.message ?: "The selected RAF could not be read.")
                }
                return@launch
            }
            cache.clearRenders()
            workspace.update { it.withRaf(imported, metadata.name) }
        }
    }

    fun chooseAnotherRaf() {
        renderJob?.cancel()
        cache.clear()
        workspace.update { it.withoutRaf() }
    }

    /** Everything goes: the RAF, the settings, the picture. */
    fun discard() {
        renderJob?.cancel()
        cache.clear()
        workspace.reset()
        transient.value = TransientState()
    }

    // ─── Editing ────────────────────────────────────────────────────────────

    fun onSettingChange(fieldId: String, value: JsonElement?) {
        workspace.update { it.withSetting(fieldId, value) }
    }

    fun applyRecipe(recipe: Recipe) {
        workspace.update { it.applying(recipe) }
    }

    /** Loads a recipe by id — the way the lab opens when it was reached from one. */
    fun applyRecipeById(recipeId: String) {
        viewModelScope.launch {
            val recipe = repository.library.first { it.hasLoaded }.recipes
                .firstOrNull { it.id == recipeId }
            if (recipe == null) {
                transient.update { it.copy(message = "That recipe is no longer in your library.") }
            } else {
                workspace.update { it.applying(recipe) }
            }
        }
    }

    fun startFromDefaults() {
        workspace.update { it.fromDefaults() }
    }

    fun setAutoPreview(enabled: Boolean) {
        workspace.update { it.withAutoPreview(enabled) }
        if (enabled) maybeAutoRender()
    }

    // ─── Rendering ──────────────────────────────────────────────────────────

    fun render(quality: RawRenderQuality) {
        val current = workspace.current
        val raf = current.raf ?: return
        if (!current.canRender) return

        renderJob = viewModelScope.launch {
            workspace.update { it.renderStarted() }
            // Snapshotted after the transition, so the picture is labelled with exactly the
            // settings that produced it however much they change while the camera works.
            val rendered = workspace.current.settings
            val output = cache.renderFile(raf, workspace.current.serial)
            try {
                val result = controller.renderRawInLab(raf, rendered, output, quality) { stage ->
                    workspace.update { it.stage(stage) }
                }
                cache.clearRenders(keep = result.jpeg)
                workspace.update { it.renderSucceeded(result, rendered) }
            } catch (error: CancellationException) {
                output.delete()
                throw error
            } catch (error: RawProfileCalibrationRequired) {
                output.delete()
                workspace.update {
                    it.calibrationRequired(
                        RawLabCalibration(
                            cameraModel = error.cameraModel,
                            profile = error.profile,
                            message = error.message
                                ?: "This camera model needs a verified RAW profile mapping.",
                        ),
                    )
                }
            } catch (error: Exception) {
                output.delete()
                workspace.update {
                    it.renderFailed(
                        error.message ?: "The camera stopped before it returned a JPEG.",
                    )
                }
            }
            maybeAutoRender()
        }
    }

    // ─── Saving ─────────────────────────────────────────────────────────────

    /** Writes the rendered file to wherever the document picker pointed. */
    fun saveJpeg(destination: Uri) {
        val source = workspace.current.preview?.file ?: return
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, message = null) }
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(destination)?.use { sink ->
                        source.inputStream().use { it.copyTo(sink) }
                    } ?: error("That location could not be written to.")
                }
            }
            transient.update {
                it.copy(
                    isSaving = false,
                    message = if (saved.isSuccess) {
                        "JPEG saved"
                    } else {
                        saved.exceptionOrNull()?.message ?: "The JPEG could not be saved."
                    },
                )
            }
        }
    }

    fun saveAsNewRecipe(name: String, onSaved: (Recipe) -> Unit = {}) {
        val settings = workspace.current.settings
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, message = null) }
            val body = buildJsonObject {
                put("name", name.trim())
                put("notes", "")
                put("rating", 0)
                put("tags", JsonArray(emptyList()))
                put("settings", settings)
            }
            when (val result = repository.create(body)) {
                is LibraryResult.Success -> {
                    workspace.update { it.savedAs(result.value) }
                    transient.update { it.copy(isSaving = false, message = "Recipe saved") }
                    onSaved(result.value)
                }
                is LibraryResult.Failure -> transient.update {
                    it.copy(
                        isSaving = false,
                        message = result.error.message ?: "The recipe could not be saved.",
                    )
                }
            }
        }
    }

    /**
     * Overwrites the applied recipe's settings.
     *
     * Only `settings` is sent: the name, notes, rating and tags belong to the recipe the user
     * started from, and the lab never asked about them.
     */
    fun updateAppliedRecipe() {
        val current = workspace.current
        val id = current.appliedRecipeId ?: return
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, message = null) }
            val body = buildJsonObject { put("settings", current.settings) }
            when (val result = repository.update(id, body)) {
                is LibraryResult.Success -> {
                    workspace.update { it.savedAs(result.value) }
                    transient.update { it.copy(isSaving = false, message = "Recipe updated") }
                }
                is LibraryResult.Failure -> transient.update {
                    it.copy(
                        isSaving = false,
                        message = result.error.message ?: "The recipe could not be updated.",
                    )
                }
            }
        }
    }

    fun clearMessage() {
        transient.update { it.copy(message = null) }
    }

    // ─── Plumbing ───────────────────────────────────────────────────────────

    private data class TransientState(
        val isSaving: Boolean = false,
        val message: String? = null,
    )

    private fun CameraState.supportedFieldIds(): Set<String> =
        if (this is CameraState.Connected) {
            rawSupportedFieldIds(identity.model).takeIf { it.isNotEmpty() }
                ?: DEFAULT_RAW_SUPPORTED_FIELD_IDS
        } else {
            DEFAULT_RAW_SUPPORTED_FIELD_IDS
        }

    private fun readMetadata(uri: Uri): RafMetadata {
        var name = "selected.raf"
        var length: Long? = null
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
                    name = cursor.getString(it) ?: name
                }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let {
                    if (!cursor.isNull(it)) length = cursor.getLong(it).takeIf { size -> size >= 0 }
                }
            }
        }
        return RafMetadata(name, length)
    }

    private data class RafMetadata(val name: String, val length: Long?)

    companion object {
        /** Long enough that a held stepper settles first; short enough to feel like an answer. */
        const val AUTO_PREVIEW_DELAY_MS = 900L

        /** How much of a RAF has to land before the progress bar is told again. */
        private const val IMPORT_PROGRESS_STEP_BYTES = 512L * 1024

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RawLabViewModel(
                    workspace = container.rawLabWorkspace,
                    repository = container.recipeRepository,
                    controller = container.cameraController,
                    cache = container.rawDevelopmentCache,
                    contentResolver = container.applicationContext.contentResolver,
                )
            }
        }
    }
}
