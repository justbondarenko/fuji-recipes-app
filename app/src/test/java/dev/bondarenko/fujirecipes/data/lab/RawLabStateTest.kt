package dev.bondarenko.fujirecipes.data.lab

import dev.bondarenko.fujirecipes.camera.raw.DEFAULT_RAW_SUPPORTED_FIELD_IDS
import dev.bondarenko.fujirecipes.camera.raw.RAW_UNRENDERED_FIELD_IDS
import dev.bondarenko.fujirecipes.camera.raw.RawProfilePatch
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentResult
import dev.bondarenko.fujirecipes.camera.usb.RawRenderQuality
import dev.bondarenko.fujirecipes.data.model.Recipe
import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RawLabStateTest {

    @Test
    fun `a recipe becomes both the working copy and the baseline`() {
        val recipe = recipe(buildJsonObject { put("filmSimulation", "velvia") })

        val state = RawLabState().applying(recipe)

        assertEquals(recipe.settings, state.settings)
        assertFalse(state.isDirty, "applying a recipe is not an edit")
        assertEquals("Velvia days", state.appliedRecipeName)
    }

    @Test
    fun `editing after applying a recipe is dirty, and offers to update it`() {
        val state = RawLabState()
            .applying(recipe(buildJsonObject { put("filmSimulation", "velvia") }))
            .withSetting("clarity", JsonPrimitive(3))

        assertTrue(state.isDirty)
        assertTrue(state.canUpdateAppliedRecipe)
    }

    @Test
    fun `ground zero has no recipe to update`() {
        val state = RawLabState().fromDefaults().withSetting("clarity", JsonPrimitive(3))

        assertTrue(state.isDirty)
        assertFalse(state.canUpdateAppliedRecipe)
    }

    @Test
    fun `a render is stale as soon as the settings move`() {
        val rendered = buildJsonObject { put("filmSimulation", "provia") }
        val state = loaded().copy(settings = rendered)
            .renderStarted()
            .renderSucceeded(result(), rendered)

        assertFalse(state.isPreviewStale)
        assertTrue(state.withSetting("clarity", JsonPrimitive(2)).isPreviewStale)
    }

    @Test
    fun `automatic rendering waits for a change, a file and an idle camera`() {
        val started = loaded().withAutoPreview(true).renderStarted()
        val idle = started.renderSucceeded(result(), started.settings)

        assertFalse(idle.shouldAutoRender(), "nothing has changed since the last render")

        val edited = idle.withSetting("clarity", JsonPrimitive(1))
        assertTrue(edited.shouldAutoRender())
        assertFalse(edited.renderStarted().shouldAutoRender(), "a render is already running")
        assertFalse(
            edited.copy(autoPreview = false).shouldAutoRender(),
            "the automatic mode is off",
        )
        assertFalse(
            RawLabState().withAutoPreview(true).shouldAutoRender(),
            "there is no RAF to render",
        )
    }

    @Test
    fun `two failures in a row switch automatic rendering off`() {
        val state = loaded().withAutoPreview(true)

        val once = state.renderFailed("Camera busy")
        assertTrue(once.autoPreview, "one refusal is not a pattern")

        val twice = once.renderStarted().renderFailed("Camera busy")
        assertFalse(twice.autoPreview)
    }

    @Test
    fun `a failed render keeps the last picture`() {
        val started = loaded().renderStarted()
        val state = started
            .renderSucceeded(result(), started.settings)
            .renderStarted()
            .renderFailed("The camera stopped responding")

        assertTrue(state.preview != null)
        assertEquals("The camera stopped responding", state.error)
        assertFalse(state.isRendering)
    }

    @Test
    fun `each render gets its own output number`() {
        // Two renders writing to one path would leave an image loader showing the first.
        val first = loaded().renderStarted()
        val second = first.renderSucceeded(result(), first.settings).renderStarted()

        assertTrue(second.serial > first.serial)
    }

    @Test
    fun `fields split into what the camera renders and what is only stored`() {
        val fields = RawLabState().fromDefaults().fields(DEFAULT_RAW_SUPPORTED_FIELD_IDS)

        assertTrue(fields.rendered.any { it.id == "filmSimulation" })
        assertTrue(fields.rendered.any { it.id == "exposureCompensation" })
        assertTrue(fields.storedOnly.all { it.id in RAW_UNRENDERED_FIELD_IDS })
        assertTrue(fields.storedOnly.any { it.id == "dRangePriority" })
    }

    @Test
    fun `a monochrome simulation removes the colour control entirely`() {
        val state = RawLabState().fromDefaults()
            .withSetting("filmSimulation", JsonPrimitive("acros"))
        val fields = state.fields(DEFAULT_RAW_SUPPORTED_FIELD_IDS)

        assertFalse((fields.rendered + fields.storedOnly).any { it.id == "color" })
    }

    @Test
    fun `saving makes the saved settings the new unchanged`() {
        val state = RawLabState()
            .applying(recipe(buildJsonObject { put("filmSimulation", "velvia") }))
            .withSetting("clarity", JsonPrimitive(3))
        val saved = state.savedAs(recipe(state.settings, name = "Velvia plus"))

        assertFalse(saved.isDirty)
        assertEquals("Velvia plus", saved.appliedRecipeName)
    }

    @Test
    fun `a new RAF drops the picture but keeps the settings`() {
        val rendered = buildJsonObject { put("filmSimulation", "velvia") }
        val state = loaded().copy(settings = rendered)
            .renderStarted()
            .renderSucceeded(result(), rendered)
            .withRaf(File("/tmp/another.raf"), "another.raf")

        assertTrue(state.preview == null)
        assertEquals(rendered, state.settings)
        assertEquals("another.raf", state.rafName)
    }

    private fun loaded(): RawLabState =
        RawLabState().withRaf(File("/tmp/source.raf"), "source.raf")

    private fun result(): RawDevelopmentResult = RawDevelopmentResult(
        jpeg = File("/tmp/render.jpg"),
        patch = RawProfilePatch(ByteArray(0), emptyList(), emptyList()),
        outputHandle = 1,
        width = 6_000,
        height = 4_000,
        quality = RawRenderQuality.FULL,
    )

    private fun recipe(settings: JsonObject, name: String = "Velvia days"): Recipe =
        Recipe(id = "r1", name = name, settings = settings)
}
