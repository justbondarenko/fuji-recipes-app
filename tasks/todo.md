# Task: Remove All Film Simulation Containers/Swatches Across All Screens

## Goal
Completely remove the film simulation container/badge elements from the recipe list, recipe view, editor, importing/exporting flows, and photo reader screens.

## Plan Items

- [x] 1. Update `RecipeCard.kt` to remove `leadingContent` (swatch container) from `ListItem` <!-- id: 1 -->
- [x] 2. Update `RecipeViewScreen.kt` to remove `FilmSimBadge` and swatch spacing from `RecipeHeaderBlock` <!-- id: 2 -->
- [x] 3. Update `EditorControls.kt` to remove `FilmSimBadge` from `FilmSimulationPicker` (picker row & dropdown items) <!-- id: 3 -->
- [x] 4. Update `ImportScreen.kt` and `FileImportScreen.kt` to remove `FilmSimBadge` from row items <!-- id: 4 -->
- [x] 5. Update `ExportScreen.kt` to remove `FilmSimBadge` from export row cards <!-- id: 5 -->
- [x] 6. Update `PhotoReaderScreen.kt` to remove `FilmSimBadge` from matched recipe card & setting rows <!-- id: 6 -->
- [x] 7. Delete `FilmSimBadge.kt` <!-- id: 7 -->
- [x] 8. Verify compilation and tests with `./gradlew compileDebugKotlin testDebugUnitTest` <!-- id: 8 -->
- [x] 9. Deploy to emulator (`installDebug`), launch, and verify UI with screenshots <!-- id: 9 -->

## Review & Verification

### What changed
1. **[`RecipeCard.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/library/RecipeCard.kt)**: Removed `leadingContent` so library list cards start directly with title and tags.
2. **[`RecipeViewScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/recipe/RecipeViewScreen.kt)**: Removed top `FilmSimBadge` and spacing so recipe details start directly with the recipe name, stars, and tags.
3. **[`EditorControls.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/editor/EditorControls.kt)**: Removed badges from both the picker field and the dropdown menu items in `FilmSimulationPicker`.
4. **[`ExportScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/exporting/ExportScreen.kt)** & **[`ImportScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/ImportScreen.kt)**: Removed badges from list items, retaining standard checkboxes.
5. **[`FileImportScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/FileImportScreen.kt)**: Removed `leadingContent` badge.
6. **[`PhotoReaderScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/photo/PhotoReaderScreen.kt)**: Removed badge from matched recipe card and setting rows.
7. **Deleted `FilmSimBadge.kt`**: Completely removed the unused badge component.

### Verification
- `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL**.
- Pushed and verified across all screens on emulator (`Pixel_10_Pro_XL(AVD)`).
