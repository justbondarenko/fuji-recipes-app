# Fuji recipe manager — Android app

**Version:** 1.0
**Status:** Draft for build
**Owner:** Andrii
**Platform:** Android 10+ (API 29), Kotlin, Jetpack Compose
**Design:** Material 3 Expressive, "Cappuccino" theme
**Scope:** this document is self-contained. Everything needed to build the app is here or in `specs/`.

---

## 1. Summary

A native Android app that stores Fujifilm film simulation recipes and writes them to the camera's C1–C7 custom slots over USB-C. Local-first: the phone holds the truth, the cloud is a backup.

**Why native:** the phone is the field device. It goes in the bag with the camera. A native app gets a USB attach intent — plug the camera in, the app opens already connected — works with no signal, survives being backgrounded mid-write, and depends on no browser being installed or configured a particular way.

---

## 2. Design constraints that shape everything else

Four facts about the environment drive most of the decisions in this document. They are stated once here and referenced throughout.

| # | Constraint | Consequence |
|---|---|---|
| C1 | **The phone has one USB-C port, and the camera occupies it.** | Cabled debugging is unavailable during every test that matters. Wireless debugging is set up first; protocol traces are written to a file readable after unplugging. |
| C2 | **There is no cloud copy of the data.** | Export is the only backup, which promotes it from a feature to a guarantee. Migrations are never destructive. Drafts survive process death. |
| C3 | **The protocol is reverse-engineered and partly unverified.** | The decision layer is a pure function testable with no hardware. Protocol work is scheduled last so a slog there does not block a working product. |
| D4 | **The field context is one-handed, outdoors, often with no signal.** | Primary actions sit in thumb reach. Haptics confirm writes. No feature may depend on a network. |

## 3. Goals and non-goals

### Goals

1. Store recipes with the full Fujifilm parameter set, across sensor generations
2. Write any recipe to any custom slot, C1 through C7
3. Work with no network connection at all
4. Launch automatically when a Fujifilm camera is plugged in
5. Import and export recipes as JSON, or as a ZIP archive of JSON files
6. Look and feel deliberate — Material 3 Expressive, not stock

### Non-goals

- Tablet-optimised layouts in v1 (build responsively, do not design for it)
- Wear OS, Android Auto, widgets
- Multi-device sync conflict resolution beyond last-write-wins
- Reading slots back off the camera in v1
- Accounts, sharing, community recipes, image editing, watermarks, borders, EXIF frames
- RAF file re-rendering through the camera engine (v2 at the earliest)
- Reading slots back off the camera in v1

---

## 4. Architecture

### 4.1 Data storage — ~~decided~~ **SUPERSEDED**

> **This section is out of date and must not be built from.**
> It was written when the platform had no server and the phone was the only client. Since
> then the Nuxt client moved to Cloudflare Workers + D1 with a real HTTP API, and the two
> apps are now peers sharing one database.
>
> **Current decision:** the server owns the data; Android reads it over the existing
> `/api` and keeps a read-only snapshot for offline reads. `INTERNET` is required. No Room.
> Authentication is a Cloudflare Access service token — see
> `specs/steering/architecture.md` §4 and §5.
>
> What survives from below: the `RecipeRepository` interface as the single seam, and
> `updatedAt` on every mutation. What does not: "Room only", "no network permission",
> and the whole Datastore rationale.

Google Cloud Datastore has **no Android client SDK**. Firestore in Datastore mode is a server-side product; only Firestore in Native mode ships a mobile SDK. Talking to Datastore from Android means either shipping service-account credentials in the APK (never do this) or standing up a server.

**Decision for v1: Room only. No backend, no auth, no network permission.**

The app declares no `INTERNET` permission at all. This is not a limitation to work around — it is a feature. No network means no sync bugs, no auth, no cold-start latency, no offline handling, no privacy surface, and the app works identically in a basement or on a plane. Backup is the export file (§7.6), which you already need for other reasons.

**Two things to do now so a future backend is cheap, and nothing more:**

1. `RecipeRepository` is an interface returning `Flow<List<Recipe>>`. ViewModels depend on the interface. `RoomRecipeRepository` is the only implementation.
2. `RecipeEntity` carries `updatedAt` as an epoch millis field, already written on every mutation. Any future sync needs it and retrofitting timestamps onto existing rows is annoying.

