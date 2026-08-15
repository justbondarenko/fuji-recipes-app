package dev.bondarenko.fujirecipes.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.IntentCompat
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.camera.usb.FUJI_VENDOR_ID

/**
 * Manifest-registered receiver for USB lifecycle broadcasts.
 *
 * Android dispatches [UsbManager.ACTION_USB_DEVICE_DETACHED] to manifest-declared
 * broadcast receivers. When the camera cable is disconnected, this ensures the active session
 * is closed and [CameraController.state] returns to [CameraState.Disconnected].
 */
class UsbReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? FujiRecipesApp ?: return
        val controller = app.container.cameraController

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val detachedDevice = IntentCompat.getParcelableExtra(
                    intent,
                    UsbManager.EXTRA_DEVICE,
                    UsbDevice::class.java,
                )
                if (detachedDevice != null && detachedDevice.vendorId != FUJI_VENDOR_ID) {
                    return
                }
                controller.onDeviceDetached(detachedDevice)
            }

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val attachedDevice = IntentCompat.getParcelableExtra(
                    intent,
                    UsbManager.EXTRA_DEVICE,
                    UsbDevice::class.java,
                )
                if (attachedDevice != null && attachedDevice.vendorId != FUJI_VENDOR_ID) {
                    return
                }
                controller.onDeviceAttached(attachedDevice)
            }
        }
    }
}
