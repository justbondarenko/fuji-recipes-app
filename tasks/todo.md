# Task: Visual Fixes & Material 3 Tooltips for Inactive Camera Actions

## Goal

Apply visual and UX fixes across Recipe View, Settings, and Library screens:
1. Consolidate Recipe View actions into a floating toolbar with a separate FAB for "Write to camera". Remove actions from top app bar, card header, and bottom of view.
2. Reduce rating star spacing and tag row vertical distance in Recipe View.
3. Rename Settings "Import from camera" title and subtitle ("Import saved presets").
4. Rename Settings section to "Backup & Restore".
5. Seed example data to the emulator.
6. Use square shape (`RoundedCornerShape(16.dp)`) for Recipe View FAB.
7. Inform user why "Write to camera" is inactive via Material 3 Plain Tooltip ("Connect camera via USB to write recipes").

## Plan Items

- [x] 1. Add string resources in `strings.xml` for Settings backup & restore, camera import, and tooltip <!-- id: 1 -->
- [x] 2. Update `SettingsScreen.kt` section header and card title/subtitle <!-- id: 2 -->
- [x] 3. Update `EditorControls.kt` for tighter `RatingInput` stars and `TagInput` row spacing <!-- id: 3 -->
- [x] 4. Refactor `RecipeViewScreen.kt` to remove legacy action locations and add floating toolbar + FAB <!-- id: 4 -->
- [x] 5. Populate sample recipes in emulator cache and verify on device <!-- id: 5 -->
- [x] 6. Update Recipe View FAB to use square shape (`RoundedCornerShape(16.dp)`) <!-- id: 6 -->
- [x] 7. Implement Material 3 Tooltips for "Write to camera" on Recipe View FAB and Library Swipe Action when camera is disconnected <!-- id: 7 -->
- [x] 8. Run automated tests and verify on emulator with screenshots <!-- id: 8 -->

## Review & Verification

### What changed

1. **[`strings.xml`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/res/values/strings.xml)**:
   - Added `camera_not_connected_tooltip`: `"Connect camera via USB to write recipes"`.
2. **[`RecipeViewScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/recipe/RecipeViewScreen.kt)**:
   - Wrapped `FloatingActionButton` in `TooltipBox` with `PlainTooltip`.
   - Used `TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)` and `tooltipState = rememberTooltipState(isPersistent = true)`.
   - On tap when `canWriteToCamera == false`, launches `tooltipState.show()`.
3. **[`LibraryScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/library/LibraryScreen.kt)**:
   - Wrapped `SwipeAction` for Write in `TooltipBox` with `PlainTooltip` for consistent feedback when swiping and tapping Write while disconnected.
4. **Verification**:
   - Built and installed on emulator (`Pixel_10_Pro_XL`).
   - Verified that tapping or long-pressing the inactive Write FAB renders the Material 3 Plain Tooltip above the button.
   - Ran `./gradlew compileDebugKotlin testDebugUnitTest` — `BUILD SUCCESSFUL`.
