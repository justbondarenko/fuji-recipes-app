# Task: Write to Camera Slot Pre-selection & Empty Slot Labeling

## Goal
Improve the "Write to camera" UI:
1. Automatically pre-select the first empty slot when slot reading finishes (without triggering write).
2. Do not pre-select any slot if all slots are occupied to avoid accidental overwrites.
3. Mark empty slots prominently as `< EMPTY SLOT >` instead of "No name set".

## Plan Items
- [x] 1. Update string resource `write_slot_unnamed` to `< EMPTY SLOT >` and add fallback button label `write_action_select_slot` <!-- id: 1 -->
- [x] 2. Update `WriteUiState` so `selectedSlot` is nullable (`Int? = null`) <!-- id: 2 -->
- [x] 3. Update `WriteViewModel` slot reading completion logic to pre-select the first empty slot (`SlotStatus.UNNAMED`), or `null` if all occupied <!-- id: 3 -->
- [x] 4. Update `WriteSheet.kt` (`PickerStage`) to handle nullable `selectedSlot` (render selection, caution banner, disabled button when none selected) <!-- id: 4 -->
- [x] 5. Update unit tests in `WriteSheetStateTest.kt` and add unit tests for pre-selection and `< EMPTY SLOT >` label mapping <!-- id: 5 -->
- [x] 6. Run all unit tests and verify build <!-- id: 6 -->
- [x] 7. Document results and verification in `tasks/todo.md` and walkthrough <!-- id: 7 -->

## Review & Verification
- Updated `strings.xml`:
  - `write_slot_unnamed` changed from `"No name set"` to `"&lt; EMPTY SLOT &gt;"`.
  - `write_action_select_slot` added (`"Choose a slot"`) for the disabled write button state when `selectedSlot == null`.
- Updated `WriteSheetState.kt`:
  - `WriteUiState.selectedSlot` changed to `Int? = null`.
  - `enteringPicker()` resets `selectedSlot = null`.
- Updated `WriteViewModel.kt`:
  - `refreshSlots()` resets `selectedSlot = null` while reading is in progress.
  - Upon successful reading from the camera, automatically pre-selects the first empty slot (`SlotStatus.UNNAMED`), or `null` if all slots are occupied.
- Updated `WriteSheet.kt`:
  - `PickerStage` receives `selectedSlot: Int?`.
  - When `selectedSlot == null`, button is disabled with label `"Choose a slot"` and no caution warning is displayed.
  - When `selectedSlot != null`, button is enabled with label `"Write to C%d"`, caution warning is displayed if applicable, and tapping initiates the write confirmation.
  - Added preview `WritePickerAllOccupiedPreview` for all-occupied slot state alongside preselected empty slot state.
- Automated Verification:
  - All 471 unit tests passed via `./gradlew testDebugUnitTest`.
  - Successfully built debug APK (`./gradlew assembleDebug`).
  - Deployed debug build to connected device (`Pixel 10 Pro XL - 17`) via `installDebug` and launched `MainActivity`.
