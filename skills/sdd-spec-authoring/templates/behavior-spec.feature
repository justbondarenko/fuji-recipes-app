# TEMPLATE — copy to specs/features/FEAT-XXX-slug/03-behavior.feature
# Must remain valid Gherkin: these scenarios are meant to run, not just be read.
# Keep steps declarative (what the user does) not mechanical (which element they click).
# Delete these comments and all placeholder text as you fill it in.

Feature: <FEAT-XXX feature name>
  As a <user type>
  I want <capability>
  So that <benefit>

  # --- Happy path ---

  Scenario: <primary success case>
    Given <starting context>
    When <action taken>
    Then <expected outcome>
    And <additional assertion, if needed>

  # --- Variations ---

  Scenario: <second valid path, e.g. combined or repeated action>
    Given <starting context>
    And <additional context>
    When <action taken>
    Then <expected outcome>

  # --- Reversal / undo ---

  Scenario: <undoing the action restores prior state>
    Given <state after the action>
    When <the user reverses it>
    Then <prior state is restored>

  # --- Edge cases: write these, don't skip them ---

  Scenario: <empty or zero-result case>
    Given <starting context>
    When <action that yields nothing>
    Then <a clear empty state is shown>
    And the application does not crash or show a blank error

  Scenario: <invalid input case>
    Given <starting context>
    When <the user supplies invalid input>
    Then <a helpful error is shown>
    And <no partial or corrupted state is saved>
