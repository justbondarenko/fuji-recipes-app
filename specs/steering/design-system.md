# Design system — Android client

**Status:** binding
**Version:** 1.0
**Parity target:** the shipped Nuxt theme in `fuji-recipes-book`
**Supersedes:** `PRD.md` §5.2 ("Cappuccino", source colour `#CC785C`), which describes a
palette the web client never shipped.

---

## 1. Direction

Material 3 **Expressive** supplies the shape and motion vocabulary. The colour and type come
from the web client and are not reinterpreted.

The web client is a warm monochrome: Tailwind/PrimeVue **stone** neutrals throughout, a
near-black primary in light and near-white in dark (PrimeVue's `noir` preset, which aliases
the primary ramp onto the surface ramp), and **amber** as the one accent — ratings, the
hover state on a recipe name, the progress bar. Headings are **Lora**; body is **Inter**.

Read that as a deliberate design, not an absence of one: the film-simulation swatches are
the only saturated colour in the interface, which is what makes them scannable.

**Source of truth:** `fuji-recipes-book/src/assets/theme/{theme,primary,surface}.ts` and
`src/assets/scss/{main,typography,fonts}.scss`. The tables below are transcribed from those
files; if they ever disagree, those files win and this document is corrected.

## 2. Colour — M3 role mapping

Stone ramp: `0 #FFFFFF · 50 #FAFAF9 · 100 #F5F5F4 · 200 #E7E5E4 · 300 #D6D3D1 · 400 #A8A29E · 500 #78716C · 600 #57534E · 700 #44403C · 800 #292524 · 900 #1C1917 · 950 #0C0A09`

### Light

| M3 role | Hex | Source | Used for |
|---|---|---|---|
| `primary` | `#0C0A09` | stone-950 | filled buttons, FAB, active nav |
| `onPrimary` | `#FFFFFF` | | |
| `primaryContainer` | `#E7E5E4` | stone-200 | selected slot, connected chip |
| `onPrimaryContainer` | `#1C1917` | stone-900 | |
| `secondary` | `#57534E` | stone-600 | secondary actions, filter chips |
| `onSecondary` | `#FFFFFF` | | |
| `secondaryContainer` | `#F5F5F4` | stone-100 | tag chips |
| `onSecondaryContainer` | `#292524` | stone-800 | |
| `tertiary` | `#D97706` | amber-600 | **the accent** — rating stars, progress, emphasis |
| `onTertiary` | `#FFFFFF` | | |
| `tertiaryContainer` | `#FEF3C7` | amber-100 | rating pill background |
| `onTertiaryContainer` | `#78350F` | amber-900 | rating pill text |
| `error` | `#DC2626` | red-600 | |
| `onError` | `#FFFFFF` | | |
| `errorContainer` | `#FEE2E2` | red-100 | |
| `onErrorContainer` | `#7F1D1D` | red-900 | |
| `surface` | `#E7E5E4` | stone-200 | app background (`html` is `bg-stone-200`) |
| `onSurface` | `#1C1917` | stone-900 | body text |
| `onSurfaceVariant` | `#57534E` | stone-600 | labels, secondary text |
| `surfaceContainerLowest` | `#FFFFFF` | stone-0 | |
| `surfaceContainerLow` | `#FAFAF9` | stone-50 | **cards at rest** |
| `surfaceContainer` | `#F5F5F4` | stone-100 | sheets, nav bar |
| `surfaceContainerHigh` | `#E7E5E4` | stone-200 | raised |
| `surfaceContainerHighest` | `#D6D3D1` | stone-300 | input fields |
| `outline` | `#78716C` | stone-500 | |
| `outlineVariant` | `#D6D3D1` | stone-300 | dividers, card borders |
| `inverseSurface` | `#1C1917` | | snackbars |
| `inverseOnSurface` | `#FAFAF9` | | |
| `inversePrimary` | `#FAFAF9` | | |

### Dark

