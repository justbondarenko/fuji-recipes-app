package dev.bondarenko.fujirecipes.camera

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.ptp.BulkChannel
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.ptp.unpackU16
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaType
import dev.bondarenko.fujirecipes.camera.usb.RawLabSession
import dev.bondarenko.fujirecipes.camera.usb.UsbBulkChannel
import dev.bondarenko.fujirecipes.camera.usb.downloadCameraJpeg
import dev.bondarenko.fujirecipes.camera.usb.downloadCameraRaf
import dev.bondarenko.fujirecipes.camera.usb.listCameraFiles
import dev.bondarenko.fujirecipes.camera.usb.mediaType
import dev.bondarenko.fujirecipes.camera.usb.readCameraDetails
import dev.bondarenko.fujirecipes.camera.usb.readCameraThumbnail
import dev.bondarenko.fujirecipes.camera.usb.readUsbMode
import dev.bondarenko.fujirecipes.data.fields.defaultRecipeSettings
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt in with -e cameraProbe true; RAW mode also needs cache/camera-probe.raf. */
class CameraUsbHardwareTest {
    @Test
    fun connectedCameraTransfers() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cameraProbe") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = requireNotNull(UsbBulkChannel.findCamera(manager)) { "Attach the camera first." }
        check(manager.hasPermission(device)) { "Grant USB permission in the app first." }
        val usb = UsbBulkChannel.open(manager, device)
        val channel = object : BulkChannel by usb {
            override fun write(bytes: ByteArray) {
                if (bytes.size >= 12 && unpackU16(bytes, 4) == 1) {
                    Log.i(TAG, "Command 0x${unpackU16(bytes, 6).toString(16)}")
                }
                usb.write(bytes)
            }
        }
        val session = PtpSession(PtpTransport(channel))
        val output = File(context.cacheDir, "camera-probe.jpg")
        try {
            val info = session.open()
            val mode = readUsbMode(session, info)
            readCameraDetails(session, info)
            Log.i(TAG, "Connected: ${info.model}, mode=$mode")
            when (mode) {
                UsbMode.RAW_CONVERSION -> {
                    val raf = File(context.cacheDir, "camera-probe.raf")
                    check(raf.isFile) { "Copy a camera RAF to cache/camera-probe.raf first." }
                    val lab = RawLabSession()
                    repeat(2) {
                        val result = lab.render(session, raf, defaultRecipeSettings(), output,
                            onStage = { stage -> Log.i(TAG, "Render ${it + 1}: $stage") })
                        assertTrue(output.length() > 0)
                        Log.i(TAG, "Rendered ${output.length()} bytes, ${result.width}x${result.height}")
                    }
                }
                UsbMode.CARD_READER -> {
                    val media = listCameraFiles(session) { current, total ->
                        if (current % 100 == 0 || current == total) Log.i(TAG, "Scan $current/$total")
                    }
                    assertTrue("No photos on the card", media.isNotEmpty())
                    for (type in CameraMediaType.entries) {
                        val photo = media.firstOrNull { it.mediaType == type } ?: continue
                        Log.i(TAG, "Downloading $type: handle=${photo.handle}, bytes=${photo.info.compressedSize}")
                        val thumbnail = readCameraThumbnail(session, photo.handle)
                        assertTrue("No valid thumbnail", thumbnail != null)
                        output.outputStream().use {
                            if (type == CameraMediaType.JPEG) downloadCameraJpeg(session, photo, it)
                            else downloadCameraRaf(session, photo, it)
                        }
                        assertTrue(output.length() > 0)
                        Log.i(TAG, "Downloaded $type: ${output.length()} bytes")
                    }
                }
                else -> error("Select RAW conversion or Card Reader mode: $mode")
            }
            session.close()
        } finally {
            session.abort()
            output.delete()
        }
    }

    private companion object {
        const val TAG = "FujiUsbProbe"
    }
}
