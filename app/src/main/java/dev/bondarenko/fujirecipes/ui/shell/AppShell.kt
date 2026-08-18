package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.Add
import dev.bondarenko.fujirecipes.ui.theme.icons.BookmarkStacks
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.ImageSearch
import dev.bondarenko.fujirecipes.ui.theme.icons.Settings

/**
 * The chrome every top-level screen sits inside.
 *
 * Implements standard Material 3 navigation:
 * - A docked [NavigationBar] at the bottom of the screen with labeled destinations.
 * - An [ExtendedFloatingActionButton] CTA positioned directly above the docked bar on the right.
 *
 * Creating a recipe opens a choice dialog (pasted text vs. manual creation) via [onCreateClick].
 */
@Composable
fun AppShell(
    showChrome: Boolean,
    isLibrarySelected: Boolean,
    isReadSelected: Boolean,
    isMoreSelected: Boolean,
    onLibraryClick: () -> Unit,
    onReadClick: () -> Unit,
    onMoreClick: () -> Unit,
    /**
     * The New recipe button was pressed.
     *
     * The shell reports the press and nothing more — `CreateRecipeFlow` owns the choice
     * between pasting text and starting from an empty form.
     */
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The camera status, rendered as the third item of the navigation bar.
     *
     * A slot rather than a `CameraState` parameter, so the shell keeps knowing nothing about
     * USB — see `CameraToolbarItem`.
     */
    cameraItem: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    NavigationBarItem(
                        selected = isLibrarySelected,
                        onClick = onLibraryClick,
                        icon = {
                            Icon(
                                imageVector = FujiIcons.BookmarkStacks,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_library)) },
                    )
                    NavigationBarItem(
                        selected = isReadSelected,
                        onClick = onReadClick,
                        icon = {
                            Icon(
                                imageVector = FujiIcons.ImageSearch,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_read)) },
                    )
                    cameraItem?.invoke(this)
                    NavigationBarItem(
                        selected = isMoreSelected,
                        onClick = onMoreClick,
                        icon = {
                            Icon(
                                imageVector = FujiIcons.Settings,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_more)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (showChrome && isLibrarySelected) {
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
        content(innerPadding)
    }
}

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
