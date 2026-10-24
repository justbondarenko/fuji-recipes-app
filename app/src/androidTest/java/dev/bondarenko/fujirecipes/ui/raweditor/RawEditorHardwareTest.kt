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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt in with -e rawEditorProbe true and files/raw-editor-fixture.raf on the device. */
class RawEditorHardwareTest {
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
            for (edits in listOf(
                RawAdjustments(temperature = .4f), RawAdjustments(tint = -.3f),
                RawAdjustments(contrast = .3f), RawAdjustments(whites = .4f),
                RawAdjustments(blacks = .4f), RawAdjustments(saturation = -1f),
                RawAdjustments(vibrance = .6f),
                RawAdjustments(sharpening = .7f, sharpenRadius = 1.8f, sharpenThreshold = .1f),
                RawAdjustments(colorNoise = .8f), RawAdjustments(luminanceNoise = .8f, noiseDetail = .2f),
            )) {
                instrumentation.runOnMainSync { model.adjust(edits) }
                assertFalse("The control must change RAW pixels: $edits", original.sameAs(awaitReady().preview))
            }
            instrumentation.runOnMainSync { model.adjust(RawAdjustments()) }
            assertTrue("Reset must reproduce the profiled starting point", original.sameAs(awaitReady().preview))
            instrumentation.runOnMainSync {
                model.adjust(RawAdjustments(shadows = .4f, highlights = -.3f,
                    temperature = .2f, tint = -.1f, contrast = .15f, whites = -.1f, blacks = .1f,
                    saturation = -.1f, vibrance = .2f, sharpening = .4f, sharpenRadius = 1.2f,
                    sharpenThreshold = .1f, colorNoise = .5f, luminanceNoise = .4f, noiseDetail = .6f))
            }
            val allControls = awaitReady()
            snapshot(allControls.preview!!, "raw-editor-batch.jpg")
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
            assertEquals(start.fullWidth, dimensions.outWidth)
            assertEquals(start.fullHeight, dimensions.outHeight)
            assertFalse(exported.hasUnsavedChanges)
            // A rejected replacement keeps the current RAW, edits, and export source intact.
            val invalid = File(exports, "raw-editor-invalid.raf").apply { writeText("not a RAF") }
            instrumentation.runOnMainSync { model.open(FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", invalid)) }
            val failed = runBlocking { withTimeout(30_000) { model.state.first { !it.busy && it.error != null } } }
            assertEquals(start.name, failed.name)
            assertEquals(exported.adjustments, failed.adjustments)
            assertTrue(fixture.length() == input.length())
            invalid.delete()
            Log.i("RawEditorProbe", "All control effects, reset, session restore, full export (${output.length()} bytes), and rejected replacement passed")
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            input.delete()
        }
    }
}
