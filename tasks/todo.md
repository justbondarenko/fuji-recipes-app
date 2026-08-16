# Task: Strip Custom Shapes and Use Standard Material 3 Defaults

## Goal
Remove custom `FujiShapes` and `Shape.kt`. Rely entirely on default Material 3 `Shapes` in `MaterialExpressiveTheme`.

## Plan Items

- [x] 1. Remove `shapes = FujiShapes` from `Theme.kt` <!-- id: 1 -->
- [x] 2. Delete `Shape.kt` <!-- id: 2 -->
- [x] 3. Remove manual `shape = RoundedCornerShape(16.dp)` from `AppShell.kt` and use M3 default `extendedFabShape` <!-- id: 3 -->
- [x] 4. Run `./gradlew compileDebugKotlin testDebugUnitTest` to verify build & tests <!-- id: 4 -->

## Review & Verification

### What changed
1. **[`Theme.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/theme/Theme.kt)**: Removed custom `shapes = FujiShapes` from `MaterialExpressiveTheme`. The theme now uses the official default Material 3 shape scale (`extraSmall = 4.dp`, `small = 8.dp`, `medium = 12.dp`, `large = 16.dp`, `extraLarge = 28.dp`).
2. **Deleted `Shape.kt`**: Removed custom shape file.
3. **[`AppShell.kt`](file:///Users/andrii/Developer/Personal/fuji-recipes-app/app/src/main/java/dev/bondarenko/fujirecipes/ui/shell/AppShell.kt)**: `ExtendedFloatingActionButton` now relies directly on standard M3 `FloatingActionButtonDefaults.extendedFabShape` (which maps to `shapes.large` = 16dp squircle) with no manual overrides.

### Verification
- Executed `./gradlew compileDebugKotlin testDebugUnitTest`: **BUILD SUCCESSFUL** (all unit tests passed).
