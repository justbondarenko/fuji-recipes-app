package dev.bondarenko.fujirecipes.ui.raweditor

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.testTag
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Exposure
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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

    private fun selectGroupedControl(label: String): Int? {
        val group = when (label) {
            "Shadows", "Blacks" -> "Shadows"
            "Highlights", "Whites" -> "Highlights"
            "Sharpening", "Radius" -> "Sharpening"
            "Color", "Luminance" -> "Noise Reduction"
            "Amount", "Size", "Roughness" -> "Grain"
            "Vignette", "Midpoints", "Roundness", "Feather" -> "Vignette"
            "Vertical", "Horizontal" -> "Perspective"
            "Green", "Purple" -> "Defringe"
            "Red/Cyan", "Blue/Yellow" -> "Color alignment"
            else -> return null
        }
        compose.onNodeWithContentDescription("$group controls").performScrollTo().performClick()
        if (group == "Grain" || group == "Vignette") {
            compose.onNodeWithTag("raw-editor-parameter-selector", useUnmergedTree = true).performClick()
            compose.onNodeWithText(if (label == "Vignette") "Amount" else label).performClick()
        } else compose.onNodeWithTag("raw-editor-parameter-selector", useUnmergedTree = true).assertDoesNotExist()
        return when (label) {
            "Shadows" -> R.string.raw_editor_shadows
            "Blacks" -> R.string.raw_editor_blacks
            "Highlights" -> R.string.raw_editor_highlights
            "Whites" -> R.string.raw_editor_whites
            "Sharpening" -> R.string.raw_editor_sharpening
            "Radius" -> R.string.raw_editor_radius
            "Color" -> R.string.raw_editor_color_noise
            "Luminance" -> R.string.raw_editor_luminance_noise
            "Amount" -> R.string.raw_editor_grain_amount
            "Size" -> R.string.raw_editor_grain_size
            "Roughness" -> R.string.raw_editor_grain_roughness
            "Vignette" -> R.string.raw_editor_vignette
            "Midpoints" -> R.string.raw_editor_vignette_midpoint
            "Roundness" -> R.string.raw_editor_vignette_roundness
            "Feather" -> R.string.raw_editor_vignette_feather
            "Vertical" -> R.string.raw_editor_perspective_vertical
            "Horizontal" -> R.string.raw_editor_perspective_horizontal
            "Green" -> R.string.raw_editor_defringe_green
            "Purple" -> R.string.raw_editor_defringe_purple
            "Red/Cyan" -> R.string.raw_editor_chromatic_red
            else -> R.string.raw_editor_chromatic_blue
        }
    }

    private fun selectCropRatio(name: String) {
        compose.onNodeWithTag("raw-crop-aspect-selector").performClick()
        compose.onNode(hasText(name) and hasAnyAncestor(isPopup())).performScrollTo().performClick()
    }

    @Test
    fun groupsAndOptionsFollowRequestedOrderWithCropAlwaysInPreview() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(RawEditorState(preview = bitmap), {}, {}, {}, {}) }
            }
        }
        val groups = listOf("Light", "Color", "Details", "Curves", "Effects", "Optics", "Masks")
        val tabs = compose.onNodeWithTag("raw-editor-groups").fetchSemanticsNode()
        fun tabLabels(node: androidx.compose.ui.semantics.SemanticsNode): List<String> =
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } + node.children.flatMap(::tabLabels)
        assertEquals(groups, tabLabels(tabs))
        val expectedOptions = mapOf(
            "Light" to listOf("Exposure controls", "Contrast controls", "Shadows controls", "Highlights controls"),
            "Color" to listOf("As shot", "Temperature controls", "Tint controls", "Saturation controls", "Vibrance controls"),
            "Details" to listOf("Sharpening controls", "Noise Reduction controls", "Preserve Detail controls"),
            "Effects" to listOf("Grain controls", "Clarity controls", "Texture controls", "Dehaze controls", "Vignette controls"),
            "Optics" to listOf("Distortion controls", "Perspective controls", "Defringe controls", "Color alignment controls"),
        )
        groups.forEach { group ->
            compose.onNodeWithText(group).performScrollTo().performClick()
            compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed()
            expectedOptions[group]?.let { expected ->
                fun descriptions(node: androidx.compose.ui.semantics.SemanticsNode): List<String> =
                    node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() } + node.children.flatMap(::descriptions)
                assertEquals(expected, descriptions(compose.onNodeWithTag("raw-editor-options").fetchSemanticsNode()))
            }
        }
        compose.onNodeWithText("Framing").assertDoesNotExist()
    }

    @Test
    fun categorySwipesUseEmptySpaceAndCurvesStaySquare() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val original = RawAdjustments(grainAmount = .4f, vignette = -.3f)
        var state by mutableStateOf(RawEditorState(preview = bitmap, adjustments = original))
        var dimensions by mutableStateOf(DpSize(840.dp, 800.dp))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(dimensions)) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        val categories = listOf("Light", "Color", "Details", "Curves", "Effects", "Optics", "Masks")
        val firstWheels = listOf(R.string.raw_editor_exposure, R.string.raw_editor_temperature, R.string.raw_editor_sharpening,
            null, R.string.raw_editor_grain_amount, R.string.raw_editor_distortion, null)
        fun swipeEmpty(index: Int, left: Boolean) {
            val controls = compose.onNodeWithTag("raw-editor-controls")
            val bounds = controls.fetchSemanticsNode().boundsInRoot
            val after = when (index) {
                3 -> compose.onNodeWithTag("raw-tone-curve").fetchSemanticsNode().boundsInRoot
                6 -> compose.onNodeWithTag("raw-editor-options").fetchSemanticsNode().boundsInRoot
                else -> compose.onNodeWithTag("raw-editor-wheel-${firstWheels[index]}").fetchSemanticsNode().boundsInRoot
            }
            controls.performTouchInput {
                val y = after.bottom - bounds.top + 3.dp.toPx()
                swipe(Offset(width * if (left) .8f else .2f, y), Offset(width * if (left) .2f else .8f, y), 200)
            }
        }
        for (size in listOf(DpSize(840.dp, 800.dp), DpSize(393.dp, 800.dp))) {
            compose.runOnIdle { dimensions = size; state = state.copy(adjustments = original) }
            compose.onNodeWithText("Light").performScrollTo().performClick()
            val pager = compose.onNodeWithTag("raw-editor-pager")
            val pagerBounds = pager.getUnclippedBoundsInRoot()
            val tabs = compose.onNodeWithTag("raw-editor-groups").getUnclippedBoundsInRoot()
            val start = compose.onNodeWithTag("raw-editor-page-0").getUnclippedBoundsInRoot()
            val exposure = compose.onNodeWithTag("raw-editor-wheel-${R.string.raw_editor_exposure}").fetchSemanticsNode().boundsInRoot
            val pagerTop = pager.fetchSemanticsNode().boundsInRoot.top
            compose.mainClock.autoAdvance = false
            pager.performTouchInput {
                val y = exposure.bottom - pagerTop + 3.dp.toPx()
                down(Offset(width * .8f, y))
                moveTo(Offset(width * .7f, y), delayMillis = 100)
            }
            compose.mainClock.advanceTimeByFrame()
            try {
                val moving = compose.onNodeWithTag("raw-editor-page-0").getUnclippedBoundsInRoot()
                val next = compose.onNodeWithTag("raw-editor-page-1").getUnclippedBoundsInRoot()
                assertTrue(start.left - moving.left > 5.dp)
                assertTrue(start.left - moving.left < (pagerBounds.right - pagerBounds.left) * .2f)
                assertTrue(next.left < pagerBounds.right && next.left > pagerBounds.left)
                assertEquals(moving.right.value, next.left.value, 1f)
                assertEquals(tabs, compose.onNodeWithTag("raw-editor-groups").getUnclippedBoundsInRoot())
            } finally {
                pager.performTouchInput { advanceEventTime(200); up() }
                compose.mainClock.autoAdvance = true
            }
            compose.onNodeWithText("Light").assertIsSelected()
            assertEquals(start, compose.onNodeWithTag("raw-editor-page-0").getUnclippedBoundsInRoot())
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("Color").performScrollTo().performClick()
            compose.mainClock.advanceTimeBy(48)
            val animated = compose.onNodeWithTag("raw-editor-page-0").getUnclippedBoundsInRoot()
            assertTrue(animated.left < start.left && animated.right > pagerBounds.left)
            compose.mainClock.autoAdvance = true
            compose.onNodeWithText("Color").assertIsSelected()
            compose.onNodeWithText("Light").performScrollTo().performClick()
            swipeEmpty(0, false)
            compose.onNodeWithText("Light").assertIsSelected()
            for (index in 0..5) {
                swipeEmpty(index, true)
                compose.onNodeWithText(categories[index + 1]).assertIsSelected()
            }
            swipeEmpty(6, true)
            compose.onNodeWithText("Masks").assertIsSelected()
            for (index in 6 downTo 1) {
                swipeEmpty(index, false)
                compose.onNodeWithText(categories[index - 1]).assertIsSelected()
            }
            compose.runOnIdle { assertEquals(original,state.adjustments); state = state.copy(busy = true) }
            swipeEmpty(0, true)
            compose.onNodeWithText("Light").assertIsSelected()
            compose.runOnIdle { state = state.copy(busy = false) }
            compose.onNodeWithTag("raw-editor-wheel-${R.string.raw_editor_exposure}").performTouchInput {
                swipe(center, Offset(width * .2f, center.y), 200)
            }
            compose.onNodeWithText("Light").assertIsSelected()
            compose.runOnIdle { assertTrue(state.adjustments.exposure > 0f) }
            compose.onNodeWithText("Curves").performScrollTo().performClick()
            val graph = compose.onNodeWithTag("raw-tone-curve")
            val square = graph.getUnclippedBoundsInRoot()
            assertEquals((square.right-square.left).value,(square.bottom-square.top).value,1f)
            graph.performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(400)
            graph.performTouchInput { swipe(center, Offset(width * .7f, center.y), 200) }
            compose.onNodeWithText("Curves").assertIsSelected()
            compose.runOnIdle { assertEquals(3,state.adjustments.curves.master.points.size); assertTrue(state.adjustments.curves.master.points[1].x > .5f) }
            if (size.width >= 700.dp) {
                compose.onNodeWithText("Effects").performScrollTo().performClick()
                val before = state.adjustments
                val list = compose.onNodeWithTag("raw-editor-wheel-list")
                list.performTouchInput { swipeUp() }
                compose.onNodeWithText("Effects").assertIsSelected()
                assertTrue(list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f)
                compose.runOnIdle { assertEquals(before,state.adjustments) }
            } else {
                compose.onNodeWithText("Effects").performScrollTo().performClick()
                val before = state.adjustments
                compose.onNodeWithTag("raw-editor-options").performTouchInput {
                    swipe(Offset(width * .8f, center.y), Offset(width * .2f, center.y), 200)
                }
                compose.onNodeWithText("Effects").assertIsSelected()
                compose.runOnIdle { assertEquals(before, state.adjustments) }
            }
        }
        compose.runOnIdle { dimensions = DpSize(840.dp, 393.dp) }
        compose.onNodeWithText("Curves").performScrollTo().performClick()
        val shortGraph = compose.onNodeWithTag("raw-tone-curve").getUnclippedBoundsInRoot()
        assertEquals((shortGraph.right-shortGraph.left).value,(shortGraph.bottom-shortGraph.top).value,1f)
    }

    @Test
    fun widePanelListsAllWheelsBelowFixedTabsAndPreservesEditsWhenResized() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val original = RawAdjustments(exposure = .6f, grainAmount = .4f, vignette = -.3f)
        var state by mutableStateOf(RawEditorState(preview = bitmap, cameraImage = bitmap, adjustments = original))
        var size by mutableStateOf(DpSize(840.dp, 800.dp))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        val expected = mapOf(
            "Light" to listOf(R.string.raw_editor_exposure, R.string.raw_editor_contrast, R.string.raw_editor_shadows, R.string.raw_editor_blacks, R.string.raw_editor_highlights, R.string.raw_editor_whites),
            "Color" to listOf(R.string.raw_editor_temperature, R.string.raw_editor_tint, R.string.raw_editor_saturation, R.string.raw_editor_vibrance),
            "Details" to listOf(R.string.raw_editor_sharpening, R.string.raw_editor_radius, R.string.raw_editor_color_noise, R.string.raw_editor_luminance_noise, R.string.raw_editor_noise_detail),
            "Effects" to listOf(R.string.raw_editor_grain_amount, R.string.raw_editor_grain_size, R.string.raw_editor_grain_roughness, R.string.raw_editor_clarity, R.string.raw_editor_texture, R.string.raw_editor_dehaze, R.string.raw_editor_vignette, R.string.raw_editor_vignette_midpoint, R.string.raw_editor_vignette_roundness, R.string.raw_editor_vignette_feather),
            "Optics" to listOf(R.string.raw_editor_distortion, R.string.raw_editor_perspective_vertical, R.string.raw_editor_perspective_horizontal, R.string.raw_editor_defringe_green, R.string.raw_editor_defringe_purple, R.string.raw_editor_chromatic_red, R.string.raw_editor_chromatic_blue),
        )
        fun wheelTags(node: androidx.compose.ui.semantics.SemanticsNode): List<String> =
            listOfNotNull(node.config.getOrElse(SemanticsProperties.TestTag) { "" }.takeIf { it.startsWith("raw-editor-wheel-") && node.config.contains(SemanticsProperties.ProgressBarRangeInfo) }) + node.children.flatMap(::wheelTags)
        expected.forEach { (category, ids) ->
            compose.onNodeWithText(category).performScrollTo().performClick()
            compose.onNodeWithTag("raw-editor-options").assertDoesNotExist()
            compose.onNodeWithTag("raw-editor-parameter-selector", useUnmergedTree = true).assertDoesNotExist()
            assertEquals(ids.map { "raw-editor-wheel-$it" }, wheelTags(compose.onNodeWithTag("raw-editor-wheel-list").fetchSemanticsNode()))
        }
        compose.onNodeWithText("Curves").performScrollTo().performClick()
        compose.onNodeWithTag("raw-tone-curve").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Masks").performScrollTo().performClick()
        compose.onNodeWithText("Edit masks (0)").assertIsDisplayed()
        for (dimensions in listOf(DpSize(840.dp, 800.dp), DpSize(1200.dp, 800.dp), DpSize(840.dp, 393.dp))) {
            compose.runOnIdle { size = dimensions }
            compose.onNodeWithText("Light").performScrollTo().performClick()
            val title = compose.onNodeWithTag("raw-editor-parameter-${R.string.raw_editor_exposure}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val icon = compose.onNodeWithTag("raw-editor-icon-${R.string.raw_editor_exposure}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val tabs = compose.onNodeWithTag("raw-editor-groups").fetchSemanticsNode().boundsInRoot
            val preview = compose.onNodeWithTag("raw-editor-preview").fetchSemanticsNode().boundsInRoot
            assertTrue("Category tabs stay above the sliders",tabs.bottom <= title.top)
            assertTrue("Icons sit beside titles",icon.right <= title.left)
            assertTrue("The slider list stays beside the photo",preview.right <= icon.left)
            compose.onNodeWithText("Effects").performScrollTo().performClick()
            val fixedTabs = compose.onNodeWithTag("raw-editor-groups").fetchSemanticsNode().boundsInRoot
            val feather = compose.onNodeWithTag("raw-editor-wheel-${R.string.raw_editor_vignette_feather}")
            feather.performScrollTo().assertIsDisplayed().performSemanticsAction(SemanticsActions.SetProgress) { it(.7f) }
            compose.runOnIdle { assertEquals(original.copy(vignetteFeather = .7f),state.adjustments) }
            assertEquals(fixedTabs,compose.onNodeWithTag("raw-editor-groups").fetchSemanticsNode().boundsInRoot)
            assertEquals(preview,compose.onNodeWithTag("raw-editor-preview").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("raw-editor-parameter-${R.string.raw_editor_vignette_feather}", useUnmergedTree = true).performTouchInput { doubleClick() }
            compose.runOnIdle { assertEquals(original,state.adjustments) }
            compose.onNodeWithText("Save").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed()
            compose.runOnIdle { size = DpSize(393.dp, 800.dp) }
            compose.onNodeWithText("Effects").assertIsSelected()
            compose.onNodeWithTag("raw-editor-wheel-list").assertDoesNotExist()
            compose.onNodeWithTag("raw-editor-options").assertIsDisplayed()
            compose.runOnIdle { assertEquals(original,state.adjustments) }
        }
    }

    @Test
    fun framingStartsDirectlyWithButtonsAndStagesChangesUntilApply() {
        val landscape = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val portrait = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        val original = RawAdjustments(exposure = .6f, tint = .2f, crop = RawCrop(.15f,.1f,.85f,.9f))
        var state by mutableStateOf(RawEditorState(name = "sample.RAF", preview = landscape, adjustments = original))
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, if (wide) 500.dp else 800.dp))) {
                FujiTheme {
                    RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {},
                        onCrop = { state = state.copy(cropPreview = landscape, cropAdjustments = state.adjustments, cropImageAspect = 1.5f) },
                        onCropAdjust = { state = state.copy(cropAdjustments = it,
                            cropPreview = if (it.quarterTurns % 2 == 0) landscape else portrait,
                            cropImageAspect = if (it.quarterTurns % 2 == 0) 1.5f else 2f/3) },
                        onDismissCrop = { state = state.copy(cropPreview = null, cropAdjustments = null) },
                    )
                }
            }
        }
        for (expanded in listOf(false, true)) {
            compose.runOnIdle { wide = expanded; state = state.copy(adjustments = original) }
            compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed().performClick()
            compose.onNodeWithTag("raw-crop-preview").assertIsDisplayed()
            compose.onNodeWithContentDescription("Rotation").assertDoesNotExist()
            compose.onNodeWithText("Crop and aspect ratio").assertDoesNotExist()
            val photo = compose.onNodeWithTag("raw-crop-preview").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle { state = state.copy(error = "Crop rendering failed") }
            compose.onNodeWithText("Crop rendering failed").assertIsDisplayed()
            assertEquals(photo, compose.onNodeWithTag("raw-crop-preview").fetchSemanticsNode().boundsInRoot)
            compose.runOnIdle { state = state.copy(error = null) }
            val slider = compose.onNodeWithContentDescription("Straighten").fetchSemanticsNode().boundsInRoot
            assertTrue("Straighten stays below the photo in both layouts", slider.top >= photo.bottom)
            compose.onNodeWithTag("raw-crop-preview").performTouchInput { down(center); moveBy(Offset(80f,0f),200); up() }
            compose.onNodeWithContentDescription("Rotate right 90°").performClick()
            compose.runOnIdle {
                assertEquals(1, state.cropAdjustments!!.quarterTurns)
                assertEquals(original, state.adjustments)
                assertTrue("Rotating keeps the dragged crop", kotlin.math.abs(state.cropAdjustments!!.crop.top - original.rotated(1).crop.top) > .001f)
            }
            compose.onNodeWithContentDescription("Rotate left 90°").performClick()
            compose.runOnIdle { assertEquals(0, state.cropAdjustments!!.quarterTurns) }
            compose.onNodeWithContentDescription("Straighten").performSemanticsAction(SemanticsActions.SetProgress) { it(6.4f) }
            compose.runOnIdle { assertEquals(6.4f,state.cropAdjustments!!.straighten); assertEquals(original,state.adjustments) }
            compose.onNodeWithText("Cancel").performClick()
            compose.runOnIdle { assertEquals(original,state.adjustments); assertEquals(null,state.cropPreview) }
            compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed().performClick()
            compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains("Free")
            compose.onNodeWithText("1:1").assertDoesNotExist()
            for (preset in listOf("9:16", "16:9", "Original")) {
                selectCropRatio(preset)
                compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains(preset)
                compose.onNodeWithText("1:1").assertDoesNotExist()
            }
            compose.onNodeWithContentDescription("Rotate right 90°").performClick()
            compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains("Original")
            compose.onNodeWithContentDescription("Rotate left 90°").performClick()
            selectCropRatio("3:2")
            compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains("3:2")
            compose.onNodeWithContentDescription("Rotate right 90°").performClick()
            compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains("2:3")
            compose.runOnIdle { state = state.copy(cropRendering = true) }
            compose.onNodeWithTag("raw-crop-aspect-selector").assertIsNotEnabled()
            compose.onNodeWithText("Apply crop").assertIsNotEnabled()
            compose.runOnIdle { state = state.copy(cropRendering = false) }
            compose.onNodeWithContentDescription("Straighten").performSemanticsAction(SemanticsActions.SetProgress) { it(-4.5f) }
            selectCropRatio("1:1")
            compose.onNodeWithText("Apply crop").performClick()
            compose.runOnIdle {
                assertEquals(1,state.adjustments.quarterTurns)
                assertEquals(-4.5f,state.adjustments.straighten)
                assertEquals(original.exposure,state.adjustments.exposure)
                assertEquals(original.tint,state.adjustments.tint)
                assertEquals(1f,state.adjustments.crop.width/state.adjustments.crop.height*2f/3,.0001f)
                assertEquals(null,state.cropPreview)
            }
            compose.onNodeWithText("Save").assertIsDisplayed()
        }
    }

    @Test
    fun pairedWheelsShareOneRowAndKeepPreviewStill() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(RawEditorState(preview = bitmap), {}, {}, {}, {}) }
            }
        }
        val photo = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
        val pairs = listOf(
            Triple("Light", "Highlights", R.string.raw_editor_highlights to R.string.raw_editor_whites),
            Triple("Light", "Shadows", R.string.raw_editor_shadows to R.string.raw_editor_blacks),
            Triple("Details", "Sharpening", R.string.raw_editor_sharpening to R.string.raw_editor_radius),
            Triple("Details", "Noise Reduction", R.string.raw_editor_color_noise to R.string.raw_editor_luminance_noise),
            Triple("Optics", "Perspective", R.string.raw_editor_perspective_vertical to R.string.raw_editor_perspective_horizontal),
            Triple("Optics", "Defringe", R.string.raw_editor_defringe_green to R.string.raw_editor_defringe_purple),
            Triple("Optics", "Color alignment", R.string.raw_editor_chromatic_red to R.string.raw_editor_chromatic_blue),
        )
        pairs.forEach { (category, option, ids) ->
            compose.onNodeWithText(category).performScrollTo().performClick()
            // Compare against a single wheel in the same category.
            if (category == "Optics") compose.onNodeWithContentDescription("Distortion controls").performScrollTo().performClick()
            val before = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
            compose.onNodeWithContentDescription("$option controls").performScrollTo().performClick()
            val left = compose.onNodeWithTag("raw-editor-wheel-${ids.first}").assertIsDisplayed().getUnclippedBoundsInRoot()
            val right = compose.onNodeWithTag("raw-editor-wheel-${ids.second}").assertIsDisplayed().getUnclippedBoundsInRoot()
            assertEquals(left.top.value, right.top.value, .5f)
            assertEquals(left.bottom.value, right.bottom.value, .5f)
            assertEquals((left.right - left.left).value, (right.right - right.left).value, .5f)
            assertTrue(left.right <= right.left)
            val page = compose.onNodeWithTag("raw-editor-pager").getUnclippedBoundsInRoot()
            assertEquals((page.right - page.left).value / 2 - 40f, (left.right - left.left).value, .5f)
            assertEquals(before, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
            assertEquals(photo, before)
        }
    }

    @Test
    fun groupedToneWheelsKeepEveryParameterIndependent() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val original = RawAdjustments(exposure = .6f, highlights = -.3f, whites = .4f, shadows = .5f, blacks = -.2f)
        var state by mutableStateOf(RawEditorState(name = "sample.RAF", preview = bitmap, adjustments = original))
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {},
                    onCrop = { state = state.copy(cropPreview = bitmap, cropAdjustments = state.adjustments, cropImageAspect = 1.5f) },
                    onCropAdjust = { state = state.copy(cropAdjustments = it) },
                    onDismissCrop = { state = state.copy(cropPreview = null, cropAdjustments = null) },
                ) }
            }
        }
        fun wheel(id: Int) = compose.onNodeWithTag("raw-editor-wheel-$id").also { if (wide) it.performScrollTo() }
        fun set(id: Int, value: Float) {
            val control = wheel(id)
            control.performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
        }
        for (expanded in listOf(false, true)) {
            compose.runOnIdle { wide = expanded; state = state.copy(adjustments = original) }
            if (!expanded) compose.onNodeWithContentDescription("Highlights controls").performClick()
            wheel(R.string.raw_editor_highlights).assertIsDisplayed()
            wheel(R.string.raw_editor_whites).assertIsDisplayed()
            if (!expanded) compose.onNodeWithContentDescription("Shadows").assertDoesNotExist()
            compose.runOnIdle { assertEquals(original, state.adjustments) }
            set(R.string.raw_editor_highlights, -.6f)
            compose.runOnIdle { assertEquals(original.copy(highlights = -.6f), state.adjustments) }
            set(R.string.raw_editor_whites, .2f)
            compose.runOnIdle { assertEquals(original.copy(highlights = -.6f, whites = .2f), state.adjustments) }
            compose.onNodeWithTag("raw-editor-parameter-${R.string.raw_editor_highlights}", useUnmergedTree = true)
                .performTouchInput { doubleClick() }
            compose.runOnIdle { assertEquals(original.copy(highlights = 0f, whites = .2f), state.adjustments) }
            if (!expanded) compose.onNodeWithContentDescription("Highlights controls").performTouchInput { doubleClick() }
            compose.runOnIdle { assertEquals(original.copy(highlights = 0f, whites = .2f), state.adjustments) }
            if (!expanded) compose.onNodeWithContentDescription("Shadows controls").performClick()
            wheel(R.string.raw_editor_shadows).assertIsDisplayed()
            wheel(R.string.raw_editor_blacks).assertIsDisplayed()
            if (!expanded) compose.onNodeWithContentDescription("Whites").assertDoesNotExist()
            set(R.string.raw_editor_shadows, .3f)
            set(R.string.raw_editor_blacks, -.6f)
            compose.runOnIdle { assertEquals(original.copy(highlights = 0f, whites = .2f, shadows = .3f, blacks = -.6f), state.adjustments) }
            compose.onNodeWithTag("raw-editor-parameter-${R.string.raw_editor_blacks}", useUnmergedTree = true)
                .performTouchInput { doubleClick() }
            compose.runOnIdle { assertEquals(original.copy(highlights = 0f, whites = .2f, shadows = .3f, blacks = 0f), state.adjustments) }
            compose.onNodeWithText("Save").assertIsDisplayed()
        }
    }

    @Test
    fun zoomUsesSensorPixelsAndKeepsControlsAndComparisonStable() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(name = "sample.RAF", fullWidth = 7728, fullHeight = 5152,
            preview = bitmap, cameraImage = bitmap))
        var requested: RawViewport? = null
        compose.setContent {
            FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}, onViewport = { requested = it }) }
        }
        val saveBounds = compose.onNodeWithText("Save").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("raw-editor-preview").performTouchInput { doubleClick(center) }
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.waitUntil(5000) { requested?.scale == 1f }
        val first = requested!!
        val bounds = compose.onNodeWithTag("raw-editor-preview").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(bounds.width - first.width) <= 1f)
        assertTrue(kotlin.math.abs(bounds.height - first.height) <= 1f)
        compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); moveBy(Offset(120f, 90f), 300); up() }
        compose.waitUntil(5000) { requested != null && requested != first }
        val panned = requested!!
        assertTrue(panned.left < first.left && panned.top < first.top)
        compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Compare camera JPEG")
        compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
        compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Back to edited RAW")
        compose.onNodeWithText("100%").assertIsDisplayed()
        assertEquals(saveBounds, compose.onNodeWithText("Save").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("raw-editor-zoom").performClick()
        compose.waitUntil(5000) { requested == null }
        compose.onNodeWithText("Fit").assertIsDisplayed()
        // Pinch from Fit, including the 100% upper bound.
        compose.onNodeWithTag("raw-editor-preview").performTouchInput {
            down(0, center + Offset(-30f, 0f)); down(1, center + Offset(30f, 0f))
            moveTo(0, center + Offset(-200f, 0f), 100); moveTo(1, center + Offset(200f, 0f), 100)
            up(0); up(1)
        }
        compose.waitUntil(5000) { requested != null }
        assertTrue(requested!!.scale <= 1f)
        compose.runOnIdle { state = state.copy(adjustments = state.adjustments.copy(exposure = .5f)) }
        compose.onNodeWithTag("raw-editor-preview").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(adjustments = state.adjustments.rotated(1)) }
        compose.waitUntil(5000) { requested == null }
    }

    @Test
    fun wheelHasFineStepsInertiaResetAndDisabledSemantics() {
        var value by mutableStateOf(0f)
        var enabled by mutableStateOf(true)
        val exposure = RawParam(R.string.raw_editor_exposure, FujiIcons.Exposure, 0f, -3f..3f, true, 1f, 1, " EV") {}
        assertEquals(59, exposure.steps)
        assertEquals(.5f, exposure.snap(.46f))
        assertEquals(-3f, exposure.snap(-100f))
        assertEquals(24, exposure.copy(range = .5f..3f).steps)
        assertEquals(99, exposure.copy(range = 0f..1f, displayScale = 100f, decimals = 0).steps)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme {
                    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                        RawParameterWheel(exposure.copy(value = value, enabled = enabled, onChange = { value = it }), Modifier.fillMaxWidth())
                    }
                }
            }
        }
        val wheel = compose.onNodeWithTag("raw-editor-wheel-${R.string.raw_editor_exposure}")
        assertEquals(59, wheel.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].steps)
        wheel.performSemanticsAction(SemanticsActions.SetProgress) {
            assertEquals(false, it(Float.NaN))
            assertEquals(false, it(Float.POSITIVE_INFINITY))
        }
        wheel.performTouchInput { click(centerRight) }
        compose.runOnIdle { assertEquals(0f, value) }
        wheel.performTouchInput { down(center); moveBy(Offset(-width * .1f, 0f), 200) }
        compose.mainClock.advanceTimeBy(300)
        var before = 0f
        compose.runOnIdle { before = value; assertTrue(value > 0f) }
        wheel.performTouchInput {
            repeat(30) { moveBy(Offset(-width / 600f, 0f), 16) }
            advanceEventTime(200)
            up()
        }
        compose.runOnIdle { assertTrue("Fine drag must retain sub-step movement", value >= before + .2f) }
        wheel.performCustomAccessibilityActionWithLabel("Reset Exposure")
        compose.runOnIdle { assertEquals(0f, value) }
        compose.mainClock.autoAdvance = false
        wheel.performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(-width * .04f, 0f), delayMillis = 10) }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { before = value }
        wheel.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertTrue("The wheel must coast after release: $before → $value", value > before) }
        compose.runOnIdle { value = 0f }
        compose.mainClock.advanceTimeBy(2000)
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals("External reset must stop the fling", 0f, value) }
        wheel.performSemanticsAction(SemanticsActions.SetProgress) { it(100f) }
        compose.runOnIdle { assertEquals(3f, value) }
        wheel.performTouchInput { swipe(center, centerLeft, 200) }
        compose.runOnIdle { assertEquals(3f, value) }
        wheel.performCustomAccessibilityActionWithLabel("Reset Exposure")
        wheel.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        wheel.performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(.1f, value) }
        wheel.performKeyInput { pressKey(Key.DirectionLeft) }
        compose.runOnIdle { assertEquals(0f, value) }
        wheel.performKeyInput { pressKey(Key.MoveEnd) }
        compose.runOnIdle { assertEquals(3f, value) }
        wheel.performKeyInput { pressKey(Key.MoveHome) }
        compose.runOnIdle { assertEquals(-3f, value) }
        wheel.performCustomAccessibilityActionWithLabel("Reset Exposure")
        compose.runOnIdle { enabled = false; before = value }
        wheel.assertIsNotEnabled().performTouchInput { swipe(center, centerLeft, 200) }
        compose.runOnIdle { assertEquals(before, value) }
    }

    @Test
    fun fixedTitleAndSelectorHeightKeepsEffectsPreviewStill() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(preview = bitmap, adjustments = RawAdjustments(grainAmount = .4f, vignette = -.3f)))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {}) }
            }
        }
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        val photo = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
        fun check(id: Int) {
            val title = compose.onNodeWithTag("raw-editor-title-$id").getUnclippedBoundsInRoot()
            assertEquals(48f, (title.bottom - title.top).value, .5f)
            compose.onNodeWithTag("raw-editor-wheel-$id").assertIsDisplayed()
            assertEquals(photo, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
        }
        check(R.string.raw_editor_grain_amount)
        listOf("Clarity" to R.string.raw_editor_clarity, "Texture" to R.string.raw_editor_texture,
            "Dehaze" to R.string.raw_editor_dehaze).forEach { (name,id) ->
            compose.onNodeWithContentDescription("$name controls").performScrollTo().performClick()
            check(id)
        }
        compose.onNodeWithContentDescription("Grain controls").performScrollTo().performClick()
        check(R.string.raw_editor_grain_amount)
        listOf("Size" to R.string.raw_editor_grain_size, "Roughness" to R.string.raw_editor_grain_roughness).forEach { (name,id) ->
            compose.onNodeWithTag("raw-editor-parameter-selector", useUnmergedTree = true).performClick()
            compose.onNodeWithText(name).performClick()
            check(id)
        }
        compose.onNodeWithContentDescription("Vignette controls").performScrollTo().performClick()
        check(R.string.raw_editor_vignette)
        listOf("Midpoints" to R.string.raw_editor_vignette_midpoint, "Roundness" to R.string.raw_editor_vignette_roundness,
            "Feather" to R.string.raw_editor_vignette_feather).forEach { (name,id) ->
            compose.onNodeWithTag("raw-editor-parameter-selector", useUnmergedTree = true).performClick()
            compose.onNodeWithText(name).performClick()
            check(id)
        }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val capture = compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(context.cacheDir, "raw-wheel-effects.png").outputStream().use {
            capture.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun helpAndErrorsDoNotResizeThePreview() {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        var state by mutableStateOf(RawEditorState(preview = bitmap, lensCorrectionApplied = true))
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, {}, {}) }
            }
        }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        for (expanded in listOf(false, true)) {
            compose.runOnIdle { wide = expanded; state = state.copy(lensCorrectionApplied = true) }
            compose.onNodeWithText("Light").performScrollTo().performClick()
            val photo = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
            compose.onNodeWithText("Optics").performScrollTo().performClick()
            assertEquals(photo, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
            for (applied in listOf(true, false)) {
                compose.runOnIdle { state = state.copy(lensCorrectionApplied = applied) }
                val note = context.getString(if (applied) R.string.raw_editor_optics_note else R.string.raw_editor_optics_unavailable)
                compose.onNodeWithText(note).assertDoesNotExist()
                compose.onNodeWithContentDescription("Help", useUnmergedTree = true).performClick()
                compose.onNodeWithText(note).assertIsDisplayed()
                assertEquals(photo, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            }
            compose.runOnIdle { state = state.copy(error = "RAW rendering failed") }
            compose.onNodeWithText("RAW rendering failed").assertIsDisplayed()
            assertEquals(photo, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
            compose.runOnIdle { state = state.copy(error = null, busy = true, detailLoading = true) }
            assertEquals(photo, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
            compose.runOnIdle { state = state.copy(busy = false, detailLoading = false) }
            compose.onNodeWithText("Curves").performScrollTo().performClick()
            val graph = compose.onNodeWithTag("raw-tone-curve").getUnclippedBoundsInRoot()
            val hint = context.getString(R.string.raw_editor_curve_hint)
            compose.onNodeWithText(hint).assertDoesNotExist()
            compose.onNodeWithContentDescription("Help", useUnmergedTree = true).performClick()
            compose.onNodeWithText(hint).assertIsDisplayed()
            assertEquals(graph, compose.onNodeWithTag("raw-tone-curve").getUnclippedBoundsInRoot())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        }
    }

    @Test
    fun comparisonMenuUsesBothSourcesOppositeCropInCompactAndWideLayouts() {
        fun solid(color: Int) = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val original = RawAdjustments(exposure = .7f)
        val state = RawEditorState(name = "sample.RAF", preview = solid(android.graphics.Color.RED),
            cameraImage = solid(android.graphics.Color.BLUE), startingImage = solid(android.graphics.Color.GREEN), adjustments = original)
        var wide by mutableStateOf(false)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(if (wide) 840.dp else 393.dp, 800.dp))) {
                FujiTheme { RawEditorScreen(state, {}, {}, {}, {}) }
            }
        }
        fun source(label: String, color: Int) {
            compose.onNodeWithContentDescription(label).assertIsDisplayed()
            if (label == "RAW with film simulation color profile") {
                compose.onNodeWithTag("raw-editor-comparison-indicator", useUnmergedTree = true).assertDoesNotExist()
            } else {
                compose.onNodeWithText(if (label == "RAW before edits") "Original" else "Embedded JPG").assertIsDisplayed()
                val indicator = compose.onNodeWithTag("raw-editor-comparison-indicator", useUnmergedTree = true).getUnclippedBoundsInRoot()
                val preview = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
                assertEquals((preview.left + preview.right).value / 2, (indicator.left + indicator.right).value / 2, 1f)
                assertEquals(8f, (indicator.top - preview.top).value, 1f)
            }
            val image = compose.onNodeWithTag("raw-editor-preview").captureToImage().asAndroidBitmap()
            assertEquals(color, image.getPixel(image.width / 2, image.height / 2))
        }
        for (expanded in listOf(false, true)) {
            compose.runOnIdle { wide = expanded }
            val preview = compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot()
            val compare = compose.onNodeWithTag("raw-editor-compare").getUnclippedBoundsInRoot()
            val crop = compose.onNodeWithTag("raw-editor-crop").getUnclippedBoundsInRoot()
            assertEquals(crop.top, compare.top)
            assertEquals(crop.bottom, compare.bottom)
            assertEquals((crop.right - crop.left).value, (compare.right - compare.left).value, .5f)
            assertEquals((preview.right - crop.right).value, (compare.left - preview.left).value, 1f)
            for ((name, label, color) in listOf(Triple("Embedded JPEG", "Original camera JPEG", android.graphics.Color.BLUE),
                Triple("Before edits", "RAW before edits", android.graphics.Color.GREEN))) {
                compose.onNodeWithTag("raw-editor-compare").performClick()
                compose.onNodeWithText(name).performClick()
                source(label, color)
                compose.onNodeWithTag("raw-editor-compare").performClick()
                source("RAW with film simulation color profile", android.graphics.Color.RED)
                compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
                compose.mainClock.advanceTimeBy(650)
                source(label, color)
                compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
                source("RAW with film simulation color profile", android.graphics.Color.RED)
                assertEquals(preview, compose.onNodeWithTag("raw-editor-preview").getUnclippedBoundsInRoot())
            }
            assertEquals(original, state.adjustments)
        }
    }

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
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {},
                    onCrop = { state = state.copy(cropPreview = bitmap, cropAdjustments = state.adjustments, cropImageAspect = 1.5f) },
                    onCropAdjust = { state = state.copy(cropAdjustments = it) },
                    onDismissCrop = { state = state.copy(cropPreview = null, cropAdjustments = null) },
                ) }
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
            compose.mainClock.advanceTimeBy(650)
            assertEquals(save, bounds("Save"))
            assertEquals(shadows, bounds("Shadows"))
            assertEquals(image, compose.onNodeWithContentDescription("Original camera JPEG").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { up() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
            compose.mainClock.advanceTimeBy(650)
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { cancel() }
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Compare camera JPEG")
            compose.onNodeWithContentDescription("Original camera JPEG").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performCustomAccessibilityActionWithLabel("Back to edited RAW")
            compose.onNodeWithContentDescription("RAW with film simulation color profile").assertIsDisplayed()
            compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
            compose.mainClock.advanceTimeBy(650)
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
            val grouped = selectGroupedControl(label) != null
            val control = compose.onNodeWithContentDescription(if (label == "Vignette") "Amount" else label)
            if (wide) control.performScrollTo()
            else if (!grouped && label != "Straighten") compose.onNodeWithContentDescription("$label controls").performScrollTo().performClick()
            control.performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
            compose.onNodeWithText("Save").assertIsDisplayed()
        }
        compose.onNodeWithText("Color").performClick()
        set("Temperature", .4f)
        set("Tint", -.3f)
        compose.runOnIdle {
            assertEquals(.4f, state.adjustments.temperature)
            assertEquals(-.3f, state.adjustments.tint)
        }
        compose.onNodeWithContentDescription("As shot").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0f, state.adjustments.temperature)
            assertEquals(0f, state.adjustments.tint)
        }
        set("Saturation", -.5f)
        set("Vibrance", .5f)
        compose.onNodeWithText("Details").performClick()
        set("Sharpening", .6f)
        set("Radius", 1.8f)
        set("Color", .6f)
        set("Luminance", .5f)
        set("Preserve Detail", .7f)
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
        set("Midpoints", .6f)
        set("Roundness", -.4f)
        set("Feather", .7f)
        compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Rotate right 90°").performClick()
        set("Straighten", -4.5f)
        compose.onNodeWithText("Apply crop").performClick()
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
        set("Vertical", .3f)
        set("Horizontal", -.2f)
        set("Red/Cyan", .5f)
        set("Blue/Yellow", -.5f)
        set("Purple", .6f)
        set("Green", .7f)
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
        compose.onNodeWithTag("raw-editor-preview").performTouchInput { down(center); advanceEventTime(600) }
            compose.mainClock.advanceTimeBy(650)
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
                FujiTheme { RawEditorScreen(state, {}, {}, { state = state.copy(adjustments = it) }, {},
                    onCrop = { state = state.copy(cropPreview = bitmap, cropAdjustments = state.adjustments, cropImageAspect = 1.5f) },
                    onCropAdjust = { state = state.copy(cropAdjustments = it) },
                    onDismissCrop = { state = state.copy(cropPreview = null, cropAdjustments = null) },
                ) }
            }
        }
        compose.onNodeWithText("Exposure").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(expected, state.adjustments) }
        compose.runOnIdle { state = state.copy(busy = true) }
        compose.onNodeWithText("Exposure").performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(expected, state.adjustments); state = state.copy(busy = false) }

        fun reset(label: String, after: RawAdjustments) {
            val parameter = selectGroupedControl(label)
            if (parameter != null) {
                compose.onNodeWithTag("raw-editor-parameter-$parameter", useUnmergedTree = true).performTouchInput { doubleClick() }
            } else compose.onNodeWithText(label).performScrollTo().performTouchInput { doubleClick() }
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
        compose.onNodeWithText("Details").performClick()
        reset("Sharpening", expected.copy(sharpening = 0f))
        reset("Radius", expected.copy(sharpenRadius = 1f))
        reset("Color", expected.copy(colorNoise = 0f))
        reset("Luminance", expected.copy(luminanceNoise = 0f))
        reset("Preserve Detail", expected.copy(noiseDetail = .5f))
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        reset("Clarity", expected.copy(clarity = 0f))
        reset("Texture", expected.copy(texture = 0f))
        reset("Dehaze", expected.copy(dehaze = 0f))
        reset("Vignette", expected.copy(vignette = 0f))
        reset("Midpoints", expected.copy(vignetteMidpoint = .5f))
        reset("Roundness", expected.copy(vignetteRoundness = 0f))
        reset("Feather", expected.copy(vignetteFeather = .5f))
        compose.onNodeWithText("Optics").performScrollTo().performClick()
        reset("Distortion", expected.copy(distortion = 0f))
        reset("Vertical", expected.copy(perspectiveVertical = 0f))
        reset("Horizontal", expected.copy(perspectiveHorizontal = 0f))
        reset("Red/Cyan", expected.copy(chromaticRed = 0f))
        reset("Blue/Yellow", expected.copy(chromaticBlue = 0f))
        reset("Purple", expected.copy(defringePurple = 0f))
        reset("Green", expected.copy(defringeGreen = 0f))
        compose.onNodeWithTag("raw-editor-crop").assertIsDisplayed().performClick()
        repeat(2) { compose.onNodeWithContentDescription("Rotate left 90°").performClick() }
        compose.onNodeWithContentDescription("Straighten").performCustomAccessibilityActionWithLabel("Reset Straighten")
        compose.onNodeWithText("Apply crop").performClick()
        expected = expected.copy(quarterTurns = 0, straighten = 0f)
        compose.runOnIdle { assertEquals(RawAdjustments(sharpenThreshold = .2f), state.adjustments) }
        compose.onNodeWithText("Light").performScrollTo().performClick()
        compose.runOnIdle { state = state.copy(adjustments = state.adjustments.copy(exposure = 1f)) }
        compose.onNodeWithText("Exposure").performCustomAccessibilityActionWithLabel("Reset Exposure")
        compose.runOnIdle { assertEquals(RawAdjustments(sharpenThreshold = .2f), state.adjustments) }
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
            val control = compose.onNodeWithContentDescription(label)
            if (label in listOf("Amount", "Size", "Roughness")) selectGroupedControl(label)
            else compose.onNodeWithContentDescription("$label controls").performScrollTo().performClick()
            control.performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
        }
        fun reset(label: String) {
            val parameter = selectGroupedControl(label)!!
            compose.onNodeWithTag("raw-editor-parameter-$parameter", useUnmergedTree = true).performTouchInput { doubleClick() }
        }
        compose.onNodeWithText("Effects").performScrollTo().performClick()
        set("Amount",.6f); set("Size",6f); set("Roughness",.8f)
        reset("Amount")
        compose.runOnIdle { assertEquals(0f,state.adjustments.grainAmount); assertEquals(6f,state.adjustments.grainSize) }
        reset("Size"); reset("Roughness")
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
        val maskPreview = compose.onNodeWithTag("raw-mask-preview").getUnclippedBoundsInRoot()
        compose.runOnIdle { state = state.copy(rendering = true, error = "Mask rendering failed") }
        compose.onNodeWithText("Mask rendering failed").assertIsDisplayed()
        assertEquals(maskPreview, compose.onNodeWithTag("raw-mask-preview").getUnclippedBoundsInRoot())
        compose.runOnIdle { state = state.copy(rendering = false, error = null) }
        val brushHint = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.raw_editor_brush_hint)
        compose.onNodeWithText(brushHint).assertDoesNotExist()
        compose.onNodeWithContentDescription("Help", useUnmergedTree = true).performClick()
        compose.onNodeWithText(brushHint).assertIsDisplayed()
        assertEquals(maskPreview, compose.onNodeWithTag("raw-mask-preview").getUnclippedBoundsInRoot())
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
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
        var edits by mutableStateOf(RawAdjustments(crop = RawCrop(.2f,.2f,.8f,.8f)))
        compose.setContent {
            FujiTheme { RawCropDialog(bitmap, edits, { dismissed = true }, { result = it.crop }, onAdjust = { edits = it }, imageAspect = 2f) }
        }
        compose.onNodeWithTag("raw-crop-preview").performTouchInput {
            down(center); moveBy(Offset(30f, 0f), 200); up()
        }
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertTrue(result!!.left > .2f); assertEquals(.6f, result!!.width, .0001f) }
        selectCropRatio("1:1")
        compose.onNodeWithTag("raw-crop-preview").performCustomAccessibilityActionWithLabel("Make crop smaller")
        compose.onNodeWithTag("raw-crop-preview").performCustomAccessibilityActionWithLabel("Move crop left")
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertEquals(1f, result!!.width / result!!.height * 2f, .0001f) }
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithTag("raw-crop-aspect-selector").assertTextContains("Free")
        compose.onNodeWithText("Apply crop").performClick()
        compose.runOnIdle { assertEquals(RawCrop(), result) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
