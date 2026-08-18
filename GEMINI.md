# Project Rules & Guidelines

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
