package dev.bondarenko.fujirecipes.ui.camera

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.CameraTransport
import dev.bondarenko.fujirecipes.camera.plan.SlotStatus
import dev.bondarenko.fujirecipes.core.result.LibraryResult
import dev.bondarenko.fujirecipes.core.store.LibraryStore
import dev.bondarenko.fujirecipes.data.repo.LocalRecipeRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class WriteViewModelTest {
    /** A lost link must not route the selected Bluetooth write through USB. No camera I/O. */
    @Test fun disconnectedBluetoothWriteKeepsBluetoothFailurePolicy() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val controller = (context.applicationContext as FujiRecipesApp).container.cameraController
        controller.selectTransport(CameraTransport.BLUETOOTH)
        withTimeout(5_000) { controller.transport.first { it == CameraTransport.BLUETOOTH } }
        controller.disconnect()
        withTimeout(5_000) { controller.state.first { it == CameraState.Disconnected } }
        controller.refreshBluetoothRecovery()
        check(controller.recovery.value == null) { "Run only without a pending camera write." }

        val protected = (File(context.noBackupFilesDir, "bluetooth-c7-recovery").listFiles().orEmpty().toList() +
            File(context.filesDir, "library.json")).filter { it.isFile }.associateWith { it.readBytes() }
        val scratch = File(context.cacheDir, "TEST-write-view-model-${UUID.randomUUID()}.json")
        val repository = LocalRecipeRepository(LibraryStore(scratch),
            now = { "2026-10-29T00:00:00.000Z" }, newId = { "TEST-disconnected-bluetooth" })
        val store = ViewModelStore()
        try {
            repository.load()
            val created = repository.create(buildJsonObject {
                put("name", "TEST Bluetooth routing")
                put("settings", buildJsonObject { put("filmSimulation", "classic-chrome"); put("color", 0) })
            })
            check(created is LibraryResult.Success)
            lateinit var model: WriteViewModel
            instrumentation.runOnMainSync {
                model = WriteViewModel(created.value.id, repository, controller)
                store.put("TEST-write", model)
            }
            withTimeout(5_000) { model.state.first { it.recipeName == "TEST Bluetooth routing" } }
            // Finish the picker rows without opening a camera connection.
            instrumentation.runOnMainSync { model.refreshSlots() }
            withTimeout(5_000) { model.state.first { it.slots.none { slot -> slot.status == SlotStatus.READING } } }
            instrumentation.runOnMainSync { model.confirm(7) }
            val finished = withTimeout(5_000) { model.state.first { !it.isWriting } }
            assertTrue("Expected Bluetooth failure, received ${finished.stage}", finished.stage is WriteStage.Failed)
            val failed = finished.stage as WriteStage.Failed
            assertEquals("Connect the X-T50 over Bluetooth before writing C7.", failed.message)
            assertFalse(failed.retryable)
            assertFalse(model.state.value.isWriting)
            assertNull(controller.recovery.value)
            protected.forEach { (file, bytes) -> assertArrayEquals(file.name, bytes, file.readBytes()) }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            scratch.delete()
        }
    }
}
