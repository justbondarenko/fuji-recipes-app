Feature: Camera connection
  As the photographer
  I want the app to find and identify my camera when I plug it in
  So that the cable does something and I can trust what it says it is talking to

  # ── Availability ────────────────────────────────────────────────────────────

  Scenario: A device without USB host support says so permanently
    Given a phone with no USB host support
    When I open the app
    Then the camera chip says there is no USB
    And it offers no way to retry

  Scenario: No camera attached
    Given a phone with USB host support and nothing plugged in
    When I open the app
    Then the camera chip invites me to connect

  # ── Attaching ───────────────────────────────────────────────────────────────

  Scenario: Plugging the camera in opens the app already connected
    Given the app is not running
    When I plug in a Fujifilm camera
    Then the app opens
    And no USB permission dialog is shown
    And the camera chip shows the body's model name

  Scenario: Connecting from inside the app asks for permission once
    Given the app is open and a camera is attached without permission
    When I tap the camera chip and choose to connect
    Then I am asked for USB permission
    And granting it leaves the chip showing the model name

  Scenario: Refusing permission is reported as refused permission
    Given the app is open and a camera is attached
    When I decline the USB permission request
    Then the chip shows an error naming the refusal
    And the sheet offers to try again

  Scenario: Unplugging returns the app to disconnected
    Given a connected camera
    When I unplug it
    Then the camera chip invites me to connect
    And the claimed interface has been released

  # ── Claiming the right interface ────────────────────────────────────────────

  Scenario: The still-image interface is preferred
    Given a camera exposing a vendor interface and a still-image interface
    When the app connects
    Then it claims the still-image interface

  Scenario: A refused interface falls through to the next candidate
    Given a camera whose first candidate interface refuses the claim
    When the app connects
    Then it claims the next candidate with a bulk pair
    And the connection succeeds

  Scenario: Every interface refused
    Given a camera whose every interface refuses the claim
    When the app connects
    Then the sheet says the camera could not be claimed
    And it names another application holding the device as the likely cause

  Scenario: A camera in the wrong USB mode
    Given a camera exposing no interface with a bulk in/out pair
    When the app connects
    Then the sheet says the camera is probably in the wrong USB mode
    And it names the RAW-conversion mode as the one to set

  Scenario: Endpoints are discovered rather than assumed
    Given a camera whose bulk endpoints are not numbered 1 and 2
    When the app connects
    Then it reads the endpoint addresses off the claimed interface
    And the connection succeeds

  # ── Framing ─────────────────────────────────────────────────────────────────

  Scenario: A command container round-trips
    Given a command container with an operation code, a transaction id and two parameters
    When it is packed and unpacked
    Then every field comes back unchanged

  Scenario: A data container's payload is not read as parameters
    Given a data container carrying a four-byte property value
    When it is unpacked
    Then the payload is the four bytes
    And no parameters are reported

  Scenario: A response longer than one transfer is read to completion
    Given a device info dataset larger than one bulk transfer
    When the app reads it
    Then it keeps reading until the container's declared length is satisfied

  Scenario: An empty PTP string is a single zero byte
    When an empty string is packed
    Then the result is one byte of zero

  Scenario: A PTP string counts characters including its terminator
    When the name "C3" is packed
    Then the length byte is 3
    And the payload is UCS-2 followed by a NUL

  Scenario: A response for a different transaction is not accepted as the answer
    Given a stale response left on the wire
    When the app issues a command
    Then it does not treat the stale response as its own

  # ── Identity ────────────────────────────────────────────────────────────────

  Scenario: A known body is identified and writable
    Given a camera reporting "X100VI"
    When the app reads its device info
    Then the chip shows "X100VI"
    And the sheet reports X-Trans V
    And writes are available

  Scenario: Model punctuation does not decide recognition
    Given a camera reporting "XT5"
    When the app identifies it
    Then it is recognised as the X-T5

  Scenario: A body with no custom slots is connected but not writable
    Given a camera reporting "X-T2"
    When the app identifies it
    Then the sheet reports X-Trans III
    And it says that generation has no custom slots
    And writes are unavailable

  Scenario: An unrecognised body is honest about being unrecognised
    Given a camera reporting "X-T99"
    When the app identifies it
    Then the sheet says the app does not know this body
    And it does not name a sensor generation
    And writes are unavailable
    And it says storing recipes works normally

  Scenario: No two known model names collide when normalised
    Given the model table
    Then no two entries normalise to the same key

  # ── Failure reporting ───────────────────────────────────────────────────────

  Scenario: A refused operation names what the camera said
    Given a camera answering DeviceBusy to open session
    When the app connects
    Then the sheet names the operation and "DeviceBusy"
    And it does not show only a hex code

  Scenario: A camera that stops answering is reported as a timeout
    Given a camera that never answers a command
    When the transfer timeout elapses
    Then the sheet says the camera stopped answering
    And it offers to try again

  Scenario: The three failures do not read the same
    Given a permission refusal, a busy interface and a timeout
    Then each produces a different message
    And each names a different remedy

  # ── State survives the app ──────────────────────────────────────────────────

  Scenario: A connection survives navigation
    Given a connected camera
    When I open a recipe and go back
    Then the camera is still connected
