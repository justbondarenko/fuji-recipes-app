# Task: Implement Segmented List Shapes for Choose a Slot Sheet

## Plan Items

- [x] 1. WriteSheet: Add `slotItemShape(index, total)` corner shaping (top rounded 16dp, middle 4dp, bottom rounded 16dp) <!-- id: 1 -->
- [x] 2. WriteSheet: Update `PickerStage` to render segmented slot cards with minimal 3dp spacing, applying segmented shapes directly to slot items <!-- id: 2 -->
- [x] 3. Run tests, assemble APK, and deploy to emulator <!-- id: 3 -->

## Review & Verification

### What Changed
1. **Segmented Slot Shapes**:
   - In [`WriteSheet.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/WriteSheet.kt), implemented `slotItemShape(index, total)`:
     - **Top slot (C1)**: `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)`
     - **Middle slots (C2–C6)**: `RoundedCornerShape(4.dp)`
     - **Bottom slot (C7)**: `RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)`
     - **Single slot**: `RoundedCornerShape(16.dp)`

2. **Continuous Segmented Single-Select List**:
   - Removed the outer wrapper card.
   - Positioned the items in a vertical column with minimal `3.dp` gap.
   - Selected slot uses `secondaryContainer` highlight with position-aware shape, and unselected slots use standard surface colors.

### Verification Results
- `./gradlew testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL** (all unit tests passed, 0 lint errors).
- Installed APK and re-launched app on `emulator-5554`.
