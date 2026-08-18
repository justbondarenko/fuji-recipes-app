# Task: Recipe Comparison Feature

## Goal
Allow users to compare any given recipe with another recipe from their library side-by-side in an expressive Material 3 Modal Bottom Sheet triggered from the bottom floating toolbar.

## Plan Items
- [x] 1. Create vector drawable for compare action (`ic_compare.xml` with exact Material Symbols `text_compare`) and localized string resources <!-- id: 1 -->
- [x] 2. Implement pure domain comparison logic `RecipeComparison.kt` for computing face-to-face field comparisons, group ordering, and difference detection <!-- id: 2 -->
- [x] 3. Create `RecipeCompareViewModel.kt` to manage comparison state, explicit user target recipe selection (no auto-preselection), and differences-only filtering <!-- id: 3 -->
- [x] 4. Build `RecipeCompareBottomSheet.kt` with equal-height Current/Target cards, Target label, quick navigation button to target recipe, center-label comparison rows, dull vs prominent difference styling, and 70%-to-full height bottom sheet behavior <!-- id: 4 -->
- [x] 5. Integrate Compare button into `RecipeFloatingToolbar` and wire bottom sheet in `RecipeViewScreen.kt` and `LibraryScreen.kt` <!-- id: 5 -->
- [x] 6. Write comprehensive unit tests for recipe comparison logic and ViewModel <!-- id: 6 -->
- [x] 7. Build, install on emulator, and push updated version <!-- id: 7 -->

## Review
- **Exact Icon**: Updated `ic_compare.xml` to match the exact official Material Symbols Outlined `text_compare` (`FILL@0, wght@400, GRAD@0, opsz@24`).
- **Initial Selection Flow**: Triggering comparison presents the candidate recipe selection list first so the user explicitly chooses what to compare with without unwanted preselection.
- **Center-Label Table Layout**: Each comparison row displays `[ Value A (Left) ]  [ Field Name + Icon (Center) ]  [ Value B (Right) ]`.
- **Equal-Size Header Boxes**: Current and Target header cards are matched to the exact same height using `IntrinsicSize.Max` and `fillMaxSize()`.
- **Target Label & Quick Navigation**: Replaced "Change" with "Target", added a direct forward navigation button to jump straight into the target recipe from comparison, and added a top Recipe Name comparison row.
- **Verification**: All 483 unit tests passing. Debug APK built and installed to emulator.
