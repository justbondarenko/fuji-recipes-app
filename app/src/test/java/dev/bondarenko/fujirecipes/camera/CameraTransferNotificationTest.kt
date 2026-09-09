package dev.bondarenko.fujirecipes.camera

import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraTransferNotificationTest {
    @Test
    fun `card reader uses the storage status icon`() {
        assertEquals(
            R.drawable.ic_notification_storage,
            connectedNotificationIcon(UsbMode.CARD_READER),
        )
    }

    @Test
    fun `raw conversion uses the camera status icon`() {
        assertEquals(
            R.drawable.ic_notification_camera,
            connectedNotificationIcon(UsbMode.RAW_CONVERSION),
        )
    }
}
