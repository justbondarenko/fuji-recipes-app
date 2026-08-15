Feature: Write to slot
  As the photographer
  I want to send a recipe to a custom slot and know exactly what landed
  So that the app finishes the job it exists for

  # ── WR-01: refusals come first ──────────────────────────────────────────────

  Scenario: A body with no custom slots is refused outright
    Given a connected X-Trans III body
    When I build a plan for any recipe
    Then the plan refuses
    And it names the generation and that it has no custom slots
    And it contains no steps

  Scenario: An unrecognised body is refused
    Given a connected body the app does not recognise
    When I open the write sheet
    Then the write action is unavailable
    And the sheet says the app does not know this body

  Scenario: A slot outside C1 to C7 is refused
    When I build a plan for slot 8
    Then the plan refuses
    And it names C1 through C7

  # ── Plan shape ──────────────────────────────────────────────────────────────

  Scenario: Every plan begins by selecting the slot and naming the preset
    Given a connected X-Trans V body
    When I build a plan for slot 3
    Then the first step writes 3 to the slot property
    And the second step writes the preset name
    And both are fatal

  Scenario: The film simulation is written first among the settings
    When I build a plan
    Then the film simulation precedes every other setting

  Scenario: Colour temperature immediately follows the white balance mode
    Given a recipe using a colour temperature white balance
    When I build a plan
    Then the colour temperature step immediately follows the white balance step

  Scenario: The plan is pure
    Given the same recipe, camera and slot
    When I build a plan twice
    Then the two plans are identical

  # ── Skip rules ──────────────────────────────────────────────────────────────

  Scenario: A monochrome simulation drops colour
    Given a recipe using ACROS
    When I build a plan
    Then no step writes the colour property

  Scenario: Grain size is skipped when the grain effect is off
    Given a recipe with grain off
    When I build a plan
    Then no step writes a grain size

  Scenario: Grain strength and size encode as one value
    Given a recipe with strong, large grain
    When I build a plan
    Then one step carries the combined grain code

  Scenario: Monochromatic colour is written only on X-Trans V monochrome
    Given a monochrome recipe with a warm/cool shift
    When I build a plan for an X-Trans IV body
    Then the monochromatic colour steps are dropped
    And they are reported as dropped

  Scenario: A monochromatic colour of zero is omitted rather than written
    Given a monochrome recipe on an X-Trans V body with a monochromatic colour of zero
    When I build a plan
    Then no step writes that property

  Scenario: Advisory fields are never written and never reported as dropped
    Given a recipe carrying a dynamic range priority and an exposure compensation
    When I build a plan
    Then no step writes them
    And they do not appear among the dropped fields

  Scenario: A field the body's generation cannot take is dropped and reported
    Given a recipe carrying a field that applies only to X-Trans V
    When I build a plan for an X-Trans IV body
    Then the field is dropped
    And it appears in the dropped list with a reason

  Scenario: A simulation the body does not have is dropped and reported
    Given a recipe using Nostalgic Negative
    When I build a plan for an X-Trans IV body
    Then the film simulation is dropped
    And it appears in the dropped list

  # ── Encoding ────────────────────────────────────────────────────────────────

  Scenario: High ISO noise reduction uses the lookup table
    Given each documented noise-reduction value
    When it is encoded
    Then the result comes from the lookup table and not from arithmetic

  Scenario: Effects are one-indexed
    When an effect of off, weak and strong is encoded
    Then the codes are 1, 2 and 3

  Scenario: Dynamic range is a raw percentage
    When a dynamic range of 100, 200 and 400 is encoded
    Then the codes are 100, 200 and 400

  Scenario: Tone parameters are multiplied by ten
    When a highlight tone of +1.5 is encoded
    Then the code is 15

  Scenario: A step whose value does not match its kind is refused
    Given a numeric step carrying a string
    When its payload is built
    Then it is refused before anything is sent
    And the message names the step

  # ── The sheet ───────────────────────────────────────────────────────────────

  Scenario: A connected camera skips the connection stage
    Given a connected camera
    When I choose to write a recipe
    Then the sheet opens on the slot picker

  Scenario: A compatible recipe shows no compatibility warning
    Given a recipe every parameter of which this body takes
    When I choose to write it
    Then no compatibility warning is shown

  Scenario: Dropped parameters are listed before anything is sent
    Given a recipe with parameters this body cannot take
    When I choose to write it
    Then the sheet lists each dropped parameter by name
    And it offers to write anyway or cancel

  Scenario: Slot contents are honestly unknown
    When I reach the slot picker
    Then each slot's contents read as unknown

  Scenario: Choosing a slot asks for a second tap
    When I choose C3
    Then I am asked to confirm before the write begins

  Scenario: Progress names the parameter currently being written
    Given a write in progress
    Then the sheet shows the completed count against the total
    And it names the parameter being written

  Scenario: The sheet cannot be dismissed mid-write
    Given a write in progress
    When I tap outside the sheet
    Then the sheet stays open

  Scenario: Backing out mid-write asks first
    Given a write in progress
    When I press back
    Then I am asked to confirm cancelling

  Scenario: A successful write names the slot and confirms by touch
    Given a write that completes
    Then the sheet names the slot written
    And a confirming haptic is played

  Scenario: A failed parameter is named with what the camera said
    Given a camera that refuses the clarity property
    When the write reaches it
    Then the sheet names clarity and the response code
    And it offers to retry
    And a rejecting haptic is played

  Scenario: One unsupported parameter does not abandon the rest
    Given a camera that refuses one non-fatal parameter
    When the write runs
    Then the remaining parameters are still written
    And the result reports the one that failed

  Scenario: A refused slot select abandons the write
    Given a camera that refuses the slot select
    When the write runs
    Then no further steps are attempted

  Scenario: Unplugging mid-write warns that the slot may be partly written
    Given a write in progress
    When the camera is unplugged
    Then the sheet reports the interruption
    And it warns that the slot may be partly written

  # ── Boundaries ──────────────────────────────────────────────────────────────

  Scenario: Writing reports nothing to the server
    Given a successful write
    Then no network request is made

  Scenario: A write works with no signal
    Given no network connection and a connected camera
    When I write a recipe
    Then the write succeeds
