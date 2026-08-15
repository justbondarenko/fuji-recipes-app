Feature: Read a recipe from a photo
  As the photographer
  I want to point the app at one of my photos
  So that it tells me which of my recipes I shot it with

  # ── Getting there ───────────────────────────────────────────────────────────

  Scenario: Read is in the bottom bar
    When I open the app
    Then the bottom bar shows Recipes, Read and Settings

  Scenario: Read is not buried in More
    When I open the More tab
    Then there is no Read entry there

  Scenario: The screen opens asking for a photo
    When I open Read
    Then I am offered to choose a photo

  Scenario: Choosing a photo asks for no permission
    When I choose a photo
    Then no storage permission is requested

  # ── Decoding ────────────────────────────────────────────────────────────────

  Scenario: A Fujifilm JPEG yields its settings
    Given a JPEG shot on a Fujifilm body with a known recipe
    When I read it
    Then the film simulation, tones, grain and white balance are shown

  Scenario: The camera model is shown when the file carries it
    Given a JPEG whose EXIF names the body
    When I read it
    Then the body is named

  Scenario: Tone curves are read with their sign the right way round
    Given a photo whose highlight tone tag is -64
    When I read it
    Then the highlight tone shows +4

  Scenario: Noise reduction is read through its table, not arithmetic
    Given a photo whose noise-reduction tag is 736
    When I read it
    Then the high ISO NR shows -4

  Scenario: Clarity is read at its own scale
    Given a photo whose clarity tag is 2000
    When I read it
    Then the clarity shows +2

  Scenario: A colour-temperature white balance carries its Kelvin value
    Given a photo shot with a Kelvin white balance
    When I read it
    Then the white balance is colour temperature
    And the temperature is shown

  Scenario: White-balance shift is read from where the tag points
    Given a photo with a white-balance shift
    When I read it
    Then the red and blue shifts are shown within their range

  Scenario: A value this build cannot name is left out rather than guessed
    Given a photo whose film simulation code is one this build does not know
    When I read it
    Then no film simulation is shown
    And the rest of the settings are

  Scenario: Reading happens on the phone
    Given no network connection
    When I read a photo
    Then the settings are shown

  # ── When there is nothing to read ───────────────────────────────────────────

  Scenario: A file that is not a JPEG is refused by name
    When I choose a file that is not a JPEG
    Then I am told it needs a JPEG
    And that a RAF file is not one

  Scenario: A file over the size limit is refused before it is read
    When I choose a photo larger than 50 MB
    Then I am told the limit

  Scenario: A JPEG with no metadata says so
    Given a JPEG stripped of its EXIF
    When I read it
    Then I am told it carries no metadata

  Scenario: A JPEG from another make is a different answer
    Given a JPEG shot on a camera that is not a Fujifilm
    When I read it
    Then I am told it is not a Fujifilm photo
    And that message differs from the one about missing metadata

  Scenario: A Fujifilm photo that cannot be read says that, rather than showing nothing
    Given a Fujifilm JPEG whose MakerNote this build cannot walk
    When I read it
    Then I am told the file could not be read
    And I am not shown an empty recipe

  # ── Matching ────────────────────────────────────────────────────────────────

  Scenario: A photo shot with a recipe I have is named
    Given a library holding "Kodachrome 64"
    And a photo whose settings are exactly that recipe
    When I read it
    Then I am told it matches "Kodachrome 64"

  Scenario: An exact match offers to open that recipe
    Given an exact match
    Then the primary action opens the matched recipe

  Scenario: An exact match still offers to save a new recipe
    Given an exact match
    Then I am also offered to save the photo as a new recipe

  Scenario: Only the fields the photo carried are compared
    Given a photo carrying eight settings
    When it is matched
    Then it is scored out of eight

  Scenario: A near match names what differs
    Given a photo matching "Kodachrome 64" except for its white balance
    When I read it
    Then I am told it is a near match
    And the white balance is listed as the difference
    And both values are shown

  Scenario: Matches are ordered exact first, then by how close they are
    Given several recipes of differing closeness
    When I read a photo
    Then an exact match comes first
    And the rest follow by percentage

  Scenario: A distant recipe is not offered as a match
    Given a library where the closest recipe matches under 70%
    When I read a photo
    Then no match is offered
    And I am offered to save it as a new recipe

  Scenario: An empty library still reads the photo
    Given an empty library
    When I read a photo
    Then the settings are shown
    And I am offered to save them as a new recipe

  # ── Saving ──────────────────────────────────────────────────────────────────

  Scenario: Saving opens the normal editor, pre-filled
    Given a photo that has been read
    When I choose to save it as a new recipe
    Then the recipe editor opens with those settings already filled in

  Scenario: The suggested name says where it came from
    Given a photo shot with Classic Chrome on an X-T50
    When I choose to save it
    Then the name is suggested from the simulation and the body

  Scenario: Saving goes through the usual validation
    Given the editor opened from a photo
    When I save
    Then the recipe is created the same way any other is

  # ── Copying ─────────────────────────────────────────────────────────────────

  Scenario: The decoded settings can be copied as text
    Given a photo that has been read
    When I copy it as text
    Then the clipboard holds the settings in the same layout the recipe screen uses
