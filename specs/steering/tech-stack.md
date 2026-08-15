# Tech stack — Android client

**Status:** binding
**Version:** 1.0

A spec may not propose a library absent from this document. Adding one requires a line in
the table below with a reason, in the same commit as the spec that needs it.

---

## 1. Platform

| | |
|---|---|
| Language | Kotlin, JDK 17 |
| Min SDK | 29 (Android 10) |
| Target SDK | latest stable at project setup |
| Build | Gradle with the version catalog in `gradle/libs.versions.toml` — no hardcoded versions in `build.gradle.kts` |
| Architecture | single activity, Jetpack Compose, MVVM with `StateFlow` |

## 2. Approved dependencies

| Concern | Choice | Why this and not the obvious alternative |
|---|---|---|
| UI | Jetpack Compose + `androidx.compose.material3` | — |
| Material 3 Expressive | **Not reachable on this toolchain — see §6.** `material3` is pinned at 1.4.0 via `compose-bom` 2026.06.01, where the entire Expressive surface is `internal`. Colour, type and shape are unaffected; motion and four components are. | The Expressive vocabulary is a product requirement, and §6 records what it costs to get it |
| Navigation | Navigation Compose, type-safe routes | — |
| Async | Coroutines + Flow | — |
| HTTP | **None.** No OkHttp, no Retrofit, no Ktor, and no `INTERNET` permission. | The app is offline: the library is a file on the device and recipes move by export and import (`architecture.md` §4). A dependency that could open a socket is one more thing that could. |
| JSON | `kotlinx.serialization` | Handles `JsonObject` passthrough for `settings` and `extra`, which is the requirement that rules out Gson |
| Preferences | DataStore (Preferences) | Remembered filters and sort |
| Library store | `java.io.File` + `kotlinx.serialization` | Tens of recipes, loaded whole and filtered in memory. Room would buy queries nothing asks and a migration story for one file. |
| Images | Bundled `drawable` resources | The 20 film-simulation swatches are fixed assets copied from `fuji-recipes-book/src/public/film-simulations/`. Nothing is loaded from a URL, so no Coil, no Glide. |
| ZIP | `java.util.zip`, stdlib | Import/export only — out of v1 scope |
| USB | `android.hardware.usb` + hand-rolled PTP | libgphoto2 via NDK is a cross-compile for a surface that is a few hundred lines of Kotlin |
| Fonts | Lora + Inter, bundled in `res/font` | Downloadable fonts cause a first-launch flash and need Play Services |
| DI | **None — a hand-written `AppContainer`** | The graph is six objects with no cycles and one scope. Hilt costs a Gradle plugin, an annotation processor and a Kotlin/KSP version alignment to maintain, for wiring that fits on one screen. Revisit if the graph grows scopes. |
| Testing (JVM) | JUnit 4 + `kotlin.test`, Turbine for Flow | JUnit 5 on Android needs a third-party Gradle plugin; JUnit 4 is what AGP runs out of the box. The pure list-selection pipeline and `buildWritePlan` are the highest-value suites |
| Testing (UI) | `androidx.compose.ui.test` | List states: loading, empty, no-match, error, populated |
| Store test double | JUnit's `TemporaryFolder` | The store is a real file in the tests, because "the bytes on disk survive a restart" is the property worth checking |

## 3. Explicitly not approved

| Not using | Because |
|---|---|
| Hilt / Dagger / Koin | See DI above |
| `material-icons-extended` | Several thousand vectors for the three this app uses — it put 31 MB into the debug APK. `material-icons-core` plus one local vector drawable covers it |
| Room | The library is one JSON file loaded whole (`architecture.md` §4). Nothing queries it, so a relational store would be schema and migrations for no reader. |
| OkHttp / Retrofit / Ktor | See HTTP above. There is nothing to talk to. |
| Firebase (any) | No accounts, no analytics, no crash reporting in v1. Adding one is a privacy decision, not a convenience one. |
| `androidx.security:security-crypto` | Deprecated by Jetpack, and there is no secret left to hold: app-private storage with `allowBackup="false"` is the bound, stated in `architecture.md` §5. |
| Material You / dynamic colour | The palette is parity with the web client. `dynamicLightColorScheme()` would replace it with wallpaper colours. |
| Coil / Glide / Picasso | Nothing loads a remote image |
| Any analytics or telemetry SDK | Single user, no product questions to answer |
| Accompanist | Everything still used from it has a first-party equivalent at this min SDK |

## 4. Permissions

| Permission | Needed by | Notes |
|---|---|---|
| **No `android.permission.INTERNET`** | — | Deliberate, and load-bearing. The app has nothing to reach, and its absence makes "your recipes stay on this phone" a claim the manifest enforces rather than a promise the code makes |
| `android.permission.WAKE_LOCK` | FEAT-006 | Held for the duration of a camera write and no longer |
| `<uses-feature android:name="android.hardware.usb.host" android:required="true" />` | FEAT-005 | Declared required: a device without USB host cannot do the thing the app exists for |
| No storage permissions | — | Export shares through a `FileProvider`; import reads through the Storage Access Framework |

`android:allowBackup="false"` in the manifest. It was there for the service token; it stays
for the library, which is the only copy of the user's recipes and is not something to hand to
a cloud backup by default.

## 5. Verification gates

Each of these must pass before a feature is called done:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Instrumented tests (`connectedDebugAndroidTest`) run on an emulator, because **C1** means a
physical device is unreachable over USB whenever the camera is attached. Wireless ADB is
configured in FEAT-000.


## 6. Material 3 Expressive — blocked on the toolchain

Established by compiling against it, not by reading release notes.

At `material3` **1.4.0** — the newest release that resolves under AGP 8.13 — every Expressive
entry point is `internal` or absent: `MaterialExpressiveTheme`, `MotionScheme`,
`ExperimentalMaterial3ExpressiveApi`, `LinearWavyProgressIndicator`, `LoadingIndicator`,
`ButtonGroup`, `HorizontalFloatingToolbar`, `MaterialShapes`. None of them compile.

Reaching them requires this whole chain:

| Step | Consequence |
|---|---|
| `material3` 1.5.0-alpha26 | An alpha library under the app's most-used screens |
| pulls Compose 1.12.0-beta01 | A beta UI toolkit |
| which requires AGP 9.1+ | A major Gradle-plugin generation change |
| which requires `compileSdk` 37 | The `android-37` platform is **not installed** on this machine, and there are no `cmdline-tools` to install it with |
| and Android Studio must support AGP 9.1 | Installed: 2025.3 (`AI-253.31033.145`) — unverified against AGP 9.1 |

**Current position:** `FujiTheme` uses the standard `MaterialTheme`. The palette, the type
scale, the shape scale and the reduced-motion signal are all in place and are the parts of
`design-system.md` that carry the parity with the web client. What is missing is the
Expressive motion scheme and four components, and every screen that wants one of those is a
later feature — the wavy write-progress bar (FEAT-004), the C1–C7 button group (FEAT-004),
the form's floating toolbar (FEAT-002).

So this is not urgent, and it is not silently dropped: `ui/theme/Theme.kt` carries the swap
instructions in a `ponytail:` comment, and the decision is `specs/roadmap.md` §6.