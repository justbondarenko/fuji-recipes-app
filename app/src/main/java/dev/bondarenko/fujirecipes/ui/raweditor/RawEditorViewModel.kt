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
import dev.bondarenko.fujirecipes.data.raweditor.RawMaskKind
import dev.bondarenko.fujirecipes.data.raweditor.MaskPoint
import dev.bondarenko.fujirecipes.data.raweditor.RawBrushStroke
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import dev.bondarenko.fujirecipes.data.raweditor.rawColorProfileIds
import dev.bondarenko.fujirecipes.data.raweditor.readRafPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
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

data class RawDetail(val viewport: RawViewport, val edits: RawAdjustments, val bitmap: Bitmap)

data class RawEditorState(
    val name: String = "",
    val simulation: String = "",
    val asShotProfile: String? = null,
    val lensCorrectionApplied: Boolean = false,
    val fullWidth: Int = 0,
    val fullHeight: Int = 0,
    val preview: Bitmap? = null,
    val cameraImage: Bitmap? = null,
    val startingImage: Bitmap? = null,
    val detail: RawDetail? = null,
    val detailLoading: Boolean = false,
    val cropPreview: Bitmap? = null,
    val cropAdjustments: RawAdjustments? = null,
    val cropRendering: Boolean = false,
    val maskEditorOpen: Boolean = false,
    val activeMask: Int = 0,
    val maskOverlay: Bitmap? = null,
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
    private var profileHandle = 0L
    private var activeProfile: String? = null
    private var sourceFile: File? = null
    private val closed = AtomicBoolean(false)
    private data class PreviewRequest(val edits: RawAdjustments, val mask: Int?, val generation: Int, val sequence: Int, val viewport: RawViewport?, val preview: Boolean, val framing: Boolean = false)
    private val previewRequests = MutableStateFlow<PreviewRequest?>(null)
    private var detailViewport: RawViewport? = null
    private var previewSequence = 0
    private var sourceGeneration = 0
    private var pendingExport: RawAdjustments? = null
    private val initialEdits = (saved.get<FloatArray>("adjustments")?.let(RawAdjustments::fromValues)
        ?: RawAdjustments(saved["exposure"] ?: 0f, saved["shadows"] ?: 0f, saved["highlights"] ?: 0f))
        .copy(filmSimulation = saved["filmSimulation"])
    private val _state = MutableStateFlow(RawEditorState(adjustments = initialEdits))
    val state = _state.asStateFlow()

    init {
        // ponytail: one CPU renderer; add native cancellation/GPU only if draft latency needs it.
        // Conflate pending settings while still showing each completed draft.
        viewModelScope.launch {
            previewRequests.collect { request ->
                if (request == null) return@collect
                try {
                    if (request.preview) renderPreview(request, draft = true)
                    val superseded = withTimeoutOrNull(160) {
                        previewRequests.first { it != request }
                        true
                    }
                    if (superseded != true) {
                        if (request.preview) renderPreview(request, draft = false)
                        if (!request.framing) renderDetail(request)
                    }
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) { if (previewRequests.value == request) fail(error) }
                catch (_: OutOfMemoryError) { if (previewRequests.value == request) failMemory() }
            }
        }
        saved.get<String>("uri")?.let { open(Uri.parse(it), initialEdits) }
    }

    fun open(uri: Uri, edits: RawAdjustments = RawAdjustments()) {
        if (_state.value.busy) return
        detailViewport = null
        sourceGeneration++
        previewRequests.value = null
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
                        profileHandle = nextHandle
                        activeProfile = profile
                        val nextState = try {
                            val dimensions = NativeRaw.outputDimensions(nextHandle, edits.nativeValues(), true)
                            val preview = renderBitmap(nextHandle, edits, false, profileId = edits.filmSimulation ?: profile)
                            val startingImage = if (edits == RawAdjustments()) preview else renderBitmap(nextHandle, RawAdjustments(), false, profileId = profile)
                            val camera = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(cameraJpeg))) { decoder, info, _ ->
                                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                            }
                            RawEditorState(
                                name = name, simulation = recipe!!.filmSimulationLabel.orEmpty(),
                                asShotProfile = profile,
                                fullWidth = dimensions[0], fullHeight = dimensions[1],
                                lensCorrectionApplied = NativeRaw.dimensions(nextHandle)[4] == 1,
                                preview = preview, cameraImage = camera, startingImage = startingImage, adjustments = edits,
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

    fun adjust(edits: RawAdjustments) = requestRender(edits)

    private fun requestRender(edits: RawAdjustments, force: Boolean = false) {
        if (_state.value.busy || handle == 0L || (!force && _state.value.adjustments == edits)) return
        if (edits.frameKey() != _state.value.adjustments.frameKey()) detailViewport = null
        rememberEdits(edits)
        _state.update { it.copy(adjustments = edits, rendering = true, detail = null,
            detailLoading = detailViewport != null, error = null) }
        enqueuePreview(edits, preview = true)
    }

    fun detailViewport(viewport: RawViewport?) {
        if (detailViewport == viewport || _state.value.busy || _state.value.cropAdjustments != null || handle == 0L) return
        detailViewport = viewport
        _state.update { it.copy(detail = if (viewport == null) null else it.detail, detailLoading = viewport != null, error = if (viewport != null) null else it.error) }
        // A pan must not supersede a pending slider refinement.
        enqueuePreview(_state.value.adjustments, preview = _state.value.rendering)
    }

    private fun enqueuePreview(edits: RawAdjustments, preview: Boolean) {
        previewRequests.value = PreviewRequest(edits, _state.value.activeMask.takeIf { _state.value.maskEditorOpen },
            sourceGeneration, ++previewSequence, detailViewport, preview)
    }

    private suspend fun renderDetail(request: PreviewRequest) {
        val result = withContext(Dispatchers.Default) {
            mutex.withLock {
                if (previewRequests.value != request || request.generation != sourceGeneration || _state.value.busy) return@withLock null
                val viewport = request.viewport
                if (viewport == null) {
                    NativeRaw.releaseDetail(handle)
                    return@withLock null
                }
                val bitmap = Bitmap.createBitmap(viewport.width, viewport.height, Bitmap.Config.ARGB_8888)
                try {
                    useProfile(handle, request.edits.filmSimulation ?: _state.value.asShotProfile)
                    NativeRaw.renderDetail(handle, bitmap, request.edits.nativeValues(), viewport.left, viewport.top, viewport.scale)
                    RawDetail(viewport, request.edits, bitmap)
                } catch (error: Throwable) { bitmap.recycle(); throw error }
            }
        }
        if (previewRequests.value == request && request.generation == sourceGeneration && !_state.value.busy) {
            _state.update { it.copy(detail = result, detailLoading = false) }
        }
    }

    private suspend fun renderPreview(request: PreviewRequest, draft: Boolean) {
        val result = withContext(Dispatchers.Default) {
            mutex.withLock {
                ensureActive()
                if (previewRequests.value != request || request.generation != sourceGeneration || _state.value.busy) return@withLock null
                val parameters = request.edits.nativeValues()
                val preview = renderBitmap(handle, request.edits, false, draft)
                val overlay = if (request.mask != null && request.mask in request.edits.masks.indices && _state.value.maskEditorOpen) {
                    Bitmap.createBitmap(preview.width, preview.height, Bitmap.Config.ARGB_8888).also {
                        NativeRaw.maskOverlay(handle, it, parameters, request.mask, draft)
                    }
                } else null
                Triple(preview, NativeRaw.outputDimensions(handle, parameters, true), overlay)
            }
        } ?: return
        val latest = previewRequests.value ?: return
        if (request.generation != sourceGeneration || _state.value.busy || (!draft && latest != request)) return
        if (request.framing) {
            if (_state.value.cropAdjustments == null) return
            _state.update { it.copy(cropPreview = result.first, cropImageAspect = result.second[0].toFloat() / result.second[1],
                cropRendering = draft || latest != request) }
            return
        }
        _state.update {
            it.copy(preview = result.first, fullWidth = result.second[0], fullHeight = result.second[1],
                rendering = draft || latest != request,
                maskOverlay = if (it.maskEditorOpen && it.activeMask == request.mask) result.third else null)
        }
    }

    fun editMasks(open: Boolean) {
        _state.update { it.copy(maskEditorOpen = open, maskOverlay = null) }
        if (open) requestRender(_state.value.adjustments, force = true)
    }

    fun selectMask(index: Int) {
        _state.update { it.copy(activeMask = index, maskOverlay = null) }
        requestRender(_state.value.adjustments, force = true)
    }

    /** Drawing arrives in the framed preview; store it in upright sensor coordinates. */
    fun drawMask(points: List<MaskPoint>, radius: Float, erase: Boolean) {
        val current = _state.value
        val index = current.activeMask
        if (current.busy || current.rendering || index !in current.adjustments.masks.indices || points.isEmpty()) return
        previewRequests.value = null
        _state.update { it.copy(rendering = true, error = null) }
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        val mask = current.adjustments.masks[index]
                        val preview = current.preview!!
                        val center = points.first()
                        val offset = radius * minOf(preview.width, preview.height) / preview.width
                        val reference = MaskPoint((center.x + if (center.x > .5f) -offset else offset).coerceIn(0f, 1f), center.y)
                        val mapped = NativeRaw.mapPoints(handle, current.adjustments.nativeValues(),
                            (points + reference).flatMap { listOf(it.x, it.y) }.toFloatArray())
                            .toList().chunked(2).map { MaskPoint(it[0], it[1]) }
                        val source = NativeRaw.dimensions(handle)
                        val r = kotlin.math.hypot((mapped.first().x - mapped.last().x) * source[2],
                            (mapped.first().y - mapped.last().y) * source[3]) / minOf(source[2], source[3])
                        val path = mapped.dropLast(1)
                        if (mask.kind == RawMaskKind.BRUSH) {
                            require(mask.strokes.size < 64 && current.adjustments.masks.sumOf { m -> m.strokes.sumOf { it.points.size } } + path.size <= 4096) {
                                context.getString(R.string.raw_editor_mask_limit)
                            }
                            mask.copy(strokes = mask.strokes + RawBrushStroke(path, r.coerceIn(.0005f, 1f), erase))
                        } else mask.copy(start = path.first(), end = path.last())
                    }
                }
                if (_state.value.adjustments == current.adjustments && _state.value.activeMask == index) {
                    requestRender(current.adjustments.copy(masks = current.adjustments.masks.toMutableList().also { it[index] = next }), force = true)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    fun prepareCrop() {
        val current = _state.value
        if (current.busy || current.cropAdjustments != null || handle == 0L) return
        previewRequests.value = null
        detailViewport = null
        _state.update { it.copy(busy = true, rendering = false, detail = null, detailLoading = false, error = null) }
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        ensureActive()
                        NativeRaw.releaseDetail(handle)
                        val edits = current.adjustments.copy(crop = RawCrop())
                        val size = NativeRaw.outputDimensions(handle, edits.nativeValues(), true)
                        renderBitmap(handle, edits, false) to size[0].toFloat() / size[1]
                    }
                }
                _state.update { it.copy(busy = false, cropPreview = bitmap.first, cropImageAspect = bitmap.second,
                    cropAdjustments = current.adjustments, cropRendering = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    fun adjustCrop(edits: RawAdjustments) {
        if (_state.value.busy || _state.value.cropAdjustments == null || (_state.value.cropAdjustments == edits && _state.value.error == null)) return
        _state.update { it.copy(cropAdjustments = edits, cropRendering = true, error = null) }
        previewRequests.value = PreviewRequest(edits.copy(crop = RawCrop()), null, sourceGeneration,
            ++previewSequence, null, preview = true, framing = true)
    }

    fun dismissCrop() {
        previewRequests.value = null
        _state.update { it.copy(cropPreview = null, cropImageAspect = null, cropAdjustments = null, cropRendering = false, error = null) }
        requestRender(_state.value.adjustments, force = true)
    }

    /** Freeze the requested edits while the system document picker is open. */
    fun prepareExport() { pendingExport = _state.value.adjustments }

    fun save(uri: Uri?) {
        val edits = pendingExport ?: return
        pendingExport = null
        if (uri == null || _state.value.busy || handle == 0L) return
        previewRequests.value = null
        _state.update { it.copy(busy = true, rendering = false, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mutex.withLock {
                        NativeRaw.releaseDetail(handle)
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
                if (detailViewport != null) enqueuePreview(_state.value.adjustments, preview = false)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error) }
            catch (_: OutOfMemoryError) { failMemory() }
        }
    }

    private fun renderBitmap(nativeHandle: Long, edits: RawAdjustments, full: Boolean, draft: Boolean = false,
        profileId: String? = edits.filmSimulation ?: _state.value.asShotProfile): Bitmap {
        useProfile(nativeHandle, profileId)
        val parameters = edits.nativeValues()
        val dimensions = NativeRaw.outputDimensions(nativeHandle, parameters, full, draft)
        val bitmap = Bitmap.createBitmap(dimensions[0], dimensions[1], Bitmap.Config.ARGB_8888)
        try {
            NativeRaw.render(nativeHandle, bitmap, parameters, full, draft)
            return bitmap
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    private fun rememberEdits(edits: RawAdjustments) {
        saved["adjustments"] = edits.nativeValues()
        saved["filmSimulation"] = edits.filmSimulation
    }

    /** Called under mutex for every preview, crop, detail tile, and export request. */
    private fun useProfile(nativeHandle: Long, profileId: String?) {
        require(profileId in rawColorProfileIds) { "Unsupported RAW color profile." }
        if (profileHandle == nativeHandle && activeProfile == profileId) return
        val lut = context.assets.open("raw-profiles/$profileId.cube").use { it.readBytes() }
        NativeRaw.setProfile(nativeHandle, lut)
        profileHandle = nativeHandle
        activeProfile = profileId
    }

    private fun fail(error: Exception) {
        _state.update { it.copy(busy = false, rendering = false, detailLoading = false, cropRendering = false, error = error.message ?: context.getString(R.string.raw_editor_unreadable)) }
    }

    private fun failMemory() {
        _state.update { it.copy(busy = false, rendering = false, detailLoading = false, cropRendering = false, error = context.getString(R.string.raw_editor_memory)) }
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
