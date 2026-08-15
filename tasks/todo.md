# Task: Fix Camera Disconnection Detection (ACTION_USB_DEVICE_DETACHED)

## Root Cause Analysis
1. Android OS dispatches `UsbManager.ACTION_USB_DEVICE_DETACHED` (`android.hardware.usb.action.USB_DEVICE_DETACHED`) as an explicit/package broadcast to components registered in `AndroidManifest.xml` (like official camera apps such as `com.fujifilm.xapp` and `com.adobe.lrmobile` do via their `<receiver>`).
2. Without a manifest-declared `<receiver android:exported="true">` for `android.hardware.usb.action.USB_DEVICE_DETACHED`, the system broadcast is filtered by `PackageManager` before it ever reaches dynamic, context-only listeners on modern Android versions.

## Plan Items

- [x] 1. Create `UsbReceiver.kt` inheriting from `BroadcastReceiver` to handle `ACTION_USB_DEVICE_DETACHED` and `ACTION_USB_DEVICE_ATTACHED` <!-- id: 1 -->
- [x] 2. Declare `UsbReceiver` in `AndroidManifest.xml` with `exported="true"` and an `<intent-filter>` for `USB_DEVICE_ATTACHED` and `USB_DEVICE_DETACHED` <!-- id: 2 -->
- [x] 3. Update `CameraController.kt`: implement `onDeviceDetached(device: UsbDevice?)` and wire it to both `UsbReceiver` and internal dynamic receivers <!-- id: 3 -->
- [x] 4. Run tests and verify the build <!-- id: 4 -->
- [x] 5. Assemble APK and install on connected physical device and emulator <!-- id: 5 -->

## Review & Verification

### Root Cause
Android's `UsbHostManager` and `UsbService` route USB detachment broadcasts through `PackageManager`'s registered receiver components. Dynamic, in-code only receivers on Android 14+ may not receive implicit detach intents unless a manifest receiver is registered for the action.

### What Changed
1. **Manifest Receiver ([`UsbReceiver.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/camera/UsbReceiver.kt))**:
   - Created a dedicated `UsbReceiver` that listens for `ACTION_USB_DEVICE_DETACHED` and `ACTION_USB_DEVICE_ATTACHED`.
   - Filters incoming intents for `vendorId == FUJI_VENDOR_ID`.
   - Calls `controller.onDeviceDetached(device)` upon disconnect.
2. **Manifest Declaration ([`AndroidManifest.xml`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/AndroidManifest.xml))**:
   - Declared `<receiver android:name=".camera.UsbReceiver" android:exported="true">` with intent filters for both USB attach and detach actions.
3. **Controller Handling ([`CameraController.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/camera/CameraController.kt))**:
   - Added `onDeviceDetached(device: UsbDevice?)` which cleanly locks, closes the PTP session, and sets `_state` to `CameraState.Disconnected` (or `CameraState.Error` if unplugged during a write).
4. **Clean Builds & Testing**:
   - Resolved Kotlin 2.5 expression body warning on `connectNow`.

### Verification Results
- `./gradlew test`: **BUILD SUCCESSFUL** (all unit tests passed).
- Verified with `adb shell dumpsys package dev.bondarenko.fujirecipes` that `UsbReceiver` is registered in Android's Receiver Resolver Table for `android.hardware.usb.action.USB_DEVICE_DETACHED`.
- Installed on physical device (`adb-58061FDCQ0057S-jAkCZy._adb-tls-connect._tcp`) and launched.
