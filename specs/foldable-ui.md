# Foldable UI

Use the current app window's space rather than a device model or orientation.
An unfolded phone can be taller than wide, and a foldable in split-screen may
have less space than a regular phone. All dimensions below are density-independent.

## Current layouts

| Area | Compact window | Wider window |
| --- | --- | --- |
| Main navigation | Existing bottom bar | Vertically centered navigation rail items at 600dp width and 480dp height |
| Library | Recipe opens full screen | List and selected recipe share the window when at least 720dp remains after navigation |
| Standalone recipe details, recipe editor, camera photo list | Existing single column | Centered body, at most 720dp wide |
| Other page content | Available width | Centered, at most 1200dp wide |
| Loaded RAW Lab | Existing stacked preview and controls | Existing 60/40 split at 720dp width, also retained for landscape windows |

The navigation destinations, callbacks, and content remain in the same composition
location when the rail replaces the bottom bar. No separate navigation controller,
route, or data workflow is introduced. The loaded Lab continues to hide navigation.
Forms and lists keep their scrolling and selection behavior. Recipe row actions
use a long-press menu on every screen size rather than horizontal swipe gestures.

## Library list/detail navigation

The library starts at its normal width. Selecting a recipe animates the list down
to 40% of the content width (between 300dp and 360dp), while revealing the recipe
at the same moving boundary. The detail is measured at its final width throughout
the 400ms transition, so its controls do not compress during the reveal. Closing
the recipe reverses this animation. Reduced-motion mode skips the transition.

The centered navigation rail stays visible. The open recipe is highlighted in the
list; selecting another recipe changes the right pane without rebuilding the list
or losing its scroll/search state. Filters, sorting, and counts wrap in narrow panes.
The create FAB hides while a recipe is open to avoid covering its actions.

Below 360dp of toolbar width, settings and camera controls occupy a separate row
above a full-width search field. The search field and placeholder are single-line,
so a narrow pane or large font cannot turn the field into a tall pill. The field
keeps its composition position while the pane animates between widths.

On all screen sizes, long-press a recipe for Edit, Write to camera, Select, and
Delete. The popup uses vertical items rather than a fixed-width row of actions.
Write is disabled with a connection explanation when the camera cannot accept it.
Delete still requires confirmation. Select enters the existing batch selection
mode; taps and long presses can then add recipes to that selection.

Back and the detail's Close action close the pane. Editing and saving a recipe
returns to the selected pane. Comparison can select another recipe in that pane.
The selected id lives on the library's saved back-stack entry, so it survives
recreation and a trip into the editor. Each detail model is cleared on replacement.

Folding into a compact window keeps the selected recipe full screen and retains
the list's composition for unfolding. The list is not placed or accessible while
hidden. The compact detail hides bottom navigation; a rail in a medium-width
window stays visible. A recipe opened initially on a compact phone continues to
use the existing standalone navigation route.

## Further layout ideas

- Recipe view: reference photos beside settings when both panes can retain a
  useful minimum width.
- RAW Lab: use Android WindowManager folding features to place the photo above
  a horizontal tabletop fold and controls below it. For a separating vertical
  hinge, size the two panes to the physical display regions instead of 60/40.

These layouts do not detect folding posture or avoid a physical hinge.
Window-width adaptation is intended for continuous unfolded displays and ordinary
resizable windows; separating hinges and tabletop placement need the posture work above.

## Device/emulator review checklist

These are manual checks to run on hardware or an Android emulator; compilation
and JVM tests do not verify their appearance.

- At 320dp and 393dp width: bottom navigation, unchanged single-column layout,
  reachable create/save actions, no clipped toolbar controls.
- At 599dp and 600dp width with at least 480dp height: bottom navigation changes
  to a rail, all five destinations navigate and show the correct selection.
- At 840 × 900dp: rail navigation and centered list/form bodies; open the Lab
  with a RAF and check its side-by-side preview and controls.
- With at least 720dp left after the rail: tap a recipe and watch the list narrow
  while the right pane is revealed. Confirm the narrow search remains one line
  with the settings/camera buttons above it. Scroll/search the list, switch recipes, use
  comparison, then close the detail and check that the list expands in place.
- On phone and foldable, long-press a row: verify Edit, Write to camera, Select,
  and Delete fit. Verify disconnected Write has an explanation, Delete asks for
  confirmation, and Select retains batch rating/delete. Horizontal drags must not
  reveal actions.
- Edit a recipe from the pane and save; confirm that the pane shows the saved
  recipe. Delete it from the editor and confirm a normal return to the library.
- Fold with a recipe open, then unfold: retain the same recipe and list state.
  Back in either layout closes the recipe. On a compact phone, opening a recipe
  still uses the original full-screen route.
- At 720 × 360dp: retain bottom navigation, and retain the Lab's landscape layout.
- Resize or fold/unfold while searching, scrolling, editing, and viewing a recipe:
  verify the destination and editor contents survive, and Back still works.
- In split-screen at less than 600dp width: return to bottom navigation.
- Repeat with large fonts, RTL, dark mode, gesture navigation, and the keyboard
  visible; verify all actions and final list/form items remain reachable.
