package dev.bondarenko.fujirecipes.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.bondarenko.fujirecipes.ui.library.LibraryRouteContent
import dev.bondarenko.fujirecipes.ui.editor.RecipeEditorRouteContent
import dev.bondarenko.fujirecipes.ui.exporting.ExportRouteContent
import dev.bondarenko.fujirecipes.ui.importing.FileImportRouteContent
import dev.bondarenko.fujirecipes.ui.importing.ImportRouteContent
import dev.bondarenko.fujirecipes.ui.photo.PhotoReaderRouteContent
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewRouteContent
import dev.bondarenko.fujirecipes.ui.settings.SettingsRouteContent
import kotlinx.serialization.Serializable

/**
 * The routes, as types.
 *
 * Type-safe navigation rather than string routes: a destination that gains an argument
 * becomes a compile error at every call site instead of a runtime "argument not found".
 */
@Serializable
data object LibraryRoute

/**
 * `id = null` is create; a non-null id is edit.
 *
 * `duplicateOf` names a recipe to copy: the form loads from it but saves as new, so the
 * original is untouched. Both null on a plain create.
 */
/**
 * `prefill` carries settings decoded from a photo (FEAT-009) as JSON, for a create that
 * starts from something rather than from nothing. A route argument rather than a holder on
 * `AppContainer`, so it survives process death like every other argument here.
 */
@Serializable
data class RecipeEditorRoute(
    val id: String? = null,
    val duplicateOf: String? = null,
    val prefill: String? = null,
    val prefillName: String? = null,
)

/** Read-only. Reached by tapping a card; its Edit action leads to [RecipeEditorRoute]. */
@Serializable
data class RecipeViewRoute(val id: String)

/** Bottom bar → Read: decode a photo's MakerNote and match it (FEAT-009). */
@Serializable
data object PhotoRoute

@Serializable
data object MoreRoute

/** More → Import: read the camera's C1–C7 into the library (FEAT-007). */
@Serializable
data object ImportRoute

/** More → Export: build a file from the library and hand it to the share sheet (FEAT-008). */
@Serializable
data object ExportRoute

/** More → Import a file: read back an export this app or the web client wrote (FEAT-012). */
@Serializable
data object FileImportRoute

@Composable
fun FujiNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    NavHost(navController = navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryRouteContent(
                onOpenRecipe = {},
                onEditRecipe = { id -> navController.navigate(RecipeEditorRoute(id)) },
                onCreateRecipe = { navController.navigate(RecipeEditorRoute(null)) },
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
                prefill = route.prefill,
                prefillName = route.prefillName,
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
            SettingsRouteContent(
                onOpenImport = { navController.navigate(ImportRoute) },
                onOpenFileImport = { navController.navigate(FileImportRoute) },
                onOpenExport = { navController.navigate(ExportRoute) },
                contentPadding = contentPadding,
            )
        }

        composable<PhotoRoute> {
            PhotoReaderRouteContent(
                onOpenRecipe = { id -> navController.navigate(RecipeViewRoute(id)) },
                onSaveAsNew = { prefill, name ->
                    navController.navigate(
                        RecipeEditorRoute(id = null, prefill = prefill, prefillName = name),
                    )
                },
                contentPadding = contentPadding,
            )
        }

        composable<FileImportRoute> {
            FileImportRouteContent(
                onBack = { navController.popBackStack() },
                // Finishing an import means going to look at what landed.
                onDone = {
                    navController.navigate(LibraryRoute) {
                        popUpTo(LibraryRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                contentPadding = contentPadding,
            )
        }

        composable<ExportRoute> {
            ExportRouteContent(
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding,
            )
        }

        composable<ImportRoute> {
            ImportRouteContent(
                onBack = { navController.popBackStack() },
                // Finishing an import means going to look at what landed.
                onDone = {
                    navController.navigate(LibraryRoute) {
                        popUpTo(LibraryRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                contentPadding = contentPadding,
            )
        }
    }
}
