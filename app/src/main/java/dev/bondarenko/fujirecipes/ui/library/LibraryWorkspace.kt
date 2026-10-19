package dev.bondarenko.fujirecipes.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewRouteContent

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
    // Keep the outgoing recipe alive until the close animation has expanded the list.
    var lastRecipeId by remember { mutableStateOf(selectedRecipeId) }
    SideEffect { if (selectedRecipeId != null) lastRecipeId = selectedRecipeId }
    BackHandler(enabled = selectedRecipeId != null) { onSelectRecipe(null) }
    SideEffect { onDetailOpenChange(selectedRecipeId != null) }
    DisposableEffect(Unit) { onDispose { onDetailOpenChange(false) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // This is the width AFTER the navigation rail. Both panes need useful room.
        val twoPane = maxWidth >= 720.dp
        LibraryDetailLayout(
            twoPane = twoPane,
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
                val id = selectedRecipeId ?: lastRecipeId
                if (id != null) key(id) {
                    // Switching the pane's recipe must get a fresh model, and closing it
                    // must cancel its collectors instead of retaining one model per click.
                    val owner = remember {
                        object : ViewModelStoreOwner {
                            override val viewModelStore = ViewModelStore()
                        }
                    }
                    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                        val paneInsets = if (twoPane) contentPadding else PaddingValues(0.dp)
                        RecipeViewRouteContent(
                            recipeId = id,
                            onBack = { onSelectRecipe(null) },
                            onEdit = { onEditRecipe(id, true) },
                            onDevelopRaw = { onDevelopRaw(id) },
                            onNavigateToRecipe = { onSelectRecipe(it) },
                            embedded = twoPane,
                            modifier = Modifier.padding(paneInsets).consumeWindowInsets(paneInsets),
                        )
                    }
                }
            },
        )
    }
}
