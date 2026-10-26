package dev.bondarenko.fujirecipes.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.CameraTransport
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.common.FujiModalSideSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class CameraTransportTest {
    @get:Rule val compose = createComposeRule()

    @Test fun BluetoothRemainsAvailableWithoutUsbHostAndNamesItsReadOnlyConnection() {
        val state = mutableStateOf<CameraState>(CameraState.NoUsbHost)
        val transport = mutableStateOf(CameraTransport.USB)
        var connects = 0
        compose.setContent {
            FujiTheme {
                FujiModalSideSheet(visible = true, onDismissRequest = {}) {
                    Column(Modifier.fillMaxSize()) {
                        CameraSheetHeader(state.value, transport.value,
                            { transport.value = it; state.value = CameraState.Disconnected }, {})
                        CameraScreen(
                            state = state.value, transport = transport.value,
                            isCameraAttached = false, contentPadding = PaddingValues(),
                            onConnect = { connects++ }, onDisconnect = {}, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        val title = compose.onNodeWithText(label(R.string.camera_sheet_title)).fetchSemanticsNode().boundsInRoot
        val usb = compose.onNodeWithContentDescription(label(R.string.camera_transport_usb))
        val bluetooth = compose.onNodeWithContentDescription(label(R.string.camera_transport_bluetooth))
        val btBounds = bluetooth.fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription(label(R.string.action_close)).fetchSemanticsNode().boundsInRoot
        assertTrue(usb.fetchSemanticsNode().boundsInRoot.left < btBounds.left && btBounds.right < close.right)
        assertTrue(btBounds.top > close.bottom)
        with(compose.density) { assertTrue(close.right - title.left + 28.dp.toPx() <= 400.dp.toPx() + 1) }
        compose.onNodeWithText(label(R.string.camera_transport_usb)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.camera_transport_bluetooth)).assertDoesNotExist()
        bluetooth.performClick().assertIsOn()
        compose.onNodeWithText(label(R.string.camera_bluetooth_help)).assertExists()
        compose.onNodeWithText(label(R.string.camera_action_connect)).performClick()
        compose.runOnIdle { assertEquals(1, connects); state.value = CameraState.Connecting }
        usb.assertIsNotEnabled()
        compose.runOnIdle {
            state.value = CameraState.Connected(CameraModels.identify("X-T50"), transport = CameraTransport.BLUETOOTH)
        }
        compose.onNodeWithText(label(R.string.camera_bluetooth_connected)).assertExists()
        bluetooth.assertIsOn()
        val snapshot = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "camera-sheet-connected.png")
        snapshot.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        usb.performClick()
        compose.onNodeWithText(label(R.string.camera_state_disconnected_body)).assertExists()
    }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
