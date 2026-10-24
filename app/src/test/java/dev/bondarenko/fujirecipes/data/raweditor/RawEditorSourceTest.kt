package dev.bondarenko.fujirecipes.data.raweditor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.file.Files

class RawEditorSourceTest {
    @Test
    fun curvesGrainAndAllMaskTypesRoundTripAndRejectMalformedPayloads() {
        val curve = RawCurve(listOf(CurvePoint(0f,0f),CurvePoint(.3f,.15f),CurvePoint(.7f,.85f),CurvePoint(1f,1f)))
        val masks = RawMaskKind.entries.map { kind -> RawMask(kind, inverted = true, opacity = .7f,
            adjustments = RawLocalAdjustments(1f,.3f,-.2f,.4f,-.1f,.2f,-.3f),
            strokes = if (kind == RawMaskKind.BRUSH) listOf(RawBrushStroke(listOf(MaskPoint(.2f,.3f),MaskPoint(.4f,.5f)),.05f),
                RawBrushStroke(listOf(MaskPoint(.3f,.4f)),.02f,true)) else emptyList()) }
        val edits = RawAdjustments(grainAmount = .6f, grainSize = 4f, grainRoughness = .8f,
            curves = RawCurves(curve,curve,curve,curve), masks = masks)
        assertEquals(edits, RawAdjustments.fromValues(edits.nativeValues()))
        assertEquals(masks, edits.rotated(1).copy(crop = RawCrop(.1f,.1f,.9f,.9f)).masks)
        for (length in listOf(16,29,36)) assertEquals(RawAdjustments(), RawAdjustments.fromValues(RawAdjustments().nativeValues().copyOf(length)))
        val values = edits.nativeValues()
        for (n in 39 until values.size) assertThrows(IllegalArgumentException::class.java) { RawAdjustments.fromValues(values.copyOf(n)) }
        for (index in listOf(36,37,38,39,40,41,values.lastIndex)) {
            val bad = values.copyOf().also { it[index] = Float.NaN }
            assertThrows(IllegalArgumentException::class.java) { RawAdjustments.fromValues(bad) }
        }
        assertThrows(IllegalArgumentException::class.java) { edits.copy(masks = List(9) { masks.first() }).nativeValues() }
        assertThrows(IllegalArgumentException::class.java) { RawCurve(listOf(CurvePoint(0f,0f),CurvePoint(.001f,.5f),CurvePoint(1f,1f))) }
        var previous = 0f
        repeat(1001) { val value = curve.sample(it/1000f); assertTrue(value >= previous && value <= 1f); previous = value }
        assertEquals(.15f,curve.sample(.3f),.00001f)
        assertTrue(curve.moved(1,1f,.5f).points[1].x <= .69f)
        assertEquals(0f,curve.moved(0,.5f,.2f).points.first().x)
        val closePoints = RawCurve(listOf(CurvePoint(0f,0f),CurvePoint(.3f,.3f),CurvePoint(.31f,.5f),CurvePoint(.32f,.7f),CurvePoint(1f,1f)))
        assertEquals(.4f,closePoints.moved(2,.9f,.4f).points[2].y)
    }

    @Test
    fun restoresOldSessionsAndRoundTripsEffectsAndFraming() {
        val old = RawAdjustments(shadows = .4f, temperature = .2f, sharpenRadius = 1.8f)
        assertEquals(old, RawAdjustments.fromValues(old.nativeValues().copyOf(16)))
        val edits = old.copy(clarity = .6f, texture = -.4f, dehaze = .3f,
            crop = RawCrop(.1f, .2f, .8f, .9f), quarterTurns = 3, straighten = -4.5f,
            vignette = -.5f, vignetteMidpoint = .4f, vignetteRoundness = -.3f, vignetteFeather = .7f)
        assertEquals(edits, RawAdjustments.fromValues(edits.nativeValues().copyOf(29)))
        assertEquals(edits, RawAdjustments.fromValues(edits.nativeValues()))
        val optics = edits.copy(distortion = -.4f, perspectiveVertical = .3f, perspectiveHorizontal = -.2f,
            chromaticRed = .5f, chromaticBlue = -.5f, defringePurple = .6f, defringeGreen = .7f)
        assertEquals(optics, RawAdjustments.fromValues(optics.nativeValues()))
        assertThrows(IllegalArgumentException::class.java) { RawAdjustments.fromValues(FloatArray(28)) }
    }

