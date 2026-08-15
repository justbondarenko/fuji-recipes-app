# FEAT-009: Read a recipe from a photo

**Tracker issue:** —
**Source PRD:** — (not in `PRD.md`; the web client added it after, and it earns its place)
**Parity target:** `fuji-recipes-book/src/utils/fuji-exif-parser.ts`,
`src/utils/recipe-matcher.ts`, `src/pages/more/recipe-reader.vue`
**Status:** Ready

---

## Problem

Every JPEG a Fujifilm body writes carries the recipe it was shot with, in the MakerNote. That
is the answer to the question people actually ask about their own photographs — *what was this
shot with?* — and today the only way to get it is to remember, or to open the file in desktop
software.

The library makes that answerable in a way a general EXIF viewer cannot: not just "these were
the settings", but **"this is Kodachrome 64"** — by name, from your own library. And when it
is not one you have, saving it takes one tap instead of twenty fields typed in.

**Bottom bar → Read** picks a photo, decodes its settings, matches them against the library,
and offers the two things worth offering: open the recipe you already have, or save this as a
new one.

## User stories

- As the photographer, I can point the app at one of my photos and be told which of my recipes it was shot with, so that I do not have to remember.
- As the photographer, I can see when a photo is *nearly* one of my recipes and exactly what differs, so that I learn what I changed.
- As the photographer, I can save a photo's settings as a new recipe in one step, so that a look I stumbled into is not lost.
- As the photographer, I can read a photo with no signal, because the decoding is local.
- As the photographer, I am told plainly when a photo carries nothing to read, so that a blank result is never a mystery.

## Scope

### Reading the file

1. A **Read** entry in the bottom bar, between Recipes and Settings. Not on the More screen —
   this is a thing you do, not a setting you change.
2. The system photo picker (`PickVisualMedia`). **No storage permission is requested**, and
   none is needed: the picker returns a one-shot read grant for the single image chosen.
3. The file is read and parsed **on the phone**. No network, no upload — the photo never
   leaves the device, which is both a privacy property and what makes it work in the field.
4. Files over 50 MB are refused before reading, and JPEG is the only accepted format. A
   Fujifilm body's RAF is not a JPEG and does not carry this MakerNote in a form this reads.

### Decoding

5. Validate the JPEG SOI marker; read the camera model from standard EXIF IFD0 when present.
6. Locate the **`FUJIFILM` MakerNote signature**, read its IFD offset, and walk its 12-byte
   entries. This is the whole reason a general EXIF library is not enough: `androidx.exifinterface`
   reads standard tags and stops at the MakerNote, which is where every recipe field lives.
7. The tags read: film simulation `5121`, white balance `4098` (+ colour temperature `4101`,
   + WB shift `4106`), dynamic range `5122`/`5123`, highlight `4161`, shadow `4160`, colour
   `4099`, sharpness `4097`, high-ISO NR `4110`, clarity `4111`, grain `4167`/`4172`, colour
   chrome `4168`, FX blue `4174`.
8. **These encodings are a third dialect and must never be crossed with the app's other two.**
   The custom-slot codes (FEAT-006) and the RAW-profile codes are different again. Here: grain
   is `0`/`32`/`64`; noise reduction is `736`/`704`/`512`/`640`/`0`/`384`/`256`/`448`/`480` for
   −4…+4; clarity is ×1000; **tone curves are negated and ×16**, so `-64` means `+4`. They live
   in their own file with a header that says so.
9. An **unknown code omits its field** rather than guessing. A recipe missing one value is
   still useful; a recipe with one wrong value is worse than useless, because it will match
   something.
10. Values are mapped into **the app's own field ids and values** — `dr400`, not `400`;
    `color-temp`, not `kelvin` — so the result feeds the matcher and the editor without a
    second translation.
11. WB shift is stored as a pointer to two `int32`s; each is divided by 20 and bounded to
    ±9, which is the range the field takes.

### Matching

12. The decoded settings are compared against every recipe in the library, over **only the
    fields the photo actually carried**. A photo that reports eight fields is scored out of
    eight, not out of twenty-two.
13. Each comparison yields a percentage and the list of fields that differ, each carrying both
    values formatted for reading.
14. Results are sorted exact matches first, then by percentage. A match at or above **70%** is
    "similar"; below that it is not offered as a match at all.
15. An **exact match** names the recipe. This is the answer the feature exists to give.

### What the screen offers

16. **Exact match** — names the recipe; the primary action opens it, and a secondary action
    still offers to save the photo as a new recipe. A deliberate variant is the
    photographer's call.
17. **Near match** — names it with its percentage and lists what differs. Both actions.
18. **No match** — the decoded settings, and one action: save as a new recipe.
19. **Save as a new recipe** opens the normal editor **pre-filled** with the decoded settings
    and a suggested name, so it goes through the same validation and the same save path as
    any other recipe.
20. **Copy as text** for the decoded settings, reusing `RecipeTextFormatter` — the same
    rendering the recipe screen uses, so a pasted recipe reads identically wherever it came
    from.

## Out of scope

| Deferred | To | Why |
|---|---|---|
| Reading RAF files | won't do | The recipe is in the JPEG. A body shooting RAW+JPEG writes both, and the JPEG is the one to hand. |
| Reading several photos at once | v2 | The question is about one photograph. A batch answer would be a different feature with a different screen. |
| Writing a recipe *into* a photo | won't do | Not a thing anyone asked for, and it would mean rewriting image files. |
| Showing the photo itself | won't do | The screen is about the settings. A thumbnail would be decoration that costs a decode. |
| Deriving anything not in the MakerNote | won't do | Exposure, lens and ISO are in standard EXIF and are not part of a recipe. |

## Error surfaces

| Failure | Surface | Message names |
|---|---|---|
| Not a JPEG | The screen, with the picker still offered | That it needs a JPEG, and that a RAF is not one |
| Over 50 MB | The screen | The limit, before anything is read |
| Valid JPEG, no EXIF at all | The screen | That the photo carries no metadata — often because it was edited or exported by another app |
| Valid JPEG, EXIF, but no Fujifilm MakerNote | The screen | That it is not a Fujifilm photo, which is a different fact from having no metadata |
| A Fujifilm photo this build could not read | The screen, with copy-as-text of whatever decoded | That the file could not be read, rather than silently showing an empty recipe |
| Nothing in the library to match against | The result, with the settings and the save action | That there is nothing to compare to yet |
