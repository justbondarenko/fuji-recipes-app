# Lessons Learned

## Android USB Lifecycle Broadcasts (Android 14+)

### Pattern
Dynamic registration (`ContextCompat.registerReceiver`) for `ACTION_USB_DEVICE_DETACHED` (`android.hardware.usb.action.USB_DEVICE_DETACHED`) does not reliably receive OS-level USB hardware detach broadcasts on modern Android versions without static manifest declaration.

### Rule
1. Always declare a static `<receiver android:exported="true">` in `AndroidManifest.xml` with `<intent-filter>` for `android.hardware.usb.action.USB_DEVICE_DETACHED` (and `ATTACHED`).
2. Verify with `adb shell dumpsys package <package_name>` that the receiver is present in the OS `Receiver Resolver Table` under `Non-Data Actions`.
3. Filter `EXTRA_DEVICE` for the expected vendor ID (`FUJI_VENDOR_ID == 0x04cb` / `1227`) before processing.
