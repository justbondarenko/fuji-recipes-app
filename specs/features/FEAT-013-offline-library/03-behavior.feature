Feature: Offline library
  As the photographer
  I want my recipes stored on my phone
  So that the app works where I use it and nothing leaves without me sending it

  # ── No setup, ever ──────────────────────────────────────────────────────────

  Scenario: A fresh install opens on the library
    Given the app has never been opened on this device
    When I launch it
    Then I see the library screen
    And I am not asked for a server address or any credentials

  Scenario: Settings offers no connection
    When I open the Settings tab
    Then I see Import, Import a file and Export
    And there is no connection entry

  # ── Reading ─────────────────────────────────────────────────────────────────

  Scenario: An empty library offers the fastest way to fill it
    Given no library file on the device
    When I open the library
    Then I am told there are no recipes yet
    # The same panel the photo reader and both imports use: one shape, one line, one action.
    And the action offered is to import from the camera
    And creating one by hand is offered underneath it

  Scenario: Recipes survive a restart
    Given a library of three recipes
    When I force-stop the app and open it again
    Then the same three recipes are listed

  # ── Writing, with no network of any kind ────────────────────────────────────

  Scenario: A recipe can be created with the device in aeroplane mode
    Given the device is in aeroplane mode
    When I create a recipe named "Kodachrome 64"
    Then it is saved
    And it appears in the library

  Scenario: A new recipe is given an identity by the app
    When I create a recipe
    Then it has an id
    And its created and updated timestamps are the same moment
    And it is ordered after every recipe already in the library

  Scenario: An edit changes only what it names
    Given a recipe rated 3 with notes and tags
    When I change its rating to 5
    Then its rating is 5
    And its name, notes, tags and settings are unchanged
    And its updated timestamp has moved

  Scenario: A setting this build does not know survives an edit
    Given a recipe carrying a property this version of the app does not display
    When I edit its rating and save
    Then that property is still on the recipe afterwards

  Scenario: Deleting a recipe that is already gone is not an error
    Given a recipe that has just been deleted
    When the same delete is repeated
    Then it reports success and nothing changes

  # ── Import, deciding collisions locally ─────────────────────────────────────

  Scenario: An exported file restores under its own ids
    Given an empty library
    And an export file containing two recipes with ids
    When I import it
    Then both are imported
    And each keeps the id the file gave it

  Scenario: An id collision nobody resolved is skipped, not overwritten
    Given a library containing a recipe with id "abc"
    And an import file containing a different recipe with id "abc"
    When the import runs with no resolution for "abc"
    Then the stored recipe is unchanged
    And the entry is counted as skipped

  Scenario: Replace overwrites in place
    Given a library containing a recipe with id "abc"
    And an import file containing a different recipe with id "abc"
    When I choose Replace for it
    Then the stored recipe has the incoming name
    And it keeps its original position in the library
    And it keeps its original created timestamp

  Scenario: Keep both lands beside the original
    Given a library containing a recipe with id "abc"
    And an import file containing a different recipe with id "abc"
    When I choose Keep both
    Then the library holds two recipes
    And they have different ids

  Scenario: One unusable entry does not fail the batch
    Given an import file of three recipes, one of which has no name
    When I import it
    Then two are imported
    And the third is named as refused

  # ── The library file is the only copy ───────────────────────────────────────

  Scenario: A library that will not parse is reported, not emptied
    Given a library file on the device that is not valid JSON
    When I open the library
    Then I am told the library could not be read
    And I am not told there are no recipes yet

  Scenario: Nothing is written on top of a library that could not be read
    Given a library file on the device that is not valid JSON
    When I try to create a recipe
    Then the save is refused with that reason stated
    And the file on disk is byte-for-byte what it was

  Scenario: A library written by a newer version is refused rather than rewritten
    Given a library file recording a format this build does not understand
    When I open the library
    Then I am told it could not be read
    And nothing overwrites it
