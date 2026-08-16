# Lessons Learned

## Deploying Changes to Emulator

### Pattern
Compiling and running unit tests is not enough to verify UI changes. The user expects every UI or code change to be automatically deployed and pushed to the running Android emulator.

### Rule
1. After every change, check `adb devices` to find any running emulator or device.
2. Build and install the debug APK via `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; ./gradlew installDebug`.
3. Launch or restart the main activity using `adb shell am start -n dev.bondarenko.fujirecipes/.MainActivity`.
4. Capture a screenshot or verify the UI state on the device when applicable.

---

## Android USB Lifecycle Broadcasts (Android 14+)

### Pattern
Dynamic registration (`ContextCompat.registerReceiver`) for `ACTION_USB_DEVICE_DETACHED` (`android.hardware.usb.action.USB_DEVICE_DETACHED`) does not reliably receive OS-level USB hardware detach broadcasts on modern Android versions without static manifest declaration.

### Rule
1. Always declare a static `<receiver android:exported="true">` in `AndroidManifest.xml` with `<intent-filter>` for `android.hardware.usb.action.USB_DEVICE_DETACHED` (and `ATTACHED`).
2. Verify with `adb shell dumpsys package <package_name>` that the receiver is present in the OS `Receiver Resolver Table` under `Non-Data Actions`.
3. Filter `EXTRA_DEVICE` for the expected vendor ID (`FUJI_VENDOR_ID == 0x04cb` / `1227`) before processing.
