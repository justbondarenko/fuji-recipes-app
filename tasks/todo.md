# Attach Analyzed Photo to Matched Recipe

- [x] Add strings in `strings.xml` for adding photo to matched recipe
- [x] Update `PhotoReaderUiState` & `PhotoReaderViewModel` with `addPhotoToRecipe()` and `imageStore`
- [x] Update `PhotoReaderScreen` and `MatchedRecipeCard` with add photo prompt/action
- [x] Add unit tests in `PhotoReaderViewModelTest`
- [x] Verify build and tests pass, verify on emulator

- [x] Display analyzed photo preview at top of view (below header, above analysis result)
- [x] Redesign `MatchedRecipeCard`
- [x] Hide "What the photo says" section on exact match

## Review
- **Prompt & Action**: Added "+ Add photo to this recipe" action button in `MatchedRecipeCard` when a photo matches an existing recipe and has open photo slots.
- **Added State**: When added, button converts to a disabled "Photo added to recipe" badge with `FujiIcons.Check`.
- **Top Photo Preview**: Added a rounded preview hero container with `AsyncImage` directly below the "Extract recipe from a photo" header and above the analysis cards.
- **Card Redesign**: Header match badge + `View recipe >` link, clean title hierarchy, and aligned action buttons.
- **Conditional Settings**: "What the photo says" table is only displayed if the photo does not have an exact match.
- **Testing**: All 525 unit tests pass. UI verified on emulator.

