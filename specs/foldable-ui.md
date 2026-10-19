# Foldable UI

Use the current app window's space rather than a device model or orientation.
An unfolded phone can be taller than wide, and a foldable in split-screen may
have less space than a regular phone. All dimensions below are density-independent.

## Current layouts

| Area | Compact window | Wider window |
| --- | --- | --- |
| Main navigation | Existing bottom bar | Vertically centered navigation rail items and a camera footer at 600dp width and height |
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
or losing its scroll/search state. Sorting and counts wrap safely in narrow panes.
The create FAB hides while a recipe is open to avoid covering its actions.

Search keeps its own stable row and remains single-line for both the query and
placeholder while the pane resizes. The controls below it put sort and recipe count
on the left, with view settings fixed at the right edge.

Filters is a circular floating action button at the bottom-right of the recipe
list on phones and wider windows. It belongs to the list pane, so opening a recipe
keeps it in that pane. When New recipe is visible, Filters sits above it; once a
recipe is open, it moves down to the bottom corner. This clearance animates with
the pane transition and respects reduced motion. The badge reports active filter
axes. The filter sheet is hosted outside the lazy header, allowing the button to
work after the header has scrolled away. List bottom padding leaves the final
items reachable above the floating controls. Selection mode replaces Filters
with the existing batch toolbar.

When the navigation rail is visible, its bottom action opens the camera connection
sheet and displays the existing connection badge. Page headers omit their camera
trigger, leaving search the full pane width. The navigation group stays centered
against the full rail independently of the footer. A minimum height of 600dp keeps
the footer clear of the five centered destinations. Shorter windows use bottom
navigation and restore the page's camera trigger; on phones the library camera
button is beside search. A loaded Lab keeps its own header trigger because its
full-screen workspace hides the rail.

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
- At 599dp and 600dp width with at least 600dp height: bottom navigation changes
  to a rail, all five destinations navigate and show the correct selection.
- At 840 × 900dp: rail navigation and centered list/form bodies; open the Lab
  with a RAF and check its side-by-side preview and controls.
- With at least 720dp left after the rail: tap a recipe and watch the list narrow
  while the right pane is revealed. Confirm the narrow search remains one line
  with view settings at the right of sort/count and the camera button at the rail bottom. Scroll/search the list, switch recipes, use
  comparison, then close the detail and check that the list expands in place.
- On phone and foldable, check the bottom-right Filters FAB, its active-count
  badge, and clearance above New recipe. Scroll the header off-screen, then open
  Filters and verify the same sheet still works.
- On phone and foldable, long-press a row: verify Edit, Write to camera, Select,
  and Delete fit. Verify disconnected Write has an explanation, Delete asks for
  confirmation, and Select retains batch rating/delete. Horizontal drags must not
  reveal actions.
- Edit a recipe from the pane and save; confirm that the pane shows the saved
  recipe. Delete it from the editor and confirm a normal return to the library.
- Fold with a recipe open, then unfold: retain the same recipe and list state.
  Back in either layout closes the recipe. On a compact phone, opening a recipe
  still uses the original full-screen route.
- Tap the camera footer from Library, Analyze, Lab, Photos, and More: open the
  same connection sheet once, retain the connection badge, and keep destinations
  centered. Fold to a phone-width window and check the camera trigger returns to
  the page header.
- At 720 × 360dp: retain bottom navigation, and retain the Lab's landscape layout.
- Resize or fold/unfold while searching, scrolling, editing, and viewing a recipe:
  verify the destination and editor contents survive, and Back still works.
- In split-screen at less than 600dp width: return to bottom navigation.
- Repeat with large fonts, RTL, dark mode, gesture navigation, and the keyboard
  visible; verify all actions and final list/form items remain reachable.
