# Task: UI Polish & Confirmation Enhancements

## Plan Items
- [x] 1. Add Swipe-to-Delete confirmation dialog in `LibraryScreen.kt` <!-- id: 1 -->
- [x] 2. Set Recipe View bottom sheet to open directly at full height (`skipPartiallyExpanded = true`) in `RecipeViewScreen.kt` <!-- id: 2 -->
- [x] 3. Add status bar gradient scrim in `AppShell.kt` to protect system status bar legibility during scrolling <!-- id: 3 -->
- [x] 4. Update save button in `RecipeEditorScreen.kt` to show "Save" text label + check icon <!-- id: 4 -->
- [x] 5. Run test suite and assemble debug build (`./gradlew testDebugUnitTest assembleDebug`) <!-- id: 5 -->
- [x] 6. Verify on Pixel 10 Pro XL emulator and capture screenshots of all 4 features <!-- id: 6 -->
- [x] 7. Update `walkthrough.md` with verification results <!-- id: 7 -->

## Review & Verification
- All 142 unit tests passed (`./gradlew testDebugUnitTest`).
- Swipe-to-delete confirmation dialog tested and verified with recipe name and cancel/delete actions.
- Recipe view bottom sheet verified opening directly at full height.
- Status bar gradient scrim in `AppShell` verified keeping the clock and system icons readable when list content scrolls underneath.
- "Save" button in Recipe Editor verified displaying both the check icon and the "Save" text label in both creation and edit modes.
