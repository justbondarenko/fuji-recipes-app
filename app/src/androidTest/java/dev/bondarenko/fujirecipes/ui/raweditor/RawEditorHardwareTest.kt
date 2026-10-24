package dev.bondarenko.fujirecipes.ui.raweditor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.data.raweditor.*
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

/** Opt in with -e rawEditorProbe true and files/raw-editor-fixture.raf on the device. */
class RawEditorHardwareTest {
    @Test
    fun previewKeepsUpdatingDuringContinuousSliderChanges() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("rawEditorProbe") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val fixture = File(app.filesDir,"raw-editor-fixture.raf")
        check(fixture.isFile)
        val profile = app.assets.open("raw-profiles/classic-chrome.cube").use { it.readBytes() }
        val handle = NativeRaw.open(fixture.path,profile)
        val heavy = RawAdjustments(clarity=.4f,texture=.3f,dehaze=.2f,colorNoise=.4f,luminanceNoise=.4f,
            sharpening=.4f,grainAmount=.5f,masks=listOf(RawMask(RawMaskKind.RADIAL,adjustments=RawLocalAdjustments(exposure=.4f))))
        try {
            for ((label,edits) in listOf("Exposure" to RawAdjustments(exposure=.5f),"Effects" to heavy)) {
                for (draft in listOf(false,true)) {
                    val size = NativeRaw.outputDimensions(handle,edits.nativeValues(),false,draft)
                    val bitmap = Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888)
                    val elapsed = measureNanoTime { NativeRaw.render(handle,bitmap,edits.nativeValues(),false,draft) } / 1_000_000
                    Log.i("RawEditorPerf","$label ${size[0]}x${size[1]}: ${elapsed}ms; RAF=${fixture.length()} bytes")
                    assertEquals(if (draft) { if (label == "Effects") 400 else 800 } else 1600,maxOf(bitmap.width,bitmap.height))
                    bitmap.recycle()
                }
            }
            assertThrows(java.io.IOException::class.java) { NativeRaw.outputDimensions(handle,heavy.nativeValues(),true,true) }
        } finally { NativeRaw.close(handle) }
        val input = File(app.cacheDir,"export/raw-editor-perf.raf").also { it.parentFile!!.mkdirs(); fixture.copyTo(it,true) }
        val saved = SavedStateHandle()
        val store = ViewModelStore()
        lateinit var model: RawEditorViewModel
        instrumentation.runOnMainSync {
            model = ViewModelProvider(object : ViewModelStoreOwner { override val viewModelStore = store }, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RawEditorViewModel(app,saved) as T
            })[RawEditorViewModel::class.java]
            model.open(FileProvider.getUriForFile(app,"${app.packageName}.fileprovider",input))
        }
        fun ready() = runBlocking { withTimeout(60_000) { model.state.first { !it.busy && !it.rendering && (it.preview != null || it.error != null) } } }
            .also { assertNull(it.error,it.error) }
        try {
            ready()
            for ((label,base) in listOf("Exposure" to RawAdjustments(),"Effects" to heavy)) {
                val frames = AtomicInteger()
                val firstFrame = AtomicLong()
                val draftFrames = AtomicInteger()
                val started = System.nanoTime()
                var lastInput = started
                var movingFrames = 0
                runBlocking {
                    var previous = model.state.value.preview
                    val collector = launch(Dispatchers.Default) {
                        model.state.collect { state ->
                            if (state.preview != null && state.preview !== previous) {
                                previous = state.preview
                                frames.incrementAndGet()
                                if (maxOf(state.preview.width,state.preview.height) <= 800) draftFrames.incrementAndGet()
                                firstFrame.compareAndSet(0,System.nanoTime())
                            }
                        }
                    }
                    repeat(120) { i ->
                        instrumentation.runOnMainSync { model.adjust(base.copy(exposure=(i+1)/120f)) }
                        delay(16)
                    }
                    lastInput = System.nanoTime()
                    movingFrames = frames.get()
                    withTimeout(60_000) { model.state.first { !it.rendering } }
                    collector.cancel()
                }
                val final = ready()
                val settle = (System.nanoTime()-lastInput)/1_000_000
                val first = (firstFrame.get()-started)/1_000_000
                Log.i("RawEditorPerf","$label drag: framesWhileMoving=$movingFrames, first=${first}ms, settle=${settle}ms, final=${final.preview!!.width}x${final.preview.height}")
                assertEquals(1600,maxOf(final.preview!!.width,final.preview.height))
                assertTrue("Dragging must use the reduced sensor preview",draftFrames.get() >= 3)
                assertEquals(base.copy(exposure=1f),final.adjustments)
                assertEquals(final.adjustments,RawAdjustments.fromValues(saved.get<FloatArray>("adjustments")!!))
                assertTrue("Preview must update while the slider is still moving ($label)",movingFrames >= 3)
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            input.delete()
        }
    }

    @Test
    fun realRafStartsWithItsProfileRecoversShadowsAndExportsFullResolution() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("rawEditorProbe") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val fixture = File(app.filesDir, "raw-editor-fixture.raf")
        check(fixture.isFile) { "Copy a real X-T50 RAF to files/raw-editor-fixture.raf first." }
        val exports = File(app.cacheDir, "export").apply { mkdirs() }
        val input = fixture.copyTo(File(exports, "raw-editor-probe.raf"), overwrite = true)
        val output = File(exports, "raw-editor-probe.jpg")
        val store = ViewModelStore()
        lateinit var model: RawEditorViewModel
        val saved = SavedStateHandle()
        instrumentation.runOnMainSync {
            val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
            model = ViewModelProvider(owner, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RawEditorViewModel(app, saved) as T
            })[RawEditorViewModel::class.java]
            model.open(FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", input))
        }
        fun awaitReady() = runBlocking {
            withTimeout(180_000) { model.state.first { !it.busy && !it.rendering && (it.preview != null || it.error != null) } }
        }.also { assertNull(it.error, it.error) }
        fun snapshot(bitmap: Bitmap, name: String) {
            File(app.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        }
        try {
            val start = awaitReady()
            assertEquals("Classic Chrome", start.simulation)
            assertTrue(maxOf(start.fullWidth, start.fullHeight) >= 7700 && minOf(start.fullWidth, start.fullHeight) >= 5100)
            val original = start.preview!!
            snapshot(original, "raw-editor-start.jpg")
            Log.i("RawEditorProbe", "Profile=${start.simulation}, preview=${original.width}x${original.height}, full=${start.fullWidth}x${start.fullHeight}")
            instrumentation.runOnMainSync { model.adjust(RawAdjustments(shadows = 1f)) }
            val lifted = awaitReady()
            val adjusted = lifted.preview!!
            var before = 0L
            var after = 0L
            for (y in 0 until original.height step 8) for (x in 0 until original.width step 8) {
                val base = original.getPixel(x, y)
                val next = adjusted.getPixel(x, y)
                before += (base shr 8) and 255
                after += (next shr 8) and 255
            }
            assertTrue("Shadow lift must increase the image's luminance", after > before)
            snapshot(adjusted, "raw-editor-shadows.jpg")
            val curve = RawCurve(listOf(CurvePoint(0f,0f),CurvePoint(.4f,.6f),CurvePoint(1f,1f)))
            val brush = RawMask(RawMaskKind.BRUSH, adjustments = RawLocalAdjustments(exposure = 1f), strokes = listOf(RawBrushStroke(listOf(MaskPoint(.2f,.5f),MaskPoint(.8f,.5f)),.15f)))
            val linear = RawMask(RawMaskKind.LINEAR, adjustments = RawLocalAdjustments(shadows = .8f))
            val radial = RawMask(RawMaskKind.RADIAL, adjustments = RawLocalAdjustments(temperature = .7f))
            for (edits in listOf(
                RawAdjustments(curves = RawCurves(master = curve)),
                RawAdjustments(curves = RawCurves(red = curve)),
                RawAdjustments(curves = RawCurves(green = curve)),
                RawAdjustments(curves = RawCurves(blue = curve)),
                RawAdjustments(grainAmount = .8f), RawAdjustments(grainAmount = .8f,grainSize = 7f,grainRoughness = 1f),
                RawAdjustments(masks = listOf(brush)), RawAdjustments(masks = listOf(linear)), RawAdjustments(masks = listOf(radial)),
                RawAdjustments(temperature = .4f), RawAdjustments(tint = -.3f),
                RawAdjustments(contrast = .3f), RawAdjustments(whites = .4f),
                RawAdjustments(blacks = .4f), RawAdjustments(saturation = -1f),
                RawAdjustments(vibrance = .6f),
                RawAdjustments(sharpening = .7f, sharpenRadius = 1.8f, sharpenThreshold = .1f),
                RawAdjustments(colorNoise = .8f), RawAdjustments(luminanceNoise = .8f, noiseDetail = .2f),
                RawAdjustments(clarity = .6f), RawAdjustments(clarity = -.6f),
                RawAdjustments(texture = .6f), RawAdjustments(texture = -.6f),
                RawAdjustments(dehaze = .6f), RawAdjustments(dehaze = -.6f),
                RawAdjustments(vignette = -.6f), RawAdjustments(vignette = .6f),
                RawAdjustments(crop = RawCrop(.1f, .1f, .9f, .9f)),
                RawAdjustments(quarterTurns = 1), RawAdjustments(straighten = 5f),
                RawAdjustments(distortion = .6f), RawAdjustments(distortion = -.6f),
                RawAdjustments(perspectiveVertical = .4f), RawAdjustments(perspectiveVertical = -.4f),
                RawAdjustments(perspectiveHorizontal = .4f), RawAdjustments(perspectiveHorizontal = -.4f),
                RawAdjustments(chromaticRed = .8f), RawAdjustments(chromaticRed = -.8f),
                RawAdjustments(chromaticBlue = .8f), RawAdjustments(chromaticBlue = -.8f),
            )) {
                instrumentation.runOnMainSync { model.adjust(edits) }
                assertFalse("The control must change RAW pixels: $edits", original.sameAs(awaitReady().preview))
            }
            instrumentation.runOnMainSync { model.adjust(RawAdjustments()) }
            assertTrue("Reset must reproduce the profiled starting point", original.sameAs(awaitReady().preview))
            // Exercise the actual preview-to-source drawing path after crop and rotation.
            val drawing = RawAdjustments(quarterTurns = 1, crop = RawCrop(.1f,.2f,.9f,.8f), masks = listOf(brush.copy(strokes = emptyList())))
            instrumentation.runOnMainSync { model.adjust(drawing) }
            awaitReady()
            instrumentation.runOnMainSync { model.editMasks(true) }
            val beforeDrawing = awaitReady().preview!!
            instrumentation.runOnMainSync { model.drawMask(listOf(MaskPoint(.3f,.4f),MaskPoint(.7f,.6f)),.1f,false) }
            val drawn = awaitReady()
            val stroke = drawn.adjustments.masks[0].strokes.single()
            assertEquals(2,stroke.points.size)
            assertTrue(stroke.radius > 0)
            assertNotEquals(MaskPoint(.3f,.4f),stroke.points.first())
            assertFalse(beforeDrawing.sameAs(drawn.preview))
            assertNotNull(drawn.maskOverlay)
            instrumentation.runOnMainSync { model.adjust(drawing.copy(masks = listOf(linear))) }
            awaitReady()
            instrumentation.runOnMainSync { model.drawMask(listOf(MaskPoint(.2f,.2f),MaskPoint(.8f,.8f)),.1f,false) }
            val placed = awaitReady().adjustments
            instrumentation.runOnMainSync { model.drawMask(listOf(MaskPoint(.2f,.2f),MaskPoint(.8f,.8f)),.1f,false) }
            assertEquals("Drawing the same gradient must still complete rendering",placed,awaitReady().adjustments)
            instrumentation.runOnMainSync { model.editMasks(false) }
            instrumentation.runOnMainSync {
                model.adjust(RawAdjustments(shadows = .4f, highlights = -.3f,
                    temperature = .2f, tint = -.1f, contrast = .15f, whites = -.1f, blacks = .1f,
                    saturation = -.1f, vibrance = .2f, sharpening = .4f, sharpenRadius = 1.2f,
                    sharpenThreshold = .1f, colorNoise = .5f, luminanceNoise = .4f, noiseDetail = .6f,
                    clarity = .3f, texture = .25f, dehaze = .2f,
                    crop = RawCrop(.1f, .1f, .85f, .9f), quarterTurns = 1, straighten = 3.5f,
                    vignette = -.4f, vignetteMidpoint = .4f, vignetteRoundness = .3f, vignetteFeather = .7f,
                    distortion = -.3f, perspectiveVertical = .15f, perspectiveHorizontal = -.1f,
                    chromaticRed = .4f, chromaticBlue = -.3f, defringePurple = .5f, defringeGreen = .4f,
                    grainAmount = .5f, grainSize = 5f, grainRoughness = .8f, curves = RawCurves(master = curve,blue = curve),
                    masks = listOf(brush.copy(strokes = brush.strokes + RawBrushStroke(listOf(MaskPoint(.5f,.5f)),.05f,true)),linear,radial)))
            }
            val allControls = awaitReady()
            snapshot(allControls.preview!!, "raw-editor-batch.jpg")
            assertTrue("Quarter-turn must swap output orientation", (allControls.fullWidth > allControls.fullHeight) != (start.fullWidth > start.fullHeight))
            instrumentation.runOnMainSync { model.editMasks(true) }
            val draftMasking = runBlocking { withTimeout(30_000) {
                model.state.first { it.maskOverlay != null && maxOf(it.maskOverlay.width,it.maskOverlay.height) <= 800 }
            } }
            assertEquals(draftMasking.preview!!.width,draftMasking.maskOverlay!!.width)
            assertEquals(draftMasking.preview.height,draftMasking.maskOverlay.height)
            val masking = awaitReady()
            assertNotNull(masking.maskOverlay)
            assertEquals(masking.preview!!.width,masking.maskOverlay!!.width)
            assertEquals(masking.preview.height,masking.maskOverlay.height)
            instrumentation.runOnMainSync { model.selectMask(1) }
            val gradientOverlay = awaitReady().maskOverlay!!
            assertFalse(masking.maskOverlay.sameAs(gradientOverlay))
            instrumentation.runOnMainSync { model.editMasks(false); model.prepareCrop() }
            val cropState = awaitReady()
            val crop = cropState.cropPreview!!
            assertTrue("Crop editor must show the uncropped rendering", crop.width > allControls.preview.width && crop.height > allControls.preview.height)
            instrumentation.runOnMainSync {
                model.dismissCrop()
                model.adjust(allControls.adjustments.copy(crop = RawCrop().fitAspect(1f, cropState.cropImageAspect!!)))
            }
            val square = awaitReady()
            assertTrue("Square preset must use full-resolution dimensions", kotlin.math.abs(square.fullWidth - square.fullHeight) <= 1)
            instrumentation.runOnMainSync { model.adjust(allControls.adjustments) }
            awaitReady()
            assertNull(model.state.value.cropPreview)
            lateinit var restored: RawEditorViewModel
            instrumentation.runOnMainSync {
                val restoredSaved = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
                val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
                restored = ViewModelProvider(owner, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RawEditorViewModel(app, restoredSaved) as T
                })["restored", RawEditorViewModel::class.java]
            }
            val restoredState = runBlocking { withTimeout(180_000) {
                restored.state.first { !it.busy && (it.preview != null || it.error != null) }
            } }
            assertNull(restoredState.error, restoredState.error)
            assertEquals(allControls.adjustments, restoredState.adjustments)
            assertTrue(allControls.preview!!.sameAs(restoredState.preview))
            instrumentation.runOnMainSync {
                model.prepareExport()
                model.save(FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", output))
            }
            val exported = awaitReady()
            assertTrue(output.length() > 1_000_000)
            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(output.path, dimensions)
            assertEquals(allControls.fullWidth, dimensions.outWidth)
            assertEquals(allControls.fullHeight, dimensions.outHeight)
            Log.i("RawEditorProbe", "Framed export=${dimensions.outWidth}x${dimensions.outHeight}")
            assertFalse(exported.hasUnsavedChanges)
            // A rejected replacement keeps the current RAW, edits, and export source intact.
            val invalid = File(exports, "raw-editor-invalid.raf").apply { writeText("not a RAF") }
            instrumentation.runOnMainSync { model.open(FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", invalid)) }
            val failed = runBlocking { withTimeout(30_000) { model.state.first { !it.busy && it.error != null } } }
            assertEquals(start.name, failed.name)
            assertEquals(exported.adjustments, failed.adjustments)
            assertTrue(fixture.length() == input.length())
            invalid.delete()
            Log.i("RawEditorProbe", "Curves, grain, brush/linear/radial masks and overlays; all control effects, reset, session restore, full export (${output.length()} bytes), and rejected replacement passed")
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            input.delete()
        }
    }
}
