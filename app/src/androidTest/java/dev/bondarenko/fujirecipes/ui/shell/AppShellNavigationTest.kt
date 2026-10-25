package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.core.settings.NavDestinationId
import dev.bondarenko.fujirecipes.ui.settings.MoreScreen
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppShellNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sidebarReordersEightPinsAndCompactKeepsItsOwnFour() {
        var dimensions by mutableStateOf(DpSize(840.dp, 900.dp))
        var compact by mutableStateOf(NavDestinationId.DEFAULT_PINNED)
        var sidebar by mutableStateOf(NavDestinationId.DEFAULT_PINNED)
        var selected by mutableStateOf(NavDestinationId.LIBRARY)
        var moreOpen by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(dimensions)) {
                FujiTheme {
                    val rail = usesNavigationRail(dimensions.width, dimensions.height, true)
                    val active = if (rail) sidebar else compact
                    AppShell(
                        showChrome = true, isLibrarySelected = false, isReadSelected = false,
                        isMoreSelected = moreOpen || selected !in active,
                        onLibraryClick = {}, onReadClick = {}, onCreateClick = {},
                        onMoreClick = { moreOpen = true }, selectedDestination = selected,
                        pinnedDestinations = active,
                        onUpdatePinnedDestinations = { if (rail) sidebar = it else compact = it },
                        onDestinationClick = { selected = it; moreOpen = false },
                        railBottomAction = { Box(Modifier.size(48.dp).testTag("test-rail-action")) },
                    ) { padding ->
                        if (moreOpen) MoreScreen(
                            {}, {}, {}, {}, {}, padding,
                            pinnedDestinations = active.toSet(),
                            onOpenDestination = { selected = it; moreOpen = false },
                        )
                    }
                }
            }
        }
        fun edit() {
            val more = compose.onNodeWithTag("nav-item-more")
            if (usesNavigationRail(dimensions.width, dimensions.height, true)) more.performScrollTo()
            more.performTouchInput { longClick(durationMillis = 650) }
            compose.onNodeWithText("Customize navigation").assertIsDisplayed()
        }
        fun drag(from: NavDestinationId, to: NavDestinationId, rail: Boolean) {
            val container = compose.onNodeWithTag(if (rail) "nav-rail-items" else "nav-bottom-bar")
            val origin = container.fetchSemanticsNode().boundsInRoot.topLeft
            val start = compose.onNodeWithTag("nav-item-${from.id}").fetchSemanticsNode().boundsInRoot.center - origin
            val end = compose.onNodeWithTag("nav-item-${to.id}").fetchSemanticsNode().boundsInRoot.center - origin
            container.performTouchInput { swipe(start, end, 700) }
        }
        edit()
        val additions = listOf(NavDestinationId.RAW_EDITOR, NavDestinationId.CLEANUP, NavDestinationId.IMPORT_CAMERA, NavDestinationId.IMPORT_FILE)
        additions.forEach { compose.onNodeWithTag("nav-pin-${it.id}").performClick().assertIsSelected() }
        compose.onNodeWithTag("nav-pin-export").assertIsNotEnabled()
        compose.onNodeWithText("Sidebar: 8/8 pinned.", substring = true).assertIsDisplayed()
        val before = NavDestinationId.DEFAULT_PINNED + additions
        drag(NavDestinationId.LIBRARY, NavDestinationId.IMPORT_FILE, true)
        val reordered = before.drop(1) + NavDestinationId.LIBRARY
        compose.runOnIdle { assertEquals(reordered, sidebar); assertEquals(NavDestinationId.DEFAULT_PINNED, compact) }
        val bounds = reordered.map { compose.onNodeWithTag("nav-item-${it.id}").fetchSemanticsNode().boundsInRoot }
        assertTrue(bounds.zipWithNext().all { (a,b) -> a.center.y < b.center.y })
        val more = compose.onNodeWithTag("nav-item-more").fetchSemanticsNode().boundsInRoot
        assertTrue(more.top >= bounds.last().bottom)
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag("nav-item-raw_editor").performClick()
        compose.runOnIdle { assertEquals(NavDestinationId.RAW_EDITOR, selected); dimensions = DpSize(393.dp, 900.dp) }
        compose.onNodeWithTag("nav-item-raw_editor").assertDoesNotExist()
        compose.onNodeWithTag("nav-item-more").performClick()
        compose.onNodeWithText("RAW Editor").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(NavDestinationId.RAW_EDITOR, selected) }
        edit()
        compose.onNodeWithText("Bottom bar: 4/4 pinned.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("nav-pin-raw_editor").assertIsNotEnabled()
        compose.onNodeWithTag("nav-pin-photos").performClick()
        compose.onNodeWithTag("nav-pin-raw_editor").performClick()
        drag(NavDestinationId.RAW_EDITOR, NavDestinationId.LIBRARY, false)
        val phoneOrder = listOf(NavDestinationId.RAW_EDITOR, NavDestinationId.LIBRARY, NavDestinationId.ANALYZE, NavDestinationId.LAB)
        compose.runOnIdle { assertEquals(phoneOrder, compact); assertEquals(reordered, sidebar) }
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { dimensions = DpSize(840.dp, 500.dp) }
        compose.onNodeWithTag("nav-bottom-bar").assertIsDisplayed()
        compose.onNodeWithTag("nav-item-import_file").assertDoesNotExist()
        compose.runOnIdle { dimensions = DpSize(840.dp, 600.dp) }
        compose.onNodeWithTag("nav-item-more").performScrollTo().assertIsDisplayed()
        val footer = compose.onNodeWithTag("test-rail-action").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("nav-item-more").fetchSemanticsNode().boundsInRoot
        assertTrue(last.bottom <= footer.top)
        compose.runOnIdle { dimensions = DpSize(393.dp, 900.dp) }
        val phoneBounds = phoneOrder.map { compose.onNodeWithTag("nav-item-${it.id}").fetchSemanticsNode().boundsInRoot }
        assertTrue(phoneBounds.zipWithNext().all { (a,b) -> a.center.x < b.center.x })
        compose.runOnIdle { assertEquals(phoneOrder, compact); assertEquals(reordered, sidebar) }
    }
}
