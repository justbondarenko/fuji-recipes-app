package dev.bondarenko.fujirecipes.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen
import dev.bondarenko.fujirecipes.ui.connection.ConnectionRouteContent
import dev.bondarenko.fujirecipes.ui.library.LibraryRouteContent
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewRouteContent
import kotlinx.serialization.Serializable

/**
 * The routes, as types.
 *
 * Type-safe navigation rather than string routes: a destination that gains an argument
 * becomes a compile error at every call site instead of a runtime "argument not found".
 */
@Serializable
data object ConnectionRoute

@Serializable
data object LibraryRoute

/** `id = null` is create; a non-null id is edit. One screen, one ViewModel (FEAT-002). */
@Serializable
data class RecipeEditorRoute(val id: String? = null)

/** Read-only. Reached by tapping a card; its Edit action leads to [RecipeEditorRoute]. */
@Serializable
data class RecipeViewRoute(val id: String)

@Serializable
data object MoreRoute

@Composable
fun FujiNavHost(
    navController: NavHostController,
    startDestination: Any,
    contentPadding: PaddingValues,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<ConnectionRoute> {
            ConnectionRouteContent(
                onSaved = {
                    // Saving is the way out of setup, and coming back to it should not
                    // reopen the form — so the library replaces it rather than stacking.
                    navController.navigate(LibraryRoute) {
                        popUpTo(ConnectionRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                contentPadding = contentPadding,
            )
        }

        composable<LibraryRoute> {
            LibraryRouteContent(
                onOpenRecipe = { id -> navController.navigate(RecipeViewRoute(id)) },
                onCreateRecipe = { navController.navigate(RecipeEditorRoute(null)) },
                onOpenConnection = { navController.navigate(ConnectionRoute) },
                contentPadding = contentPadding,
            )
        }

        composable<RecipeViewRoute> { entry ->
            val route = entry.toRoute<RecipeViewRoute>()
            RecipeViewRouteContent(
                recipeId = route.id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(RecipeEditorRoute(route.id)) },
            )
        }

        composable<RecipeEditorRoute> { entry ->
            // Read even though it is unused, so a serialization mistake surfaces now rather
            // than at FEAT-002.
            entry.toRoute<RecipeEditorRoute>()
            PlaceholderScreen(
                titleRes = R.string.placeholder_editor_title,
                bodyRes = R.string.placeholder_editor_body,
                contentPadding = contentPadding,
            )
        }

        composable<MoreRoute> {
            PlaceholderScreen(
                titleRes = R.string.placeholder_more_title,
                bodyRes = R.string.placeholder_more_body,
                contentPadding = contentPadding,
            )
        }
    }
}
