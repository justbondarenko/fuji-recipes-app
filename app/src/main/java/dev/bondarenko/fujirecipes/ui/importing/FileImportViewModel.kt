package dev.bondarenko.fujirecipes.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.data.repo.ImportFailure
import dev.bondarenko.fujirecipes.data.importing.FileRow
import dev.bondarenko.fujirecipes.data.importing.FileImportSummary
import dev.bondarenko.fujirecipes.data.importing.ImportRejected
import dev.bondarenko.fujirecipes.data.importing.RejectionReason
import dev.bondarenko.fujirecipes.data.importing.Resolution
import dev.bondarenko.fujirecipes.data.importing.fileImportBody
import dev.bondarenko.fujirecipes.data.importing.readImportFile
import dev.bondarenko.fujirecipes.data.importing.reviewFile
import dev.bondarenko.fujirecipes.data.importing.summarise
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import dev.bondarenko.fujirecipes.core.store.ImageStore
import dev.bondarenko.fujirecipes.data.importing.looksLikeZip
import dev.bondarenko.fujirecipes.data.importing.unzip
import java.io.ByteArrayInputStream

/** A file the picker handed back: its bytes, and what to call it on screen. */
data class ChosenFile(val name: String, val bytes: ByteArray) {
    // Data classes over arrays compare by reference; this is never compared, and saying so
    // beats leaving a `==` that lies.
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

sealed interface FileImportStage {

    /** Nothing chosen yet. */
    data object Ready : FileImportStage

    data object Reading : FileImportStage

    data object Review : FileImportStage

    data object Importing : FileImportStage

    data class Done(val imported: Int, val skipped: Int, val replaced: Int) : FileImportStage

    /**
     * The **file** was refused — a wrong format, a future version, an archive that escapes its
     * own root. Distinct from [Failed] because nothing was attempted: the remedy is another
     * file, not another try.
     */
    data class Rejected(val reason: RejectionReason, val message: String) : FileImportStage

    /** The library could not be written. The review is kept, so retrying costs nothing. */
    data class Failed(val message: String) : FileImportStage
}

data class FileImportUiState(
    val stage: FileImportStage = FileImportStage.Ready,
    val fileName: String? = null,
    val rows: List<FileRow> = emptyList(),
    val resolutions: Map<Int, Resolution> = emptyMap(),
    /** Entries the import itself refused, one line each (SF-014). */
    val rejectedEntries: List<ImportFailure> = emptyList(),
) {
    val summary: FileImportSummary get() = summarise(rows, resolutions)
    val canImport: Boolean get() = summary.undecided == 0 && summary.importing > 0
}

/**
 * Import from a file — FEAT-012.
 *
 * Every stage is local now, which removes the reason the two halves used to be separate —
 * except the one that still holds: a write that fails must not cost the review that preceded
 * it, so the rows stay in state and Retry is free.
 */
class FileImportViewModel(
    private val repository: RecipeRepository,
    private val imageStore: ImageStore,
    private val readFile: suspend (String) -> ChosenFile?,
) : ViewModel() {

    private val _state = MutableStateFlow(FileImportUiState())
    val state: StateFlow<FileImportUiState> = _state.asStateFlow()

    private var pendingImages: Map<String, ByteArray> = emptyMap()

    fun choose(uri: String) {
        _state.value = FileImportUiState(stage = FileImportStage.Reading)
        pendingImages = emptyMap()

        viewModelScope.launch {
            val file = readFile(uri)
            if (file == null) {
                _state.value = FileImportUiState(
                    stage = FileImportStage.Rejected(
                        RejectionReason.UNREADABLE,
                        "That file could not be opened. If it came from another app, try saving " +
                            "it to this phone first.",
                    ),
                )
                return@launch
            }

            if (looksLikeZip(file.bytes)) {
                runCatching {
                    val unzipped = unzip(file.bytes)
                    pendingImages = unzipped.filterKeys { key ->
                        key.startsWith("images/") || key.endsWith(".webp") || key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(".png")
                    }
                }
            }

            val parsed = runCatching { readImportFile(file.bytes, file.name) }
                .getOrElse { error ->
                    _state.value = FileImportUiState(
                        fileName = file.name,
                        stage = when (error) {
                            is ImportRejected -> FileImportStage.Rejected(error.reason, error.message.orEmpty())
                            // Anything else is a file that is not what it claimed to be — a
                            // truncated archive, bytes that are not text. Same remedy.
                            else -> FileImportStage.Rejected(
                                RejectionReason.UNREADABLE,
                                error.message ?: "That file could not be read.",
                            )
                        },
                    )
                    return@launch
                }

            val library = repository.library.first { it.hasLoaded }.recipes

            _state.value = FileImportUiState(
                stage = FileImportStage.Review,
                fileName = file.name,
                rows = reviewFile(parsed.recipes, library),
            )
        }
    }

    fun resolve(index: Int, resolution: Resolution) {
        _state.value = _state.value.copy(
            resolutions = _state.value.resolutions.toMutableMap().apply {
                // Tapping the chosen option again clears it, which is the only way back to
                // "undecided" once something has been picked.
                if (this[index] == resolution) remove(index) else this[index] = resolution
            },
        )
    }

    fun import() {
        val current = _state.value
        if (!current.canImport) return

        _state.value = current.copy(
            stage = FileImportStage.Importing,
            rejectedEntries = emptyList(),
        )

        viewModelScope.launch {
            val body = fileImportBody(current.rows, current.resolutions)

            _state.value = when (val result = repository.importAll(body)) {
                is LibraryResult.Success -> {
                    // Extract any images carried by the import
                    pendingImages.forEach { (path, bytes) ->
                        val fileName = path.substringAfterLast('/')
                        runCatching {
                            imageStore.saveStream(fileName, ByteArrayInputStream(bytes))
                        }
                    }

                    _state.value.copy(
                        stage = FileImportStage.Done(
                            imported = result.value.imported,
                            skipped = result.value.skipped,
                            replaced = result.value.replaced,
                        ),
                        // SF-014 again, from the other side: the import validates every entry as
                        // it writes, and one it refused is one the user has to be told about by
                        // name rather than left to discover missing.
                        rejectedEntries = result.value.failed,
                    )
                }

                is LibraryResult.Failure -> _state.value.copy(
                    stage = FileImportStage.Failed(messageFor(result.error)),
                )
            }
        }
    }

    /** After a failed write: back to the review that is still held. */
    fun backToReview() {
        _state.value = _state.value.copy(
            stage = if (_state.value.rows.isEmpty()) FileImportStage.Ready else FileImportStage.Review,
        )
    }

    fun reset() {
        _state.value = FileImportUiState()
    }

    private fun messageFor(error: LibraryError): String = when (error) {
        is LibraryError.Storage ->
            "Your library could not be saved to this phone, so nothing was imported" +
                (error.message?.let { ": $it" } ?: ".") +
                " Your choices are still here to try again."

        is LibraryError.Unreadable ->
            "The library already on this phone could not be read, so nothing was written to " +
                "it. Importing on top of it would replace recipes that may still be " +
                "recoverable."

        is LibraryError.Invalid -> {
            val field = error.fields.firstOrNull()
            "The import was refused" +
                (field?.let { " — ${it.path}: ${it.message}" } ?: "") +
                ". Nothing was written."
        }

        is LibraryError.NotFound ->
            "A recipe this file replaces is no longer in your library. Nothing was written."
    }

    companion object {
        fun factory(
            container: AppContainer,
            readFile: suspend (String) -> ChosenFile?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FileImportViewModel(
                    container.recipeRepository,
                    container.imageStore,
                    readFile,
                )
            }
        }
    }
}
