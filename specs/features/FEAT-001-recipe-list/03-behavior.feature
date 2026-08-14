Feature: Recipe list
  As the photographer
  I want my whole recipe library on my phone, searchable and readable offline
  So that I can find the right recipe in seconds with a camera in my other hand

  Background:
    Given the app is installed

  # --- Connection setup ---

  Scenario: First launch with nothing configured
    Given no connection settings are stored
    When I open the app
    Then I am shown the connection screen
    And I am not shown an empty library

  Scenario: A valid service token is accepted
    Given I am on the connection screen
    And the library contains 3 recipes
    When I enter the base URL and a service token that the Access policy admits
    And I test the connection
    Then the connection is reported as working
    And saving it takes me to the list showing 3 recipes

  Scenario: An invalid service token is refused clearly
    Given I am on the connection screen
    When I enter a base URL and a service token the Access policy does not admit
    And I test the connection
    Then I am told the credentials were refused
    And I am not told that the library is empty
    And nothing is saved as a working configuration

  Scenario: The connection test does not rely on the ungated health route
    Given a deployment whose health route answers without credentials
    And a service token the Access policy does not admit
    When I test the connection
    Then the test reports the credentials as refused
    # Regression guard: testing against /api/health would report success here.

  # --- Happy path ---

  Scenario: The library is listed
    Given a configured connection
    And the library contains recipes
    When I open the list
    Then each recipe shows its name, film simulation, rating and tags
    And the header states how many recipes are in the library

  Scenario: A recipe that has been written to a slot shows where and when
    Given a recipe last written to slot 3 on 12 August 2026
    When I view it in the list
    Then its card shows "C3 · 12 Aug 2026"

  Scenario: A recipe that has never been written shows no written line
    Given a recipe that has never been written to a slot
    When I view it in the list
    Then its card shows no last-written line
    And it does not say "Never"

  Scenario: An unrated recipe shows no rating
    Given a recipe with a rating of 0
    When I view it in the list
    Then its card shows no rating
    And it does not show a zero

  Scenario: Opening a recipe goes to the editor
    Given a list of recipes
    When I open a recipe from the list
    Then I am taken to that recipe's editor

  # --- Search ---

  Scenario: Search narrows the list as I type
    Given recipes named "Kodachrome 64", "Acros Night" and "Portra Warm"
    When I search for "acro"
    Then only "Acros Night" is listed

  Scenario: Search matches tags as well as names
    Given a recipe named "Portra Warm" tagged "street"
    When I search for "street"
    Then "Portra Warm" is listed

  Scenario: Every search term must match somewhere
    Given a recipe named "Acros Night" tagged "street"
    And a recipe named "Acros Day" tagged "landscape"
    When I search for "acros street"
    Then only "Acros Night" is listed

  Scenario: Search ignores case
    Given a recipe named "Kodachrome 64"
    When I search for "KODACHROME"
    Then "Kodachrome 64" is listed

  Scenario: An empty search matches everything
    Given a library of 5 recipes
    When I clear the search
    Then all 5 recipes are listed

  # --- Toolbar ---

  Scenario: The filter controls are not on screen until asked for
    Given a library of recipes
    When I open the list
    Then I see a search field, a filters button and a sort control
    And the rating, film simulation and tag controls are not shown

  Scenario: Opening the filters reveals the controls
    Given a library of recipes
    When I open the filters
    Then I can choose a minimum rating, a film simulation and a tag

  Scenario: The filters button reports how many axes are set
    Given no filters are set
    When I filter by a minimum rating and a tag
    Then the filters button shows a badge of 2

  Scenario: A narrowed list says so and offers a way out
    Given a library of 10 recipes
    When I filter down to 3 of them
    Then I am told I am seeing 3 of 10
    And I am offered a way to clear everything

  Scenario: The summary is absent when nothing is narrowed
    Given a library of recipes with no search and no filters
    When I open the list
    Then no narrowing summary is shown

  # --- Filters ---

  Scenario: Filtering by minimum rating excludes lower-rated recipes
    Given recipes rated 5, 3 and 0
    When I filter by a minimum rating of 4
    Then only the recipe rated 5 is listed

  Scenario: A minimum rating of zero includes unrated recipes
    Given recipes rated 5, 3 and 0
    When I set the minimum rating to 0
    Then all 3 recipes are listed

  Scenario: Filtering by several tags requires all of them
    Given a recipe tagged "street" and "warm"
    And a recipe tagged "street" only
    When I filter by the tags "street" and "warm"
    Then only the recipe with both tags is listed

  Scenario: Filtering by several film simulations includes any of them
    Given recipes using Classic Chrome, ACROS and Velvia
    When I filter by Classic Chrome and Velvia
    Then the Classic Chrome and Velvia recipes are listed
    And the ACROS recipe is not listed

  Scenario: The filter badge counts axes, not values
    Given no filters are set
    When I filter by three tags and a minimum rating
    Then the filter badge shows 2

  Scenario: Search and filters combine
    Given a recipe named "Acros Night" rated 5
    And a recipe named "Acros Day" rated 2
    When I search for "acros" and filter by a minimum rating of 4
    Then only "Acros Night" is listed

  # --- Sorting ---

  Scenario: The default sort is by name
    Given a library of recipes in manual order
    When I open the list for the first time
    Then the recipes are ordered by name ascending

  Scenario: Name sort orders numbers the way people read them
    Given recipes named "Portra 2" and "Portra 10"
    When I sort by name
    Then "Portra 2" is listed before "Portra 10"

  Scenario: Rating sort puts the highest first
    Given recipes rated 2, 5 and 3
    When I sort by rating
    Then the recipes are listed in the order 5, 3, 2

  Scenario: Equal sort values fall back to manual order
    Given two recipes both rated 4
    And the first has a lower sort key than the second
    When I sort by rating
    Then the recipe with the lower sort key is listed first

  Scenario: Equal sort keys fall back to creation time
    Given two recipes with the same sort key
    And the first was created earlier
    When the list falls back to manual order
    Then the earlier recipe is listed first

  Scenario: Sorting does not change the underlying order
    Given a library in manual order
    When I sort by name and then clear the sort back to the default
    Then the manual order is unchanged

  # --- Remembered view ---

  Scenario: Filters and sort survive a restart
    Given I have filtered by the tag "street" and sorted by rating
    When I close and reopen the app
    Then the tag filter "street" is still applied
    And the sort is still by rating

  Scenario: The search text does not survive a restart
    Given I have searched for "acros"
    When I close and reopen the app
    Then the search is empty
    And the full library is listed

  Scenario: A stored filter naming an unknown film simulation is dropped
    Given a stored view naming a film simulation this build does not know
    When I open the list
    Then that simulation filter is not applied
    And the rest of the stored view is applied

  Scenario: A stored rating outside the valid range is clamped
    Given a stored view with a minimum rating of 9
    When I open the list
    Then the minimum rating in effect is 5

  Scenario: A stored tag that matches nothing is kept
    Given a stored view filtering by a tag no recipe currently carries
    When I open the list
    Then the tag filter is still applied
    And I am shown the no-matches state

  # --- Reversal ---

  Scenario: Clearing search and filters restores the full list
    Given a list narrowed by search and filters
    When I clear the search and filters
    Then the full library is listed in its previous order

  # --- Offline and caching ---

  Scenario: A successful fetch is cached
    Given a configured connection and a reachable server
    When the list loads successfully
    Then the response is stored as a snapshot

  Scenario: The library is readable with no network
    Given a snapshot from an earlier successful fetch
    And the device has no network connection
    When I open the app
    Then my library is listed
    And the end of the list says when it was last updated

  Scenario: A freshly refreshed library also reports when it was updated
    Given a configured connection and a reachable server
    When the list loads successfully
    Then the end of the list says when it was last updated
    And no warning is shown about the library being cached

  Scenario: The cached list appears before the refresh completes
    Given a snapshot from an earlier successful fetch
    And a slow network
    When I open the app
    Then my library is listed immediately
    And a refresh runs without blocking the list

  Scenario: A snapshot from a different deployment is not shown
    Given a snapshot taken from one base URL
    When I change the base URL to a different deployment
    Then the old snapshot is not listed

  Scenario: No network and no snapshot
    Given no snapshot is stored
    And the device has no network connection
    When I open the list
    Then I am shown a connection error with a retry
    And I am not shown an empty library

  # --- Empty and no-match states ---

  Scenario: An empty library invites me to start
    Given a configured connection
    And the library contains no recipes
    When I open the list
    Then I am shown an empty-library state offering to create a recipe
    And no error is shown

  Scenario: Search and filters that match nothing
    Given a library of 40 recipes
    When I search for something no recipe matches
    Then I am told that none of my 40 recipes match
    And I am offered a way to clear the search and filters

  # --- Failure states, each distinct ---

  Scenario: The credentials are refused
    Given a configured connection whose service token has been revoked
    When I open the list
    Then I am told my credentials were refused
    And I am offered a route to the connection screen
    And I am not shown an empty library

  Scenario: The server's Access gate is not configured
    Given a server that answers with access_unconfigured
    When I open the list
    Then I am told the server's access configuration is incomplete
    And the message names the setting the server reported as missing
    And I am not told my credentials are wrong

  Scenario: The database is unreachable
    Given a server that answers with storage_unavailable
    When I open the list
    Then I am told the library is stored and temporarily unreachable
    And I am offered a retry
    And I am not told anything has been lost

  Scenario: An unexpected server failure
    Given a server that answers with an internal error
    When I open the list
    Then I am shown the request id from the response

  Scenario: A malformed response does not lose the cached library
    Given a snapshot from an earlier successful fetch
    And a server that answers with a body this build cannot parse
    When I open the list
    Then my cached library is still listed
    And the snapshot is not overwritten

  # --- Forward compatibility ---

  Scenario: A field this build does not understand is preserved
    Given the server sends a recipe carrying a property this build does not know
    When the response is cached and read back
    Then that property is still present on the recipe

  Scenario: An unknown film simulation still renders
    Given a recipe whose film simulation id this build does not know
    When I view it in the list
    Then its card is shown with a fallback swatch
    And the list does not crash or omit the recipe
