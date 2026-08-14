# FEAT-004 — tasks

- [x] **T-01** — `ConnectionSettings.clear()`, wiping all three keys.
- [x] **T-02** — `ConnectionRoute` gains `firstRun: Boolean`. Saving on first run goes to the
      library; saving from settings pops back. The route carries the case rather than the
      screen guessing.
- [x] **T-03** — `ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt`: connection summary
      (host, credentials set or not, never the secret), clear-with-confirmation, about row.
- [x] **T-04** — Nav: the More tab renders settings instead of `PlaceholderScreen`.
- [x] **T-05** — Tests: `clear()` empties the store; the summary reports configured and
      unconfigured correctly and never contains the secret.
- [x] **T-06** — `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` green,
      and verified on device: reach settings, change credentials, return, clear.


---

## Verified on device

More tab → settings, showing `10.0.2.2:8787 · credentials set`, a clear-credentials row and
the version. Tapping Connection opens the form with the secret masked and a Back affordance
that first-run setup does not have.
