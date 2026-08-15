Feature: Recipe form
  As the photographer
  I want to create and change recipes on the phone
  So that a new idea does not have to wait until I am at a desk

  Background:
    Given a configured connection and a loaded library

  # --- Creating ---

  Scenario: Creating a recipe
    When I start a new recipe
    And I give it a name and choose a film simulation
    And I save
    Then it appears in my library

  Scenario: A name is required
    When I start a new recipe and leave the name empty
    Then saving is refused
    And I am told the name is required

  Scenario: A new recipe starts at the documented defaults
    When I start a new recipe
    Then every parameter is at its default from the field definitions

  # --- Editing ---

  Scenario: Editing an existing recipe
    Given a recipe named "Kodachrome 64"
    When I open it for editing, change its sharpness, and save
    Then the change is stored
    And the rest of the recipe is unchanged

  Scenario: An edit sends only what changed
    Given a recipe I open for editing
    When I change only the rating and save
    Then only the rating is sent to the server

  Scenario: A field this build does not know survives an edit
    Given a recipe carrying a setting written by a newer client
    When I edit its sharpness and save
    Then the unknown setting is still present afterwards

  # --- Live applicability ---

  Scenario: Choosing a monochrome simulation removes the colour control
    Given I am editing a recipe using Classic Chrome
    When I change the film simulation to ACROS
    Then the colour control is no longer offered

  Scenario: Turning the grain effect off removes grain size
    Given I am editing a recipe whose grain effect is weak
    When I set the grain effect to off
    Then the grain size control is no longer offered

  # --- Numeric input ---

  Scenario: Stepping a value
    Given a numeric field at 0
    When I press its plus button
    Then it reads 1

  Scenario: Stepping stops at the range end
    Given a numeric field at its maximum
    When I press its plus button
    Then the value does not change

  Scenario: A wide-range value can be typed
    Given the colour temperature field
    When I type 7200
    Then the value is 7200

  Scenario: A typed value outside the range is refused
    Given the colour temperature field
    When I type 99000
    Then I am told the value is out of range
    And saving is refused

  # --- Saving failures ---

  Scenario: A save with no signal keeps what I typed
    Given I have filled in a new recipe
    And the device has no network connection
    When I save
    Then I am told the save could not reach the server
    And every value I entered is still on screen

  Scenario: A rejected save names the field
    Given the server rejects a value as invalid
    When I save
    Then the message names the field the server complained about

  Scenario: A refused token does not look like a validation problem
    Given the service token has been revoked
    When I save
    Then I am told my credentials were refused

  # --- Delete ---

  Scenario: Deleting asks first
    Given a recipe I am editing
    When I choose to delete it
    Then I am asked to confirm, and the recipe is named

  Scenario: Confirming a delete removes it
    Given a delete confirmation
    When I confirm
    Then the recipe is gone from my library
    And I am back at the list

  Scenario: Cancelling a delete changes nothing
    Given a delete confirmation
    When I cancel
    Then the recipe is still in my library

  # --- Duplicate ---

  Scenario: Duplicating leaves the original alone
    Given a recipe named "Kodachrome 64"
    When I duplicate it and save
    Then there are two recipes
    And the original is unchanged

  Scenario: A duplicate does not share the original's name exactly
    Given a recipe named "Kodachrome 64"
    When I duplicate it
    Then the new recipe's name is distinguishable from the original

  # --- In place on the view ---

  Scenario: Setting a rating while reading
    Given a recipe I am reading
    When I set its rating to 4
    Then the rating is saved without opening the form

  Scenario: Adding a tag while reading
    Given a recipe I am reading
    When I add the tag "street"
    Then the tag is saved without opening the form

  Scenario: Everything else stays read-only while reading
    Given a recipe I am reading
    Then no parameter other than rating and tags can be changed

  # --- Leaving ---

  Scenario: Leaving with unsaved changes asks first
    Given I have changed a value in the form
    When I try to leave
    Then I am asked whether to discard my changes

  Scenario: Leaving an untouched form does not ask
    Given I have opened a form and changed nothing
    When I leave
    Then I am not asked anything

  Scenario: Discarding means discarding
    Given I have changed a value and chosen to discard
    Then the recipe is unchanged
