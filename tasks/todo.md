# Multi-Photo Analysis

- [x] Update `PhotoReaderViewModel` to support batch URI analysis (`read(uris: List<String>)`, `AnalyzedPhoto`, `selectedIndex`, `selectPhoto(index)`)
- [x] Update `PhotoReaderScreen` to use `ActivityResultContracts.PickMultipleVisualMedia` and interactive `HorizontalPager` for multi-photo navigation
- [x] Add unit tests in `PhotoReaderViewModelTest` for multi-photo parsing and state management
- [x] Verify build, run all unit tests, and test on emulator

## Review
- **In-App Multi-Photo Picker**: In-app picker now uses `PickMultipleVisualMedia` for multi-selection while share-sheet remains single-photo.
- **Batch Processing**: Concurrently parses all selected photos off the main thread into `AnalyzedPhoto` models.
- **Interactive Multi-Photo Carousel**: Built with `HorizontalPager` and page indicator counter badge ("1 / 3") synchronized with active recipe matching and per-photo actions.
- **Tests**: All 526 unit tests pass. Build verified.

