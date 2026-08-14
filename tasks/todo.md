# Task: Verify M3 Menus Component & Add Icons to Filtering and Sorting Controls

## Plan Items
- [x] 1. Verify that sorting controls use the Material 3 `DropdownMenu` and `DropdownMenuItem` components matching M3 Menus specifications (`https://m3.material.io/components/menus/overview`) <!-- id: 1 -->
- [x] 2. Create vector drawable icons for sorting and filtering controls:
  - `ic_tune.xml` (filter sliders icon) for the Filters button and filter sections
  - `ic_sort.xml` (sort icon) for the Sort menu trigger
  - `ic_sort_by_alpha.xml` (Name A–Z)
  - `ic_schedule.xml` (Date updated)
  - `ic_label.xml` (Tags filter header) <!-- id: 2 -->
- [x] 3. Update `LibraryToolbar.kt`:
  - Add leading filter icon (`ic_tune`) to the `FiltersButton` alongside the label and active badge
  - Add leading sort icon (`ic_sort`) and trailing arrow icon (`KeyboardArrowDown`) to the `SortMenu` trigger button
  - Update `SortMenu`'s `DropdownMenuItem`s to include appropriate M3 `leadingIcon` for each sort option (Alphabetical, Star for Rating, History/Schedule for Updated) and `trailingIcon` checkmark for the selected item
  - Add section icons in `FiltersSheet` for Rating (Star), Film Simulation (Camera), and Tags (Label/Tag)
  - Add leading visual cues / icons to filter chips where appropriate <!-- id: 3 -->
- [x] 4. Build and run unit tests (`./gradlew testDebugUnitTest assembleDebug`) <!-- id: 4 -->
- [x] 5. Test and capture screenshots on device/emulator to visually verify:
  - Filters button with leading icon & active badge
  - Sort button with leading sort icon & dropdown menu with leading option icons and trailing checkmark
  - Filters sheet with section icons and chips <!-- id: 5 -->
- [x] 6. Update `walkthrough.md` and present results <!-- id: 6 -->
