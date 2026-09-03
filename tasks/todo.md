# Create From Text UI Redesign

- [x] Redesign `PasteRecipeScreen` UI with standard TopAppBar and back navigation
- [x] Upgrade "Paste from clipboard" to prominent CTA with `FujiIcons.ContentPaste`
- [x] Rename "Fill the form" label to "Analyze" in `strings.xml` and UI
- [x] Demote "Insert an example" to secondary subtle affordance shown only when empty
- [x] Polish recognized settings card, error handling, and text clear action
- [x] Verify build, run unit tests, and install to emulator

## Review
- **Standard Navigation**: Added `TopAppBar` with `FujiIcons.ArrowBack` for consistent back navigation.
- **Hero CTA**: Replaced plain outlined button with prominent `FilledTonalButton` + `FujiIcons.ContentPaste`.
- **Secondary Example**: Demoted sample insertion to a subtle, muted text button below the paste action, shown only when text input is empty.
- **Clear Action**: Added trailing close icon to clear text with one tap.
- **Relabeled Primary Action**: Renamed button to "Analyze" with `FujiIcons.Check` icon.
- **Vertical Button Stack**: Reordered controls: Recipe Text Area -> [Paste from clipboard] -> [Analyze] -> [Try an example].
- **Header Alignment**: Matched `FileImportScreen` exactly with standard `TopAppBar`, `titleLarge` font size, `FujiIcons.ArrowBack`, and removed redundant root padding in `FujiNavHost.kt`.
- **Verification**: Built, tested (all unit tests passed), installed on emulator, and verified pixel-perfect alignment with "Import a file" screen.

