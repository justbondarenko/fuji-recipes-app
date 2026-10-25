package dev.bondarenko.fujirecipes.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.store.ImageStore
import dev.bondarenko.fujirecipes.data.fields.*
import dev.bondarenko.fujirecipes.data.library.SortDirection
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.ui.editor.*
import dev.bondarenko.fujirecipes.ui.library.LibraryToolbar
import dev.bondarenko.fujirecipes.ui.library.LibraryUiState
import dev.bondarenko.fujirecipes.ui.recipe.RecipeHeader
import dev.bondarenko.fujirecipes.ui.recipe.compare.*
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.io.File

@OptIn(ExperimentalTestApi::class)
class MaterialMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectorsScrollKeepChecksAndActionsRetainConfirmation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(context.cacheDir.toPath(), "menu-test-").toFile()
        val images = ImageStore(directory)
        var screen by mutableIntStateOf(0)
        var value by mutableStateOf<String?>(null)
        var film by mutableStateOf(FilmSimulations.all.first().id)
        var selectedTarget: String? = null
        var duplicates = 0
        var deletes = 0
        var library by mutableStateOf(LibraryUiState(sort = SortId.NAME, hasLoaded = true))
        val options = listOf(EnumOption("off", "Off"), EnumOption("weak", "Weak"), EnumOption("strong", "Strong"))
        val field = EnumFieldDef("test", FieldGroup.EFFECTS, "Effect", options, "off")
        val base = RecipeHeader("base", "Base recipe", "classic-chrome", "Classic Chrome", 0, emptyList(), "")
        val target = base.copy(id = "target", name = "Target recipe", filmSimulationLabel = "ASTIA / Soft")
        val candidates = listOf(RecipeCandidate("target", "Target recipe", "ASTIA / Soft", 0),
            RecipeCandidate("alternate", "Alternate recipe", "Velvia / Vivid", 0))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(300.dp, 700.dp))) {
                FujiTheme {
                    Surface(Modifier.fillMaxSize()) {
                        when (screen) {
                            0 -> Column(Modifier.padding(16.dp)) { EnumDropdown(field, value, { value = it }) }
                            1 -> Column(Modifier.padding(16.dp)) {
                                EnumMenu(field, value, { value = it }, groups = listOf(options.take(1), options.drop(1)))
                            }
                            2 -> Column(Modifier.padding(16.dp)) { FilmSimulationPicker(film, { film = it }) }
                            3 -> RecipeCompareContent(
                                RecipeCompareUiState(isLoading = false, baseRecipe = base, targetRecipe = target, availableCandidates = candidates),
                                onSelectTargetRecipe = { selectedTarget = it }, onClearTargetRecipe = {},
                                onDifferencesOnlyChange = {}, onDismiss = {},
                            )
                            4 -> RecipeEditorScreen(
                                state = EditorUiState(isLoading = false, name = "Menu test"), imageStore = images,
                                onNameChange = {}, onNotesChange = {}, onRatingChange = {}, onTagsChange = {},
                                onAddImages = {}, onRemoveImage = {}, onSettingChange = { _, _ -> },
                                onSave = {}, onDelete = { deletes++ }, onDuplicate = { duplicates++ }, onBack = {},
                            )
                            else -> LibraryToolbar(library, {}, { library = library.copy(sort = it) },
                                { library = library.copy(sortDirection = it) }, {}, {}, {})
                        }
                    }
                }
            }
        }
        fun popup(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isPopup()))
        fun label(id: Int) = context.getString(id)
        try {
            compose.onNodeWithText("Off").performClick()
            popup("Off").assertIsSelected()
            val window = compose.onNode(isPopup()).fetchSemanticsNode().boundsInRoot
            val first = popup("Off").fetchSemanticsNode().boundsInRoot
            val last = popup("Strong").fetchSemanticsNode().boundsInRoot
            val shadowSpace = with(compose.density) { 12.dp.toPx() }
            assertTrue("Menu shadow needs space above the surface", first.top - window.top >= shadowSpace)
            assertTrue("Menu shadow needs space below the surface", window.bottom - last.bottom >= shadowSpace)
            File(context.cacheDir, "material-menu.png").outputStream().use {
                compose.onNode(isPopup()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            popup("Strong").performClick()
            compose.runOnIdle { assertEquals("strong", value); screen = 1 }
            compose.onNodeWithText("Strong").performClick()
            popup("Strong").assertIsSelected()
            popup("Weak").performClick()
            compose.runOnIdle { assertEquals("weak", value); screen = 2 }
            compose.onNodeWithText(FilmSimulations.all.first().label).performClick()
            popup(FilmSimulations.all.first().label).assertIsSelected()
            popup(FilmSimulations.all.last().label).performScrollTo().performClick()
            compose.runOnIdle { assertEquals(FilmSimulations.all.last().id, film) }
            compose.onNodeWithText(FilmSimulations.all.last().label).performClick()
            popup(FilmSimulations.all.last().label).performScrollTo().assertIsSelected()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.runOnIdle { screen = 3 }
            compose.onNode(hasText("Target recipe") and hasClickAction()).performClick()
            popup("Target recipe").assertIsSelected()
            popup("Velvia / Vivid").assertIsDisplayed()
            popup("Alternate recipe").performClick()
            compose.runOnIdle { assertEquals("alternate", selectedTarget); screen = 4 }
            compose.onNodeWithContentDescription(label(R.string.action_more)).performClick()
            popup(label(R.string.action_duplicate)).performClick()
            compose.runOnIdle { assertEquals(1, duplicates) }
            compose.onNodeWithContentDescription(label(R.string.action_more)).performClick()
            popup(label(R.string.action_delete)).performClick()
            compose.onNodeWithText(label(R.string.delete_title)).assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, deletes) }
            compose.onNodeWithText(label(R.string.action_cancel)).performClick()
            compose.runOnIdle { screen = 5 }
            compose.onNodeWithText(label(R.string.sort_name)).performClick()
            popup(label(R.string.sort_name)).assertIsSelected()
            popup(label(R.string.sort_name_asc)).assertIsSelected()
            popup(label(R.string.sort_rating)).performClick()
            compose.runOnIdle { assertEquals(SortId.RATING, library.sort) }
            compose.onNodeWithText(label(R.string.sort_rating)).performClick()
            popup(label(R.string.sort_rating)).assertIsSelected()
            popup(label(R.string.sort_rating_desc)).performClick()
            compose.runOnIdle { assertEquals(SortDirection.DESCENDING, library.sortDirection) }
        } finally {
            directory.deleteRecursively()
        }
    }
}
