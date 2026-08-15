package dev.bondarenko.fujirecipes

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.bondarenko.fujirecipes.ui.camera.CameraChipHost
import dev.bondarenko.fujirecipes.ui.nav.ConnectionRoute
import dev.bondarenko.fujirecipes.ui.nav.FujiNavHost
import dev.bondarenko.fujirecipes.ui.nav.LibraryRoute
import dev.bondarenko.fujirecipes.ui.nav.MoreRoute
import dev.bondarenko.fujirecipes.ui.nav.PhotoRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeEditorRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeViewRoute
import dev.bondarenko.fujirecipes.ui.shell.AppShell
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        connectIfLaunchedByCamera(intent)
        // Mandatory on Android 15+ regardless, so it is done deliberately here rather than
        // discovered in a release build.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        var startDestination by mutableStateOf<Any?>(null)

        // The splash stays up until the stored connection has been read, because the
        // alternative is showing the library for one frame and replacing it with setup —
        // which reads as the app losing the library rather than never having had one.
        splash.setKeepOnScreenCondition { startDestination == null }

        setContent {
            val container = (application as FujiRecipesApp).container

            LaunchedEffect(Unit) {
                startDestination = if (container.connectionSettings.current().isConfigured) {
                    LibraryRoute
                } else {
                    ConnectionRoute(firstRun = true)
                }
            }

            startDestination?.let { FujiApp(it) }
        }
    }

    /** A camera plugged in while the app was already running comes through here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        connectIfLaunchedByCamera(intent)
    }

    /**
     * The attach-intent path (`PRD.md` §8.2).
     *
     * Permission is granted implicitly for the connection that launched the app, so the
     * connection is started here rather than waiting for the user to tap the chip: plug in,
     * and the app is already talking to the camera by the time it has drawn.
     */
    private fun connectIfLaunchedByCamera(intent: Intent) {
        if (intent.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return

        val device = IntentCompat.getParcelableExtra(
            intent,
            UsbManager.EXTRA_DEVICE,
            UsbDevice::class.java,
        )
        (application as FujiRecipesApp).container.cameraController.onDeviceAttached(device)
    }
}

@Composable
private fun FujiApp(startDestination: Any) {
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
        // The view screen carries its own top bar and back affordance, and a bottom nav
        // under a recipe would invite leaving the thing you just opened.
        val onRecipeView = destination?.hasRoute<RecipeViewRoute>() == true
        val showChrome = !onConnection && !onEditor && !onRecipeView

        AppShell(
            showChrome = showChrome,
            isLibrarySelected = destination?.hasRoute<LibraryRoute>() == true,
            isReadSelected = destination?.hasRoute<PhotoRoute>() == true,
            isMoreSelected = destination?.hasRoute<MoreRoute>() == true,
            onLibraryClick = {
                navController.navigate(LibraryRoute) {
                    popUpTo(LibraryRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onReadClick = { navController.navigate(PhotoRoute) { launchSingleTop = true } },
            onMoreClick = { navController.navigate(MoreRoute) { launchSingleTop = true } },
            onCreateClick = { navController.navigate(RecipeEditorRoute(id = null)) },
            cameraChip = { CameraChipHost() },
        ) { contentPadding ->
            FujiNavHost(
                navController = navController,
                startDestination = startDestination,
                contentPadding = contentPadding,
            )
        }
    }
}
