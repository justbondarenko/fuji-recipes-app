# Project Rules & Guidelines

## Device testing

Use Pixel for routine automated testing. Samsung is a manual QA device; connect
it for automated checks only when investigating a user-reported Samsung bug.

## Camera preset tests and test recipes

Always use C2 for camera preset test writes. Preserve its original recipe before
the test and restore it afterward.

Always use the `TEST` prefix for test recipe names, including recipes in the app
library and test presets written to camera custom slots. Before deleting test
recipes, identify their new IDs against the pre-test library. Never delete or
modify existing user recipes as test cleanup. Camera slot restoration is separate
from deleting a recipe in the app library.

## Material 3 components

Always use official Material 3 components. Before adding or changing a UI
component, consult its official documentation, specifications and guidelines at
[Material 3 components](https://m3.material.io/components). Use the official
Jetpack Compose Material 3 implementation and follow the documented structure,
behavior, interactions and accessibility guidance. Do not invent custom
replacements for official components. The icon rules below still apply.

The RAW Editor's `RawParameterWheel` is a deliberate exception. The custom
wheel proved a better fit for precise photo adjustments: fixed tick spacing
and a stationary center marker let users make small changes by swiping the
scale. Use the existing wheel for RAW Editor, framing, and mask parameters,
and preserve its keyboard and screen-reader controls.

## 1. Icon System: Google Fonts Material Symbols Only (STRICT)

All icons in this application MUST come exclusively from **Google Fonts Material Symbols Rounded** definitions compiled into `FujiIcons`.

### 🚫 Strictly Forbidden
- **NO Compose Material Icons**: Do NOT import or use `androidx.compose.material.icons.*` (`Icons.Filled.*`, `Icons.Outlined.*`, `Icons.Default.*`, `Icons.AutoMirrored.*`, etc.).
- **NO XML Vector Drawables for Icons**: Do NOT add or reference vector XML drawables in `res/drawable/` for UI icons.

### ✅ Mandatory Standard
- **Source of Truth**: Always use `dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons.<IconName>` (e.g., `FujiIcons.Search`, `FujiIcons.Tune`, `FujiIcons.StarRate`, `FujiIcons.Add`, `FujiIcons.ArrowBack`, etc.).
- **Global Weight Control**: Icon stroke weight across the entire app is controlled via `FujiIconConfig.weight` (`IconWeight.W300`, `IconWeight.W400`, `IconWeight.W500`).
- **Adding New Icons**:
  When introducing a new icon to the app:
  1. Locate the glyph on [Google Fonts Material Symbols](https://fonts.google.com/icons).
  2. Fetch its `ImageVector` definitions for `W300` (`grad=-25`), `W400` (`grad=0`), and `W500` (`grad=0`) from `https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/<icon_name>.kt`.
  3. Register the icon as a property in [`FujiIcons.kt`](app/src/main/java/dev/bondarenko/fujirecipes/ui/theme/icons/FujiIcons.kt) with lazy cached initialization per weight.
