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
                FujiTheme { RawEditorScreen(state, {}, {}, {}, {}) }
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
    }
}
