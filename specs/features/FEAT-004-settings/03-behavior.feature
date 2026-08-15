Feature: Settings
  As the photographer
  I want to reach my connection settings whenever I need them
  So that a rotated token is a quick fix rather than a reinstall

  Scenario: Settings are reachable from the app
    Given a configured app
    When I open the More tab
    Then I see a settings screen with a connection entry

  Scenario: The connection entry summarises the current state
    Given the app is pointed at "recipes.example.com" with credentials set
    When I open settings
    Then the connection entry names that host
    And it says credentials are set
    And it does not show the secret

  Scenario: An unconfigured app says so
    Given no connection settings are stored
    When I open settings
    Then the connection entry says the app is not configured

  Scenario: Changing credentials returns to settings
    Given I opened the connection screen from settings
    When I save
    Then I am back on the settings screen

  Scenario: First-run setup still goes to the library
    Given no connection settings are stored
    When I open the app, enter valid settings and save
    Then I am taken to my library

  Scenario: Clearing credentials asks first
    When I choose to clear my credentials
    Then I am asked to confirm

  Scenario: Clearing credentials removes them from the device
    Given stored credentials
    When I clear them and confirm
    Then no connection settings remain
    And the connection entry says the app is not configured

  Scenario: Cancelling a clear changes nothing
    Given stored credentials
    When I choose to clear them and cancel
    Then my credentials are unchanged

  Scenario: The secret is hidden until asked for
    Given stored credentials
    When I open the connection screen
    Then the secret is masked
    And I can reveal it deliberately