Do not build a `RemoteSyncSource` stub, a sync status field, a dirty flag, or a conflict-resolution strategy. They are speculative and they will be wrong when you actually need them.

**If a second client ever needs the same library**, the path is: stand up one small service exposing CRUD over the recipe collection behind a static bearer token, add a second `RecipeRepository` implementation that reads Room and syncs via WorkManager, and keep Datastore as the backing store. That is a v2 conversation triggered by a real need, not a v1 design constraint.

Firestore in Native mode would give free offline sync and real-time updates, but drags in Firebase Auth and a cloud account for a single user with no sharing requirement. Rejected.

### 4.2 Layers

```
┌──────────────────────────────────────────────────┐
│  UI — Jetpack Compose                            │
│  Screens, CappuccinoTheme, M3 Expressive         │
└───────────────────┬──────────────────────────────┘
                    │ StateFlow<UiState>
┌───────────────────▼──────────────────────────────┐
│  ViewModels                                       │
│  RecipeListVm · RecipeEditVm · RecipeViewVm       │
│  CameraVm (singleton-scoped) · ImportVm · ExportVm│
└─────────┬──────────────────────────┬──────────────┘
          │                          │
┌─────────▼────────────┐   ┌─────────▼──────────────┐
│  RecipeRepository    │   │  CameraController      │
│  (interface)         │   │  ├── UsbManager        │
│  └── RoomRecipeRepo  │   │  ├── PtpSession        │
└─────────┬────────────┘   │  └── RecipeWriter      │
          │                └─────────┬──────────────┘
┌─────────▼────────────┐             │ bulk transfer
│  Room / SQLite       │             ▼
└──────────────────────┘         Camera (USB-C)

No network layer. The app declares no INTERNET permission.
```

**Single-activity, Compose Navigation.** `CameraController` is a `@Singleton` held above the nav graph so connection state survives screen changes — losing the camera connection because you navigated to the edit screen would be maddening.

---

## 5. Design system — "Cappuccino"

### 5.1 Direction

Warm, low-contrast, paper-like. The reference point is a printed film datasheet: cream stock, brown ink, generous margins, serif headings that look typeset rather than rendered. The app is a notebook for looks, so it should feel like a notebook.

Material 3 Expressive supplies the motion and shape vocabulary; Cappuccino supplies the colour and type. Use Expressive's springiness deliberately — on state changes that matter (camera connects, write completes) — and keep everything else calm. An app that bounces on every tap is exhausting.

### 5.2 Colour tokens — **SUPERSEDED**

> **This palette must not be built from.** "Cappuccino" describes a warm brown scheme the
> web client never shipped. What `fuji-recipes-book` actually runs is PrimeVue Aura with
> `surface.stone` primitives and the `noir` primary preset: stone neutrals throughout, a
> near-black primary in light and near-white in dark, amber as the single accent, **Lora**
> for headings and **Inter** for body.
>
> The Android palette matches that, because the two clients are one product.
> Authoritative tokens: `specs/steering/design-system.md` §2.
>
> §5.3's *structure* (serif for titles, sans for content, tabular figures for numerics)
> stands — only the families change: Lora and Inter, not Source Serif 4 and Inter.
> §5.4 (shape, motion, reduced-motion) stands unchanged.

Source colour: `#CC785C`. Palette generated from it and hand-tuned toward brown rather than orange.

**Light scheme**

| M3 role | Hex | Used for |
|---|---|---|
| `primary` | `#8F4E2E` | primary buttons, FAB, active nav |
| `onPrimary` | `#FFFFFF` | |
| `primaryContainer` | `#FFDBCA` | connected-state chip, selected slot |
| `onPrimaryContainer` | `#351000` | |
| `secondary` | `#77574A` | secondary actions, filter chips |
| `onSecondary` | `#FFFFFF` | |
| `secondaryContainer` | `#FFDBCA` | tag chips |
| `onSecondaryContainer` | `#2C160B` | |
| `tertiary` | `#6A5D2F` | rating stars, accents |
| `onTertiary` | `#FFFFFF` | |
| `tertiaryContainer` | `#F3E1A7` | |
| `onTertiaryContainer` | `#221B00` | |
| `error` | `#BA1A1A` | |
| `errorContainer` | `#FFDAD6` | |
| `surface` | `#FFF8F5` | app background |
| `onSurface` | `#221A16` | body text |
| `onSurfaceVariant` | `#52443D` | labels, secondary text |
| `surfaceContainerLowest` | `#FFFFFF` | |
| `surfaceContainerLow` | `#FFF1EB` | cards at rest |
| `surfaceContainer` | `#FCEBE3` | nav bar, sheets |
| `surfaceContainerHigh` | `#F6E5DE` | raised cards |
| `surfaceContainerHighest` | `#F0E0D8` | input fields |
| `outline` | `#85736C` | |
| `outlineVariant` | `#D7C2B9` | dividers |

