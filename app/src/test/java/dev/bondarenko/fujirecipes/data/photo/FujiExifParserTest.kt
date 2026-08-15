package dev.bondarenko.fujirecipes.data.photo

import dev.bondarenko.fujirecipes.data.photo.SyntheticJpeg.i32
import dev.bondarenko.fujirecipes.data.photo.SyntheticJpeg.u16
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FEAT-009 T-06.
 *
 * The reference's suite guards on a sample JPEG absent from its repository, so its real case
 * never runs. These build the file, which means every value asserted here was put into a
 * chosen tag on purpose — and a byte offset that is wrong by two shows up as a wrong recipe
 * rather than as an exception.
 */
class FujiExifParserTest {

    private fun read(bytes: ByteArray): PhotoRecipe {
        val result = parseRecipeFromJpeg(bytes)
        return assertIs<PhotoReadResult.Success>(result).recipe
    }

    private fun failure(bytes: ByteArray): PhotoReadFailure =
        assertIs<PhotoReadResult.Failure>(parseRecipeFromJpeg(bytes)).reason

    /** A full recipe: Classic Chrome, DR400, tones, grain, chrome, Kelvin white balance. */
    private val fullRecipe = SyntheticJpeg.fujifilm(
        tags = listOf(
            u16(5121, 1536), // film mode — Classic Chrome
            u16(5122, 513), // DR setting — 400
            i32(4161, -32), // highlight tone — +2
            i32(4160, -16), // shadow tone — +1
            u16(4099, 256), // colour — +2
            u16(4097, 132), // sharpness — +1
            u16(4110, 736), // high ISO NR — −4
            i32(4111, 2000), // clarity — +2
            u16(4167, 64), // grain roughness — strong
            u16(4172, 32), // grain size — large
            u16(4168, 32), // colour chrome — weak
            u16(4174, 64), // FX blue — strong
            u16(4098, 4080), // white balance — Kelvin
            u16(4101, 6500), // colour temperature
        ),
        wbShift = 40 to -80, // ÷20 → +2 / −4
    )

    // ─── The whole thing ────────────────────────────────────────────────────

    @Test
    fun `a Fujifilm JPEG yields its recipe`() {
        val recipe = read(fullRecipe)

        assertEquals("classic-chrome", recipe.settings["filmSimulation"])
        assertEquals("Classic Chrome", recipe.filmSimulationLabel)
        assertEquals("dr400", recipe.settings["dynamicRange"])
        assertEquals("strong", recipe.settings["grainEffect"])
        assertEquals("large", recipe.settings["grainSize"])
        assertEquals("weak", recipe.settings["colorChromeEffect"])
        assertEquals("strong", recipe.settings["colorChromeFxBlue"])
    }

    @Test
    fun `the camera model comes from standard EXIF`() {
        assertEquals("X-T50", read(fullRecipe).cameraModel)
    }

    @Test
    fun `a file with no model still reads its recipe`() {
        val recipe = read(SyntheticJpeg.fujifilm(listOf(u16(5121, 1536)), model = null))

        assertNull(recipe.cameraModel)
        assertEquals("classic-chrome", recipe.settings["filmSimulation"])
    }

    // ─── The dialect, value by value ────────────────────────────────────────

    /**
     * The single easiest thing in this file to get backwards. `-64` is `+4`, not `-4`, and
     * inverting it produces a recipe that is entirely plausible and exactly wrong.
     */
    @Test
    fun `tone curves are negated and scaled`() {
        fun tone(raw: Int) = read(SyntheticJpeg.fujifilm(listOf(i32(4161, raw))))
            .settings["highlightTone"]

        assertEquals(4.0, tone(-64))
        assertEquals(2.0, tone(-32))
        assertEquals(0.0, tone(0))
        assertEquals(-1.0, tone(16))
        assertEquals(-2.0, tone(32))
    }

    /** Not monotonic and not arithmetic — a table, like the custom-slot one. */
    @Test
    fun `noise reduction reads through its table`() {
        fun nr(raw: Int) = read(SyntheticJpeg.fujifilm(listOf(u16(4110, raw))))
            .settings["highIsoNR"]

        assertEquals(-4, nr(736))
        assertEquals(0, nr(0))
        assertEquals(2, nr(256))
        assertEquals(4, nr(480))
    }

    @Test
    fun `clarity reads at its own scale`() {
        fun clarity(raw: Int) = read(SyntheticJpeg.fujifilm(listOf(i32(4111, raw))))
            .settings["clarity"]

        assertEquals(2, clarity(2000))
        assertEquals(-5, clarity(-5000))
        assertEquals(0, clarity(0))
    }

    @Test
    fun `saturation and sharpness read through their tables`() {
        assertEquals(-4, read(SyntheticJpeg.fujifilm(listOf(u16(4099, 1248)))).settings["color"])
        assertEquals(-1, read(SyntheticJpeg.fujifilm(listOf(u16(4097, 130)))).settings["sharpness"])
    }

    @Test
    fun `dynamic range maps to this app's own values`() {
        fun dr(setting: Int) = read(SyntheticJpeg.fujifilm(listOf(u16(5122, setting))))
            .settings["dynamicRange"]

        assertEquals("dr-auto", dr(0))
        assertEquals("dr100", dr(256))
        assertEquals("dr400", dr(513))
    }

