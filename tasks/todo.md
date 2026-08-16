# Task: Rating Badge Proportional Padding & Surface Container

## Goal
Fix rating badge container proportions by replacing `Badge` with `Surface(shape = CircleShape)` and applying internal `padding(horizontal = 10.dp, vertical = 5.dp)` to give the `labelLarge` text and star icon proportional breathing room.

## Plan Items

- [x] 1. Switch `RatingBadge` to `Surface(shape = CircleShape)` with internal padding in `RecipeCard.kt` <!-- id: 1 -->
- [x] 2. Run `./gradlew compileDebugKotlin testDebugUnitTest` to verify build & tests <!-- id: 2 -->
- [x] 3. Deploy APK to emulator (`installDebug`), launch, and capture screenshot <!-- id: 3 -->

## Review & Verification

### What changed
- Replaced `Badge` with `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer)`.
- Added internal padding of `horizontal = 10.dp, vertical = 5.dp` inside the pill container, perfectly framing the bold `labelLarge` rating number and 15dp star icon.

### Verification
- Executed `./gradlew compileDebugKotlin testDebugUnitTest`: **BUILD SUCCESSFUL**.
- Installed and deployed to the emulator (`Pixel_10_Pro_XL(AVD)`).
- Captured screenshot: Verified the rating badge pill has generous, proportional padding with clear text and icon legibility.
