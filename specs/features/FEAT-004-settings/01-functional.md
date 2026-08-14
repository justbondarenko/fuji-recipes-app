# FEAT-004: Settings

**Status:** Ready

**Closes a gap this project created.** `FEAT-001` T-13 specified "route the app to the
connection screen on launch when settings are absent, **and expose it from the list's
overflow afterwards**". Only the first half was built, and the task was ticked anyway. The
result: once credentials are saved, the connection screen is reachable **only** by
provoking a 403 error panel.

---

## Problem

A service token expires, gets rotated, or was typed wrong in a way that only shows up later.
Today the only remedy is to trigger a credentials failure and use the error panel's shortcut,
or to uninstall the app. The "More" tab — the obvious place to look — is still the
FEAT-000 placeholder.

## User stories

- As the photographer, I can find and change my connection settings whenever I want, so that a rotated token is a thirty-second fix rather than a reinstall.
- As the photographer, I can see at a glance which server I am pointed at and whether credentials are set, so that "why is my library empty" has an answer on one screen.
- As the photographer, I can remove my stored credentials from the device, so that lending or retiring the phone does not hand over access to my library.
- As the photographer, saving from settings returns me to settings, so that changing a token does not dump me somewhere unexpected.

## Scope

1. The **More** tab becomes a real settings screen, replacing the FEAT-000 placeholder.
2. A **Connection** row, showing the configured host and whether credentials are set. It
   never shows the secret.
3. Tapping it opens the existing connection screen. Saving there **returns to settings**,
   not to the library — the first-run path still goes to the library, so the route carries
   which case it is rather than guessing.
4. **Clear credentials**, behind a confirmation, wiping base URL, client id and secret from
   the device. Afterwards the app is in its unconfigured state.
5. An **About** row: app name and version.
6. The connection screen keeps its existing behaviour — masked secret with a reveal toggle,
   and **Test connection** against `GET /api/recipes`, never `GET /api/health`.

## Out of scope

- Camera help (FEAT-005, which is when there is a camera to help with)
- Theme or display preferences — the palette is parity with the web client (`design-system.md`)
- Export, import, diagnostics (v2)
- Any second credential set or profile switching — one user, one library (`coding-standards.md` P1)

## Constraints

- The secret is never rendered in full unless the user asks, and is never written to a log.
- Clearing is immediate and local; it does not revoke anything server-side, and the screen
  must not imply that it does.

## No data contract

No new persisted or transmitted shape. The stored keys are FEAT-001's, listed in its
`01-functional.md` under Local preferences.

## Open questions

None.
