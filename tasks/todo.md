# Task: Public Open-Source Release Preparation

## Goal
Prepare the **Fuji Recipes** repository for public open-source release, setup release APK/AAB builds, signing configurations, and CI/CD pipelines.

## Plan Items
- [x] 1. Repository hygiene: Untrack and delete `bugreport-*.zip` files <!-- id: 0 -->
- [x] 2. Create `LICENSE` file (MIT) <!-- id: 1 -->
- [x] 3. Update `.gitignore` with `.claude/`, `bugreport-*.zip`, root `*.zip` <!-- id: 2 -->
- [x] 4. Update `app/build.gradle.kts` with release signing config & fallback <!-- id: 3 -->
- [x] 5. Create `.github/workflows/release.yml` for automated releases <!-- id: 4 -->
- [x] 6. Create `.github/ISSUE_TEMPLATE/` (bug, camera compatibility, feature) & PR template <!-- id: 5 -->
- [x] 7. Verify builds (`assembleDebug`, `assembleRelease`, `bundleRelease`, `testDebugUnitTest`) <!-- id: 6 -->

## Review
- **Repository Hygiene**: Removed committed ~17MB bugreport ZIP files from git tracking. Added `.claude/`, `bugreport-*.zip`, and general `*.zip` rules to `.gitignore`.
- **Licensing**: Added official `LICENSE` file (MIT License) to match README declarations.
- **APK & Bundle Assembly**: Configured `release` signing in `app/build.gradle.kts` to support environment variables (`RELEASE_KEYSTORE_PATH`, etc.) while gracefully falling back to debug key signing so local release builds are always installable.
- **GitHub Release CI**: Created `.github/workflows/release.yml` to automatically build, test, and publish signed `.apk` and `.aab` packages when tags (`v*`) are pushed or triggered manually.
- **Community Health**: Added Issue forms for Camera Compatibility reports, Bug reports, Feature requests, and a Pull Request template.
- **Verification**: Verified `./gradlew assembleRelease`, `./gradlew bundleRelease`, and `./gradlew testDebugUnitTest` all build successfully with minified release outputs.


