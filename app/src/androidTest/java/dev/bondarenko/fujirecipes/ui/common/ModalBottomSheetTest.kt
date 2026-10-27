package dev.bondarenko.fujirecipes.ui.common

import android.graphics.Bitmap
import android.graphics.Color
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.library.LibraryFiltersSheet
import dev.bondarenko.fujirecipes.ui.library.LibraryUiState
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModalBottomSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filtersDimAndBlockThePageAndDismissByScrimBackAndSwipe() {
        val visible = mutableStateOf(false)
        var pagePresses = 0
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.setContent {
            FujiTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Button(
                        onClick = { pagePresses++ },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
                    ) { Text("Page action") }
                    if (visible.value) LibraryFiltersSheet(
                        state = LibraryUiState(),
                        onFiltersChange = {},
                        onDismiss = { visible.value = false },
                    )
                }
            }
        }
        val page = compose.onNodeWithText("Page action").fetchSemanticsNode().boundsInWindow
        val x = (page.left - 16).toInt()
        val y = page.center.y.toInt()
        val before = requireNotNull(instrumentation.uiAutomation.takeScreenshot()).getPixel(x, y)
        compose.runOnIdle { visible.value = true }
        val scrim = compose.onNodeWithContentDescription("Close sheet")
        val handle = compose.onNodeWithContentDescription("Drag handle")
        scrim.assertExists()
        handle.assertExists()
        compose.waitForIdle()
        val window = scrim.fetchSemanticsNode().boundsInWindow
        val sheet = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
            .fetchSemanticsNode().boundsInWindow
        with(compose.density) {
            assertEquals("Full width on phones; capped on larger windows",
                minOf(window.width, 640.dp.toPx()), sheet.width, 1f)
        }
        assertEquals("The sheet must be horizontally centered", window.center.x, sheet.center.x, 1f)
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val after = screenshot.getPixel(x, y)
        assertTrue("The modal scrim must dim the page",
            Color.red(after) + Color.green(after) + Color.blue(after) <
                Color.red(before) + Color.green(before) + Color.blue(before))
        File(instrumentation.targetContext.cacheDir, "modal-bottom-sheet.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val bounds = scrim.fetchSemanticsNode().boundsInWindow
        scrim.performTouchInput { click(page.center - bounds.topLeft) }
        scrim.assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, pagePresses); visible.value = true }
        handle.assertExists()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        scrim.assertDoesNotExist()
        compose.runOnIdle { visible.value = true }
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.filters))
            .performTouchInput { swipe(start = center, end = center + Offset(0f, 400f), durationMillis = 250) }
        scrim.assertDoesNotExist()
        compose.onNodeWithText("Page action").performClick()
        compose.runOnIdle { assertEquals(1, pagePresses) }
    }
}