| M3 role | Hex | Source |
|---|---|---|
| `primary` | `#FAFAF9` | stone-50 |
| `onPrimary` | `#0C0A09` | stone-950 |
| `primaryContainer` | `#44403C` | stone-700 |
| `onPrimaryContainer` | `#F5F5F4` | stone-100 |
| `secondary` | `#D6D3D1` | stone-300 |
| `onSecondary` | `#292524` | stone-800 |
| `secondaryContainer` | `#44403C` | stone-700 |
| `onSecondaryContainer` | `#E7E5E4` | stone-200 |
| `tertiary` | `#FBBF24` | amber-400 |
| `onTertiary` | `#451A03` | amber-950 |
| `tertiaryContainer` | `#78350F` | amber-900 |
| `onTertiaryContainer` | `#FDE68A` | amber-200 |
| `error` | `#F87171` | red-400 |
| `onError` | `#450A0A` | red-950 |
| `errorContainer` | `#7F1D1D` | red-900 |
| `onErrorContainer` | `#FECACA` | red-200 |
| `surface` | `#1C1917` | stone-900 (`html` is `dark:bg-surface-900`) |
| `onSurface` | `#FAFAF9` | stone-50 |
| `onSurfaceVariant` | `#D6D3D1` | stone-300 |
| `surfaceContainerLowest` | `#0C0A09` | stone-950 |
| `surfaceContainerLow` | `#1C1917` | stone-900 |
| `surfaceContainer` | `#292524` | stone-800 — **cards at rest** |
| `surfaceContainerHigh` | `#44403C` | stone-700 |
| `surfaceContainerHighest` | `#57534E` | stone-600 |
| `outline` | `#A8A29E` | stone-400 |
| `outlineVariant` | `#44403C` | stone-700 |

**Dynamic colour is off.** Do not call `dynamicLightColorScheme()` / `dynamicDarkColorScheme()`.
Material You would replace this palette with wallpaper colours and destroy the parity that
is the whole point.

## 3. Film-simulation swatches — data, not theme

A fixed, non-theme palette. These do **not** shift between light and dark. Transcribed from
`fuji-recipes-book/shared/recipe-fields.ts` (`FILM_SIMULATIONS`).

| id | Label | Swatch | Mono |
|---|---|---|---|
| `provia` | PROVIA / Standard | `#8C7B6B` | |
| `velvia` | Velvia / Vivid | `#A83A2E` | |
| `astia` | ASTIA / Soft | `#C08B7A` | |
| `classic-chrome` | Classic Chrome | `#6E7A78` | |
| `pro-neg-hi` | PRO Neg. Hi | `#9C8878` | |
| `pro-neg-std` | PRO Neg. Std | `#9C8878` | |
| `classic-negative` | Classic Negative | `#7B6A5A` | |
| `nostalgic-negative` | Nostalgic Neg. | `#A5703F` | |
| `reala-ace` | REALA ACE | `#8A7150` | |
| `eterna` | ETERNA / Cinema | `#6B6659` | |
| `eterna-bleach-bypass` | ETERNA Bleach Bypass | `#6B6659` | |
| `acros` | ACROS | `#5A5A5A` | ✓ |
| `acros-ye` | ACROS + Ye filter | `#5A5A5A` | ✓ |
| `acros-r` | ACROS + R filter | `#5A5A5A` | ✓ |
| `acros-g` | ACROS + G filter | `#5A5A5A` | ✓ |
| `monochrome` | Monochrome | `#7A7A7A` | ✓ |
| `monochrome-ye` | Monochrome + Ye | `#7A7A7A` | ✓ |
| `monochrome-r` | Monochrome + R | `#7A7A7A` | ✓ |
| `monochrome-g` | Monochrome + G | `#7A7A7A` | ✓ |
| `sepia` | Sepia | `#8B6F47` | ✓ |

Unknown id → `#9CA3AF`, matching the web fallback.

**The badge** is a circle with a 1dp ring (`black 10%` light, `white 20%` dark), filled with
the swatch, with the simulation's image drawn over it (`ContentScale.Crop`). The 20 images
are copied from `fuji-recipes-book/src/public/film-simulations/*.webp` into
`res/drawable/`. If an image is missing, the swatch fill alone is the badge — the web client
does exactly this on image error, and it is why the swatch exists as a separate value.

