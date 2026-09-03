package dev.bondarenko.fujirecipes

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.bondarenko.fujirecipes.ui.camera.CameraToolbarItemHost
import dev.bondarenko.fujirecipes.ui.create.CreateRecipeFlow
import dev.bondarenko.fujirecipes.ui.nav.AboutRoute
import dev.bondarenko.fujirecipes.ui.nav.CameraRoute
import dev.bondarenko.fujirecipes.ui.nav.CleanupRoute
import dev.bondarenko.fujirecipes.ui.nav.DisclaimerRoute
import dev.bondarenko.fujirecipes.ui.nav.ExportRoute
import dev.bondarenko.fujirecipes.ui.nav.FileImportRoute
import dev.bondarenko.fujirecipes.ui.nav.FujiNavHost
import dev.bondarenko.fujirecipes.ui.nav.ImportRoute
import dev.bondarenko.fujirecipes.ui.nav.LibraryRoute
import dev.bondarenko.fujirecipes.ui.nav.MoreRoute
import dev.bondarenko.fujirecipes.ui.nav.PhotoRoute
import dev.bondarenko.fujirecipes.ui.nav.PasteRecipeRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeEditorRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeViewRoute
import dev.bondarenko.fujirecipes.ui.shell.AppShell
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    private var sharedPhotoUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        connectIfLaunchedByCamera(intent)
        sharedPhotoUri = extractSharedImageUri(intent)
        // Mandatory on Android 15+ regardless, so it is done deliberately here rather than
        // discovered in a release build.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Straight to the library. There is nothing to decide first — no stored connection to
        // read, no setup to do — so the splash is not held open for anything. The list screen
        // draws its own loading and error states while the store is read.
        setContent {
            FujiApp(
                sharedPhotoUri = sharedPhotoUri,
                onSharedPhotoHandled = { sharedPhotoUri = null },
            )
        }
    }

    /** A camera plugged in or photo shared while the app was already running comes through here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        connectIfLaunchedByCamera(intent)
        extractSharedImageUri(intent)?.let { uri -> sharedPhotoUri = uri }
    }

    /**
     * Extracts a shared image Uri if launched via ACTION_SEND.
     */
    private fun extractSharedImageUri(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        if (!type.startsWith("image/")) return null
        
        val uri: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
            ?: intent.getStringExtra(Intent.EXTRA_STREAM)?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: intent.data

        return uri?.toString()
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
private fun FujiApp(
    sharedPhotoUri: String? = null,
    onSharedPhotoHandled: () -> Unit = {},
) {
    FujiTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination

        LaunchedEffect(sharedPhotoUri) {
            val uri = sharedPhotoUri ?: return@LaunchedEffect
            navController.navigate(PhotoRoute(initialUri = uri)) {
                popUpTo(LibraryRoute)
                launchSingleTop = true
            }
            onSharedPhotoHandled()
        }

        /**
         * The shell chrome is hidden wherever it would be wrong rather than wherever it
         * happens to look busy: the editor owns its own bottom bar (FEAT-002), and subpages
         * have their own top app bar.
         */
        val onEditor = destination?.hasRoute<RecipeEditorRoute>() == true
        // The view screen carries its own top bar and back affordance, and a bottom nav
        // under a recipe would invite leaving the thing you just opened.
        val onRecipeView = destination?.hasRoute<RecipeViewRoute>() == true
        val onImport = destination?.hasRoute<ImportRoute>() == true
        val onFileImport = destination?.hasRoute<FileImportRoute>() == true
        val onExport = destination?.hasRoute<ExportRoute>() == true
        val onPasteRecipe = destination?.hasRoute<PasteRecipeRoute>() == true
        val showChrome =
            !onEditor && !onRecipeView && !onImport && !onFileImport && !onExport && !onPasteRecipe

        // The create choice is transient, but parsing pasted text gets a full route because it
        // can hold a long recipe plus an explicit validation result.
        var creating by remember { mutableStateOf(false) }

        CreateRecipeFlow(
            visible = creating,
            onDismiss = { creating = false },
            onParseText = { navController.navigate(PasteRecipeRoute) },
            onCreate = { prefill, prefillName ->
                navController.navigate(
                    RecipeEditorRoute(id = null, prefill = prefill, prefillName = prefillName),
                )
            },
        )

        val isMoreSelected = destination?.hasRoute<MoreRoute>() == true ||
            destination?.hasRoute<AboutRoute>() == true ||
            destination?.hasRoute<DisclaimerRoute>() == true

        AppShell(
            showChrome = showChrome,
            isLibrarySelected = destination?.hasRoute<LibraryRoute>() == true,
            isReadSelected = destination?.hasRoute<PhotoRoute>() == true,
            isCleanupSelected = destination?.hasRoute<CleanupRoute>() == true,
            isMoreSelected = isMoreSelected,
            onLibraryClick = {
                navController.navigate(LibraryRoute) {
                    popUpTo(LibraryRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onReadClick = { navController.navigate(PhotoRoute()) { launchSingleTop = true } },
            onCleanupClick = { navController.navigate(CleanupRoute) { launchSingleTop = true } },
            onMoreClick = {
                navController.navigate(MoreRoute) {
                    popUpTo(MoreRoute) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onCreateClick = { creating = true },
            cameraItem = {
                CameraToolbarItemHost(
                    selected = destination?.hasRoute<CameraRoute>() == true,
                    onClick = { navController.navigate(CameraRoute) { launchSingleTop = true } },
                )
            },
        ) { contentPadding ->
            FujiNavHost(
                navController = navController,
                contentPadding = contentPadding,
            )
        }
    }
}
