# Task: UI Polish — FAB Dark Green, Read Screen UX, and Settings About Section

## Plan Items

- [x] 1. Colors & Strings: Update `CameraChipTone.READY` to darker green (`0xFF1B5E20`/`0xFF2E7D32`), add `photo_action_view_recipe` string ("View recipe") <!-- id: 1 -->
- [x] 2. Settings Screen: Remove About card/section header and list version text simply at the bottom of the page <!-- id: 2 -->
- [x] 3. Read Page Starting State: Center starting UI vertically without border/container wrapping <!-- id: 3 -->
- [x] 4. Read Page Match Result: Wrap matched recipe UI into card matching Recipe View style (showing name, rating, match info, differences, and horizontal "Save as new" on left and "View recipe" on right) <!-- id: 4 -->
- [x] 5. Run tests, assemble APK, and deploy to emulator <!-- id: 5 -->

## Review & Verification

### What Changed
1. **Darker Green FAB**:
   - In [`CameraChip.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/CameraChip.kt), updated `CameraChipTone.READY` to use deep rich forest green (`0xFF2E7D32` light / `0xFF1B5E20` dark) with pure white content (`0xFFFFFFFF`).

2. **Read Page Starting UI & Matched Recipe Card**:
   - In [`PhotoReaderScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/photo/PhotoReaderScreen.kt), removed the container with border on the starting screen (`PhotoReaderStage.Empty`), vertically centering the camera icon, headline, intro text, and "Choose a photo" button directly on the surface.
   - When a recipe match is found from an image, rendered a `MatchedRecipeCard` styled like Recipe View with:
     - Avatar film simulation badge
     - Recipe name & film simulation subtitle
     - Star rating pill (`RatingBadge`)
     - Match description / mismatch details
     - Horizontal action buttons: "Save as new recipe" (`OutlinedButton`) on left, "View recipe" (`Button`) on right.

3. **Settings Screen Version Display**:
   - In [`SettingsScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/settings/SettingsScreen.kt), removed the "About" section header and clickable-looking SettingsCard, replacing it with a simple, centered version caption (`Version 0.1.0`) at the bottom of the page.

### Verification Results
- `./gradlew testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL** (all unit tests passed, 0 lint errors).
- Installed APK and re-launched app on `emulator-5554`.
