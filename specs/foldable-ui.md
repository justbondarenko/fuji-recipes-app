# Foldable UI

Use the current app window's space rather than a device model or orientation.
An unfolded phone can be taller than wide, and a foldable in split-screen may
have less space than a regular phone. All dimensions below are density-independent.

## First pass

| Area | Compact window | Wider window |
| --- | --- | --- |
| Main navigation | Existing bottom bar | Vertically centered navigation rail items at 600dp width and 480dp height |
| Library, recipe details, recipe editor, camera photo list | Existing single column | Centered body, at most 720dp wide |
| Other page content | Available width | Centered, at most 1200dp wide |
| Loaded RAW Lab | Existing stacked preview and controls | Existing 60/40 split at 720dp width, also retained for landscape windows |

The navigation destinations, callbacks, and content remain in the same composition
location when the rail replaces the bottom bar. No separate navigation controller,
route, or data workflow is introduced. The loaded Lab continues to hide navigation.
Forms and lists keep their current scrolling, selection, and swipe interactions.

## Further layout ideas

- Library: a list/detail view on sufficiently wide windows, with the selected
  recipe beside the list. Preserve the selected recipe when returning to a
  compact window and make Back behavior explicit before implementing this.
- Recipe view: reference photos beside settings when both panes can retain a
  useful minimum width.
- RAW Lab: use Android WindowManager folding features to place the photo above
  a horizontal tabletop fold and controls below it. For a separating vertical
  hinge, size the two panes to the physical display regions instead of 60/40.

This first pass does not detect folding posture or avoid a physical hinge.
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
- At 720 × 360dp: retain bottom navigation, and retain the Lab's landscape layout.
- Resize or fold/unfold while searching, scrolling, editing, and viewing a recipe:
  verify the destination and editor contents survive, and Back still works.
- In split-screen at less than 600dp width: return to bottom navigation.
- Repeat with large fonts, RTL, dark mode, gesture navigation, and the keyboard
  visible; verify all actions and final list/form items remain reachable.
