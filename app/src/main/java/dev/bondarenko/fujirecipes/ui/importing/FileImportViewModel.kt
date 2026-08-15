package dev.bondarenko.fujirecipes.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.core.net.ApiResult
import dev.bondarenko.fujirecipes.core.net.ImportFailure
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

    /** The server refused the import. The review is kept, so retrying costs nothing. */
    data class Failed(val message: String) : FileImportStage
}

data class FileImportUiState(
    val stage: FileImportStage = FileImportStage.Ready,
    val fileName: String? = null,
    val rows: List<FileRow> = emptyList(),
    val resolutions: Map<Int, Resolution> = emptyMap(),
    /** What the server reported per recipe, when it reported anything (`contracts.md`). */
    val serverFailures: List<ImportFailure> = emptyList(),
) {
    val summary: FileImportSummary get() = summarise(rows, resolutions)
    val canImport: Boolean get() = summary.undecided == 0 && summary.importing > 0
}

/**
 * Import from a file — FEAT-012.
 *
 * Reading and reviewing are local and need no signal; importing is a server write and does
 * (`architecture.md` §4). Two stages, deliberately, so a failed import never costs the review
 * that preceded it.
 */
class FileImportViewModel(
    private val repository: RecipeRepository,
    private val readFile: suspend (String) -> ChosenFile?,
) : ViewModel() {

    private val _state = MutableStateFlow(FileImportUiState())
    val state: StateFlow<FileImportUiState> = _state.asStateFlow()

    fun choose(uri: String) {
        _state.value = FileImportUiState(stage = FileImportStage.Reading)

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

            // The library from the snapshot when there is no signal, which is the point: the
            // duplicate and conflict checks are as useful offline as on.
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

        _state.value = current.copy(stage = FileImportStage.Importing, serverFailures = emptyList())

        viewModelScope.launch {
            val body = fileImportBody(current.rows, current.resolutions)

            _state.value = when (val result = repository.importAll(body)) {
                is ApiResult.Success -> _state.value.copy(
                    stage = FileImportStage.Done(
                        imported = result.value.imported,
                        skipped = result.value.skipped,
                        replaced = result.value.replaced,
                    ),
                    // SF-014 again, from the other side: the server validates too, and a recipe
                    // it refused is one the user has to be told about by name.
                    serverFailures = result.value.failed,
                )

                is ApiResult.Failure -> _state.value.copy(
                    stage = FileImportStage.Failed(messageFor(result.error)),
                )
            }
        }
    }

    /** After a server failure: back to the review that is still held. */
    fun backToReview() {
        _state.value = _state.value.copy(
            stage = if (_state.value.rows.isEmpty()) FileImportStage.Ready else FileImportStage.Review,
        )
    }

    fun reset() {
        _state.value = FileImportUiState()
    }

    private fun messageFor(error: ApiError): String = when (error) {
        is ApiError.Network ->
            "Saving needs a connection — reading the file did not. Nothing was imported, and " +
                "your choices are still here to try again."

        is ApiError.Forbidden ->
            "The server refused the app's credentials, so nothing was imported. Check the " +
                "connection settings."

        is ApiError.ValidationFailed -> {
            val field = error.fields.firstOrNull()
            "The server refused the import" +
                (field?.let { " — ${it.path}: ${it.message}" } ?: "") +
                ". Nothing was written."
        }

        is ApiError.StorageUnavailable ->
            "The library is unreachable right now, so nothing was imported. Try again shortly."

        else -> "The import failed, so nothing was written: ${error::class.simpleName}"
    }

    companion object {
        fun factory(
            container: AppContainer,
            readFile: suspend (String) -> ChosenFile?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { FileImportViewModel(container.recipeRepository, readFile) }
        }
    }
}