**Dark scheme**

| M3 role | Hex |
|---|---|
| `primary` | `#FFB694` |
| `onPrimary` | `#542105` |
| `primaryContainer` | `#71371A` |
| `onPrimaryContainer` | `#FFDBCA` |
| `secondary` | `#E7BEAC` |
| `onSecondary` | `#442A1F` |
| `secondaryContainer` | `#5D4034` |
| `onSecondaryContainer` | `#FFDBCA` |
| `tertiary` | `#D7C58D` |
| `onTertiary` | `#3A3005` |
| `tertiaryContainer` | `#52461A` |
| `onTertiaryContainer` | `#F3E1A7` |
| `error` | `#FFB4AB` |
| `surface` | `#1A120E` |
| `onSurface` | `#F0DFD8` |
| `onSurfaceVariant` | `#D7C2B9` |
| `surfaceContainerLowest` | `#140C09` |
| `surfaceContainerLow` | `#221A16` |
| `surfaceContainer` | `#271E1A` |
| `surfaceContainerHigh` | `#322824` |
| `surfaceContainerHighest` | `#3D332E` |
| `outline` | `#A08D85` |
| `outlineVariant` | `#52443D` |

**Dynamic colour is off.** Material You would replace this palette with the user's wallpaper colours, which defeats the point. Set the scheme explicitly; do not call `dynamicLightColorScheme()`.

**Film simulation swatches** are a separate, non-theme palette — each simulation gets a fixed colour approximating its look (Velvia saturated red, Classic Chrome desaturated slate, Acros neutral grey). These are data, not theme tokens. Keep them in a `FilmSimColors` object and do not let them shift with the scheme.

### 5.3 Typography

**Serif for titles and headings. Sans for content.** Both from Google Fonts, bundled in the APK rather than downloadable, so there is no first-launch font flash.

| Role | Family | Weight | Notes |
|---|---|---|---|
| Display, Headline, Title | **Source Serif 4** | 400 / 600 | variable font, close to the transitional serif Claude uses; set optical size axis where sizes allow |
| Body, Label | **Inter** | 400 / 500 / 600 | high x-height, excellent at small sizes, neutral enough not to fight the serif |
| Numeric readouts | **Inter**, tabular figures | 500 | slider values, progress counts, colour temperature — turn on `tnum` so digits stop jittering as they change |

**Type scale** — override the M3 defaults so the serif lands where it should:

| Token | Family | Size / line height | Where |
|---|---|---|---|
| `headlineLarge` | Source Serif 4 600 | 32 / 40 | large top app bar, expanded |
| `headlineMedium` | Source Serif 4 600 | 28 / 36 | screen titles |
| `titleLarge` | Source Serif 4 600 | 22 / 28 | recipe name in list rows and on the view page |
| `titleMedium` | Source Serif 4 600 | 17 / 24 | section headers in form and view |
| `bodyLarge` | Inter 400 | 16 / 24 | notes, descriptions |
| `bodyMedium` | Inter 400 | 14 / 20 | setting values |
| `labelLarge` | Inter 600 | 14 / 20 | buttons |
| `labelMedium` | Inter 500 | 12 / 16 | field labels, tags |
| `labelSmall` | Inter 500 | 11 / 16 | metadata, timestamps |

M3 Expressive adds *emphasized* variants (`headlineMediumEmphasized` and friends) with tighter tracking and heavier weight for moments that should land. Use them in exactly three places: the empty state headline, the write-success confirmation, and the app bar title on the view page. Everywhere else, standard weights.

### 5.4 Shape and motion

