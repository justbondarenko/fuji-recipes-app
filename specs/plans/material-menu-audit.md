# Material popup menu audit

Updated 2026-10-25. The crop aspect-ratio menu is the visual reference.

All app popup menus now use `FujiAnchoredMenu`: the native Material 3 menu
popup, rounded vibrant groups, native anchoring/animation, and bounded vertical
scrolling. Choice rows use `FujiSelectionMenuItem`, which shares crop's checked
leading icon, selected fill, item shapes, and vibrant colors. Action rows use
`FujiMenuItem`; destructive editor actions retain their error color and delete
confirmation.

| Surface | Finding | Status |
| --- | --- | --- |
| RAW preview comparison | Legacy dropdown with radio buttons | Migrated; selected source uses crop-style check and fill |
| RAW Grain parameter selector | Legacy dropdown without selected-row styling | Migrated; Amount/Size/Roughness shows the active parameter |
| RAW Vignette parameter selector | Same legacy selector | Migrated; Amount/Midpoints/Roundness/Feather shows the active parameter |
| Crop aspect ratio | Already the reference Material menu | Uses the shared choice-row implementation |
| Recipe editor enum fields | Legacy enum dropdowns | Delegates to the shared enum menu |
| RAW Lab enum fields | Mixed legacy/default Material menu styles | Uses the shared enum menu, retaining related option groups |
| Recipe editor film simulation | Legacy dropdown with trailing check | Migrated; long simulation list scrolls |
| Recipe comparison target | Legacy dropdown with custom selected text colors | Migrated; recipe name and simulation supporting text retained |
| Library sorting and order | Legacy dropdown with custom divider/checks | Migrated; sort field and direction have separate Material groups |
| Recipe editor Save secondary actions | Legacy Duplicate/Delete dropdown | Migrated; deletion remains a separate destructive group |
| Library recipe context actions | Already shared Material menu | Retained; now inherits shared bounded scrolling |
| Library batch action menu | Already shared Material menu | Retained; now inherits shared bounded scrolling |
| Recipe detail action menu | Already shared Material menu | Retained; now inherits shared bounded scrolling |

`DropdownMenuPopup`, `DropdownMenuGroup`, and `DropdownMenuItem` construction is
centralized in `ui/common/AnchoredMenu.kt`. There are no remaining legacy
`DropdownMenu(...)` calls elsewhere in the app.

Verification: debug/instrumentation APK assembly, all 734 local unit tests,
and 23 device UI checks passed on SM-F976B. The new app-wide UI check exercises
default and grouped enum selection, long film-simulation scrolling, recipe
comparison supporting text/selection, editor Duplicate/Delete confirmation,
and both library sort groups. All 17 RAW Editor layout checks and the five
existing library interaction checks passed. An app-only popup capture confirms
the crop-style vibrant container, rounded selected row, and leading check.
The updated app and test APKs are installed.

Shadow/corner follow-up: checked against the official
[Material 3 menu specs](https://m3.material.io/components/menus/specs) and the
installed Compose Material 3 implementation. The scroll viewport now reserves
space around the elevated groups so their shadows and bottom corners are not
clipped. Group shapes use `MenuDefaults.groupShape(index, count)`, retaining the
native symmetric standalone corners and the specified shapes for grouped menus.
The device regression check asserts space above and below the menu surface; an
app-only capture verifies the complete bottom shadow and equal outer radii.
Rebuilt both APKs and reran all 23 menu, RAW layout, and library device checks;
all passed on SM-F976B with the shadow fix installed.
