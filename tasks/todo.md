# OS Share Sheet JPG Accept & Analyze Flow

- [x] Add `ACTION_SEND` intent filter in `AndroidManifest.xml`
- [x] Add shared image URI extraction and intent lifecycle handling in `MainActivity.kt`
- [x] Update `PhotoRoute` to accept optional `initialUri: String?` in `FujiNavHost.kt`
- [x] Wire shared image URI navigation in `FujiApp`
- [x] Trigger photo analysis on `initialUri` in `PhotoReaderRouteContent` and `PhotoReaderViewModel`
- [x] Add unit tests for `PhotoReaderViewModel`
- [x] Verify test suite passes with `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`

## Review
- **Intent Filter**: Added `ACTION_SEND` intent filter to `MainActivity` matching `image/jpeg`, `image/jpg`, and `image/*`.
- **Activity Lifecycle**: Both cold starts (`onCreate`) and warm starts (`onNewIntent`) parse the incoming URI and pass it to Compose navigation.
- **Routing**: `PhotoRoute(initialUri = ...)` triggers immediate analysis while remaining fully compatible with manual photo picking.
- **Verification**: Added 5 unit tests in `PhotoReaderViewModelTest` covering successful parsing, failure handling, URI deduplication, and reset logic. All tests pass.