- **Shape scale:** more rounded than M3 default. `extraSmall` 8dp, `small` 12dp, `medium` 20dp, `large` 28dp, `extraLarge` 36dp. Cards and sheets read as soft paper rather than panels.
- **Motion scheme:** `MotionScheme.expressive()` at the theme level — spring-based, slightly overshooting.
- **Where motion is spent** (and nowhere else):
  1. Camera connect — the indicator morphs shape and colour, spring settle
  2. Write progress — `LinearWavyProgressIndicator`, the wave amplitude is the visual signal that something is genuinely happening on the wire
  3. Write success — the progress bar collapses into a check with a shape morph
  4. FAB press — standard Expressive press physics
- **Reduced motion:** honour `Settings.Global.ANIMATOR_DURATION_SCALE == 0` by falling back to `MotionScheme.standard()` and a non-wavy progress indicator.

---

## 6. Navigation and layout

### 6.1 Scaffold

```
┌──────────────────────────────────────┐
│  TopAppBar   Recipes      [◗ X100VI] │  large, collapses on scroll
├──────────────────────────────────────┤
│                                      │
│           NavHost content            │
│                                      │
├──────────────────────────────────────┤
│   ▣ List        ⊕        ⋯ More      │  NavigationBar + centre FAB
└──────────────────────────────────────┘
```

`Scaffold` with `TopAppBar` (`LargeTopAppBar` on the list screen, `TopAppBar` elsewhere), `NavigationBar`, and a `FloatingActionButton` docked centre.

**Insets:** consume `WindowInsets.safeDrawing`. Edge-to-edge is mandatory on Android 15+ anyway — do it deliberately rather than discovering it in a release build.

### 6.2 Top bar camera indicator

An `AssistChip` on the right of the app bar, tappable, opening the camera sheet.

| State | Container | Content | Motion |
|---|---|---|---|
| `NoUsbHost` | `surfaceContainerHighest` | "No USB" | none |
| `Disconnected` | `surfaceContainerHigh` | "Connect" | none |
| `Connecting` | `tertiaryContainer` | `LoadingIndicator` | Expressive shape-morphing loader |
| `Connected` | `primaryContainer` | model name, e.g. "X100VI" | one-shot spring scale on entry |
| `Writing` | `primaryContainer` | "Writing…" + tiny wavy bar | continuous |
| `Error` | `errorContainer` | "Error" | short shake |

The connected transition is the app's signature moment. It is the thing that tells you the tool is ready to work, and it should feel good. Give it a real spring, not a fade.

### 6.3 Bottom navigation

Three targets. The centre is not a nav destination — it is a docked FAB that navigates to create.

| Slot | Destination | Icon |
|---|---|---|
| Left | `list` | `Icons.AutoMirrored.Filled.List` |
| Centre FAB | `recipe/new` | `Icons.Filled.Add` |
| Right | `more` | `Icons.Filled.MoreHoriz` |

The FAB stays visible on the list and more screens, hides on create/edit/view. Use `FloatingActionButton` with `MaterialShapes`-derived shape rather than a plain circle — a subtly squircled FAB reads as considered.

### 6.4 Routes

```
list
recipe/new
recipe/{id}          ← view
recipe/{id}/edit
more
more/export
more/import
more/camera-help
more/about
```

Deep link `fujirecipes://recipe/{id}` on the view route, so exported recipe cards can link back.

---

## 7. Screens

Field semantics, ranges, and applicability rules for every screen below come from `specs/shared/field-definitions.md`. What follows is layout and interaction.

### 7.1 `list` — recipe list

`LazyColumn` of recipe cards. Card at `surfaceContainerLow`, 20dp corners, no elevation — separation comes from spacing and the warm background, not shadows.

**Card anatomy:**

```
┌────────────────────────────────────────────┐
│ ▊  Kodachrome 64              ★★★★★        │  titleLarge, serif
│ ▊  Classic Chrome · X-Trans V              │  bodyMedium
│ ▊  [street] [warm] [+2]                    │  tag chips
│ ▊  Last written C3 · 2 days ago            │  labelSmall
└────────────────────────────────────────────┘
   ↑ 4dp film-sim colour bar, full card height
```

The colour bar on the leading edge is the fastest way to scan a long list — you learn the sims by colour within a week.

- **Tap** → view screen, with a shared-element transition on the recipe name (Compose `SharedTransitionLayout`). Cheap, and it makes the list-to-detail relationship legible.
- **Long press** → `ModalBottomSheet` with View, Edit, Duplicate, Write to camera, Export, Delete.
- **Swipe** → `SwipeToDismissBox`, end-to-start reveals Delete with undo `Snackbar`. Do not use swipe for anything but delete; multi-action swipe is undiscoverable.

