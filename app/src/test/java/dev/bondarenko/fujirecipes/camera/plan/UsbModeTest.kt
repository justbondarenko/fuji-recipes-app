package dev.bondarenko.fujirecipes.camera.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsbModeTest {

    @Test
    fun `maps the three values libfuji documents`() {
        assertEquals(UsbMode.TETHER_SHOOTING, usbModeFor(5))
        assertEquals(UsbMode.RAW_CONVERSION, usbModeFor(6))
        assertEquals(UsbMode.WEBCAM, usbModeFor(8))
    }

    @Test
    fun `a value this build has not seen is unrecognised, not guessed at`() {
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(0))
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(7))
        assertEquals(UsbMode.UNRECOGNISED, usbModeFor(99))
    }

    /**
     * The load-bearing half. A mode warning shown to someone whose camera is set correctly is
     * worse than no warning at all, so only a positively-identified wrong mode may raise one.
     */
    @Test
    fun `only a positively identified wrong mode warns`() {
        assertTrue(UsbMode.TETHER_SHOOTING.isKnownWrongMode)
        assertTrue(UsbMode.WEBCAM.isKnownWrongMode)
        // Right mode for the card, wrong mode for the slots — and this flag is about slots.
        assertTrue(UsbMode.CARD_READER.isKnownWrongMode)

        assertFalse(UsbMode.RAW_CONVERSION.isKnownWrongMode)
        assertFalse(UsbMode.UNREPORTED.isKnownWrongMode)
        assertFalse(UsbMode.UNRECOGNISED.isKnownWrongMode)
    }

    @Test
    fun `each mode allows exactly the work it is for`() {
        assertTrue(UsbMode.CARD_READER.allowsCardBrowsing)
        assertFalse(UsbMode.CARD_READER.allowsRecipeWork)

        assertTrue(UsbMode.RAW_CONVERSION.allowsRecipeWork)
        assertFalse(UsbMode.RAW_CONVERSION.allowsCardBrowsing)

        listOf(UsbMode.TETHER_SHOOTING, UsbMode.WEBCAM, UsbMode.UNREPORTED, UsbMode.UNRECOGNISED)
            .forEach { mode ->
                assertFalse(mode.allowsCardBrowsing, "$mode")
                assertFalse(mode.allowsRecipeWork, "$mode")
            }
    }

    // ─── Card-reader detection ──────────────────────────────────────────────

    /** What an X-T50 in USB CARD READER actually advertised, from the 2026-09-09 report. */
    private val cardReaderOperations =
        listOf(0x1001, 0x1002, 0x1007, 0x1008, 0x1009, 0x9801, 0x9802, 0x9803, 0x9805)
    private val cardReaderProperties = listOf(0x5001, 0xd406, 0xd407)

    /** And what the same body advertised in RAW conv./backup restore. */
    private val rawConversionOperations = listOf(0x1001, 0x1002, 0x1014, 0x1015, 0x1016, 0x900c)
    private val rawConversionProperties = listOf(0xd16e, 0xd18c, 0xd192, 0xd36a)

    @Test
    fun `the MTP signature identifies a card reader`() {
        assertTrue(looksLikeCardReader(cardReaderOperations, cardReaderProperties))
    }

    @Test
    fun `the same body in RAW conversion mode is not mistaken for one`() {
        assertFalse(looksLikeCardReader(rawConversionOperations, rawConversionProperties))
    }

    /**
     * Both halves are required. Either alone is thin evidence, and a false positive here sends
     * the user to change a camera setting that was already right.
     */
    @Test
    fun `neither half of the signature is enough on its own`() {
        assertFalse(looksLikeCardReader(cardReaderOperations, rawConversionProperties))
        assertFalse(looksLikeCardReader(rawConversionOperations, cardReaderProperties))
    }

    @Test
    fun `a body that advertises nothing is not a card reader`() {
        assertFalse(looksLikeCardReader(emptyList(), emptyList()))
    }

    /** One missing object-property operation is a body this build has not seen. Say nothing. */
    @Test
    fun `a partial operation signature is not accepted`() {
        assertFalse(looksLikeCardReader(listOf(0x9801, 0x9802), cardReaderProperties))
    }

    // ─── Which source wins ──────────────────────────────────────────────────

    /**
     * The property is the body's own statement about its menu; the device info is an
     * inference. Where they could disagree, the statement wins.
     */
    @Test
    fun `the property beats the inference wherever it answers`() {
        assertEquals(
            UsbMode.RAW_CONVERSION,
            usbModeFrom(6, cardReaderOperations, cardReaderProperties),
        )
    }

    @Test
    fun `a refusal falls through to the signature`() {
        assertEquals(
            UsbMode.CARD_READER,
            usbModeFrom(null, cardReaderOperations, cardReaderProperties),
        )
    }

    /**
     * The inference may only ever conclude "card reader" or "still could not tell". Producing
     * one of the values 0xD16E would have given would put a fabrication where a fact belongs.
     */
    @Test
    fun `a refusal with no signature stays unreported`() {
        assertEquals(
            UsbMode.UNREPORTED,
            usbModeFrom(null, rawConversionOperations, rawConversionProperties),
        )
        assertEquals(UsbMode.UNREPORTED, usbModeFrom(null, emptyList(), emptyList()))
    }

    @Test
    fun `an unrecognised reported value is still reported as unrecognised`() {
        assertEquals(
            UsbMode.UNRECOGNISED,
            usbModeFrom(99, cardReaderOperations, cardReaderProperties),
        )
    }
}
