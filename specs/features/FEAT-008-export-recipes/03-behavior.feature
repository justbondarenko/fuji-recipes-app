Feature: Export recipes
  As the photographer
  I want my library in a file I can put wherever I keep things
  So that it exists somewhere other than one server behind one token

  # ── Getting there ───────────────────────────────────────────────────────────

  Scenario: Export is reachable from More
    When I open the More tab
    Then I see an Export entry

  Scenario: Everything is selected when the screen opens
    Given a library of nine recipes
    When I open Export
    Then all nine are selected

  Scenario: An empty library says so
    Given an empty library
    When I open Export
    Then I am told there is nothing to export yet
    And I am not offered an export action

  Scenario: Nothing selected disables the action with a reason
    Given the export screen
    When I deselect everything
    Then the export action is unavailable
    And it says at least one recipe is needed

  Scenario: The count follows the selection
    Given a library of nine recipes
    When I deselect two
    Then I am told seven of nine are selected

  # ── The envelope ────────────────────────────────────────────────────────────

  Scenario: A library export is an envelope
    When I export more than one recipe
    Then the file has format "fuji-recipe"
    And version 1
    And an exportedAt timestamp
    And a recipes array

  Scenario: The file is readable as a diff
    When I export
    Then the JSON is indented two spaces
    And it ends with a newline

  Scenario: Only the selected recipes are in the file
    Given a library of nine recipes with three selected
    When I export
    Then the file holds three recipes

  # ── SF-008: what never leaves ───────────────────────────────────────────────

  Scenario: Local bookkeeping is excluded
    Given a recipe carrying a sortKey, a lastWrittenSlot and a lastWrittenAt
    When I export it
    Then none of those three appear in the file

  Scenario: The dropped sensor generation column is excluded
    Given a recipe carrying a sensorGeneration
    When I export it
    Then it does not appear in the file

  # ── SF-017: nothing is lost ─────────────────────────────────────────────────

  Scenario: An unknown top-level key survives the export
    Given a recipe carrying a property this build does not know
    When I export it
    Then that property is in the file, unchanged

  Scenario: An unknown setting survives the export
    Given a recipe whose settings carry a key this build does not know
    When I export it
    Then that key is in the file's settings, unchanged

  Scenario: Unknown keys come after the documented ones
    Given a recipe carrying an unknown property
    When I export it
    Then the documented keys appear first, in the format's order

  Scenario: Values are copied rather than reformatted
    Given a recipe whose highlight tone is 1.5
    When I export it
    Then the file holds 1.5

  Scenario: A key the recipe does not have is left out
    Given a recipe whose settings omit the clarity
    When I export it
    Then the file's settings have no clarity key

  Scenario: A null value stays null
    Given a recipe whose monochromatic colour is null
    When I export it
    Then the file holds null for it

  # ── SF-018: filenames ───────────────────────────────────────────────────────

  Scenario: A library export is named by date
    When I export several recipes as one file
    Then the filename is fuji-recipes-YYYY-MM-DD.json

  Scenario: An archive is named by date too
    When I export several recipes as an archive
    Then the filename is fuji-recipes-YYYY-MM-DD.zip

  Scenario: The date is UTC
    Given a phone in a timezone where the local date differs from UTC
    When I export
    Then the filename carries the UTC date

  Scenario: One recipe is named after itself
    Given a recipe called "Kodachrome 64"
    When I export just that one
    Then the filename is kodachrome-64.json

  Scenario: A name with punctuation slugifies
    Given a recipe called "Portra 400 — warm / soft"
    When I export it
    Then the filename has no punctuation and no repeated dashes

  Scenario: A name in another script keeps its letters
    Given a recipe called "Cinéma"
    When I export it
    Then the filename keeps the accented letter

  Scenario: A name that slugifies to nothing falls back to the id
    Given a recipe called "!!!"
    When I export it
    Then the filename is recipe- followed by the start of its id

  # ── The archive ─────────────────────────────────────────────────────────────

  Scenario: An archive holds one file per recipe
    Given three selected recipes
    When I export as an archive
    Then it holds three JSON files

  Scenario: Each file in the archive is a bare recipe
    When I export as an archive
    Then each entry is a recipe object with no envelope around it

  Scenario: Two recipes with one name both survive
    Given two recipes both called "Portra 400"
    When I export as an archive
    Then the archive holds two entries
    And the second is suffixed rather than overwriting the first

  # ── One recipe ──────────────────────────────────────────────────────────────

  Scenario: A single recipe exports as a bare object
    When I export one recipe from its own screen
    Then the file is a recipe object with no envelope

  # ── Sharing ─────────────────────────────────────────────────────────────────

  Scenario: The file goes to the share sheet
    When I export
    Then the system share sheet opens with the file attached

  Scenario: No storage permission is asked for
    When I export
    Then no permission prompt appears

  Scenario: Previous exports do not accumulate
    Given I have exported once
    When I export again
    Then the earlier file is gone from the app's cache

  Scenario: Export works with no signal
    Given no network connection
    When I export
    Then the file is produced and shared
