# Task: Duplicates Finder Feature

## Goal
Implement a complete Duplicates Finder feature with:
1. Navigation item in 3rd position of bottom bar (after Analyze, before Connect) with `cleaning_services` icon and "Cleanup" label.
2. Cleanup screen with top header (Square shape containing `cleaning_services` icon, Title "Duplicates cleanup", and "Find duplicated" CTA button).
3. Section 1: 100% match duplicates with selection of which recipe to leave/keep and deletion of duplicates.
4. Section 2: Highly similar recipes (same film simulation, difference in 1–3 fields only) with differences highlighted.

## Plan Items
- [x] 1. Add `CleaningServices` icon definitions (W300, W400, W500) to `FujiIcons.kt` <!-- id: 1 -->
- [x] 2. Add string resources in `strings.xml` for Cleanup navigation, header, CTA, sections, and actions <!-- id: 2 -->
- [x] 3. Create pure domain logic `DuplicateFinder.kt` for exact matches & 1-3 field similarity analysis <!-- id: 3 -->
- [x] 4. Create comprehensive unit tests in `DuplicateFinderTest.kt` <!-- id: 4 -->
- [x] 5. Implement `CleanupViewModel.kt` for scan state, keep selection, and duplicate deletion <!-- id: 5 -->
- [x] 6. Implement `CleanupScreen.kt` with M3 square header, "Find duplicated" CTA, and 2 result sections <!-- id: 6 -->
- [x] 7. Update `AppShell.kt`, `FujiNavHost.kt`, and `MainActivity.kt` with `CleanupRoute` in 3rd nav position <!-- id: 7 -->
- [x] 8. Run unit tests and verify the complete flow <!-- id: 8 -->
- [x] 9. Remove guidance container and center the Cleanup initial screen (keeping 72dp shape size) <!-- id: 9 -->
- [x] 10. Add minimum 3-second delay to findDuplicates loading state in CleanupViewModel <!-- id: 10 -->
- [x] 11. Verify with unit tests <!-- id: 11 -->
- [x] 12. Add `StarShine` icon (W300, W400, W500) and implement clean screen centered with 72dp Square shape without Scan again buttons <!-- id: 12 -->
- [x] 13. Remove subtitle/description from Results screen header <!-- id: 13 -->

## Review
- **Navigation & Icon System**: Registered `FujiIcons.CleaningServices` and `FujiIcons.StarShine` (W300, W400, W500) from Google Fonts Material Symbols. Updated bottom navigation to place `Cleanup` in 3rd position (after `Analyze`, before `Connect`).
- **Screen & Layout**: Clean centered initial state matching app conventions with preserved 72dp square container shape (`cleaning_services` icon), Title "Duplicates cleanup", subtitle, and "Find duplicated" CTA button.
- **Clean State**: When no duplicates or similarities are found, displays a centered 72dp square container with `FujiIcons.StarShine`, title "Library is clean", and subtitle, with zero scan buttons.
- **Results Header**: Clean title-only header without subtitle, paired with the "Scan again" button and an additional bottom "Scan again" action.
- **Loading Behavior**: Configured minimum 3-second spinner duration on "Find duplicated" scan action.
- **Verification**: All unit tests passed (`DuplicateFinderTest`, `CleanupViewModelTest`) and APK assembled successfully.

