# Task: No Recipes Match Screen Redesign

## Goal
Simplify the "No recipes match" screen to remove containered card styling and display only the "No recipes match" text with the `FilterAltOff` icon inside the Material Shape `Arch`.

## Plan Items
- [x] 1. Add `FilterAltOff` Material Symbols Rounded icon (`W300`, `W400`, `W500`) to `FujiIcons.kt` <!-- id: 13 -->
- [x] 2. Update `LibraryScreen.kt` `hasNoMatches` state to use uncontained `FujiIconPanel` with `MaterialShapes.Arch` and `FujiIcons.FilterAltOff` <!-- id: 14 -->
- [x] 3. Verify compilation and test suite with `./gradlew testDebugUnitTest :app:assembleDebug` <!-- id: 15 -->
- [x] 4. Deploy to emulator and verify live UI with search query <!-- id: 16 -->

## Review
- Successfully simplified the "No recipes match" empty search/filter state in `LibraryScreen.kt`.
- Removed container card background, border, extra body text, and button.
- Positioned `FilterAltOff` inside `MaterialShapes.Arch.toShape()` directly above the centered "No recipes match" headline.
- Verified on emulator and automated tests passed.
