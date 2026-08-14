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
import dev.bondarenko.fujirecipes.ui.editor.RecipeEditorRouteContent
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

/**
 * `id = null` is create; a non-null id is edit.
 *
 * `duplicateOf` names a recipe to copy: the form loads from it but saves as new, so the
 * original is untouched. Both null on a plain create.
 */
@Serializable
data class RecipeEditorRoute(val id: String? = null, val duplicateOf: String? = null)

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
            val route = entry.toRoute<RecipeEditorRoute>()
            RecipeEditorRouteContent(
                recipeId = route.id,
                duplicateOf = route.duplicateOf,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    // Back to the recipe just saved, replacing the form so back does not
                    // return to an editor for work that is already committed.
                    navController.navigate(RecipeViewRoute(id)) {
                        popUpTo(LibraryRoute)
                    }
                },
                onDeleted = {
                    navController.navigate(LibraryRoute) {
                        popUpTo(LibraryRoute) { inclusive = true }
                    }
                },
                onDuplicate = { id ->
                    navController.navigate(RecipeEditorRoute(id = null, duplicateOf = id))
                },
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
