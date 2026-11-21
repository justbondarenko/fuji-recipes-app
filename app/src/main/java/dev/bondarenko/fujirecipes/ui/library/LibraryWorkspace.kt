package dev.bondarenko.fujirecipes.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewRouteContent
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons

/** The selected id is saved on the library's back-stack entry, including while editing. */
const val LIBRARY_SELECTED_RECIPE_KEY = "librarySelectedRecipe"

@Composable
fun LibraryWorkspace(
    selectedRecipeId: String?,
    onSelectRecipe: (String?) -> Unit,
    onOpenRecipeFullScreen: (String) -> Unit,
    onEditRecipe: (String, Boolean) -> Unit,
    onCreateRecipe: () -> Unit,
    onImportFromCamera: () -> Unit,
    onDevelopRaw: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    onDetailOpenChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    BackHandler(enabled = selectedRecipeId != null) { onSelectRecipe(null) }
    SideEffect { onDetailOpenChange(selectedRecipeId != null) }
    DisposableEffect(Unit) { onDispose { onDetailOpenChange(false) } }

    val split = LocalLibraryPaneSplit.current
    val twoPane = split != null
    LibraryDetailLayout(
        split = split,
        detailOpen = selectedRecipeId != null,
        modifier = Modifier.fillMaxSize(),
        list = {
            LibraryRouteContent(
                onOpenRecipe = { id ->
                    if (twoPane) onSelectRecipe(id) else onOpenRecipeFullScreen(id)
                },
                onEditRecipe = { id -> onEditRecipe(id, twoPane) },
                onCreateRecipe = onCreateRecipe,
                onImportFromCamera = onImportFromCamera,
                onDevelopRaw = onDevelopRaw,
                onOpenSettings = onOpenSettings,
                onSelectionChange = onSelectionChange,
                contentPadding = contentPadding,
                activeRecipeId = selectedRecipeId,
            )
        },
        detail = {
            val paneInsets = if (twoPane) {
                ScaffoldDefaults.contentWindowInsets
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            } else {
                PaddingValues(0.dp)
            }
            val paneModifier = Modifier.padding(paneInsets).consumeWindowInsets(paneInsets)
            if (selectedRecipeId == null) {
                FujiIconPanel(
                    icon = FujiIcons.BookmarkStacks,
                    shape = CircleShape,
                    title = stringResource(R.string.library_select_recipe),
                    modifier = paneModifier,
                )
            } else key(selectedRecipeId) {
                // Changing the selected recipe replaces its model and cancels its collectors.
                val owner = remember {
                    object : ViewModelStoreOwner {
                        override val viewModelStore = ViewModelStore()
                    }
                }
                DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    RecipeViewRouteContent(
                        recipeId = selectedRecipeId,
                        onBack = { onSelectRecipe(null) },
                        onEdit = { onEditRecipe(selectedRecipeId, true) },
                        onDevelopRaw = { onDevelopRaw(selectedRecipeId) },
                        onNavigateToRecipe = onSelectRecipe,
                        embedded = twoPane,
                        modifier = paneModifier,
                    )
                }
            }
        },
    )
}
