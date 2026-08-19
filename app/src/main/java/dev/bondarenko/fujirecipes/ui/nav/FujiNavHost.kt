package dev.bondarenko.fujirecipes.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import dev.bondarenko.fujirecipes.ui.about.AboutRouteContent
import dev.bondarenko.fujirecipes.ui.about.DisclaimerRouteContent
import dev.bondarenko.fujirecipes.ui.camera.CameraRouteContent
import dev.bondarenko.fujirecipes.ui.cleanup.CleanupRouteContent
import dev.bondarenko.fujirecipes.ui.library.LibraryRouteContent
import dev.bondarenko.fujirecipes.ui.editor.RecipeEditorRouteContent
import dev.bondarenko.fujirecipes.ui.exporting.ExportRouteContent
import dev.bondarenko.fujirecipes.ui.importing.FileImportRouteContent
import dev.bondarenko.fujirecipes.ui.importing.ImportRouteContent
import dev.bondarenko.fujirecipes.ui.photo.PhotoReaderRouteContent
import dev.bondarenko.fujirecipes.ui.recipe.RecipeViewRouteContent
import dev.bondarenko.fujirecipes.ui.settings.SettingsRouteContent
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion
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

/** Bottom bar → Cleanup: find and manage duplicated and similar recipes. */
@Serializable
data object CleanupRoute

@Serializable
data object MoreRoute

/** Bottom bar -> USB status: what is connected, and the connect/disconnect action. */
@Serializable
data object CameraRoute

/** More → Import: read the camera's C1–C7 into the library (FEAT-007). */
@Serializable
data object ImportRoute

/** More → Export: build a file from the library and hand it to the share sheet (FEAT-008). */
@Serializable
data object ExportRoute

/** More → Import a file: read back an export this app or the web client wrote (FEAT-012). */
@Serializable
data object FileImportRoute

/** More → About: app info, contact and disclaimer. */
@Serializable
data object AboutRoute

/** More → About → Disclaimer: full legal waiver and limitation of liability. */
@Serializable
data object DisclaimerRoute

/**
 * Where a destination sits in the toolbar, left to right, or -1 if it is not in the bar.
 *
 * Only used to work out which way a transition should travel: tapping something to the right
 * of where you are slides the screens left, and vice versa, so the motion agrees with the
 * thing you pressed. Keep in step with the item order in `AppShell`.
 */
private fun NavDestination?.toolbarIndex(): Int = when {
    this == null -> -1
    hasRoute<LibraryRoute>() -> 0
    hasRoute<PhotoRoute>() -> 1
    hasRoute<CleanupRoute>() -> 2
    hasRoute<CameraRoute>() -> 3
    hasRoute<MoreRoute>() -> 4
    else -> -1
}

/** 💡 The arriving screen starts slightly small and grows in; reversed on the way back. */
private const val DepthScaleIn = 0.92f

/** 💡 The departing screen grows past full size as it fades, which reads as passing under. */
private const val DepthScaleOut = 1.06f

/**
 * Which way the pair should slide, or null when the move is not along the toolbar.
 *
 * Anything involving a subpage — the editor, export, a recipe — has no position in the bar, so
 * it gets no direction and takes the depth transition instead. A sideways slide there would
 * imply a peer relationship that does not exist.
 */
private fun slideDirection(
    from: NavDestination?,
    to: NavDestination?,
): AnimatedContentTransitionScope.SlideDirection? {
    val start = from.toolbarIndex()
    val end = to.toolbarIndex()
    return when {
        start < 0 || end < 0 || start == end -> null
        // Start/End rather than Left/Right, so this still reads correctly in RTL.
        end > start -> AnimatedContentTransitionScope.SlideDirection.Start
        else -> AnimatedContentTransitionScope.SlideDirection.End
    }
}

@Composable
fun FujiNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    // Hoisted: the transition lambdas below are not composable, so they cannot read either of
    // these themselves.
    val reducedMotion = LocalReducedMotion.current
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    /**
     * Going deeper, or coming back up.
     *
     * Along the toolbar the pair slides sideways. Everywhere else the move is hierarchical —
     * a recipe into its editor, settings into export — and that reads as depth rather than
     * distance: the arriving screen grows into place while the one behind it recedes.
     *
     * A plain cross-fade was the previous answer and it looked like no transition at all,
     * because the effects spring is quick enough to be imperceptible on a full-screen change.
     */
    fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(forward: Boolean): EnterTransition {
        val direction = slideDirection(initialState.destination, targetState.destination)
        return when {
            reducedMotion -> fadeIn(effects)
            direction != null -> slideIntoContainer(direction, spatial)
            else -> fadeIn(effects) +
                scaleIn(initialScale = if (forward) DepthScaleIn else DepthScaleOut, animationSpec = scale)
        }
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition {
        val direction = slideDirection(initialState.destination, targetState.destination)
        return when {
            reducedMotion -> fadeOut(effects)
            direction != null -> slideOutOfContainer(direction, spatial)
            else -> fadeOut(effects) +
                scaleOut(targetScale = if (forward) DepthScaleOut else DepthScaleIn, animationSpec = scale)
        }
    }

    NavHost(
        navController = navController,
        // Always the library. There is no setup to route around any more.
        startDestination = LibraryRoute,
        enterTransition = { enter(forward = true) },
        exitTransition = { exit(forward = true) },
        // Back along the bar is the same journey reversed, and the index comparison already
        // says which way that is; back out of a subpage reverses the depth instead.
        popEnterTransition = { enter(forward = false) },
        popExitTransition = { exit(forward = false) },
    ) {
        composable<LibraryRoute> {
            LibraryRouteContent(
                onOpenRecipe = {},
                onEditRecipe = { id -> navController.navigate(RecipeEditorRoute(id)) },
                onCreateRecipe = { navController.navigate(RecipeEditorRoute(null)) },
                onImportFromCamera = { navController.navigate(ImportRoute) },
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
                onOpenAbout = { navController.navigate(AboutRoute) },
                contentPadding = contentPadding,
            )
        }

        composable<AboutRoute> {
            AboutRouteContent(
                onOpenDisclaimer = { navController.navigate(DisclaimerRoute) },
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding,
            )
        }

        composable<DisclaimerRoute> {
            DisclaimerRouteContent(
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding,
            )
        }

        composable<CameraRoute> {
            CameraRouteContent(contentPadding = contentPadding)
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

        composable<CleanupRoute> {
            CleanupRouteContent(
                onOpenRecipe = { id -> navController.navigate(RecipeViewRoute(id)) },
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
                // An empty library has nothing to export, so the page offers the one thing
                // that fixes that. Replacing export in the back stack: coming back to a page
                // that said "nothing to export" from the recipe you just made would be odd.
                onOpenEditor = { prefill, prefillName ->
                    navController.navigate(
                        RecipeEditorRoute(id = null, prefill = prefill, prefillName = prefillName),
                    ) {
                        popUpTo<ExportRoute> { inclusive = true }
                    }
                },
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
