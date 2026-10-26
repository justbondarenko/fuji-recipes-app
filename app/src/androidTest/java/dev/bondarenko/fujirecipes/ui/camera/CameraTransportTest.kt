package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.camera.CameraTransport
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraTransportTest {
    @get:Rule val compose = createComposeRule()

    @Test fun BluetoothRemainsAvailableWithoutUsbHostAndNamesItsReadOnlyConnection() {
        val state = mutableStateOf<CameraState>(CameraState.NoUsbHost)
        val transport = mutableStateOf(CameraTransport.USB)
        var connects = 0
        compose.setContent {
            FujiTheme {
                CameraScreen(
                    state = state.value, transport = transport.value,
                    isCameraAttached = false, contentPadding = PaddingValues(),
                    onSelectTransport = { transport.value = it; state.value = CameraState.Disconnected },
                    onConnect = { connects++ }, onDisconnect = {},
                )
            }
        }
        compose.onNodeWithText(label(R.string.camera_transport_bluetooth)).performClick().assertIsSelected()
        compose.onNodeWithText(label(R.string.camera_bluetooth_help)).assertExists()
        compose.onNodeWithText(label(R.string.camera_action_connect)).performClick()
        compose.runOnIdle { assertEquals(1, connects); state.value = CameraState.Connecting }
        compose.onNodeWithText(label(R.string.camera_transport_usb)).assertIsNotEnabled()
        compose.runOnIdle {
            state.value = CameraState.Connected(CameraModels.identify("X-T50"), transport = CameraTransport.BLUETOOTH)
        }
        compose.onNodeWithText(label(R.string.camera_bluetooth_connected)).assertExists()
    }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
