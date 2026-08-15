Feature: Import from camera
  As the photographer
  I want the recipes already on my camera in my library
  So that a look I tuned on the body is not trapped there

  # ── Getting there ───────────────────────────────────────────────────────────

  Scenario: Import is reachable from More
    When I open the More tab
    Then I see an Import entry

  Scenario: Import without a camera offers to connect
    Given no camera is connected
    When I open Import
    Then I am offered to connect a camera
    And I am not offered to read slots

  Scenario: Import with a camera connected offers to read
    Given a connected camera
    When I open Import
    Then I am offered to read the slots

  # ── Reading ─────────────────────────────────────────────────────────────────

  Scenario: Reading reports progress slot by slot
    Given a connected camera
    When I read the slots
    Then I am told which slot is being read, out of seven

  Scenario: Each slot is selected before its properties are read
    When the slots are read
    Then each slot is selected, the body is given time to settle, and then read

  Scenario: A property carrying two fields is read once
    When a slot is read
    Then the grain property is read once
    And it yields both the grain effect and the grain size

  Scenario: A slot with no settings is not imported as an empty recipe
    Given a camera whose C4 holds nothing readable
    When I read the slots
    Then C4 does not appear in the review

  Scenario: A slot with no name is labelled by its slot number
    Given a camera whose C2 has settings but no name
    When I read the slots
    Then C2 appears as "Slot C2"

  Scenario: A value this build cannot name costs one field, not the slot
    Given a camera whose C3 reports an unrecognised film simulation code
    When I read the slots
    Then C3 still appears in the review
    And it carries no film simulation

  Scenario: A camera with nothing saved says so
    Given a camera whose slots are all empty
    When I read the slots
    Then I am told the camera has no custom recipes saved

  Scenario: Reading works with no signal
    Given no network connection and a connected camera
    When I read the slots
    Then the slots are read

  Scenario: A read that fails as a whole can be retried
    Given a camera that stops answering while the slots are being read
    When I read the slots
    Then I am told the camera stopped answering
    And I am offered to try again

  # ── The review ──────────────────────────────────────────────────────────────

  Scenario: Each row shows what the slot contains
    When the review appears
    Then each row shows its slot number, its name and its film simulation

  Scenario: A slot the library does not have is new and selected
    Given a camera slot matching nothing in my library
    When the review appears
    Then that row is marked new
    And it is selected

  Scenario: A slot whose settings match an existing recipe is marked as already held
    Given a camera slot whose settings exactly match "Kodachrome 64" in my library
    When the review appears
    Then that row says it is already in my library as "Kodachrome 64"
    And it is not selected

  Scenario: The match is on settings, not on name
    Given a camera slot named "1970s Summer" whose settings match "Summer 70s" in my library
    When the review appears
    Then that row is marked as already held

  Scenario: A difference in any applicable setting makes it new
    Given a camera slot matching an existing recipe except for its sharpness
    When the review appears
    Then that row is marked new

  Scenario: Settings are compared after normalisation
    Given a library recipe that omits a setting and a camera slot that reports its default
    When the review appears
    Then the two are treated as the same configuration

  Scenario: A setting that does not apply to the simulation is not compared
    Given a monochrome camera slot and a monochrome library recipe that differ only in colour
    When the review appears
    Then that row is marked as already held

  Scenario: Two identical slots do not both import
    Given two camera slots with identical settings
    When the review appears
    Then the second is marked as already held
    And it names the first

  Scenario: A name already in use is a warning, not a problem
    Given a camera slot named the same as a library recipe with different settings
    When the review appears
    Then that row warns that the name is already used
    And it is selected

  Scenario: A duplicate can be imported anyway
    Given a row marked as already held
    When I select it
    Then it will be imported

  Scenario: Nothing new is still shown rather than hidden
    Given every camera slot is already in my library
    When the review appears
    Then every row is listed and deselected
    And I am told nothing is new

  Scenario: There is no replace option
    Given a row marked as already held
    Then I am offered to skip it or import it
    And I am not offered to replace anything

  # ── Importing ───────────────────────────────────────────────────────────────

  Scenario: Only the selected rows are imported
    Given a review with three rows and two selected
    When I import
    Then two recipes are sent

  Scenario: Recipes are sent without ids
    When I import
    Then no recipe in the request carries an id

  Scenario: The import is one atomic request
    When I import three recipes
    Then one request is made
    And a failure leaves nothing written

  Scenario: A successful import says how many landed
    Given an import of two recipes that succeeds
    Then I am told two were imported

  Scenario: A successful import refreshes the library
    Given an import that succeeds
    When I return to the library
    Then the imported recipes are there

  Scenario: Importing with no connection says that reading worked and saving will not
    Given no network connection and slots that have been read
    When I import
    Then I am told that saving needs a connection and reading did not
    And the review is still on screen
    And I am offered to try again

  Scenario: A refused import names what the server said and keeps the review
    Given a server that refuses the import
    When I import
    Then I am told what was refused
    And the review is still on screen
    And nothing was written