**Top of list:** `SearchBar` (M3, expanding), then a horizontally scrolling `FilterChip` row for tags, then a sort `ButtonGroup`.

**Reorder mode:** toggled from the app bar overflow. Switches the list to `ReorderableLazyColumn` with a drag handle **and** up/down arrows on each card. Arrows are the primary mechanism — dragging inside a scrolling list on a phone is genuinely bad and everyone pretends otherwise.

**Empty state:** serif `headlineMediumEmphasized` "No recipes yet", a line of body text, and two buttons — *Create one* and *Import*. Set on a generous amount of empty space; do not fill it with an illustration.

### 7.2 `recipe/new` and `recipe/{id}/edit`

One `RecipeFormScreen` composable, one `RecipeEditViewModel`, `id` nullable.

**Structure:** a `LazyColumn` of expandable sections rather than a tabbed form. Sections and fields come from `RecipeFields.kt` (§9) — the single-source rule.

**Field controls:**

| Field type | Control |
|---|---|
| Enum, ≤4 options | `SingleChoiceSegmentedButtonRow` |
| Enum, >4 options | `ExposedDropdownMenuBox` |
| Film simulation | Full-width `LazyVerticalGrid` of swatch tiles, 3 across — a dropdown of 20 sims is the wrong control for the most important field in the app |
| Signed numeric | `Slider` with `steps`, value shown in tabular figures above the thumb |
| Colour temperature | `Slider` 2500–10000 in 100K steps + `OutlinedTextField` |
| WB shift R/B | 2-axis pad, drag to set, with numeric readouts — this maps directly to the camera's own UI |
| Tags | `FlowRow` of input chips + autocomplete field |
| Rating | `Row` of five tappable star icons, `tertiary` colour |

**Bottom bar:** `HorizontalFloatingToolbar` pinned above the navigation bar with Cancel · Save · Save and write. Expressive's floating toolbar is a better fit here than a stuck-to-the-edge button row, and it keeps the last form field from hiding behind it.

**Unsaved changes:** `BackHandler` intercepts, shows an `AlertDialog`.

**State restoration:** the form must survive process death. Hold the draft in `SavedStateHandle`, not just the ViewModel. Losing twenty fields of tuning because Android killed the app while you checked something on the camera is unacceptable.

### 7.3 `recipe/{id}` — view screen

A dedicated read-only route, not a modal. It is linkable, survives a refresh, back works as expected, and the content can breathe. The full spec:

- `LargeTopAppBar` with the recipe name in serif, collapsing to a small bar on scroll
- Header block: film-sim swatch, sim name, sensor generation, star rating (**tappable, saves immediately**), tag chips (**editable inline**)
- Setting groups as `Card`s at `surfaceContainerLow`, label left in `onSurfaceVariant`, value right in `onSurface` with tabular figures
- Fields at their default value render at 60% alpha; a "Show only changed" `ToggleSwitch` sits in the settings block header and persists to `DataStore` preferences
- Inapplicable fields omitted entirely, never shown as "N/A"
- `HorizontalFloatingToolbar` at the bottom: **Write to camera** as the filled primary action, Edit as an outlined button, overflow `⋯` for Duplicate / Export / Copy as text / Delete
- **Copy as text** puts a plain-text rendering on the clipboard — for pasting into a message when someone asks what you shot
- Horizontal swipe navigates to the previous / next recipe in the current filtered order, with a slide transition

**Not found state:** reachable via deep link to a deleted recipe. Show it properly, with a button back to the list.

### 7.4 Write-to-camera flow

A `ModalBottomSheet`, expanding through stages rather than a stack of dialogs.

**Stage 1 — connection.** If disconnected, a Connect button inline. If the app was launched by the USB attach intent, this stage is skipped entirely.

**Stage 2 — compatibility.** If the recipe's generation exceeds the camera's, a `Card` in `errorContainer` listing exactly which fields will be dropped, with *Write anyway* and *Cancel*.

**Stage 3 — slot picker.** Seven large targets in a `ButtonGroup`, C1…C7. Expressive's button group gives the neighbouring-button squeeze animation on press, which makes a row of seven identical buttons feel physical rather than abstract. Each shows the last recipe this app wrote to it, or "Unknown".

**Stage 4 — confirm.** A second tap on a slot with known contents.

