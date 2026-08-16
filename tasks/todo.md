# Task: Reference Photos Enhancements

## Goal
1. Tap on photo in Recipe View & Recipe Edit expands full-screen preview lightbox.
2. During photo processing, show animation and text "Processing image(s)...".
3. Disable Save button while images are processing.
4. If edit is canceled, cancel processing job and clean up any newly staged orphan images.
5. Relocate carousel to be right after title in both Recipe View and Recipe Edit.

## Plan Items

- [x] 1. Add strings (`processing_images`, `close_preview`) to `strings.xml` <!-- id: 1 -->
- [x] 2. Create `ImagePreviewDialog.kt` supporting full-screen swipeable preview with close button <!-- id: 2 -->
- [x] 3. Update `RecipeEditorViewModel.kt` to manage processing job, track staged images, block save during processing, and clean up staged orphans on cancel/dismiss <!-- id: 3 -->
- [x] 4. Update `EditorControls.kt` & `RecipeEditorScreen.kt` for processing copy/animation, save blocking, photo tap preview, and reordered layout (Title -> Photos -> Tags) <!-- id: 4 -->
- [x] 5. Update `RecipeViewScreen.kt` to place carousel right after title/header and enable tap to expand preview <!-- id: 5 -->
- [x] 6. Run `./gradlew testDebugUnitTest` <!-- id: 6 -->
- [x] 7. Deploy to emulator (`installDebug`) and verify interactions <!-- id: 7 -->

## Review & Verification
- Unit Tests: All 468 tests passed via `./gradlew testDebugUnitTest`.
- Emulator UI Verification:
  - Validated full-screen `ImagePreviewDialog` lightbox with horizontal paging, page indicator, and close button on photo tap in both Recipe View and Recipe Editor.
  - Validated processing indicator with "Processing image(s)…" during background image compression/saving.
  - Verified Save button is disabled and guarded while image processing is active.
  - Verified cancellation stops processing job and removes newly staged image files from disk.
  - Verified layout reordering: Carousel directly below Recipe Title in both View and Edit screens.
  - Verified Recipe Card thumbnail displays 1st photo on the left.
