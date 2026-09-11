# Camera side sheet refactor

- [x] `FujiModalSideSheet` in `ui/common/SideSheet.kt` — scrim, end-anchored surface, slide in/out, back-dismiss
- [x] Camera icon button with the nav item's badge logic (`ui/camera/CameraSheetButton.kt`), plus `LocalCameraSheetOpener`
- [x] `CameraRouteContent` → `CameraSheetContent`, hosted in the sheet with a title/close header
- [x] `AppShell`: drop the camera nav item, add a top-right action slot
- [x] `MainActivity`: own the sheet state, provide the opener, place the button, drop `CameraRoute` navigation
- [x] `FujiNavHost`: delete `CameraRoute` and fix the toolbar index used for transitions
- [x] Library header becomes (cog) [search] (camera)
- [x] Build + unit tests

## Review

- The camera is no longer a destination: `CameraRoute` is gone, the nav bar is Library / Read /
  Photos / More, and `toolbarIndex` renumbered so transitions still slide the right way.
- `FujiModalSideSheet` is hand-written — Material 3 for Compose ships no side sheet. Scrim +
  end-anchored surface inside the app's own layout, so it animates both ways and covers the
  navigation bar.
- The opener reaches screens as `LocalCameraSheetOpener` rather than through four layers of
  parameters; the sheet's state stays in `FujiApp`.
- Not verified on a device — build and unit tests only.