**Stage 5 — progress.** `LinearWavyProgressIndicator`, with `n / total properties` in tabular figures and the current property name below. Sheet is non-dismissible; back is intercepted with a cancel confirmation.

**Stage 6 — result.** Success: the wavy bar collapses into a check with a shape morph, `headlineMediumEmphasized` "Written to C3", haptic confirm. Failure: the failing property name and PTP response code, plus a Retry button.

**Haptics:** `HapticFeedbackType.Confirm` on success, `Reject` on failure. This matters more than it sounds — you will often be looking at the camera, not the phone, when the write lands.

### 7.5 `more`

A `LazyColumn` of `ListItem`s: Export, Import, Camera help, About. Camera help is a static screen with the USB-mode instructions and troubleshooting, written as prose with real headings — this is the screen you will send other people to.

### 7.6 `more/export`

1. Selection list with `Checkbox`es, select all / none in the app bar
2. `SingleChoiceSegmentedButtonRow`: Single JSON · ZIP of JSONs
3. Export button → `ActivityResultContracts.CreateDocument` → writes via `ContentResolver`

Use the Storage Access Framework. Do not write to app-external directories and do not request `WRITE_EXTERNAL_STORAGE`.

### 7.7 `more/import`

1. `ActivityResultContracts.OpenDocument` with MIME filter `application/json` and `application/zip`
2. Parse off the main thread; ZIP via `java.util.zip.ZipInputStream` (no dependency needed)
3. Validate each recipe against the shared schema
4. Review list: one row per recipe with status — valid / name conflict / invalid, with the specific failing field named
5. Conflict resolution per row (Skip / Replace / Keep both) plus a bulk action bar
6. Import → Room transaction → summary

---

## 8. Camera integration

### 8.1 USB Host API

```kotlin
val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
val device = manager.deviceList.values.firstOrNull { it.vendorId == 0x04CB }
// permission via PendingIntent, then:
val connection = manager.openDevice(device)
connection.claimInterface(stillImageInterface, true)
// bulkTransfer on the in/out endpoints
```

PTP framing is implemented in pure Kotlin — container header, operation/data/response phases, transaction IDs. Roughly 400–600 lines. Do not try to NDK-build libgphoto2 for this; the surface you need is small and the cross-compile is not worth it.

**Protocol reference:** `eggricesoy/filmkit` on GitHub — its `QUICK_REFERENCE.md` is the closest thing to a public specification that exists. Cross-check against the `fujihack` wiki's PTP/USB page. Preset parameters live in the property range `0xD18E`–`0xD1A5`, manipulated with the standard `GetDevicePropValue` and `SetDevicePropValue` operations — the same surface the manufacturer's own RAW-conversion software uses. Read it before writing a byte (C3).

### 8.2 USB attach intent — the feature that justifies going native

`AndroidManifest.xml`:

```xml
<intent-filter>
  <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
<meta-data
  android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
  android:resource="@xml/device_filter" />
```

`res/xml/device_filter.xml` — note vendor-id is **decimal**, `0x04CB` = `1227`:

```xml
<resources>
  <usb-device vendor-id="1227" />
</resources>
```

Also declare `<uses-feature android:name="android.hardware.usb.host" android:required="true" />`.

Launching this way grants USB permission implicitly for that connection — no permission dialog. Plug in the camera, the app opens, the indicator is already green. That is the whole experience.

### 8.3 Write ordering and encoding rules

Full rules in `specs/shared/field-definitions.md` §6. The five that will bite:

| Rule | Behaviour |
|---|---|
| White balance mode before colour temperature | The camera rejects `colorTemperature` unless WB mode is already Color Temp. Write mode first, always. |
| Monochrome rejects colour | For any Acros / Monochrome / Sepia simulation, skip the `color` property entirely. The camera rejects it. |
| High ISO NR encoding | Proprietary and non-linear. Do not assume the range maps linearly to the wire value — use the lookup table. |
| Grain size depends on grain effect | Skip `grainSize` when `grainEffect` is off. |
| Generation gating | X-Trans III and below do not implement the slot registers. Detect and refuse rather than half-writing. |

**Safety note:** the firmware validates property values and rejects out-of-range or malformed writes rather than accepting them, which makes experimentation reasonably safe. Treat that as a backstop, not a substitute for validation.

Implement as a `WritePlan` data class produced by a pure function `buildWritePlan(recipe, cameraInfo): WritePlan`. Pure means unit-testable without a camera attached, which you will be grateful for.

