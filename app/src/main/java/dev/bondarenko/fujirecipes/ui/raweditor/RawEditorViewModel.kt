package dev.bondarenko.fujirecipes.ui.raweditor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.store.RawDevelopmentCache
import dev.bondarenko.fujirecipes.data.photo.PhotoReadResult
import dev.bondarenko.fujirecipes.data.photo.parseRecipeFromJpeg
import dev.bondarenko.fujirecipes.data.raweditor.NativeRaw
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import dev.bondarenko.fujirecipes.data.raweditor.rawColorProfileIds
import dev.bondarenko.fujirecipes.data.raweditor.readRafPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class RawEditorState(
    val name: String = "",
    val simulation: String = "",
    val fullWidth: Int = 0,
    val fullHeight: Int = 0,
    val preview: Bitmap? = null,
    val cameraImage: Bitmap? = null,
    val cropPreview: Bitmap? = null,
    val cropImageAspect: Float? = null,
    val adjustments: RawAdjustments = RawAdjustments(),
    val savedAdjustments: RawAdjustments? = null,
    val busy: Boolean = false,
    val rendering: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val hasUnsavedChanges: Boolean get() = adjustments.isEdited && adjustments != savedAdjustments
}

class RawEditorViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val context = application
    private val resolver = application.contentResolver
    private val directory = Files.createTempDirectory(application.cacheDir.toPath(), "raw-editor-").toFile()
    private val cache = RawDevelopmentCache(directory)
    private val mutex = Mutex()
    private var handle = 0L
    private var sourceFile: File? = null
    private val closed = AtomicBoolean(false)
    private var renderJob: Job? = null
    private var pendingExport: RawAdjustments? = null
    private val initialEdits = saved.get<FloatArray>("adjustments")?.let(RawAdjustments::fromValues)
        ?: RawAdjustments(saved["exposure"] ?: 0f, saved["shadows"] ?: 0f, saved["highlights"] ?: 0f)
    private val _state = MutableStateFlow(RawEditorState(adjustments = initialEdits))
    val state = _state.asStateFlow()

    init {
        saved.get<String>("uri")?.let { open(Uri.parse(it), initialEdits) }
    }

    fun open(uri: Uri, edits: RawAdjustments = RawAdjustments()) {
        if (_state.value.busy) return
        renderJob?.cancel()
        _state.update { it.copy(busy = true, rendering = false, error = null) }
        viewModelScope.launch {
            var imported: File? = null
            try {
                // Always finish native ownership transfer, even if the page closes during decode.
                withContext(Dispatchers.IO + NonCancellable) {
                    mutex.withLock {
                        check(!closed.get())
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        } ?: "photo.RAF"
                        val file = resolver.openInputStream(uri)?.use { cache.importRaf(UUID.randomUUID().toString(), it) }
                            ?: error(context.getString(R.string.raw_editor_unreadable))
                        imported = file
                        val cameraJpeg = readRafPreview(file)
                        val recipe = (parseRecipeFromJpeg(cameraJpeg) as? PhotoReadResult.Success)?.recipe
                        val profile = recipe?.settings?.get("filmSimulation") as? String
                        require(profile in rawColorProfileIds) {
                            context.getString(R.string.raw_editor_unsupported_profile, recipe?.filmSimulationLabel ?: "Unknown")
                        }
                        // Android's asset packager expands .gz sources and drops that suffix.
                        val lut = context.assets.open("raw-profiles/$profile.cube").use { it.readBytes() }
                        val nextHandle = NativeRaw.open(file.absolutePath, lut)
                        val nextState = try {
                            val dimensions = NativeRaw.outputDimensions(nextHandle, edits.nativeValues(), true)
                            val preview = renderBitmap(nextHandle, edits, false)
                            val camera = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(cameraJpeg))) { decoder, info, _ ->
                                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                            }
                            RawEditorState(
                                name = name, simulation = recipe!!.filmSimulationLabel.orEmpty(),
                                fullWidth = dimensions[0], fullHeight = dimensions[1],
                                preview = preview, cameraImage = camera, adjustments = edits,
                            )
                        } catch (error: Throwable) {
                            NativeRaw.close(nextHandle)
                            throw error
                        }
                        if (handle != 0L) NativeRaw.close(handle)
                        sourceFile?.delete()
                        sourceFile = file
                        handle = nextHandle
                        _state.value = nextState
                        try { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        catch (_: SecurityException) { /* The current grant still permits this edit session. */ }
                    }
                }
                saved["uri"] = uri.toString()
                rememberEdits(edits)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
            finally { if (imported != sourceFile) imported?.delete() }
        }
    }

    fun adjust(edits: RawAdjustments) {
        if (_state.value.busy || handle == 0L || _state.value.adjustments == edits) return
        rememberEdits(edits)
        _state.update { it.copy(adjustments = edits, rendering = true, error = null) }
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(120)
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        ensureActive()
                        renderBitmap(handle, edits, false) to NativeRaw.outputDimensions(handle, edits.nativeValues(), true)
                    }
                }
                if (_state.value.adjustments == edits) _state.update {
                    it.copy(preview = bitmap.first, fullWidth = bitmap.second[0], fullHeight = bitmap.second[1], rendering = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    fun prepareCrop() {
        val current = _state.value
        if (current.busy || current.rendering || handle == 0L) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        ensureActive()
                        val edits = current.adjustments.copy(crop = RawCrop())
                        val size = NativeRaw.outputDimensions(handle, edits.nativeValues(), true)
                        renderBitmap(handle, edits, false) to size[0].toFloat() / size[1]
                    }
                }
                _state.update { it.copy(busy = false, cropPreview = bitmap.first, cropImageAspect = bitmap.second) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    fun dismissCrop() { _state.update { it.copy(cropPreview = null, cropImageAspect = null) } }

    /** Freeze the requested edits while the system document picker is open. */
    fun prepareExport() { pendingExport = _state.value.adjustments }

    fun save(uri: Uri?) {
        val edits = pendingExport ?: return
        pendingExport = null
        if (uri == null || _state.value.busy || handle == 0L) return
        renderJob?.cancel()
        _state.update { it.copy(busy = true, rendering = false, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mutex.withLock {
                        val bitmap = renderBitmap(handle, edits, true)
                        val completed = File(directory, "export.jpg")
                        try {
                            completed.outputStream().use { output ->
                                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 97, output)) {
                                    context.getString(R.string.raw_editor_export_failed)
                                }
                            }
                            resolver.openOutputStream(uri, "wt")?.use { output ->
                                completed.inputStream().use { it.copyTo(output) }
                            } ?: error(context.getString(R.string.raw_editor_export_failed))
                        } finally {
                            bitmap.recycle()
                            completed.delete()
                        }
                    }
                }
                _state.update { it.copy(busy = false, savedAdjustments = edits, message = context.getString(R.string.raw_editor_saved)) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    private fun renderBitmap(nativeHandle: Long, edits: RawAdjustments, full: Boolean): Bitmap {
        val dimensions = NativeRaw.outputDimensions(nativeHandle, edits.nativeValues(), full)
        val bitmap = Bitmap.createBitmap(dimensions[0], dimensions[1], Bitmap.Config.ARGB_8888)
        try {
            NativeRaw.render(nativeHandle, bitmap, edits.nativeValues(), full)
            return bitmap
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    private fun rememberEdits(edits: RawAdjustments) {
        saved["adjustments"] = edits.nativeValues()
    }

    private fun fail(error: Exception) {
        _state.update { it.copy(busy = false, rendering = false, error = error.message ?: context.getString(R.string.raw_editor_unreadable)) }
    }

    private fun failMemory() {
        _state.update { it.copy(busy = false, rendering = false, error = context.getString(R.string.raw_editor_memory)) }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }

    override fun onCleared() {
        closed.set(true)
        // A native render cannot be interrupted by coroutine cancellation. Close after it finishes.
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                if (handle != 0L) NativeRaw.close(handle)
                handle = 0
                directory.deleteRecursively()
            }
        }
    }
}
