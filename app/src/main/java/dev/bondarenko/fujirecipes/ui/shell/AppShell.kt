package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.readableWidth
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.Add
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.CameraRoll
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.ImageSearch
import dev.bondarenko.fujirecipes.ui.theme.icons.MoreVert
import dev.bondarenko.fujirecipes.ui.theme.icons.Science

/**
 * The chrome every top-level screen sits inside.
 *
 * Implements standard Material 3 navigation:
 * - Bottom navigation on narrow windows; a rail on windows at least 600dp wide and 480dp tall.
 * - An [ExtendedFloatingActionButton] CTA in the bottom-right corner of the content.
 *
 * Creating a recipe opens a choice dialog (pasted text vs. manual creation) via [onCreateClick].
 */
@Composable
fun AppShell(
    showChrome: Boolean,
    isLibrarySelected: Boolean,
    isReadSelected: Boolean,
    isLabSelected: Boolean = false,
    isCameraPhotosSelected: Boolean = false,
    isMoreSelected: Boolean,
    onLibraryClick: () -> Unit,
    onReadClick: () -> Unit,
    onLabClick: () -> Unit = {},
    onCameraPhotosClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    /**
     * The library is picking rows. Its own floating toolbar takes the corner, so the create
     * button gets out of the way rather than sitting under it.
     */
    isSelecting: Boolean = false,
    /**
     * The New recipe button was pressed.
     *
     * The shell reports the press and nothing more — `CreateRecipeFlow` owns the choice
     * between pasting text and starting from an empty form.
     */
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * What sits in the top-right corner of the page.
     *
     * A slot rather than a named control, so the shell keeps knowing nothing about the camera
     * — see `CameraSheetButton`. Null on pages that put the control in a row of their own,
     * which is what the library does with its search bar.
     */
    topBarAction: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Use the current window, so folding and multi-window resizing select the same layouts.
        // A short landscape phone keeps the bottom bar: five rail items need vertical room.
        val useRail = maxWidth >= 600.dp && maxHeight >= 480.dp
        val destinations = listOf(
            Triple(R.string.nav_library, FujiIcons.BookmarkStacks, isLibrarySelected),
            Triple(R.string.nav_read, FujiIcons.ImageSearch, isReadSelected),
            Triple(R.string.nav_lab, FujiIcons.Science, isLabSelected),
            Triple(R.string.nav_camera_photos, FujiIcons.CameraRoll, isCameraPhotosSelected),
            Triple(R.string.nav_more, FujiIcons.MoreVert, isMoreSelected),
        )
        val actions = listOf(onLibraryClick, onReadClick, onLabClick, onCameraPhotosClick, onMoreClick)
        Row(Modifier.fillMaxSize()) {
            if (showChrome && useRail) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    destinations.forEachIndexed { index, (label, icon, selected) ->
                        NavigationRailItem(
                            selected = selected,
                            onClick = actions[index],
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    if (showChrome && topBarAction != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            topBarAction()
                        }
                    }
                },
                bottomBar = {
                    if (showChrome && !useRail) {
                        NavigationBar {
                            destinations.forEachIndexed { index, (label, icon, selected) ->
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = actions[index],
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = showChrome && isLibrarySelected && !isSelecting,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth * 2 },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            ),
                        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                        exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth * 2 },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) + fadeOut(animationSpec = tween(durationMillis = 150)),
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = onCreateClick,
                            icon = {
                                Icon(
                                    imageVector = FujiIcons.Add,
                                    contentDescription = null,
                                )
                            },
                            text = { Text(stringResource(R.string.nav_create)) },
                        )
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().readableWidth(1200.dp)) {
                    content(innerPadding)
                }
            }
        }
    }
}

@Preview(name = "Shell — unfolded", showBackground = true, widthDp = 840, heightDp = 900)
@Preview(name = "Shell — cover", showBackground = true, widthDp = 320, heightDp = 700)
@Preview(name = "Shell — light", showBackground = true, heightDp = 400)
@Preview(name = "Shell — dark", showBackground = true, uiMode = 0x20, heightDp = 400)
@Composable
private fun AppShellPreview() {
    FujiTheme {
        AppShell(
            showChrome = true,
            isLibrarySelected = true,
            isReadSelected = false,
            isMoreSelected = false,
            onLibraryClick = {},
            onReadClick = {},
            onMoreClick = {},
            onCreateClick = {},
        ) { padding ->
            dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen(
                titleRes = R.string.placeholder_more_title,
                bodyRes = R.string.placeholder_more_body,
                contentPadding = padding,
            )
        }
    }
}