### 8.4 Connection lifecycle

`CameraController` is a singleton exposing `StateFlow<CameraState>`:

```kotlin
sealed interface CameraState {
  data object NoUsbHost : CameraState
  data object Disconnected : CameraState
  data object Connecting : CameraState
  data class Connected(val model: String, val generation: SensorGeneration) : CameraState
  data class Writing(val slot: Int, val done: Int, val total: Int, val current: String) : CameraState
  data class Error(val message: String, val ptpCode: Int?) : CameraState
}
```

- Register a `BroadcastReceiver` for `ACTION_USB_DEVICE_DETACHED`; unplug mid-write must abort cleanly
- Writes run in a `viewModelScope` coroutine on `Dispatchers.IO`
- **Hold a partial wake lock during a write.** Writes take seconds, but a screen timeout mid-transfer with a doze kicking in is a bad way to leave a slot half-written
- No foreground service needed — the operation is short and user-initiated

### 8.5 Android-specific gotchas

| Issue | Handling |
|---|---|
| Phone has one USB-C port | The camera occupies it. No cable debugging while the camera is attached — **set up wireless ADB before you start** (`adb pair`, Android 11+) |
| Phone tries to power the camera | Some bodies will charge from the phone and drain it. Note in camera help; nothing the app can do |
| Another app grabs the device | `claimInterface(force = true)`, and if it still fails, name the likely culprit in the error |
| OEM USB host quirks | Some budget devices ship without host mode. `packageManager.hasSystemFeature(FEATURE_USB_HOST)` → `NoUsbHost` state |
| Permission revoked on detach | USB permission does not persist across replug unless granted via the attach intent. Handle re-request silently |

---

## 9. Shared field definitions

`RecipeFields.kt` — one object defining, per field: group, label, type, range, step, default, and an applicability predicate over `(SensorGeneration, FilmSimulation)`.

Consumers: the form (§7.2), the view screen (§7.3), the write plan builder (§8.3), the "copy as text" renderer, the import validator. Adding a parameter for a future camera means editing one file.

Settings fields, ranges, defaults, and applicability rules: `specs/shared/field-definitions.md` §4.

---

## 10. Data model

### 10.1 Room

```kotlin
@Entity(tableName = "recipes")
data class RecipeEntity(
  @PrimaryKey val id: String,          // UUID
  val name: String,
  val notes: String,
  val rating: Int,                     // 0–5
  val tags: List<String>,              // TypeConverter → JSON string
  val sortKey: Double,                 // fractional ordering
  val sensorGeneration: String,
  val settings: RecipeSettings,        // @Embedded or TypeConverter → JSON
  val createdAt: Long,
  val updatedAt: Long,
  val lastWrittenSlot: Int?,
  val lastWrittenAt: Long?,
)
```

Indices on `sortKey`, `rating`, `updatedAt`. Tags stored as a JSON string via `TypeConverter` — SQLite has no array type, and a separate join table is over-engineering for a personal library. Filter tags in memory.

**Fractional sort keys.** On create, `max + 1000`. On move, the midpoint of the new neighbours. To the top, `first − 1000`; to the bottom, `last + 1000`. An integer position column would rewrite every row per reorder; this touches one.

**Rebalance:** when a computed gap falls below `0.0001`, renumber every row to `index * 1000` in one transaction. This will never fire on a hand-curated library, but the failure mode without a guard is baffling, so write the guard.

**Migrations:** write them from schema version 1. `exportSchema = true`, commit the JSON schemas. Losing a recipe library to a lazy `fallbackToDestructiveMigration()` is a self-inflicted wound.

### 10.2 Import/export format

Full specification: `specs/shared/recipe-format.spec.md`. Summary:

```json
{
  "format": "fuji-recipe",
  "version": 1,
  "exportedAt": "2026-08-04T10:00:00.000Z",
  "recipes": [ { "id": "...", "name": "...", "settings": { } } ]
}
```

Import also accepts a bare array and a bare single object. `sortKey`, `lastWrittenSlot`, and `lastWrittenAt` are excluded from export — local bookkeeping, not recipe content. Unknown properties are preserved on import and re-emitted on export, so a file that passes through an older build does not lose data from a newer one.

Because export is the only backup (C2), the conformance fixtures in `specs/shared/recipe-format.spec.md` §6 are a release gate, not a nice-to-have.

