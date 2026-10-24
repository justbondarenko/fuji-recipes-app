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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
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
            compose.onNodeWithText("Compare camera JPEG").performClick()
            assertEquals(save, bounds("Save JPEG"))
            assertEquals(shadows, bounds("Shadows"))
            assertEquals(image, compose.onNodeWithContentDescription("Original camera JPEG").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithText("Back to edited RAW").performClick()
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
}