    /** Setting 1 means "manual", and the percentage is in the next tag along. */
    @Test
    fun `a manual dynamic range reads its value from the second tag`() {
        val recipe = read(SyntheticJpeg.fujifilm(listOf(u16(5122, 1), u16(5123, 200))))

        assertEquals("dr200", recipe.settings["dynamicRange"])
    }

    @Test
    fun `a Kelvin white balance carries its temperature`() {
        val recipe = read(fullRecipe)

        assertEquals("color-temp", recipe.settings["whiteBalance"])
        assertEquals(6500, recipe.settings["colorTemperature"])
    }

    @Test
    fun `a non-Kelvin white balance carries no temperature`() {
        val recipe = read(SyntheticJpeg.fujifilm(listOf(u16(4098, 256), u16(4101, 6500))))

        assertEquals("daylight", recipe.settings["whiteBalance"])
        assertFalse(recipe.settings.containsKey("colorTemperature"))
    }

    @Test
    fun `white balance maps to this app's ids, not the camera's wording`() {
        fun wb(raw: Int) = read(SyntheticJpeg.fujifilm(listOf(u16(4098, raw))))
            .settings["whiteBalance"]

        assertEquals("auto", wb(0))
        assertEquals("auto-ambience-priority", wb(2))
        assertEquals("color-temp", wb(4080))
    }

    /** The value field is an *offset*, not the value: two int32s live elsewhere in the block. */
    @Test
    fun `the white-balance shift is read from where its tag points`() {
        val recipe = read(fullRecipe)

        assertEquals(2, recipe.settings["wbShiftRed"])
        assertEquals(-4, recipe.settings["wbShiftBlue"])
    }

    @Test
    fun `a white-balance shift beyond the field's range is bounded rather than dropped`() {
        val recipe = read(
            SyntheticJpeg.fujifilm(listOf(u16(4098, 256)), wbShift = 1000 to -1000),
        )

        assertEquals(9, recipe.settings["wbShiftRed"])
        assertEquals(-9, recipe.settings["wbShiftBlue"])
    }

    /** WR-04's rule from the reading side: no size when the grain is off. */
    @Test
    fun `grain off carries no size`() {
        val recipe = read(SyntheticJpeg.fujifilm(listOf(u16(4167, 0), u16(4172, 32))))

        assertEquals("off", recipe.settings["grainEffect"])
        assertFalse(recipe.settings.containsKey("grainSize"))
    }

    // ─── What it refuses to guess ───────────────────────────────────────────

    /**
     * The rule that keeps a wrong recipe from matching something. A missing field is useful;
     * a fabricated one is worse than useless.
     */
    @Test
    fun `a code this build cannot name omits its field and keeps the rest`() {
        val recipe = read(
            SyntheticJpeg.fujifilm(listOf(u16(5121, 9999), u16(4097, 132))),
        )

        assertFalse(recipe.settings.containsKey("filmSimulation"))
        assertNull(recipe.filmSimulationLabel)
        assertEquals(1, recipe.settings["sharpness"])
    }

    @Test
    fun `an unknown tag is skipped rather than breaking the walk`() {
        val recipe = read(
            SyntheticJpeg.fujifilm(listOf(u16(9998, 1), u16(5121, 1536), u16(9999, 2))),
        )

        assertEquals("classic-chrome", recipe.settings["filmSimulation"])
    }

    // ─── The named failures (P5) ────────────────────────────────────────────

    @Test
    fun `a file that is not a JPEG is refused as such`() {
        assertEquals(PhotoReadFailure.NOT_JPEG, failure(SyntheticJpeg.notJpeg()))
    }

    @Test
    fun `a JPEG with no EXIF and one from another make are different answers`() {
        assertEquals(PhotoReadFailure.NO_EXIF, failure(SyntheticJpeg.noExif()))
        assertEquals(PhotoReadFailure.NOT_FUJIFILM, failure(SyntheticJpeg.otherMake()))
    }

    @Test
    fun `an empty file is not a JPEG rather than a crash`() {
        assertEquals(PhotoReadFailure.NOT_JPEG, failure(ByteArray(0)))
    }

    @Test
    fun `a truncated Fujifilm file is unreadable rather than an empty recipe`() {
        val full = fullRecipe
        val truncated = full.copyOfRange(0, full.size - 60)

        val result = parseRecipeFromJpeg(truncated)

        // Either it cannot find a plausible MakerNote at all, or it finds one it cannot walk.
        // What it must never do is report success with nothing in it.
        if (result is PhotoReadResult.Success) {
            assertTrue(
                result.recipe.settings.isNotEmpty(),
                "reported success with an empty recipe",
            )
        }
    }

    @Test
    fun `a file over the size limit is refused before it is read`() {
        val huge = ByteArray((MAX_PHOTO_BYTES + 1).toInt())

        assertEquals(PhotoReadFailure.TOO_LARGE, failure(huge))
    }

    // ─── For display ────────────────────────────────────────────────────────

    @Test
    fun `the readable values carry signs and units`() {
        val raw = read(fullRecipe).rawValues

        assertEquals("Classic Chrome", raw["Film simulation"])
        assertEquals("DR400", raw["Dynamic range"])
        assertEquals("+2", raw["Highlight tone"])
        assertEquals("-4", raw["High ISO NR"])
        assertEquals("6500K", raw["Colour temperature"])
        assertEquals("R +2 / B -4", raw["WB shift"])
        assertEquals("Strong, Large", raw["Grain effect"])
    }
}
