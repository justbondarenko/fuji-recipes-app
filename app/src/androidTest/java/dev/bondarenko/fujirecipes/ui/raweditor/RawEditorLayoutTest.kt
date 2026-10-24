package dev.bondarenko.fujirecipes.ui.raweditor

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.data.raweditor.RawAdjustments
import dev.bondarenko.fujirecipes.data.raweditor.*
import dev.bondarenko.fujirecipes.data.raweditor.RawCrop
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RawEditorLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun previewControlsAndSaveStayInPlaceDuringRenderingAndComparison() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(
            name = "sample.RAF", simulation = "Classic Chrome", fullWidth = 7728, fullHeight = 5152,
            preview = bitmap, cameraImage = bitmap,
        ))
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        fun bounds(text: String): Rect = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        fun checkStable() {
            compose.onNodeWithText("sample.RAF").assertIsDisplayed()
            compose.onNodeWithText("RAW Editor").assertDoesNotExist()
            compose.onNodeWithText("Open RAF").assertDoesNotExist()
            compose.onNodeWithContentDescription("Reset").assertIsDisplayed()
            compose.onNodeWithText("Save").assertIsDisplayed()
            val save = bounds("Save")
            val shadows = bounds("Shadows")
            val image = compose.onNodeWithContentDescription("RAW with film simulation color profile").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle { state = state.copy(rendering = true) }
            assertEquals(save, bounds("Save"))
            assertEquals(shadows, bounds("Shadows"))
            compose.runOnIdle { state = state.copy(rendering = false) }
            compose.onNodeWithText("Compare camera JPEG").assertDoesNotExist()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
            assertEquals(save, bounds("Save"))
            assertEquals(shadows, bounds("Shadows"))
            assertEquals(image, compose.onNodeWithContentDescription("Original camera JPEG").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center) }
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { cancel() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Compare camera JPEG")
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Back to edited RAW")
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center) }
            compose.runOnIdle { state = state.copy(busy = true) }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up(); down(center) }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
            compose.runOnIdle { state = state.copy(busy = false) }
            compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
        }
        checkStable()
        compose.runOnIdle { wide = true }
        checkStable()
        compose.runOnIdle { wide = false }
        fun set(label: String, value: Float) {
            compose.onNodeWithContentDescription(label).performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
            compose.onNodeWithText("Save").assertIsDisplayed()
        }
        compose.onNodeWithText("Color").performClick()
        set("Temperature", .4f)
        set("Tint", -.3f)
        compose.runOnIdle {
            assertEquals(.4f, state.adjustments.temperature)
            assertEquals(-.3f, state.adjustments.tint)
        }
        compose.onNodeWithText("As Shot white balance").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0f, state.adjustments.temperature)
            assertEquals(0f, state.adjustments.tint)
        }
        set("Saturation", -.5f)
        set("Vibrance", .5f)
        compose.onNodeWithText("Detail").performClick()
        set("Sharpening", .6f)
        set("Radius", 1.8f)
        set("Threshold", .2f)
        set("Color noise reduction", .6f)
        set("Luminance noise reduction", .5f)
        set("Preserve detail", .7f)
        compose.onNodeWithText("Light").performClick()
        set("Contrast", .3f)
        set("Whites", .2f)
        set("Blacks", -.2f)
        compose.runOnIdle {
            assertEquals(.6f, state.adjustments.sharpening)
            assertEquals(1.8f, state.adjustments.sharpenRadius)
            assertEquals(.7f, state.adjustments.noiseDetail)
            assertEquals(-.2f, state.adjustments.blacks)
        }
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        set("Clarity", .4f)
        set("Texture", -.3f)
        set("Dehaze", .2f)
        set("Vignette", -.5f)
        set("Midpoint", .6f)
        set("Roundness", -.4f)
        set("Feather", .7f)
        compose.onNodeWithText("Framing").performScrollTo().performClick()
        set("Rotation", 1f)
        set("Straighten", -4.5f)
        compose.runOnIdle {
            assertEquals(.4f, state.adjustments.clarity)
            assertEquals(-.3f, state.adjustments.texture)
            assertEquals(.2f, state.adjustments.dehaze)
            assertEquals(.7f, state.adjustments.vignetteFeather)
            assertEquals(1, state.adjustments.quarterTurns)
            assertEquals(-4.5f, state.adjustments.straighten)
        }
        compose.onNodeWithText("Optics").performScrollTo().performClick()
        set("Distortion", -.4f)
        set("Vertical perspective", .3f)
        set("Horizontal perspective", -.2f)
        set("Red/cyan alignment", .5f)
        set("Blue/yellow alignment", -.5f)
        set("Purple defringe", .6f)
        set("Green defringe", .7f)
        compose.runOnIdle {
            assertEquals(-.4f, state.adjustments.distortion)
            assertEquals(.3f, state.adjustments.perspectiveVertical)
            assertEquals(-.2f, state.adjustments.perspectiveHorizontal)
            assertEquals(.5f, state.adjustments.chromaticRed)
            assertEquals(-.5f, state.adjustments.chromaticBlue)
            assertEquals(.6f, state.adjustments.defringePurple)
            assertEquals(.7f, state.adjustments.defringeGreen)
            wide = true
        }
        compose.onNodeWithText("Save").assertIsDisplayed()
        set("Distortion", -.3f)
        compose.onNodeWithTag("raw-editor-optics-grid", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center) }
        compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
        compose.onNodeWithTag("raw-editor-optics-grid", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
        compose.onNodeWithTag("raw-editor-optics-grid", useUnmergedTree = true).assertExists()
    }

    @Test
    fun doubleTapResetsOnlyNamedParameterIncludingInactiveDetailControls() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var expected = RawAdjustments(
            exposure = 1f, shadows = .4f, highlights = -.3f, contrast = .2f, whites = .3f, blacks = -.2f,
            temperature = .4f, tint = -.3f, saturation = -.5f, vibrance = .5f,
            sharpening = .6f, sharpenRadius = 1.8f, sharpenThreshold = .2f,
            colorNoise = .6f, luminanceNoise = .5f, noiseDetail = .7f,
            clarity = .4f, texture = -.3f, dehaze = .2f, quarterTurns = 2, straighten = 5f,
            vignette = -.5f, vignetteMidpoint = .6f, vignetteRoundness = -.4f, vignetteFeather = .7f,
            distortion = -.4f, perspectiveVertical = .3f, perspectiveHorizontal = -.2f,
            chromaticRed = .5f, chromaticBlue = -.5f, defringePurple = .6f, defringeGreen = .7f,
        )
        var state by mutableStateOf(RawEditorState(name = "sample.RAF", preview = bitmap, cameraImage = bitmap, adjustments = expected))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        compose.onNodeWithText("Exposure").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(expected, state.adjustments) }
        compose.runOnIdle { state = state.copy(busy = true) }
        compose.onNodeWithText("Exposure").performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(expected, state.adjustments); state = state.copy(busy = false) }

        fun reset(label: String, after: RawAdjustments) {
            compose.onNodeWithText(label).performScrollTo().performTouchInput { doubleClick() }
            expected = after
            compose.runOnIdle { assertEquals("Reset $label must preserve every other value", expected, state.adjustments) }
        }
        reset("Exposure", expected.copy(exposure = 0f))
        reset("Shadows", expected.copy(shadows = 0f))
        reset("Highlights", expected.copy(highlights = 0f))
        reset("Contrast", expected.copy(contrast = 0f))
        reset("Whites", expected.copy(whites = 0f))
        reset("Blacks", expected.copy(blacks = 0f))
        compose.onNodeWithText("Color").performClick()
        reset("Temperature", expected.copy(temperature = 0f))
        reset("Tint", expected.copy(tint = 0f))
        reset("Saturation", expected.copy(saturation = 0f))
        reset("Vibrance", expected.copy(vibrance = 0f))
        compose.onNodeWithText("Detail").performClick()
        reset("Sharpening", expected.copy(sharpening = 0f))
        reset("Radius", expected.copy(sharpenRadius = 1f))
        reset("Threshold", expected.copy(sharpenThreshold = 0f))
        reset("Color noise reduction", expected.copy(colorNoise = 0f))
        reset("Luminance noise reduction", expected.copy(luminanceNoise = 0f))
        reset("Preserve detail", expected.copy(noiseDetail = .5f))
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        reset("Clarity", expected.copy(clarity = 0f))
        reset("Texture", expected.copy(texture = 0f))
        reset("Dehaze", expected.copy(dehaze = 0f))
        reset("Vignette", expected.copy(vignette = 0f))
        reset("Midpoint", expected.copy(vignetteMidpoint = .5f))
        reset("Roundness", expected.copy(vignetteRoundness = 0f))
        reset("Feather", expected.copy(vignetteFeather = .5f))
        compose.onNodeWithText("Optics").performScrollTo().performClick()
        reset("Distortion", expected.copy(distortion = 0f))
        reset("Vertical perspective", expected.copy(perspectiveVertical = 0f))
        reset("Horizontal perspective", expected.copy(perspectiveHorizontal = 0f))
        reset("Red/cyan alignment", expected.copy(chromaticRed = 0f))
        reset("Blue/yellow alignment", expected.copy(chromaticBlue = 0f))
        reset("Purple defringe", expected.copy(defringePurple = 0f))
        reset("Green defringe", expected.copy(defringeGreen = 0f))
        compose.onNodeWithText("Framing").performScrollTo().performClick()
        reset("Rotation", expected.copy(quarterTurns = 0))
        reset("Straighten", expected.copy(straighten = 0f))
        compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
        compose.onNodeWithText("Light").performScrollTo().performClick()
        compose.runOnIdle { state = state.copy(adjustments = state.adjustments.copy(exposure = 1f)) }
        compose.onNodeWithText("Exposure").performCustomAccessibilityActionWithLabel("Reset Exposure")
        compose.runOnIdle { assertEquals(RawAdjustments(), state.adjustments) }
    }

    @Test
    fun curvesGrainAndSelectiveControlsUseTheCurrentCarouselAndGestures() {
        val bitmap = Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(name = "sample.RAF",preview = bitmap))
        var drawn = emptyList<MaskPoint>()
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp,if (wide) 393.dp else 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {},
                    onMasks = { state = state.copy(maskEditorOpen = it) },
                    onSelectMask = { state = state.copy(activeMask = it) },
                    onDrawMask = { points,_,_ -> drawn = points }) }
            }
        }
        fun set(label: String, value: Float) {
            compose.onNodeWithContentDescription(label).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
        }
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        set("Grain",.6f); set("Grain size",6f); set("Roughness",.8f)
        compose.onNodeWithText("Grain").performScrollTo().performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(0f,state.adjustments.grainAmount); assertEquals(6f,state.adjustments.grainSize) }
        compose.onNodeWithText("Grain size").performScrollTo().performTouchInput { doubleClick() }
        compose.onNodeWithText("Roughness").performScrollTo().performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(RawAdjustments(),state.adjustments) }
        compose.onNodeWithText("Curves").performScrollTo().performClick()
        compose.onNodeWithTag("raw-tone-curve").assertIsDisplayed()
        listOf("Edit tone curves","Input","Output","Previous","Next","Add point","Remove point").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { down(center); moveTo(Offset(width*.5f,height*.3f),200); up() }
        compose.runOnIdle { assertEquals(.7f,state.adjustments.curves.master.points[1].y,.02f) }
        compose.onNodeWithContentDescription("Red").performClick()
        compose.onNodeWithText("Red").assertDoesNotExist()
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { down(center); moveTo(Offset(width*.5f,height*.7f),200); up() }
        compose.runOnIdle { assertEquals(.3f,state.adjustments.curves.red.points[1].y,.02f); assertEquals(.7f,state.adjustments.curves.master.points[1].y,.02f) }
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { doubleClick(Offset(width*.5f,height*.7f)) }
        compose.runOnIdle { assertEquals(2,state.adjustments.curves.red.points.size); assertTrue(state.adjustments.curves.red.isIdentity) }
        compose.onNodeWithText("Master").performClick()
        compose.onNodeWithTag("raw-tone-curve").performTouchInput { click(Offset(width*.5f,height*.95f)) }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(3,state.adjustments.curves.master.points.size) }
        compose.onNodeWithTag("raw-tone-curve").performCustomAccessibilityActionWithLabel("Add point")
        compose.onNodeWithTag("raw-tone-curve").performCustomAccessibilityActionWithLabel("Move point up")
        compose.runOnIdle { assertEquals(4,state.adjustments.curves.master.points.size) }
        compose.onNodeWithTag("raw-tone-curve").performCustomAccessibilityActionWithLabel("Remove point")
        compose.runOnIdle { assertEquals(3,state.adjustments.curves.master.points.size) }
        compose.onNodeWithText("Masks").performScrollTo().performClick()
        compose.onNodeWithText("Edit masks (0)").performClick()
        compose.onNodeWithText("+ Brush").performScrollTo().performClick()
        compose.onNodeWithTag("raw-mask-preview").performTouchInput { down(center); moveBy(Offset(30f,20f),200); up() }
        compose.runOnIdle { assertTrue(drawn.isNotEmpty()); assertTrue(drawn.first() != drawn.last()); assertTrue(drawn.all { it.x in 0f..1f && it.y in 0f..1f }) }
        set("Exposure",.8f)
        compose.onNodeWithText("Center dab").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(.8f,state.adjustments.masks[0].adjustments.exposure); assertEquals(1,state.adjustments.masks[0].strokes.size) }
        compose.onNodeWithText("Undo stroke").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(state.adjustments.masks[0].strokes.isEmpty()) }
        compose.onNodeWithText("+ Linear").performScrollTo().performClick()
        compose.onNodeWithTag("raw-mask-preview").performTouchInput { down(center); moveBy(Offset(60f,80f),200); up() }
        compose.runOnIdle { assertTrue(drawn.first() != drawn.last()) }
        compose.onNodeWithText("Mask shape").performClick()
        set("Start X",.2f); set("End Y",.9f); set("Opacity",.6f); set("Feather",.7f)
        compose.runOnIdle { assertEquals(.2f,state.adjustments.masks[1].start.x); assertEquals(.9f,state.adjustments.masks[1].end.y) }
        compose.onNodeWithText("Invert").performScrollTo().performClick()
        compose.onNodeWithText("+ Radial").performScrollTo().performClick()
        compose.runOnIdle { wide = true }
        set("Opacity",.4f)
        compose.onNodeWithTag("raw-mask-preview").assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithText("Delete mask").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2,state.adjustments.masks.size); assertTrue(state.adjustments.masks[1].inverted) }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun cropSupportsDragAspectLockAccessibleMovementAndCancellation() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var result: RawCrop? = null
        var dismissed = false
        compose.setContent {
            FujiTheme { RawCropDialog(bitmap, RawCrop(.2f,.2f,.8f,.8f), { dismissed = true }, { result = it }, imageAspect = 2f) }
        }
        compose.onNodeWithTag("raw-crop-preview").performTouchInput {
            down(center); moveBy(Offset(30f, 0f), 200); up()
        }
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertTrue(result!!.left > .2f); assertEquals(.6f, result!!.width, .0001f) }
        compose.onNodeWithText("1:1").performScrollTo().performClick()
        compose.onNodeWithTag("raw-crop-preview").performCustomAccessibilityActionWithLabel("Make crop smaller")
        compose.onNodeWithTag("raw-crop-preview").performCustomAccessibilityActionWithLabel("Move crop left")
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertEquals(1f, result!!.width / result!!.height * 2f, .0001f) }
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertEquals(RawCrop(), result) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
