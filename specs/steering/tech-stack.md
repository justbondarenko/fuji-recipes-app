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
| Material 3 Expressive | The `material3` artifact version exposing `MaterialExpressiveTheme`, `LinearWavyProgressIndicator`, `ButtonGroup`, `LoadingIndicator`, `HorizontalFloatingToolbar`. **Verify availability and pin the exact version in FEAT-000** — do not assume; these APIs move between alphas. | The Expressive vocabulary is a product requirement, not decoration |
| Navigation | Navigation Compose, type-safe routes | — |
| DI | Hilt | — |
| Async | Coroutines + Flow | — |
| HTTP | **OkHttp** + `kotlinx.serialization` directly. No Retrofit, no Ktor. | Six endpoints. A `Retrofit` interface plus a converter factory plus an OkHttp client is three dependencies and an annotation layer over what is ~80 lines of `Request.Builder`. Revisit if the route count passes ~15. |
| JSON | `kotlinx.serialization` | Handles `JsonObject` passthrough for `settings` and `extra`, which is the requirement that rules out Gson |
| Preferences | DataStore (Preferences) | Connection settings, remembered filters and sort |
| Snapshot cache | `java.io.File` + `kotlinx.serialization` | It is one file holding one response verbatim. A database for that is ceremony. |
| Images | Bundled `drawable` resources | The 20 film-simulation swatches are fixed assets copied from `fuji-recipes-book/src/public/film-simulations/`. Nothing is loaded from a URL, so no Coil, no Glide. |
| ZIP | `java.util.zip`, stdlib | Import/export only — out of v1 scope |
| USB | `android.hardware.usb` + hand-rolled PTP | libgphoto2 via NDK is a cross-compile for a surface that is a few hundred lines of Kotlin |
| Fonts | Lora + Inter, bundled in `res/font` | Downloadable fonts cause a first-launch flash and need Play Services |
| Testing (JVM) | JUnit 5 + `kotlin.test`, Turbine for Flow | The pure list-selection pipeline and `buildWritePlan` are the highest-value suites |
| Testing (UI) | `androidx.compose.ui.test` | List states: loading, empty, no-match, error, populated |
| HTTP test double | OkHttp `MockWebServer` | Contract tests against recorded envelopes, no network |

## 3. Explicitly not approved

| Not using | Because |
|---|---|
| Room | The server owns the data (`architecture.md` §4). A local relational mirror implies a sync engine that v1 does not need. |
| Retrofit / Ktor client | See HTTP above |
| Firebase (any) | No accounts, no analytics, no crash reporting in v1. Adding one is a privacy decision, not a convenience one. |
| `androidx.security:security-crypto` | Deprecated by Jetpack. App-private DataStore with `allowBackup="false"` is the v1 bound, stated in `architecture.md` §5. |
| Material You / dynamic colour | The palette is parity with the web client. `dynamicLightColorScheme()` would replace it with wallpaper colours. |
| Coil / Glide / Picasso | Nothing loads a remote image |
| Any analytics or telemetry SDK | Single user, no product questions to answer |
| Accompanist | Everything still used from it has a first-party equivalent at this min SDK |

## 4. Permissions

| Permission | Needed by | Notes |
|---|---|---|
| `android.permission.INTERNET` | Everything from FEAT-001 | The v1 architecture is server-backed, so this is required — unlike the earlier local-only draft in `PRD.md` §4.1, which is superseded |
| `<uses-feature android:name="android.hardware.usb.host" android:required="true" />` | FEAT-003 | Declared required: a device without USB host cannot do the thing the app exists for |
| No storage permissions | — | Import/export, when it lands, uses the Storage Access Framework |

`android:allowBackup="false"` in the manifest, because the service token lives in app-private
DataStore.

## 5. Verification gates

Each of these must pass before a feature is called done:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Instrumented tests (`connectedDebugAndroidTest`) run on an emulator, because **C1** means a
physical device is unreachable over USB whenever the camera is attached. Wireless ADB is
configured in FEAT-000.