---

## 11. Stack

| Concern | Choice |
|---|---|
| Language | Kotlin, JDK 17 |
| UI | Jetpack Compose, `androidx.compose.material3` — the version exposing `MaterialExpressiveTheme`, `LinearWavyProgressIndicator`, `ButtonGroup`, `LoadingIndicator`, `HorizontalFloatingToolbar` |
| Navigation | Navigation Compose, type-safe routes |
| DI | Hilt |
| Async | Coroutines + Flow |
| Local DB | Room |
| Preferences | DataStore (Preferences) — theme mode, show-only-changed toggle |
| JSON | `kotlinx.serialization` |
| ZIP | `java.util.zip`, stdlib |
| USB | `android.hardware.usb`, plus hand-rolled PTP |
| Fonts | Source Serif 4 + Inter, bundled as resources |
| Testing | JUnit + Turbine for flows, Compose UI tests for the form, `buildWritePlan` unit tests as the highest-value suite |
| Min SDK | 29 · **Target SDK** latest |

`MaterialExpressiveTheme` wraps `CappuccinoColorScheme`, `CappuccinoTypography`, `CappuccinoShapes`, and `MotionScheme.expressive()`.

---

## 12. Build order

1. **Project + theme.** Compose scaffold, `CappuccinoTheme` with both schemes, fonts bundled, a theme preview screen showing every token. *Deliverable: the app looks right before it does anything.*
2. **Room + repository.** Entity, DAO, TypeConverters, migrations, seed data. *Deliverable: data persists across launches.*
3. **Shell.** Scaffold, top bar, navigation bar, FAB, empty routes. *Deliverable: navigable app.*
4. **`RecipeFields.kt`.** Groups, labels, ranges, applicability. *Deliverable: nothing visible, everything downstream depends on it.*
5. **List screen.** Cards, search, filter chips, sort, swipe-delete, empty state.
6. **Form screen.** All controls, conditional visibility, `SavedStateHandle` draft, create and edit.
7. **View screen.** Read-only render, inline rating and tags, show-only-changed, shared-element transition.
8. **Reorder.** Fractional keys, arrows and drag.
9. **Export.** SAF, JSON and ZIP.
10. **Import.** SAF, parse, validate, conflict review.
11. **USB connect.** Host API, permission, attach intent, `device_filter.xml`, PTP session, `GetDeviceInfo`, model detection, indicator states.
12. **USB write.** Property encoding, write plan, slot picker, wavy progress, haptics, error handling.
13. **Polish.** Motion timing, haptics, reduced-motion fallback, dark scheme audit, predictive back.

Steps 1–10 have zero protocol risk and ship a usable recipe manager. **[same reasoning as web]** — the USB work is deliberately last so a hard slog there does not block having a working tool.

---

## 13. Error handling

Every message names what failed and what the user can do about it. No "Something went wrong."

| Failure | Surface |
|---|---|
| No USB host support | Permanent state in the indicator + explanation in camera help |
| Permission denied | `Snackbar` with a Retry action |
| Device claim failed | `AlertDialog` naming the likely conflicting app |
| Unsupported camera generation | Blocking `Card` in the write sheet, write disabled |
| PTP write rejected | Write sheet failure stage with property name and code, Retry |
| Unplug mid-write | Sheet jumps to failure, warns about partial slot contents |
| Room migration failure | Should never ship; if it does, offer export-then-reset rather than silent data loss |
| Import file unreadable | Inline on the import screen, nothing committed |

---

## 14. Open questions

1. **When does Room-only stop being enough?** The trigger is a specific behaviour, not a date: you edit a recipe on the laptop and want it on the phone without exporting. Until that actually annoys you twice, do nothing. The migration path is written down in §4.1 and it stays cheap because of the two prep items listed there.
2. **Property availability per body** — dump the supported property list from your own camera and diff it against the assumed field set before building the form around it. This is the single highest-value hour in the project.
3. **Reading slots back** — would turn the slot picker's "Unknown" labels into real contents. Highest-value v1.1 feature.
4. **Font licensing** — Source Serif 4 and Inter are both OFL, fine to bundle. If you swap either, check the licence before shipping to a store.
5. **Do you want this on the Play Store at all?** If it stays a sideloaded personal APK, you can skip target-SDK churn, data-safety declarations, and store listing work entirely. Recommended.
