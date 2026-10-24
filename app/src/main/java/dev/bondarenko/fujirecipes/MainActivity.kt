package dev.bondarenko.fujirecipes

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.bondarenko.fujirecipes.camera.DESTINATION_CAMERA
import dev.bondarenko.fujirecipes.camera.DESTINATION_PHOTOS
import dev.bondarenko.fujirecipes.camera.EXTRA_DESTINATION
import dev.bondarenko.fujirecipes.ui.camera.CameraSheetButtonHost
import dev.bondarenko.fujirecipes.ui.camera.CameraSheetHost
import dev.bondarenko.fujirecipes.ui.camera.LocalCameraSheetOpener
import dev.bondarenko.fujirecipes.ui.create.CreateRecipeFlow
import dev.bondarenko.fujirecipes.ui.nav.AboutRoute
import dev.bondarenko.fujirecipes.ui.nav.CleanupRoute
import dev.bondarenko.fujirecipes.ui.nav.CameraPhotosRoute
import dev.bondarenko.fujirecipes.ui.nav.DisclaimerRoute
import dev.bondarenko.fujirecipes.ui.nav.ExportRoute
import dev.bondarenko.fujirecipes.ui.nav.FileImportRoute
import dev.bondarenko.fujirecipes.ui.nav.FujiNavHost
import dev.bondarenko.fujirecipes.ui.nav.ImportRoute
import dev.bondarenko.fujirecipes.ui.nav.LibraryRoute
import dev.bondarenko.fujirecipes.ui.nav.MoreRoute
import dev.bondarenko.fujirecipes.ui.nav.PhotoRoute
import dev.bondarenko.fujirecipes.ui.nav.RawLabRoute
import dev.bondarenko.fujirecipes.ui.nav.RawEditorRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeEditorRoute
import dev.bondarenko.fujirecipes.ui.nav.RecipeViewRoute
import dev.bondarenko.fujirecipes.ui.nav.SettingsRoute
import dev.bondarenko.fujirecipes.ui.shell.AppShell
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import dev.bondarenko.fujirecipes.core.settings.NavDestinationId
import dev.bondarenko.fujirecipes.core.settings.StoredUiPreferences
import dev.bondarenko.fujirecipes.ui.nav.toNavDestinationId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var sharedPhotoUri by mutableStateOf<String?>(null)
    private var sharedRawUri by mutableStateOf<String?>(null)

    /** Set by a notification action; consumed once, by the navigation below. */
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        connectIfLaunchedByCamera(intent)
        handleSharedIntent(intent)
        pendingDestination = intent.getStringExtra(EXTRA_DESTINATION)
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
                sharedRawUri = sharedRawUri,
                onSharedRawHandled = { sharedRawUri = null },
                pendingDestination = pendingDestination,
                onDestinationHandled = { pendingDestination = null },
            )
        }
    }

    /** A camera plugged in or photo shared while the app was already running comes through here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        connectIfLaunchedByCamera(intent)
        handleSharedIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let { pendingDestination = it }
    }

    private fun handleSharedIntent(intent: Intent?) {
        val uri = extractSharedImageUri(intent) ?: return
        val targetComponent = intent?.component?.className.orEmpty()
        val isExplicitRawTarget = targetComponent.endsWith("ShareRawEditorActivity")
        val isRafFile = uri.lowercase().contains(".raf") || intent?.type.orEmpty().contains("raf")

        if (isExplicitRawTarget || isRafFile) {
            sharedRawUri = uri
        } else {
            sharedPhotoUri = uri
        }
    }

    /**
     * Extracts a shared image Uri if launched via ACTION_SEND.
     */
    private fun extractSharedImageUri(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        
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
    sharedRawUri: String? = null,
    onSharedRawHandled: () -> Unit = {},
    pendingDestination: String? = null,
    onDestinationHandled: () -> Unit = {},
) {
    FujiTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination

        // The camera side sheet opens over any screen, so its state belongs above all of them.
        var cameraSheetOpen by remember { mutableStateOf(false) }

        /**
         * A notification action naming a screen.
         *
         * `popUpTo(LibraryRoute)` rather than a plain navigate: arriving from outside the app
         * should land *on* that screen, not on top of whatever back stack a previous session
         * left behind.
         */
        LaunchedEffect(pendingDestination) {
            when (pendingDestination) {
                DESTINATION_PHOTOS -> navController.navigate(CameraPhotosRoute) {
                    popUpTo(LibraryRoute)
                    launchSingleTop = true
                }

                // The camera is a sheet over whatever is on screen, so the notification
                // opens it where the user already is rather than moving them first.
                DESTINATION_CAMERA -> cameraSheetOpen = true

                else -> return@LaunchedEffect
            }
            onDestinationHandled()
        }

        LaunchedEffect(sharedPhotoUri) {
            val uri = sharedPhotoUri ?: return@LaunchedEffect
            navController.navigate(PhotoRoute(initialUri = uri)) {
                popUpTo(LibraryRoute)
                launchSingleTop = true
            }
            onSharedPhotoHandled()
        }

        LaunchedEffect(sharedRawUri) {
            val uri = sharedRawUri ?: return@LaunchedEffect
            navController.navigate(RawEditorRoute(initialUri = uri)) {
                popUpTo(LibraryRoute)
                launchSingleTop = true
            }
            onSharedRawHandled()
        }

        /**
         * The shell chrome is hidden wherever it would be wrong rather than wherever it
         * happens to look busy: the editor owns its own bottom bar (FEAT-002), and subpages
         * have their own top app bar.
         */
        val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
        val uiPrefs by container.uiPreferences.preferences.collectAsStateWithLifecycle(initialValue = StoredUiPreferences())
        val pinnedDestinations = uiPrefs.pinnedNavIds
        val coroutineScope = rememberCoroutineScope()

        val onEditor = destination?.hasRoute<RecipeEditorRoute>() == true
        val onRawEditor = destination?.hasRoute<RawEditorRoute>() == true
        // The view screen carries its own top bar and back affordance, and a bottom nav
        // under a recipe would invite leaving the thing you just opened.
        val onRecipeView = destination?.hasRoute<RecipeViewRoute>() == true
        val onImport = destination?.hasRoute<ImportRoute>() == true
        val onFileImport = destination?.hasRoute<FileImportRoute>() == true
        val onExport = destination?.hasRoute<ExportRoute>() == true
        val onCleanup = destination?.hasRoute<CleanupRoute>() == true
        // Settings is a subpage with its own bar and back arrow, like About.
        val onSettings = destination?.hasRoute<SettingsRoute>() == true
        val onLab = destination?.hasRoute<RawLabRoute>() == true
        // Developing a RAF takes the whole screen; the lab's own header carries the way back.
        val labState by container.rawLabWorkspace.state.collectAsStateWithLifecycle()
        var rawEditorActive by remember { mutableStateOf(false) }
        var libraryDetailOpen by remember { mutableStateOf(false) }
        val onLibrary = destination?.hasRoute<LibraryRoute>() == true
        val currentDestId = destination.toNavDestinationId()
        val isCurrentPinned = currentDestId != null && currentDestId in pinnedDestinations

        val hideSubpageChrome = (!isCurrentPinned && (onRawEditor || onImport || onFileImport || onExport || onCleanup))
        val showChrome = !onEditor && !onRecipeView && !onSettings && !hideSubpageChrome &&
            !(onLab && labState.hasRaf) && !(onRawEditor && rawEditorActive)

        // Not a route: the dialog and the sheet behind it are ways of *starting* the editor,
        // and giving either a destination of its own would put a half-made choice in the back
        // stack behind every recipe.
        var creating by remember { mutableStateOf(false) }
        // Owned here because the flag decides the shell's FAB, which the library cannot reach.
        var librarySelecting by remember { mutableStateOf(false) }

        CreateRecipeFlow(
            visible = creating,
            onDismiss = { creating = false },
            onCreate = { prefill, prefillName ->
                navController.navigate(
                    RecipeEditorRoute(id = null, prefill = prefill, prefillName = prefillName),
                )
            },
        )

        val isMoreSelected = destination?.hasRoute<MoreRoute>() == true ||
            destination?.hasRoute<AboutRoute>() == true ||
            destination?.hasRoute<DisclaimerRoute>() == true ||
            (currentDestId != null && currentDestId !in pinnedDestinations)

        val navigateToDestination: (NavDestinationId) -> Unit = { dest ->
            when (dest) {
                NavDestinationId.LIBRARY -> navController.navigate(LibraryRoute) {
                    popUpTo(LibraryRoute) { inclusive = true }
                    launchSingleTop = true
                }
                NavDestinationId.ANALYZE -> navController.navigate(PhotoRoute()) { launchSingleTop = true }
                NavDestinationId.LAB -> navController.navigate(RawLabRoute()) { launchSingleTop = true }
                NavDestinationId.PHOTOS -> navController.navigate(CameraPhotosRoute) { launchSingleTop = true }
                NavDestinationId.RAW_EDITOR -> navController.navigate(RawEditorRoute()) { launchSingleTop = true }
                NavDestinationId.CLEANUP -> navController.navigate(CleanupRoute) { launchSingleTop = true }
                NavDestinationId.IMPORT_CAMERA -> navController.navigate(ImportRoute) { launchSingleTop = true }
                NavDestinationId.IMPORT_FILE -> navController.navigate(FileImportRoute) { launchSingleTop = true }
                NavDestinationId.EXPORT -> navController.navigate(ExportRoute) { launchSingleTop = true }
            }
        }

        CompositionLocalProvider(LocalCameraSheetOpener provides { cameraSheetOpen = true }) {
            // The sheet is drawn over the shell rather than inside it: a modal covers the
            // navigation bar too.
            Box(modifier = Modifier.fillMaxSize()) {
                AppShell(
                    showChrome = showChrome,
                    hideBottomNavigation = onLibrary && libraryDetailOpen,
                    isLibrarySelected = onLibrary,
                    isReadSelected = destination?.hasRoute<PhotoRoute>() == true,
                    isLabSelected = onLab,
                    isCameraPhotosSelected = destination?.hasRoute<CameraPhotosRoute>() == true,
                    isMoreSelected = isMoreSelected,
                    onLibraryClick = { navigateToDestination(NavDestinationId.LIBRARY) },
                    onReadClick = { navigateToDestination(NavDestinationId.ANALYZE) },
                    onLabClick = { navigateToDestination(NavDestinationId.LAB) },
                    onCameraPhotosClick = { navigateToDestination(NavDestinationId.PHOTOS) },
                    onMoreClick = {
                        navController.navigate(MoreRoute) {
                            popUpTo(MoreRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    pinnedDestinations = pinnedDestinations,
                    selectedDestination = currentDestId,
                    onDestinationClick = navigateToDestination,
                    onUpdatePinnedDestinations = { updated ->
                        coroutineScope.launch {
                            container.uiPreferences.setPinnedNavIds(updated)
                        }
                    },
                    onCreateClick = { creating = true },
                    isSelecting = librarySelecting || (onLibrary && libraryDetailOpen),
                    railBottomAction = { CameraSheetButtonHost() },
                    // Compact library/Lab pages carry their own camera trigger. The shell
                    // moves this shared action into the rail on wider windows.
                    topBarAction = if (onLibrary || onLab || onRawEditor || onCleanup || onImport || onFileImport || onExport) {
                        null
                    } else {
                        { CameraSheetButtonHost() }
                    },
                ) { contentPadding ->
                    FujiNavHost(
                        navController = navController,
                        contentPadding = contentPadding,
                        onLibrarySelectionChange = { librarySelecting = it },
                        onLibraryDetailChange = { libraryDetailOpen = it },
                        onRawEditorActiveChange = { rawEditorActive = it },
                        pinnedDestinations = pinnedDestinations,
                    )
                }

                CameraSheetHost(
                    visible = cameraSheetOpen,
                    onDismiss = { cameraSheetOpen = false },
                )
            }
        }
    }
}