## 4. Typography

Bundled in `res/font`, not downloadable — a first-launch font flash is not acceptable on the
one screen the app is judged by.

| Family | Weights | Where |
|---|---|---|
| **Lora** | 400, 600, 700 | display, headline, title — everything the web client renders in `font-serif` (all headings, dialog and card titles) |
| **Inter** | 400, 500, 600 | body and label — everything in `font-sans` |

Numeric readouts use Inter with `FontFeatureSetting("tnum")`, mirroring the web client's
`.stable-number` class. Digits that jitter as a slider moves are a defect.

| M3 token | Family / weight | Size / line | Where |
|---|---|---|---|
| `headlineMedium` | Lora 600 | 28 / 36 | screen title ("Fuji Recipes") |
| `titleLarge` | Lora 700 | 18 / 24 | recipe name on a card |
| `titleMedium` | Lora 600 | 16 / 22 | section headers |
| `bodyLarge` | Inter 400 | 16 / 24 | notes, prose |
| `bodyMedium` | Inter 400 | 14 / 20 | setting values |
| `bodySmall` | Inter 400 | 12 / 16 | simulation label, secondary metadata |
| `labelLarge` | Inter 600 | 14 / 20 | buttons |
| `labelMedium` | Inter 500 | 12 / 16 | tag chips, field labels |
| `labelSmall` | Inter 500 | 11 / 16 | metadata |

Expressive's `*Emphasized` variants are spent in exactly three places: the empty-state
headline, the write-success confirmation, and the app-bar title on a detail screen.
Everywhere else, standard weights.

## 5. Shape and motion

- **Shape scale:** `extraSmall` 8dp, `small` 12dp, `medium` 20dp, `large` 28dp,
  `extraLarge` 36dp. The web cards are `rounded-xl` (12dp) with a 1px border and no
  elevation; the Android cards use `medium` and the same borderless-with-outline treatment.
  Separation comes from spacing and the stone background, not shadows.
- **Motion:** `MotionScheme.expressive()` at the theme level.
- **Where motion is spent, and nowhere else:** camera connect (shape + colour morph, spring
  settle) · write progress (`LinearWavyProgressIndicator`) · write success (bar collapses
  into a check) · FAB press.
- **Reduced motion:** when `Settings.Global.ANIMATOR_DURATION_SCALE == 0`, fall back to
  `MotionScheme.standard()` and a non-wavy progress indicator.

## 6. App icon

Same mark as the web client. Sources in `fuji-recipes-book/src/public/`:

| Purpose | Source file | Android target |
|---|---|---|
| Adaptive foreground | `icons/icon-maskable-512.png` — the mark already sits inside the 80% safe zone | `res/mipmap-*/ic_launcher_foreground` |
| Adaptive background | solid `#F6F6F4`, the mark's own backdrop | `res/values/ic_launcher_background.xml` |
| Monochrome (Android 13+ themed icons) | derived from the mark, single-colour | `res/drawable/ic_launcher_monochrome.xml` |
| Play Store listing | `icons/icon-512.png` | store asset, not in the APK |

Do not regenerate the mark, re-trace it, or "clean it up". It is the same product; it gets
the same icon. `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` reference
the layers above.

## 7. Card anatomy — list parity

The web recipe card, as the Android card must read:

```
┌───────────────────────────────────────────────┐
```

- Card: `surfaceContainerLow` (light) / `surfaceContainer` (dark), 1dp `outlineVariant`
  border, `medium` corners, no elevation.
- Rating pill is **omitted** when rating is 0, not shown as zero.
- Tag row is omitted when there are no tags; at most 5 shown, then `+n`.
- **No last-written line.** The Android client does not track when a recipe reached a
  camera; the web client's `C3 · 12 Aug 2026` row has no counterpart here.
- Overflow `⋮` is a 40dp touch target, sibling of the card's click target, never nested
  inside it.
