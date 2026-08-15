# Task: Redesign Slot Chooser with Top Refresh, M3 Segmented List, and In-place Confirmation

## Plan Items

- [x] 1. WriteSheet Header: Move "Read the slots again" button to the top header as a round icon button next to "Choose a slot" <!-- id: 1 -->
- [x] 2. WriteSheet Segmented List: Wrap slots in M3 Segmented single-select list container with highlighted selected item <!-- id: 2 -->
- [x] 3. WriteSheet In-place Confirmation: Show cautionary message (if any) and Confirm button directly below the list in PickerStage <!-- id: 3 -->
- [x] 4. Run tests, assemble APK, and deploy to emulator <!-- id: 4 -->

## Review & Verification

### What Changed
1. **Top Header Refresh Button**:
   - In [`WriteSheet.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/camera/WriteSheet.kt), placed a round `IconButton` with `Icons.Default.Refresh` on the right side of the "Choose a slot" header row, removing the bottom text button.

2. **M3 Segmented Single-Select List**:
   - Wrapped the slot choices in a `RoundedCornerShape(20.dp)` container (`surfaceContainerLow`).
   - Each item is a `SegmentedSlotItem` with single-selection state (`selectedSlot`).
   - Selected item renders with `secondaryContainer` fill, rounded pill corners (`16.dp`), `primary` circle badge, and bold text. Unselected items render with transparent background.

3. **In-place Confirmation**:
   - Removed the separate confirmation stage transition from slot picking.
   - If the currently selected slot holds an existing recipe or unreadable data, a caution alert panel is dynamically rendered right below the list.
   - A full-width "Write to C[X]" confirm button sits directly below the list/caution to execute the write directly.

### Verification Results
- `./gradlew testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL** (all unit tests passed, 0 lint errors).
- Installed APK and re-launched app on `emulator-5554`.
