package dev.bondarenko.fujirecipes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.bondarenko.fujirecipes.ui.nav.ConnectionRoute
import dev.bondarenko.fujirecipes.ui.nav.FujiNavHost
import dev.bondarenko.fujirecipes.ui.nav.LibraryRoute
import dev.bondarenko.fujirecipes.ui.nav.MoreRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeEditorRoute
import dev.bondarenko.fujirecipes.ui.shell.AppShell
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Mandatory on Android 15+ regardless, so it is done deliberately here rather than
        // discovered in a release build (`coding-standards.md`, Compose conventions).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent { FujiApp() }
    }
}

@Composable
private fun FujiApp() {
    FujiTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination

        /**
         * The shell chrome is hidden wherever it would be wrong rather than wherever it
         * happens to look busy: connection setup is reached before there is a library to
         * navigate to, and the editor owns its own bottom bar (FEAT-002).
         */
        val onConnection = destination?.hasRoute<ConnectionRoute>() == true
        val onEditor = destination?.hasRoute<RecipeEditorRoute>() == true
        val showChrome = !onConnection && !onEditor

        AppShell(
            showChrome = showChrome,
            isLibrarySelected = destination?.hasRoute<LibraryRoute>() == true,
            isMoreSelected = destination?.hasRoute<MoreRoute>() == true,
            onLibraryClick = {
                navController.navigate(LibraryRoute) {
                    popUpTo(LibraryRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onMoreClick = {
                navController.navigate(MoreRoute) { launchSingleTop = true }
            },
            onCreateClick = { navController.navigate(RecipeEditorRoute(id = null)) },
        ) { contentPadding ->
            FujiNavHost(
                navController = navController,
                // FEAT-001 T-13 replaces this with "connection first when unconfigured".
                // Until settings exist there is nothing to branch on.
                startDestination = LibraryRoute,
                contentPadding = contentPadding,
            )
        }
    }
}
