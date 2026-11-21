package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.shell.AppShell
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LibraryDetailLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun defaultSplitCountsTheRailAndKeepsCreateOnTheListSide() {
        compose.setContent {
            ShellAtWidth(840.dp) { split ->
                TestPanes(split, detailOpen = false)
            }
        }

        val list = bounds("list")
        val detail = bounds("detail")
        val shell = bounds("shell")
        assertEquals(shell.center.x, detail.left, 1f)
        assertEquals(shell.width / 2, detail.width, 1f)
        assertEquals(list.right, detail.left, 1f)
        assertTrue(list.left > shell.left)
        val create = compose.onNodeWithText(label(R.string.nav_create)).fetchSemanticsNode().boundsInRoot
        assertTrue(create.right < detail.left)
    }

    @Test
    fun unfoldedPortraitShowsBothPanesAndKeepsTheSplitWhenOpeningARecipe() {
        val open = mutableStateOf(false)
        compose.setContent {
            ShellAtWidth(660.dp, height = 820.dp, detailOpen = open.value) { split ->
                TestPanes(split, detailOpen = open.value)
            }
        }

        compose.onNodeWithTag("nav-rail-items").assertIsDisplayed()
        val start = bounds("detail").left
        assertEquals(bounds("shell").center.x, start, 1f)
        val createStart = createButtonRight()
        val distance = 0.1f * bounds("shell").width
        divider().performTouchInput { swipe(center, center + Offset(distance, 0f)) }
        val resized = bounds("detail").left
        assertTrue(resized > start)
        assertEquals(resized, bounds("list").right, 1f)
        assertEquals(resized, divider().fetchSemanticsNode().boundsInRoot.center.x, 1f)
        assertEquals(resized - start, createButtonRight() - createStart, 1f)
        compose.runOnIdle { open.value = true }
        assertEquals(resized, bounds("detail").left, 1f)
        compose.runOnIdle { open.value = false }
        assertEquals(resized, bounds("detail").left, 1f)
    }

    @Test
    fun bottomNavigationStaysInTheListPaneInAShortWideWindow() {
        compose.setContent {
            ShellAtWidth(840.dp, height = 400.dp, detailOpen = true) { split ->
                TestPanes(split, detailOpen = true)
            }
        }

        val boundary = bounds("detail").left
        assertEquals(bounds("shell").center.x, boundary, 1f)
        assertEquals(boundary, bounds("nav-bottom-bar").right, 1f)
        val distance = 0.1f * bounds("shell").width
        divider().performTouchInput { swipe(center, center + Offset(distance, 0f)) }
        assertTrue(bounds("detail").left > boundary)
        assertEquals(bounds("detail").left, bounds("nav-bottom-bar").right, 1f)
    }

    @Test
    fun compactDetailHidesTheListAndUnfoldingRestoresTheListAndSplit() {
        val width = mutableStateOf(840.dp)
        var listCompositions = 0
        compose.setContent {
            ShellAtWidth(width.value, detailOpen = true) { split ->
                LibraryDetailLayout(
                    split = split,
                    detailOpen = true,
                    list = {
                        val instance = remember { ++listCompositions }
                        Text("List $instance", Modifier.testTag("list"))
                    },
                    detail = { Box(Modifier.fillMaxSize().testTag("detail")) },
                )
            }
        }
        val distance = 0.1f * bounds("shell").width
        divider().performTouchInput { swipe(center, center + Offset(distance, 0f)) }
        val splitPosition = bounds("detail").left
        compose.runOnIdle { width.value = 393.dp }
        compose.onNodeWithTag("list").assertIsNotDisplayed()
        compose.onNodeWithTag("detail").assertIsDisplayed()
        divider().assertDoesNotExist()
        compose.runOnIdle { width.value = 840.dp }
        assertEquals(splitPosition, bounds("detail").left, 1f)
        assertEquals(1, listCompositions)
    }

    @Composable
    private fun ShellAtWidth(
        width: Dp,
        height: Dp = 700.dp,
        detailOpen: Boolean = false,
        content: @Composable (LibraryPaneSplit?) -> Unit,
    ) {
        FujiTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, height))) {
                AppShell(
                    showChrome = true,
                    isLibrarySelected = true,
                    isReadSelected = false,
                    isMoreSelected = false,
                    onLibraryClick = {},
                    onReadClick = {},
                    onMoreClick = {},
                    onCreateClick = {},
                    railBottomAction = {},
                    hideBottomNavigation = detailOpen,
                    modifier = Modifier.testTag("shell"),
                ) {
                    content(LocalLibraryPaneSplit.current)
                }
            }
        }
    }

    @Composable
    private fun TestPanes(split: LibraryPaneSplit?, detailOpen: Boolean) {
        LibraryDetailLayout(
            split = split,
            detailOpen = detailOpen,
            modifier = Modifier.fillMaxSize(),
            list = { Box(Modifier.fillMaxSize().testTag("list")) },
            detail = { Box(Modifier.fillMaxSize().testTag("detail")) },
        )
    }

    private fun divider() = compose.onNodeWithContentDescription(label(R.string.library_resize_panes))
    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    private fun createButtonRight() =
        compose.onNodeWithText(label(R.string.nav_create)).fetchSemanticsNode().boundsInRoot.right
    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
