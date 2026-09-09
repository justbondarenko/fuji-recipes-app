package dev.bondarenko.fujirecipes.camera

import android.net.Uri
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaError
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaFailure
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
import dev.bondarenko.fujirecipes.core.store.CameraPhotoExporter
import dev.bondarenko.fujirecipes.core.store.CameraTransferRecord
import dev.bondarenko.fujirecipes.core.store.CameraTransferStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** One batch, as the screen asked for it. */
data class CameraTransferJob(
    val files: List<CameraMediaObject>,
    val treeUri: String,
    /** What to call the camera in the notification, e.g. "X-T50 · USB Card Reader". */
    val cameraLabel: String,
)

/**
 * Where a batch has got to, for anything that wants to draw it.
 *
 * The terminal states stay put until [CameraTransfers.acknowledge] clears them, so a screen
 * that was not on top when the batch ended still finds out what happened.
 */
sealed interface CameraTransferState {
    data class Running(val progress: CameraExportProgress) : CameraTransferState

    data class Finished(val savedCount: Int) : CameraTransferState

    data class Failed(val message: String) : CameraTransferState

    data object Cancelled : CameraTransferState
}

/**
 * The one object a transfer runs through, shared by the service that owns it and the screen
 * that started it.
 *
 * The batch deliberately does **not** live in a `ViewModel`. A `viewModelScope` job is torn
 * down with its screen and, worse, gives Android no reason to keep the process: once the
 * activity stops, a cached process may be killed at any time, and a USB transfer that is
 * minutes long is exactly the shape Android reclaims. [CameraTransferService] runs the batch
 * instead, and this holds the state both sides read.
 *
 * The job is handed over here rather than through the service `Intent` because a
 * [CameraMediaObject] carries a `PtpObjectInfo` from the pure protocol layer, which must not
 * learn about `Parcelable` (`coding-standards.md` P4).
 */
class CameraTransfers(
    private val exporter: CameraPhotoExporter,
    private val store: CameraTransferStore,
) {
    private val _state = MutableStateFlow<CameraTransferState?>(null)
    val state: StateFlow<CameraTransferState?> = _state.asStateFlow()

    private val cancelRequested = AtomicBoolean(false)

    @Volatile
    private var pending: CameraTransferJob? = null

    /** True while a batch is under way, so a second Download does not start on top of it. */
    val isRunning: Boolean get() = _state.value is CameraTransferState.Running

    /** Stashes the batch for the service to pick up, and shows it as started right away. */
    fun request(job: CameraTransferJob) {
        cancelRequested.set(false)
        pending = job
        _state.value = CameraTransferState.Running(
            CameraExportProgress(
                currentFile = 1,
                totalFiles = job.files.size,
                filename = job.files.first().info.filename,
                written = 0,
                totalBytes = job.files.sumOf { it.info.compressedSize },
            ),
        )
    }

    fun takePending(): CameraTransferJob? = pending.also { pending = null }

    /**
     * Asks the batch to stop. It is honoured between chunks, so a cancel lands within one USB
     * read rather than at the end of the current file.
     */
    fun cancel() {
        cancelRequested.set(true)
    }

    /** Clears a terminal state once a screen has shown it. */
    fun acknowledge() {
        if (_state.value !is CameraTransferState.Running) _state.value = null
    }

    /**
     * Runs one batch to its end, keeping the on-disk record in step with it.
     *
     * Never throws: the outcome is the state, because the caller is a service with nowhere to
     * report an exception to.
     */
    suspend fun run(job: CameraTransferJob) {
        var record = store.begin(
            CameraTransferRecord(
                batchId = UUID.randomUUID().toString(),
                treeUri = job.treeUri,
                startedAt = nowIso(),
                totalFiles = job.files.size,
            ),
        )

        try {
            val result = exporter.download(
                files = job.files,
                treeUri = Uri.parse(job.treeUri),
                onProgress = { progress -> _state.value = CameraTransferState.Running(progress) },
                isCancelled = cancelRequested::get,
                onFileStarted = { filename -> record = store.fileStarted(record, filename) },
                onFileFinished = { filename -> record = store.fileFinished(record, filename) },
            )
            _state.value = CameraTransferState.Finished(result.filenames.size)
        } catch (cancelled: CameraMediaError) {
            _state.value = if (cancelled.reason == CameraMediaFailure.CANCELLED) {
                CameraTransferState.Cancelled
            } else {
                CameraTransferState.Failed(cancelled.message.orEmpty())
            }
        } catch (error: Exception) {
            _state.value = CameraTransferState.Failed(
                error.message ?: "The selected files could not be downloaded.",
            )
        } finally {
            // The batch ended in a way this process saw, so there is nothing to recover from —
            // whatever happened, the exporter has already removed the documents it created.
            store.clear()
            cancelRequested.set(false)
        }
    }

    private fun nowIso(): String = AppContainer.isoNow()
}
