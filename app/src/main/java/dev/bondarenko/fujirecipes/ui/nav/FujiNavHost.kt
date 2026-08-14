package dev.bondarenko.fujirecipes.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen
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

@Serializable
data object MoreRoute

/**
 * Every destination in the app, and nothing else.
 *
 * The bodies are placeholders at FEAT-000 on purpose — this stage ships a navigable shell,
 * so what is being proved here is the graph, the back stack and the insets, not any screen.
 * Each placeholder is replaced by the feature that owns it, and the route declarations
 * above do not change when that happens.
 */
@Composable
fun FujiNavHost(
    navController: NavHostController,
    startDestination: Any,
    contentPadding: PaddingValues,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<ConnectionRoute> {
            PlaceholderScreen(
                titleRes = R.string.placeholder_connection_title,
                bodyRes = R.string.placeholder_connection_body,
                contentPadding = contentPadding,
            )
        }

        composable<LibraryRoute> {
            PlaceholderScreen(
                titleRes = R.string.placeholder_library_title,
                bodyRes = R.string.placeholder_library_body,
                contentPadding = contentPadding,
            )
        }

        composable<RecipeEditorRoute> { entry ->
            // Read even though it is unused, so the argument is exercised by the shell and
            // a serialization mistake surfaces at FEAT-000 rather than at FEAT-002.
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
