# Task: Collapsible Tag Filter, Rating Range Filter & Directional Sorting

## Goal
1. Fix bottom sheet glitching/stuck loop and lack of scrolling by adding proper vertical scrolling and making tag filtering collapsible.
2. Upgrade rating filtering to allow selecting a range from X to Y (0/unrated to 5 stars).
3. Enhance sorting to separate parameter selection (Name, Rating, Recently Updated) from sort direction (Ascending / Descending), allowing users to control both independently.

## Plan Items
- [x] 1. Update domain models (`LibraryFilters`, `SortDirection`, `LibraryView`, `StoredLibraryView`) and comparator logic for rating range and directional sorting <!-- id: 1 -->
- [x] 2. Update `ViewPreferences` to persist and restore `max_rating` and `sort_direction` <!-- id: 2 -->
- [x] 3. Update string resources and add vector drawables for sort direction and filter controls <!-- id: 3 -->
- [x] 4. Fix `FiltersSheet` scrolling container bug and implement collapsible tag filter section with animated expansion <!-- id: 4 -->
- [x] 5. Implement Rating Range selector (0★/unrated to 5★) in `FiltersSheet` <!-- id: 5 -->
- [x] 6. Implement Sort Parameter and Direction controls in `LibraryToolbar`, `LibraryViewModel`, and `LibraryScreen` <!-- id: 6 -->
- [x] 7. Write comprehensive unit tests for rating range matching, directional sorting, and stored view repair <!-- id: 7 -->
- [x] 8. Verify with unit tests, build & install APK to emulator, and hand off for user testing <!-- id: 8 -->

## Review
- **Bottom Sheet Fix**: Added `Modifier.verticalScroll` and proper bottom inset padding to `FiltersSheet`, eliminating the layout measure/layout infinite oscillation loop.
- **Collapsible Tag Filter**: Added animated expand/collapse for tags with selected count badge and compact active selection preview row when collapsed.
- **Rating Range Filter**: Replaced single-rating choice with a smooth Material 3 `RangeSlider` spanning 0★ (Unrated) to 5★, dynamic range summaries, and reset button.
- **Directional Sorting**: Decoupled sort parameter (Name, Rating, Recently Updated) from sort direction (`SortDirection.ASCENDING`, `SortDirection.DESCENDING`). Added quick 1-tap direction toggle in toolbar and contextual direction choices in the sort menu.
- **Verification**: All unit tests pass; debug APK installed on emulator and `MainActivity` launched.
