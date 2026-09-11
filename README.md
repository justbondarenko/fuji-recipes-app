# 📷 Fuji Recipes (Android)

[![Platform: Android](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%E2%80%9337)-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose%20%7C%20Material%203%20Expressive-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Network)-009688.svg?style=flat)](https://github.com/justbondarenko/fuji-recipes-app)
[![USB PTP](https://img.shields.io/badge/Protocol-USB--C%20PTP%20Direct-FF5722.svg?style=flat)](https://github.com/justbondarenko/fuji-recipes-app)
[![License: MIT](https://img.shields.io/badge/License-MIT-607D8B.svg?style=flat)](LICENSE)

**Fuji Recipes** is a native, local-first Android companion app for Fujifilm camera owners. It lets you collect and edit film simulation recipes, write them directly to the camera's C1–C7 custom slots, browse and download JPEG/RAF files from the card, analyze camera JPEGs without copying them first, and use the camera's processor to develop RAW files from your phone. Everything works over USB-C, completely offline with zero cloud dependencies.

> [!NOTE]
> **About this project**: I am not a professional photographer — just a Fujifilm camera owner and developer with insomnia :D. This app was vibecoded first and foremost as a personal project for personal use, experimentation, and entertainment, but shared openly with the public because, why not! :)
>
> It is provided completely free and **"as is"**. While I cannot make any commercial promises or guarantees, I am very happy to receive feedback, collaborate with fellow Fuji owners, and update/fix issues as they arise!

---

## 📑 Table of Contents

- [📥 Installation](#-installation)
- [📱 Screenshots](#-screenshots)
- [🌟 Highlights](#-highlights)
- [📸 Camera Compatibility & Testing](#-camera-compatibility--testing)
- [🔌 Camera USB Modes & Workflows](#-camera-usb-modes--workflows)
  - [Connect via USB-C](#connect-via-usb-c)
  - [Browse and Download Photos](#browse-and-download-photos)
  - [Analyze JPEGs from the Camera](#analyze-jpegs-from-the-camera)
  - [Develop a RAW in the Camera](#develop-a-raw-in-the-camera)
  - [Custom Slots and Camera Tools](#custom-slots-and-camera-tools)
- [🧪 Testing & Feedback](#-testing--feedback)
- [🛠️ Building & Developing](#️-building--developing)
  - [Prerequisites](#prerequisites)
  - [Clone & Build](#clone--build)
  - [Sideload to Device](#sideload-to-device)
- [☕ Open Source Acknowledgements](#-open-source-acknowledgements)
- [📜 Legal Disclaimer & Trademarks](#-legal-disclaimer--trademarks)

---

## 📥 Installation

Grab the latest APK from the [Releases page](https://github.com/justbondarenko/fuji-recipes-app/releases) and install it on your phone.

> [!NOTE]
> **"App blocked to protect your device" / Play Protect warning**:
> Since this app is distributed directly via GitHub Releases rather than the Google Play Store, Android's **Play Protect** has no install history for it and will warn that it "hasn't seen this app from this developer before." This is expected for any app sideloaded outside Play — it's not a sign of malware, just a reputation check Play Protect can only build up through Play Store distribution.
>
> To install anyway: on the warning screen, tap **More details** → **Install anyway**.

### Not sure it's safe? Ask an AI to check for you

You don't have to take my word for it. Point any AI coding assistant (Claude, ChatGPT, Gemini, etc.) at this repository and ask it to independently verify the app's behavior — the source code is public, so it can check for itself rather than trusting this README.

Example prompt:

```
Review the source code at https://github.com/justbondarenko/fuji-recipes-app. I'm about to sideload the released APK and want an independent safety check before I do.

First, confirm you actually fetched and read files from that URL — don't guess. If you can't browse live URLs, say so explicitly instead of assuming the repo is empty or has no source code, and tell me what you'd need me to paste instead (e.g. AndroidManifest.xml, build.gradle.kts).

Once you've confirmed access, answer:

1. Does the app request or use INTERNET or any network-related permission? Search the manifest and codebase for any networking code (HTTP clients, sockets, analytics/crash-reporting SDKs, ad SDKs).
2. What permissions does AndroidManifest.xml declare, and is each one justified by what the app actually does (e.g. USB access for the camera connection, storage for saving recipes)?
3. Does the app collect, log, or transmit any personal data, camera contents, or device identifiers anywhere?
4. Are there any obfuscated, minified, or suspicious code paths that don't match what's described in the README?
5. Overall, is there any risk to me in installing this app — to my data, my device, or my camera — and if so, what exactly is it?

Give me a plain verdict: is this app safe to install, based only on what's in the repository?
```

Because the app is 100% offline with zero network access, any AI that actually reads the source will confirm there's no code path capable of sending data anywhere — there's simply no networking code to send it with.

> [!WARNING]
> **Some AI chat tools can't actually browse a URL you give them.** When that happens, instead of saying "I can't access this," they can confidently claim the repo has no source code, no manifest, etc. — which is *wrong*, not a real finding. The source is genuinely public (`app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, and the rest of `app/src/`). In practice, Claude and ChatGPT seem to actually fetch and explore the repository, while Gemini has been observed just implying/guessing at an answer instead of checking. If you get a "no code found" response, either enable that AI's live browsing/search mode and retry, or `git clone` the repo yourself and paste the relevant files directly into the chat.

---

## 📱 Screenshots

See the app in action: **[screenshot tour](screenshots/README.md)** — the recipe library, recipe views, photo analysis, camera sync, custom slots, downloads, and clean-up, each screen explained.

---

## 🌟 Highlights

- ⚡ **[Direct USB-C Camera Sync](screenshots/README.md#-camera-connection--custom-slots)**: Connect your camera to your phone via USB-C. The app launches automatically on connection, reads your current `C1`–`C7` custom slot states, and writes full recipe parameter sets directly to the camera body in seconds.
- 📥 **[Import Directly from Camera](screenshots/README.md#-maintenance--tools)**: Read existing custom slot recipes off the camera body and save them straight into your offline phone library.
- 📷 **[Browse and Download the Camera Card](screenshots/README.md#-camera-connection--custom-slots)**: The dedicated **Photos** tab lists JPEG and RAF files independently, with thumbnails, capture dates, file sizes, RAW/JPEG filters, multi-selection, and batch download to a folder chosen through Android's system picker. The camera card is read-only; the app does not delete, rename, or move its files.
- 🔎 **[Analyze JPEGs Straight from the Camera](screenshots/README.md#-analysing-photos)**: Open **Analyze**, choose photos from the connected camera, and run the existing Fujifilm EXIF extraction and recipe matching flow without first importing the files through another gallery app.
- 🎞️ **[In-Camera RAW Development](screenshots/README.md#-developing-a-raw-in-the-camera)**: Start from a recipe, choose a RAF from the phone, apply the recipe to the camera's native `0xD185` conversion profile, let the camera render the JPEG, preview it, and save it through Android's document picker. The app preserves camera-native profile fields it does not own.
- 🔄 **[Background Camera Downloads](screenshots/README.md#-camera-connection--custom-slots)**: Batch downloads run in an Android connected-device foreground service and continue while the app is minimized or the phone is locked. An ongoing notification shows progress and offers cancellation; Android 16 can promote it to a Live Update. If the process is killed, the Photos screen reports the interrupted batch and lets the user keep completed files or remove the incomplete file.
- 🔋 **[Mode-Aware Camera Status](screenshots/README.md#-camera-connection--custom-slots)**: The Camera screen reports the current connection mode, camera battery level, firmware and serial number, plus shutter count and lens when the body exposes them. Slot controls appear only in a mode that can reach them.
- 🧰 **Camera Diagnostics**: Share a detailed camera capability report. This makes it possible to investigate untested bodies without pretending they are compatible.
- 📸 **[Extract Recipe from Photos](screenshots/README.md#-analysing-photos)**: Pick straight-out-of-camera Fujifilm JPEGs (single or multiple in batch) to decode their embedded MakerNote EXIF metadata. The app extracts the exact film simulation, tone curves, and white balance settings, presents them in a swipeable card carousel, and lets you attach photos directly to matching recipes or save new ones.
- 📲 **System Share Sheet Action**: Share any photo directly from Google Photos, Gallery, or Files via the "Extract recipe" share sheet action to inspect recipe settings on the fly.
- 🔍 **[Highlight Matching & Likely Recipes](screenshots/README.md#-analysing-photos)**: When analyzing photos, the app compares decoded parameters against your entire library, highlighting exact matches or surfacing likely recipe candidates with percentage similarity and specific differences.
- 🧹 **[Duplicate Detection & Clean-Up](screenshots/README.md#-maintenance--tools)**: Intelligent review flow detects duplicate and conflicting recipes during camera or file imports, allowing you to easily resolve collisions, replace older versions, or skip duplicates to keep your library clean.
- 📝 **[Create from Pasted Text](screenshots/README.md#-creating-recipes)**: Copy recipe text from websites (like *Fuji X Weekly*), forums, or notes. The built-in parser automatically identifies parameters and pre-fills the recipe editor.
- ✅ **[Batch Edits in the Library](screenshots/README.md#-the-recipe-library)**: Long-press any recipe to start selecting, then delete the whole selection or set one rating across it from the floating toolbar. Each batch is a single write to the library rather than one per recipe.
- ⚖️ **[Side-by-Side Recipe Comparison](screenshots/README.md#-viewing-a-recipe)**: Compare any two recipes in your library to inspect exact parameter differences side by side.
- 🗂️ **[Comprehensive 27-Parameter Engine](screenshots/README.md#-viewing-a-recipe)**: Full support for Fujifilm recipe parameters across sensor generations — Film Simulations (Provia to Reala Ace), Grain Size/Effect, Color Chrome FX & FX Blue, Smooth Skin, Highlight/Shadow tone curves (0.5 steps), Clarity, and 2D White Balance shift with Kelvin temperature.
- 🧭 **[Tools Under More, Settings on Every Screen](screenshots/README.md#-maintenance--tools)**: The bottom bar's last item is **More** — maintenance, backup and restore, and about. Display preferences moved to their own **Settings** page, reached by the round settings button each screen carries (in the search row on the recipe list).
- 🔒 **100% Offline & Private**: Declares **zero `INTERNET` permissions**. Your recipes live exclusively in app-private storage on your phone (`filesDir/library.json`). No accounts, no cloud sync, and no tracking.
- 📦 **[Lossless Export & Import](screenshots/README.md#-maintenance--tools)**: Export your library as a single `.json` file or a `.zip` archive using Android's Storage Access Framework and system share sheet.
- 🎨 **Material 3 Expressive UI**: Built with Material You Dynamic Color adapting fluidly to your device theme, expressive spring motion, and dark/light mode support.

---

## 📸 Camera Compatibility & Testing

Camera communication is handled via PTP (Picture Transfer Protocol) over USB Host mode.

The camera features in this branch were developed against and manually tested with a **Fujifilm X-T50**. Compatibility is feature-specific: standard PTP media operations may exist on other bodies, but custom slots, camera properties, and RAW profile layouts are Fuji-specific and can differ by model or firmware.

| Feature | Required camera mode | Verified support |
|---|---|---|
| Browse, analyze, and download JPEG/RAF files | `USB CARD READER` | X-T50 hardware |
| Read/write C1–C7, reports | `USB RAW CONV. / BACKUP RESTORE` | X-T50 hardware |
| In-camera RAW development (RAF from the phone) | `USB RAW CONV. / BACKUP RESTORE` | X-T50 hardware; an X100VI 625-byte profile adapter is present but not hardware-tested by the author |
| Background downloads and Live Updates | `USB CARD READER` | Android foreground service; promoted Live Update requires Android 16 and system approval |

The compatibility list below describes **custom-slot support**, not a blanket guarantee for every camera feature:

* 🟢 **Tested & Verified on Hardware**: **Fujifilm X-T50** (the only body I personally own)
* 🟡 **Expected custom-slot support (untested on hardware — feedback welcome!)**:
  * **X-Trans V bodies** (*X-T5, X100VI, X-H2, X-H2S, X-S20, X-M5, X-E5*)
  * **X-Trans IV bodies** (*X-T4, X-T3, X-T30, X-T30 II, X-Pro3, X-S10, X-E4, X100V*)
  * **GFX 100-series** (*GFX100 II, GFX100, GFX100S, GFX100S II*)
* 🔴 **Known Hardware Limitations (Custom slot writing not supported by camera firmware)**:
  * **X-Trans III & older** (*X-Pro2, X-T2, X-T20, X-E3, X-H1, X100F*)
  * **Bayer CMOS & GFX 50-series** (*X-T100, X-T200, X-A series, GFX 50S, GFX 50R, GFX50S II, XF10*)
  *(These camera bodies lack custom slot write registers in their firmware; recipe library management and photo EXIF extraction still work normally.)*

> 💬 **Feedback & Collaboration**: If you own an untested Fujifilm camera body and would like to help verify compatibility or report issues, please [open an issue on GitHub](https://github.com/justbondarenko/fuji-recipes-app/issues)! I'm happy to update and fix anything that comes up.

---

## 🔌 Camera USB Modes & Workflows

The app uses two camera connection modes because Fujifilm exposes card files and recipe/conversion properties through different USB interfaces.

| Camera mode | Use it for |
|---|---|
| **`USB CARD READER`** | Browse the card, download JPEG/RAF files, or analyze camera JPEGs |
| **`USB RAW CONV. / BACKUP RESTORE`** | Read/write C1–C7, inspect camera details, share a report, or render a RAF with a recipe |

On the camera, the setting is normally under **`MENU / OK` → `SET UP` (Wrench) → `CONNECTION SETTING` → `USB MODE`**. The app reports the detected mode without treating a useful Card Reader connection as an error. Screens hide controls that the current mode cannot support and explain which mode they need.

### Connect via USB-C

> 📱 See it: [camera connection screens](screenshots/README.md#-camera-connection--custom-slots)

- Connect a **USB-C to USB-C** cable between your Android phone and camera.
- The phone will prompt you to open **Fuji Recipes** automatically via the USB device attach intent.
- Allow USB access when Android asks. Notification permission is optional for the transfer itself, but it is needed to see background progress.
- The ongoing notification identifies the connected state with a memory-card icon in Card Reader mode and a camera icon in RAW Conversion mode. During a download it switches to the transfer icon and progress state.

Changing USB mode may make the camera disconnect and reconnect. Where appropriate, the app keeps the pending workflow and resumes after the correct mode returns.

### Browse and Download Photos

> 📱 See it: [browsing and downloading the card](screenshots/README.md#-camera-connection--custom-slots)

1. Set the camera to **`USB CARD READER`** and connect it.
2. Open the **Photos** tab. The app scans JPEG and RAF objects on the card and shows progress for large cards.
3. Filter by **All**, **JPEG**, or **RAW**, select any files, then tap **Download**.
4. Choose a destination folder with Android's folder picker. The app writes each camera file independently and leaves the card unchanged.

The foreground service keeps a large batch running while the app is minimized or the screen is locked. Keep the camera connected and powered until it completes. Cancellation is available both in the Photos screen and its notification.

If Photos opens while the camera is in another mode, the screen shows only the Card Reader instructions and a **Refresh** button. Refresh reopens the camera connection after you change its setting.

### Analyze JPEGs from the Camera

> 📱 See it: [the Analyze tab](screenshots/README.md#-analysing-photos)

1. Connect in **`USB CARD READER`** mode.
2. Open **Analyze** and choose the camera source.
3. Select one or more JPEGs from the card and analyze them.

Only the selected JPEGs are copied into the app's temporary cache. From there, the existing Fujifilm MakerNote parser and `RecipeMatcher` produce the same settings, exact matches, likely matches, and differences shown for phone photos.

### Develop a RAW in the Camera

> 📱 See it: [the recipe action menu](screenshots/README.md#-viewing-a-recipe) · [a rendered result](screenshots/README.md#-developing-a-raw-in-the-camera)

1. Open a recipe and choose **Develop RAW** from its menu.
2. Choose a RAF from the phone.
3. With the camera in **`USB RAW CONV. / BACKUP RESTORE`** mode, review the recipe and RAF, then tap **Render JPEG**. The app uploads the RAF and patched native profile, waits for the camera processor, downloads the rendered JPEG, and cleans up the temporary camera object.
4. Preview and save the JPEG through Android's document picker.

The RAF is always uploaded from the phone: RAW Conversion mode accepts a RAF as a host upload, and the app does not ask the camera to develop a card handle in place. To develop something still on the card, download it first from the **Photos** tab in Card Reader mode.

RAW recipe mapping is deliberately model-specific. The verified X-T50 adapter applies film simulation, exposure compensation, dynamic range, grain, Color Chrome effects, white balance and shift, Kelvin temperature, highlight/shadow tone, color, sharpness, high-ISO noise reduction, and clarity. Camera-native fields without a verified recipe mapping are preserved. An unknown profile is captured for calibration and conversion stops before the app guesses at its layout.

### Custom Slots and Camera Tools

> 📱 See it: [custom slots](screenshots/README.md#-camera-connection--custom-slots) · [More tab](screenshots/README.md#-maintenance--tools)

Connect in **`USB RAW CONV. / BACKUP RESTORE`** mode, then:

- **Writing a recipe**: Open any recipe → tap **Write to camera** → choose target slot (`C1` through `C7`) → confirm write.
- **Reading from the camera**: In the **Camera** tab or via **More → Backup & Restore → Import from camera**, read all custom slots from the body into your library.
- **Inspecting the body**: The Camera tab shows the camera model, battery and connection mode, with any available firmware, serial, shutter-count, and lens details.
- **Sharing a diagnostic report**: Use **Camera tools → Share report** to export supported operations, properties, mode, details, and slot readings for compatibility investigation.

---

## 🧪 Testing & Feedback

Any feedback, bug reports, and compatibility test results are welcome!

- **Found a bug or tested a new camera model?** Please [open an issue on GitHub](https://github.com/justbondarenko/fuji-recipes-app/issues) with your camera model, firmware, phone model, Android version, selected USB mode, and the feature you tested.
- **Direct contact & social links**: Open the in-app **About** screen (**More → About**) for direct email and contact links.

---

## 🛠️ Building & Developing

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+) or newer
- **JDK 17** (or Android Studio bundled JBR)
- **Android SDK Platform 37** (`compileSdk = 37`, `minSdk = 29`)
- An Android device with USB Host mode support (API 29+ / Android 10+)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/justbondarenko/fuji-recipes-app.git
cd fuji-recipes-app

# Run all unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

### Sideload to Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> [!TIP]
> **Wireless Debugging**:
> Because the phone's USB-C port is occupied by the camera during hardware testing, use **Wireless ADB** (`adb pair` and `adb connect`) for live logcat inspection and debugging.

---

## ☕ Open Source Acknowledgements

This project is built using fantastic open-source libraries, tools, and research:

### Android & Kotlin Ecosystem
- [Jetpack Compose & Material 3 Expressive](https://developer.android.com/jetpack/compose) — Modern declarative UI toolkit and Material 3 components.
- [Kotlin & Kotlinx Coroutines / Serialization](https://github.com/Kotlin/kotlinx.serialization) — Reactive streams and JSON serialization.
- [Coil](https://github.com/coil-kt/coil) — Image loading for Compose.
- [AndroidX DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — Reactive key-value storage.
- [Google Fonts Material Symbols Rounded](https://fonts.google.com/icons) — Clean iconography.
- [Turbine](https://github.com/cashapp/turbine) & [JUnit](https://junit.org/) — Flow testing and unit testing.

### Camera Protocol & Reverse-Engineering Research
- [**filmkit** (`eggricesoy/filmkit`)](https://github.com/eggricesoy/filmkit) (MIT License) — Research on Fujifilm PTP property ranges, parameter encoding, and custom slot codes.
- [**FujiHack Community** (`fujihack/fujihack`)](https://github.com/fujihack/fujihack) (GPL-3.0 License) — Reverse-engineering documentation, PTP/USB communication research, and MakerNote tag structures. Referenced as technical documentation for camera interoperability.
- [**libfuji** (`petabyt/libfuji`)](https://github.com/petabyt/libfuji) — Reference implementation for Fujifilm USB modes, object transfer, and in-camera RAW conversion sequencing.

---

## 📜 Legal Disclaimer & Trademarks

### 1. Independent Personal Project & Non-Affiliation
Fuji Recipes is an independent personal open-source utility developed for private use and experimentation. It is **not** an official product of, nor is it endorsed, certified, supported, sponsored, or affiliated with **FUJIFILM Corporation** or any of its subsidiaries.

### 2. "AS IS" Software Disclaimer
Fuji Recipes is provided strictly on an **"AS IS"** and **"AS AVAILABLE"** basis under the MIT License without warranties of any kind, whether express, implied, statutory, or otherwise. To the maximum extent permitted by applicable law, the developer expressly disclaims all warranties, including but not limited to implied warranties of merchantability, fitness for a particular purpose, title, and non-infringement.

### 3. Camera Connection & Protocol Risks
Connecting a camera to a mobile device via USB (Picture Transfer Protocol / PTP) involves low-level data communication with the camera's internal firmware, memory controllers, and volatile custom preset registers. You acknowledge that USB communication carries inherent risks, including unexpected disconnections, protocol errors, camera lockups, or parameter corruption.

### 4. Limitation of Liability & User Assumption of Risk
All use of this application — including creating recipes, modifying custom slots, transferring presets to or from cameras, and importing or exporting files — is performed entirely at your own discretion, judgment, and sole risk. 

To the fullest extent permitted by applicable law, in no event shall the developer, contributors, or copyright holders be liable for any direct, indirect, incidental, special, consequential, exemplary, or punitive damages (including, without limitation, camera inoperability, firmware corruption, data loss, photo loss, hardware failure, repair costs, or loss of profits) arising out of or in connection with the software or the use of the software.

### 5. Trademarks & Brand Acknowledgements
`FUJIFILM`, `FUJINON`, `X-Trans`, and all film simulation designations (*Provia*, *Velvia*, *Astia*, *Classic Chrome*, *PRO Neg*, *Classic Negative*, *Nostalgic Neg*, *Eterna*, *Acros*, *Reala Ace*, etc.) are trademarks or registered trademarks of **FUJIFILM Corporation**. Reference to these names, marks, and technologies is made solely for descriptive, identification, and technical interoperability purposes.

### 6. Third-Party Brands, Logos & Intellectual Property
All other product names, logos, brand identities, websites, trademarks, service marks, trade names, or intellectual property referenced, displayed, or mentioned within this application or documentation are the property of their respective owners. Any reference to such third-party names, websites, logos, or brands is made purely for informational, reference, and interoperability purposes only, and does not constitute or imply any affiliation, sponsorship, endorsement, association, or partnership with the developer.

---

Created for personal use and shared with the Fujifilm community by **[Andrii Bondarenko](https://github.com/justbondarenko)**.
