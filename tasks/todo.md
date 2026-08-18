# Task: Analyze Navigation & Copy Refinements

## Goal
- Rename "Read" nav item and screen to "Analyze" with page title "Extract recipe from a photo".
- Remove mentions of third-party trademarks ("Fuji X Weekly") from the parse text / create recipe UI copy.

## Plan Items
- [x] 1. Update `strings.xml` `nav_read` to "Analyze" and `photo_title` to "Extract recipe from a photo" <!-- id: 20 -->
- [x] 2. Update `strings.xml` `paste_intro` to remove third-party TM mentions <!-- id: 21 -->
- [x] 3. Verify compilation and test suite with `./gradlew testDebugUnitTest :app:assembleDebug` <!-- id: 22 -->
- [x] 4. Deploy and verify on emulator <!-- id: 23 -->

## Review
- Successfully updated navigation label to "Analyze" and screen title to "Extract recipe from a photo".
- Sanitized text pasting copy to "Paste a recipe from a website, forum or your own notes..." removing third-party trademark references.
- Verified on emulator and automated tests passed.
