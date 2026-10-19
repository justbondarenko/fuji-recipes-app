package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LibraryDetailLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun openingAndClosingRemeasuresListAlongTheRevealingEdge() {
        val open = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FujiTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 700.dp))) {
                    LibraryDetailLayout(
                        twoPane = true,
                        detailOpen = open.value,
                        list = { Box(Modifier.fillMaxSize().testTag("list")) },
                        detail = { Box(Modifier.fillMaxSize().testTag("detail")) },
                    )
                }
            }
        }
        val fullWidth = bounds("list").width
        compose.runOnIdle { open.value = true }
        compose.mainClock.advanceTimeBy(200)
        val halfway = bounds("list")
        val revealingDetail = bounds("detail")
        assertTrue(halfway.width < fullWidth)
        assertTrue(halfway.width > fullWidth * 0.4f)
        assertEquals(halfway.right, revealingDetail.left, 1f)

        compose.mainClock.advanceTimeBy(500)
        val splitWidth = bounds("list").width
        assertEquals(fullWidth * 0.4f, splitWidth, 1f)
        compose.runOnIdle { open.value = false }
        compose.mainClock.advanceTimeBy(200)
        val closingWidth = bounds("list").width
        assertTrue(closingWidth > splitWidth && closingWidth < fullWidth)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(fullWidth, bounds("list").width, 1f)
    }

    @Test
    fun compactDetailHidesListAndUnfoldingRestoresItsComposition() {
        val twoPane = mutableStateOf(true)
        var listCompositions = 0
        compose.setContent {
            FujiTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                    DpSize(if (twoPane.value) 840.dp else 393.dp, 700.dp),
                )) {
                    LibraryDetailLayout(
                        twoPane = twoPane.value,
                        detailOpen = true,
                        list = {
                            val instance = remember { ++listCompositions }
                            Text("List $instance", Modifier.testTag("list"))
                        },
                        detail = { Box(Modifier.fillMaxSize().testTag("detail")) },
                    )
                }
            }
        }
        compose.runOnIdle { twoPane.value = false }
        compose.onNodeWithTag("list").assertIsNotDisplayed()
        compose.onNodeWithTag("detail").assertIsDisplayed()
        compose.runOnIdle { twoPane.value = true }
        compose.waitForIdle()
        assertEquals(1, listCompositions)
    }

    @Test
    fun reducedMotionOpensWithoutAnIntermediateWidth() {
        val open = mutableStateOf(false)
        compose.setContent {
            FujiTheme {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 700.dp))) {
                        LibraryDetailLayout(
                            twoPane = true,
                            detailOpen = open.value,
                            list = { Box(Modifier.fillMaxSize().testTag("list")) },
                            detail = { Box(Modifier.fillMaxSize().testTag("detail")) },
                        )
                    }
                }
            }
        }
        val width = bounds("list").width
        compose.runOnIdle { open.value = true }
        compose.waitForIdle()
        assertEquals(width * 0.4f, bounds("list").width, 1f)
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
}