    @Test
    fun rotationKeepsPerspectiveAttachedToThePhoto() {
        val start = RawAdjustments(perspectiveVertical = .4f, perspectiveHorizontal = -.2f,
            crop = RawCrop(.1f, .2f, .8f, .9f), distortion = .3f, chromaticRed = -.3f)
        val rotated = start.rotated(1)
        assertEquals(-.2f, rotated.perspectiveVertical)
        assertEquals(-.4f, rotated.perspectiveHorizontal)
        assertEquals(start.distortion, rotated.distortion)
        assertEquals(start.chromaticRed, rotated.chromaticRed)
        val restored = rotated.rotated(0)
        assertEquals(start.perspectiveVertical, restored.perspectiveVertical)
        assertEquals(start.perspectiveHorizontal, restored.perspectiveHorizontal)
        assertEquals(start.crop.left, restored.crop.left, .00001f)
        assertEquals(RawAdjustments(), RawAdjustments().rotated(1).rotated(0))
    }

    @Test
    fun cropMovementResizeAndRatiosStayBounded() {
        val crop = RawCrop(.2f, .2f, .8f, .8f)
        val moved = crop.move(4f, -4f)
        assertEquals(1f, moved.right, .00001f)
        assertEquals(0f, moved.top, .00001f)
        assertEquals(crop.width, moved.width, .00001f)
        val resized = crop.resize(-4f, -4f, true, true)
        assertEquals(0f, resized.left, .00001f)
        assertEquals(0f, resized.top, .00001f)
        val ratio = crop.fitAspect(16f / 9, 1.5f)
        assertEquals(16f / 9, ratio.width / ratio.height * 1.5f, .00001f)
        val locked = ratio.resize(.1f, .2f, false, false, (16f / 9) / 1.5f)
        assertEquals(16f / 9, locked.width / locked.height * 1.5f, .00001f)
        assertTrue(locked.right <= 1 && locked.bottom <= 1)
        assertTrue(crop.resize(4f, 4f, true, true).width >= .04999f)
        var rotated = crop
        repeat(4) { rotated = rotated.rotateClockwise() }
        assertEquals(crop.left, rotated.left, .00001f)
        assertEquals(crop.bottom, rotated.bottom, .00001f)
        assertThrows(IllegalArgumentException::class.java) { RawCrop(.9f, 0f, .1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { RawCrop(Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { crop.fitAspect(100f, 1f) }
        val random = kotlin.random.Random(42)
        var bounded = RawCrop()
        repeat(2_000) {
            bounded = bounded.move(random.nextFloat() - .5f, random.nextFloat() - .5f)
                .resize(random.nextFloat() - .5f, random.nextFloat() - .5f, random.nextBoolean(), random.nextBoolean())
            assertTrue(bounded.width >= .04999f && bounded.height >= .04999f)
        }
    }
    @Test
    fun readsOnlyTheBoundedCameraJpegAndRejectsInvalidRanges() {
        val file = Files.createTempFile("raf-preview-", ".raf").toFile()
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 0xff.toByte(), 0xd9.toByte())
        val bytes = ByteArray(148 + jpeg.size)
        "FUJIFILMCCD-RAW ".toByteArray().copyInto(bytes)
        val header = ByteBuffer.wrap(bytes)
        header.putInt(84, 148)
        header.putInt(88, jpeg.size)
        jpeg.copyInto(bytes, 148)
        try {
            file.writeBytes(bytes)
            assertArrayEquals(jpeg, readRafPreview(file))
            header.putInt(88, Int.MAX_VALUE)
            file.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
            header.putInt(88, jpeg.size)
            header.putInt(84, -1)
            file.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
            file.writeBytes(byteArrayOf(1))
            assertThrows(IllegalArgumentException::class.java) { readRafPreview(file) }
        } finally { file.delete() }
    }
}
