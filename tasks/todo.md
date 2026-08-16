# Task: Remove DownArrow and Migrate Consumers to MaterialShapes.Pill

## Goal
Remove `DownArrow` polygon transformation workaround entirely. Migrate all former `DownArrow` consumers (`FileImportScreen`, `ImportScreen`, `LibraryScreen`) to use `MaterialShapes.Pill.toShape()`.

## Plan Items

- [x] 1. Remove `DownArrow` and unused graphics/matrix imports from `IconPanel.kt` <!-- id: 1 -->
- [x] 2. Update `FileImportScreen.kt` to use `MaterialShapes.Pill.toShape()` and remove `DownArrow` import <!-- id: 2 -->
- [x] 3. Update `ImportScreen.kt` to use `MaterialShapes.Pill.toShape()` and remove `DownArrow` import <!-- id: 3 -->
- [x] 4. Update `LibraryScreen.kt` to use `MaterialShapes.Pill.toShape()` and remove `DownArrow` import <!-- id: 4 -->
- [x] 5. Run `./gradlew compileDebugKotlin testDebugUnitTest` to verify clean build <!-- id: 5 -->

## Review & Verification

### What changed

1. **[`IconPanel.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/common/IconPanel.kt)**:
   - Removed `DownArrow` polygon matrix rotation workaround (`MaterialShapes.Arrow.transformed(Matrix().apply { postRotate(180f) })`).
   - Removed unused `androidx.graphics.shapes.*` and `android.graphics.Matrix` imports.
   - Updated KDoc to reference standard Material 3 Expressive shapes (`MaterialShapes.Pill`, `MaterialShapes.Pentagon`, `MaterialShapes.Square`).

2. **[`FileImportScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/FileImportScreen.kt)**:
   - Removed `DownArrow` import and added `MaterialShapes` import.
   - Migrated `FujiIconPanel` shape to `MaterialShapes.Pill.toShape()`.

3. **[`ImportScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/importing/ImportScreen.kt)**:
   - Removed `DownArrow` import and added `MaterialShapes` import.
   - Migrated `ImportPanel` shape to `MaterialShapes.Pill.toShape()`.

4. **[`LibraryScreen.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/library/LibraryScreen.kt)**:
   - Removed `DownArrow` import and added `MaterialShapes` import.
   - Migrated empty library `FujiIconPanel` shape to `MaterialShapes.Pill.toShape()`.

### Verification
- Executed `./gradlew compileDebugKotlin testDebugUnitTest`: **BUILD SUCCESSFUL** (all unit tests passed).
