package dev.bondarenko.fujirecipes.ui.raweditor

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RawEditorLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun previewControlsAndSaveStayInPlaceDuringRenderingAndComparison() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(
            name = "sample.RAF", simulation = "Classic Chrome", fullWidth = 7728, fullHeight = 5152,
            preview = bitmap, cameraImage = bitmap,
        ))
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        fun bounds(text: String): Rect = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        fun checkStable() {
            compose.onNodeWithText("Save JPEG").assertIsDisplayed()
            val save = bounds("Save JPEG")
            val shadows = bounds("Shadows")
            val image = compose.onNodeWithContentDescription("RAW with film simulation color profile").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle { state = state.copy(rendering = true) }
            assertEquals(save, bounds("Save JPEG"))
            assertEquals(shadows, bounds("Shadows"))
            compose.runOnIdle { state = state.copy(rendering = false) }
            compose.onNodeWithText("Compare camera JPEG").assertDoesNotExist()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
            assertEquals(save, bounds("Save JPEG"))
            assertEquals(shadows, bounds("Shadows"))
            assertEquals(image, compose.onNodeWithContentDescription("Original camera JPEG").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center) }
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { cancel() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Compare camera JPEG")
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Back to edited RAW")
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center) }
            compose.runOnIdle { state = state.copy(busy = true) }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up(); down(center) }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
            compose.runOnIdle { state = state.copy(busy = false) }
            compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
        }
        checkStable()
        compose.runOnIdle { wide = true }
        checkStable()
        compose.runOnIdle { wide = false }
        fun set(label: String, value: Float) {
            compose.onNodeWithContentDescription(label).performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
            compose.onNodeWithText("Save JPEG").assertIsDisplayed()
        }
        compose.onNodeWithText("Color").performClick()
        set("Temperature", .4f)
        set("Tint", -.3f)
        compose.runOnIdle {
            assertEquals(.4f, state.adjustments.temperature)
            assertEquals(-.3f, state.adjustments.tint)
        }
        compose.onNodeWithText("As Shot white balance").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0f, state.adjustments.temperature)
            assertEquals(0f, state.adjustments.tint)
        }
        set("Saturation", -.5f)
        set("Vibrance", .5f)
        compose.onNodeWithText("Detail").performClick()
        set("Sharpening", .6f)
        set("Radius", 1.8f)
        set("Threshold", .2f)
        set("Color noise reduction", .6f)
        set("Luminance noise reduction", .5f)
        set("Preserve detail", .7f)
        compose.onNodeWithText("Light").performClick()
        set("Contrast", .3f)
        set("Whites", .2f)
        set("Blacks", -.2f)
        compose.runOnIdle {
            assertEquals(.6f, state.adjustments.sharpening)
            assertEquals(1.8f, state.adjustments.sharpenRadius)
            assertEquals(.7f, state.adjustments.noiseDetail)
            assertEquals(-.2f, state.adjustments.blacks)
        }
    }

    @Test
    fun doubleTapResetsOnlyNamedParameterIncludingInactiveDetailControls() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var expected = RawAdjustments(
            exposure = 1f, shadows = .4f, highlights = -.3f, contrast = .2f, whites = .3f, blacks = -.2f,
            temperature = .4f, tint = -.3f, saturation = -.5f, vibrance = .5f,
            sharpening = .6f, sharpenRadius = 1.8f, sharpenThreshold = .2f,
            colorNoise = .6f, luminanceNoise = .5f, noiseDetail = .7f,
        )
        var state by mutableStateOf(RawEditorState(name = "sample.RAF", preview = bitmap, cameraImage = bitmap, adjustments = expected))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        compose.onNodeWithText("Exposure").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(expected, state.adjustments) }
        compose.runOnIdle { state = state.copy(busy = true) }
        compose.onNodeWithText("Exposure").performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(expected, state.adjustments); state = state.copy(busy = false) }

        fun reset(label: String, after: RawAdjustments) {
            compose.onNodeWithText(label).performScrollTo().performTouchInput { doubleClick() }
            expected = after
            compose.runOnIdle { assertEquals("Reset $label must preserve every other value", expected, state.adjustments) }
        }
        reset("Exposure", expected.copy(exposure = 0f))
        reset("Shadows", expected.copy(shadows = 0f))
        reset("Highlights", expected.copy(highlights = 0f))
        reset("Contrast", expected.copy(contrast = 0f))
        reset("Whites", expected.copy(whites = 0f))
        reset("Blacks", expected.copy(blacks = 0f))
        compose.onNodeWithText("Color").performClick()
        reset("Temperature", expected.copy(temperature = 0f))
        reset("Tint", expected.copy(tint = 0f))
        reset("Saturation", expected.copy(saturation = 0f))
        reset("Vibrance", expected.copy(vibrance = 0f))
        compose.onNodeWithText("Detail").performClick()
        reset("Sharpening", expected.copy(sharpening = 0f))
        reset("Radius", expected.copy(sharpenRadius = 1f))
        reset("Threshold", expected.copy(sharpenThreshold = 0f))
        reset("Color noise reduction", expected.copy(colorNoise = 0f))
        reset("Luminance noise reduction", expected.copy(luminanceNoise = 0f))
        reset("Preserve detail", expected.copy(noiseDetail = .5f))
        compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
        compose.onNodeWithText("Light").performClick()
        compose.runOnIdle { state = state.copy(adjustments = state.adjustments.copy(exposure = 1f)) }
        compose.onNodeWithText("Exposure").performCustomAccessibilityActionWithLabel("Reset Exposure")
        compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
    }
}
