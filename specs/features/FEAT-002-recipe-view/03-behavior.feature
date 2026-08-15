Feature: Recipe view
  As the photographer
  I want to read a recipe's settings without being in a form
  So that answering "what is in this recipe" never risks changing it

  Background:
    Given a library that has loaded

  # --- Opening ---

  Scenario: Tapping a card opens the recipe for reading
    Given a recipe named "Kodachrome 64"
    When I tap its card
    Then I see the recipe's name, film simulation, rating and tags
    And I see its settings laid out for reading
    And there are no editable inputs

  Scenario: A recipe that no longer exists says so
    Given a recipe id that is not in the library
    When I open it
    Then I am told the recipe was not found
    And I am offered a way back to the list

  Scenario: Reading a recipe needs no network
    Given the library was loaded from the snapshot
    And the device has no network connection
    When I open a recipe
    Then its settings are shown

  # --- Grouping and omission ---

  Scenario: Settings are grouped in the canonical order
    Given a recipe
    When I read it
    Then the groups appear in the order simulation, tone, effects, white balance, monochromatic, shooting

  Scenario: A colour parameter is omitted for a monochrome simulation
    Given a recipe using ACROS
    When I read it
    Then no "Color" row is shown
    And it is not shown as disabled or "N/A"

  Scenario: Monochromatic colour appears only for a monochrome simulation
    Given a recipe using Classic Chrome
    When I read it
    Then no monochromatic colour rows are shown

  Scenario: Grain size is hidden when the grain effect is off
    Given a recipe whose grain effect is off
    When I read it
    Then no "Grain size" row is shown

  Scenario: Grain size appears once the grain effect is on
    Given a recipe whose grain effect is weak
    When I read it
    Then a "Grain size" row is shown

  Scenario: Colour temperature appears only when white balance is set to it
    Given a recipe whose white balance is Daylight
    When I read it
    Then no colour temperature row is shown

  # --- Display rules ---

  Scenario: Enums show their label, not their id
    Given a recipe whose dynamic range is "dr400"
    When I read it
    Then the row shows "DR400"

  Scenario: Non-zero numerics carry an explicit sign
    Given a recipe whose highlight tone is 2 and whose shadow tone is -1
    When I read it
    Then the highlight tone shows "+2"
    And the shadow tone shows "-1"

  Scenario: Zero is bare
    Given a recipe whose sharpness is 0
    When I read it
    Then the sharpness shows "0"
    And it does not show "+0"

  Scenario: Half steps show one decimal
    Given a recipe whose highlight tone is 1.5
    When I read it
    Then the row shows "+1.5"

  Scenario: An off state says Off
    Given a recipe whose grain effect is off
    When I read it
    Then the row shows "Off"
    And it does not show a blank or a dash

  Scenario: Colour temperature has no space before K
    Given a recipe whose white balance is colour temperature at 5500
    When I read it
    Then the row shows "5500K"

  Scenario: White balance shift is one combined row
    Given a recipe whose white balance shift is red 3 and blue -2
    When I read it
    Then a single row shows "R +3 / B -2"

  Scenario: An unset advisory field is omitted entirely
    Given a recipe with no minimum ISO set
    When I read it
    Then no minimum ISO row is shown

  Scenario: An unknown enum value still renders
    Given a recipe whose dynamic range is a value this build does not know
    When I read it
    Then the row shows the raw value
    And the screen does not crash or drop the row

  # --- Changed versus default ---

  Scenario: Defaults are visually distinct from changed values
    Given a recipe with sharpness changed and clarity left at its default
    When I read it
    Then the sharpness reads as changed
    And the clarity reads as a default

  Scenario: Hiding defaults leaves only what was changed
    Given a recipe with exactly three parameters changed from their defaults
    When I turn on "Changed only"
    Then only those three parameters are listed

  Scenario: A recipe with nothing changed says so rather than showing nothing
    Given a recipe entirely at its defaults
    When I turn on "Changed only"
    Then I am told that nothing has been changed from the defaults

  Scenario: The full set comes back
    Given I have turned on "Changed only"
    When I turn it off
    Then every applicable parameter is listed again

  # --- Getting to editing ---

  Scenario: One action moves from reading to editing
    Given a recipe I am reading
    When I choose to edit
    Then I am taken to that recipe's editor
