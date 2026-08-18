# Task: Global Icon Weight Configuration via Single Variable

## Goal
Implement a single consumed Kotlin configuration variable (`FujiIconConfig.weight`) that controls the stroke weight of all icons in the app simultaneously (supporting `W300`, `W400`, `W500`), with automatic Compose state recomposition and lazy cached vector generation.

## Plan Items
- [x] 1. Write fetch & generation script for all 49 unique icons across `W300`, `W400`, `W500` weights <!-- id: 9 -->
- [x] 2. Update `FujiIcons.kt` with `enum class IconWeight`, `object FujiIconConfig`, and dynamic weight dispatch per icon <!-- id: 10 -->
- [x] 3. Verify compilation and test suite with `./gradlew testDebugUnitTest :app:assembleDebug` <!-- id: 11 -->
- [x] 4. Deploy to emulator and test live weight switching <!-- id: 12 -->

## Review
- Pre-bundled weights `W300`, `W400`, and `W500` for all 49 unique icons in `FujiIcons.kt`.
- Provided `FujiIconConfig.weight` backed by Compose `mutableStateOf` for immediate, global, zero-boilerplate runtime and compile-time control.
- Vector builders are evaluated lazily per weight to ensure optimal memory consumption.
- Verified build and live execution on the Android emulator.
