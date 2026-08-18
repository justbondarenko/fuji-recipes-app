# Task: Slot Configuration View on Bento Slot Tile Tap

## Goal
Enable tapping on any slot in the Connected Camera Bento Grid to open its configuration details in a view matching the Recipe View (with Bento grid parameter cards, changed-only toggle, and action buttons).

## Plan Items
- [x] 1. Add `readSlotRecipe(slot)` to `SlotReader.kt` and `CameraController.kt` for reading a single slot's parameters <!-- id: 1 -->
- [x] 2. Make `BentoGroupGrid`, `BentoParameterTile`, and `SettingsGroup` reusable across `RecipeViewScreen` and Camera UI <!-- id: 2 -->
- [x] 3. Create `SlotDetailBottomSheet` composable displaying slot header, M3 loading indicator while reading, changed-only switch, parameter Bento grid, and Copy/Save actions <!-- id: 3 -->
- [x] 4. Wire `BentoSlotTile` click on `CameraScreen` to open `SlotDetailBottomSheet` for the tapped slot <!-- id: 4 -->
- [x] 5. Add unit tests for single slot recipe reading in `SlotRecipeReaderTest.kt` <!-- id: 5 -->
- [x] 6. Add Compose previews for `SlotDetailBottomSheet` (loading, loaded with parameters, empty slot, dark theme) <!-- id: 6 -->
- [x] 7. Run unit tests and verify build & deploy to emulator/device <!-- id: 7 -->
- [x] 8. Document results and verification <!-- id: 8 -->

## Review & Verification
- `SlotReader.kt` & `CameraController.kt`:
  - Added `readSlotRecipe(session, slot)` and `CameraController.readSlotRecipe(slot)` for single-slot property register queries.
- `RecipeViewScreen.kt`:
  - Made `BentoGroupGrid` and `BentoParameterTile` internal composables for shared usage.
- `SlotDetailSheet.kt`:
  - Built `SlotDetailBottomSheet` and `SlotDetailContent` matching `RecipeViewScreen` aesthetics:
    - **Header Block**: Slot badge (`C#`), recipe name, film simulation label, and camera generation tag.
    - **Actions**: "Copy recipe" (formats plain text with `RecipeTextFormatter` and copies to clipboard) and "Save to library" (persists to local repository with confirmation toast).
    - **Filter**: "Changed only" toggle switch.
    - **Bento Grid**: 2-column Bento tiles grouped into Film simulation, Tone/Exposure, White balance, Details/Grain/Color.
    - **Loading State**: Shape-morphing M3 `FujiLoadingIndicator` centered with reading feedback.
- `CameraScreen.kt` & `CameraChipHost.kt`:
  - Updated `BentoSlotTile` with click handling and ripple.
  - Tapping opens `SlotDetailBottomSheet` for the selected slot.
- Unit Tests:
  - Added test cases in `SlotRecipeReaderTest.kt` for configured slot decoding and unconfigured slot handling.
  - All 473 tests executed and passed (`./gradlew testDebugUnitTest`).
- Build:
  - `./gradlew assembleDebug` succeeded with no errors.
