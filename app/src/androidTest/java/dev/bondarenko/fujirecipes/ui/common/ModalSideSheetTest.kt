package dev.bondarenko.fujirecipes.ui.common

import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModalSideSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detachedModalKeepsInsetsBlocksThePageAndDismissesFromEitherEdge() {
        val visible = mutableStateOf(false)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        var pagePresses = 0
        var modalView: View? = null
        compose.setContent {
            FujiTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Box(Modifier.fillMaxSize()) {
                        Button(onClick = { pagePresses++ }, modifier = Modifier.align(Alignment.CenterStart)) {
                            Text("Page action")
                        }
                        FujiModalSideSheet(visible.value, title = "Test sheet", onDismissRequest = { visible.value = false }) {
                            val view = LocalView.current
                            SideEffect { modalView = view }
                            Column(Modifier.fillMaxSize()) {
                                Button(onClick = { visible.value = false }) { Text("Close sheet") }
                                Text("Sheet content")
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Page action").performClick()
        compose.runOnIdle { assertEquals(1, pagePresses); visible.value = true }
        val dialog = compose.onNode(isDialog())
        val sheet = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Test sheet"))
        val window = dialog.fetchSemanticsNode().boundsInRoot
        val bounds = sheet.fetchSemanticsNode().boundsInRoot
        with(compose.density) {
            assertTrue(bounds.width <= 400.dp.toPx() + 1)
            assertTrue(bounds.left >= 16.dp.toPx() - 1 && window.right - bounds.right >= 16.dp.toPx() - 1)
            assertTrue(bounds.top >= 16.dp.toPx() - 1 && window.bottom - bounds.bottom >= 16.dp.toPx() - 1)
        }
        sheet.performTouchInput { click(center) }
        dialog.assertExists()
        dialog.performTouchInput { click(Offset(4f, center.y)) }
        dialog.assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, pagePresses); visible.value = true }
        dialog.assertExists()
        compose.waitUntil(5_000) { modalView?.hasWindowFocus() == true }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        dialog.assertDoesNotExist()
        compose.onNodeWithText("Page action").performClick()
        compose.runOnIdle { assertEquals(2, pagePresses); visible.value = true }
        compose.onNodeWithText("Close sheet").performClick()
        dialog.assertDoesNotExist()
        compose.runOnIdle { visible.value = true }
        sheet.performTouchInput { swipeRight() }
        dialog.assertDoesNotExist()
        compose.runOnIdle { direction.value = LayoutDirection.Rtl; visible.value = true }
        sheet.performTouchInput { swipeLeft() }
        dialog.assertDoesNotExist()
    }
}
