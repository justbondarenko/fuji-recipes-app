package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.PhotoCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppShellCameraTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun footerDoesNotMoveCenteredDestinationsAndStillOpensCamera() {
        val footer = mutableStateOf(false)
        var presses = 0
        renderShell(mutableStateOf(840.dp), footer, onCamera = { presses++ })
        val centerBefore = navigationCenter()
        compose.runOnIdle { footer.value = true }
        assertEquals(centerBefore, navigationCenter(), 1f)
        val more = compose.onNodeWithText(label(R.string.nav_more)).fetchSemanticsNode().boundsInRoot
        val camera = compose.onNodeWithContentDescription("Camera trigger")
        assertTrue(camera.fetchSemanticsNode().boundsInRoot.top > more.bottom)
        camera.performClick()
        compose.runOnIdle { assertEquals(1, presses) }
    }

    @Test
    fun railFooterReplacesTopBarCameraAndPhoneRestoresIt() {
        val width = mutableStateOf(840.dp)
        renderShell(width, mutableStateOf(true), topBarCamera = true)
        compose.onAllNodesWithContentDescription("Camera trigger").assertCountEquals(1)
        compose.runOnIdle { width.value = 393.dp }
        compose.onAllNodesWithContentDescription("Camera trigger").assertCountEquals(1)
        val camera = compose.onNodeWithContentDescription("Camera trigger").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithText(label(R.string.nav_more)).fetchSemanticsNode().boundsInRoot
        assertTrue(camera.bottom < more.top)
    }

    @Test
    fun libraryCameraReturnsToContentWhenRailIsHidden() {
        val width = mutableStateOf(840.dp)
        renderShell(width, mutableStateOf(true), contentCamera = true)
        compose.onAllNodesWithContentDescription("Camera trigger").assertCountEquals(1)
        compose.runOnIdle { width.value = 393.dp }
        compose.onAllNodesWithContentDescription("Camera trigger").assertCountEquals(1)
        val camera = compose.onNodeWithContentDescription("Camera trigger").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithText(label(R.string.nav_more)).fetchSemanticsNode().boundsInRoot
        assertTrue(camera.bottom < more.top)
    }

    private fun renderShell(
        width: State<Dp>,
        footer: State<Boolean>,
        topBarCamera: Boolean = false,
        contentCamera: Boolean = false,
        onCamera: () -> Unit = {},
    ) {
        compose.setContent {
            FujiTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.value, 900.dp))) {
                    val action: @Composable () -> Unit = { CameraAction(onCamera) }
                    AppShell(
                        showChrome = true,
                        isLibrarySelected = true,
                        isReadSelected = false,
                        isMoreSelected = false,
                        onLibraryClick = {}, onReadClick = {}, onMoreClick = {}, onCreateClick = {},
                        topBarAction = if (topBarCamera) action else null,
                        railBottomAction = if (footer.value) action else null,
                    ) { padding ->
                        if (contentCamera && !LocalRailActionProvided.current) {
                            IconButton(onClick = onCamera, modifier = Modifier.padding(padding)) {
                                Icon(FujiIcons.PhotoCamera, contentDescription = "Camera trigger")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CameraAction(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(FujiIcons.PhotoCamera, contentDescription = "Camera trigger")
        }
    }

    private fun navigationCenter(): Float {
        val first = compose.onNodeWithText(label(R.string.nav_library)).fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithText(label(R.string.nav_more)).fetchSemanticsNode().boundsInRoot
        return (first.top + last.bottom) / 2f
    }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
