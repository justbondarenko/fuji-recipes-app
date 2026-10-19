package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.PhotoCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LibraryInteractionsTest {
    @get:Rule val compose = createComposeRule()
    private val recipe = RecipeCardModel("test-recipe", "Test recipe", "classic-chrome", 5, emptyList())
    private var openedId: String? = null
    private var editedId: String? = null
    private var deletedId: String? = null

    @Test
    fun narrowSearchKeepsPlaceholderAndLongQueriesOnOneLine() {
        val query = mutableStateOf("")
        compose.setContent {
            FujiTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(300.dp, 700.dp))) {
                    LibraryToolbar(
                        state = LibraryUiState(search = query.value, hasLoaded = true, totalCount = 30),
                        onSearchChange = { query.value = it },
                        onSortChange = {}, onSortDirectionChange = {}, onToggleSortDirection = {},
                        onFiltersChange = {}, onClearSearchAndFilters = {}, onOpenSettings = {},
                        cameraButton = {
                            IconButton(onClick = {}) {
                                Icon(FujiIcons.PhotoCamera, contentDescription = "Test camera")
                            }
                        },
                    )
                }
            }
        }
        val field = compose.onNode(hasSetTextAction())
        val before = field.fetchSemanticsNode().boundsInRoot
        val camera = compose.onNodeWithContentDescription("Test camera").fetchSemanticsNode().boundsInRoot
        assertTrue(camera.bottom <= before.top)
        val placeholderLayout = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label(R.string.search_placeholder), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(placeholderLayout) }
        assertEquals(1, placeholderLayout.single().lineCount)
        field.performTextInput("A very long recipe name with several searchable tags")
        assertEquals(before.height, field.fetchSemanticsNode().boundsInRoot.height, 1f)
    }

    @Test
    fun phoneLongPressOpensMenuAndEditDoesNotOpenTheRecipe() {
        renderLibrary(393)
        longPressRecipe()
        compose.onNodeWithText(label(R.string.action_edit)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(recipe.id, editedId)
            assertNull(openedId)
        }
    }

    @Test
    fun narrowMenuExplainsUnavailableWriteAndRetainsBatchSelection() {
        renderLibrary(300)
        longPressRecipe()
        compose.onNodeWithText(label(R.string.action_write_to_camera)).assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.camera_not_connected_tooltip)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.action_select)).performClick()
        compose.onNodeWithContentDescription(label(R.string.action_clear_selection)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.action_edit)).assertDoesNotExist()
        compose.runOnIdle { assertNull(openedId) }
    }

    @Test
    fun menuDeleteRequiresConfirmation() {
        renderLibrary(300)
        longPressRecipe()
        compose.onNodeWithText(label(R.string.action_delete)).performClick()
        compose.onNodeWithText(label(R.string.delete_title)).assertIsDisplayed()
        compose.runOnIdle { assertNull(deletedId) }
        compose.onNodeWithText(label(R.string.action_delete)).performClick()
        compose.runOnIdle { assertEquals(recipe.id, deletedId) }
    }

    private fun renderLibrary(width: Int) {
        compose.setContent {
            FujiTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 700.dp))) {
                    LibraryScreen(
                        state = LibraryUiState(visible = listOf(recipe), totalCount = 1, hasLoaded = true),
                        onSearchChange = {}, onSortChange = {}, onFiltersChange = {},
                        onClearSearchAndFilters = {}, onRetry = {},
                        onOpenRecipe = { openedId = it }, onEditRecipe = { editedId = it },
                        onDeleteRecipe = { deletedId = it }, onDeleteRecipes = {},
                        onCreateRecipe = {}, onImportFromCamera = {},
                        contentPadding = PaddingValues(0.dp),
                    )
                }
            }
        }
    }

    private fun longPressRecipe() = compose.onNodeWithText(recipe.name).performTouchInput { longClick() }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
