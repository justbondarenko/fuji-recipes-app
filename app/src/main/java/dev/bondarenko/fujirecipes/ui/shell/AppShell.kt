package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The chrome every top-level screen sits inside — `PRD.md` §6.1 and §6.3.
 *
 * Three targets, and the middle one is not a destination: it is a docked FAB that starts a
 * new recipe. Keeping it out of the `NavigationBar` is deliberate — a selected-state
 * "create" tab is a lie, since you never come to rest there.
 *
 * Takes state and lambdas, never a ViewModel, so it previews and tests without a graph
 * (`coding-standards.md`, Compose conventions).
 */
@Composable
fun AppShell(
    showChrome: Boolean,
    isLibrarySelected: Boolean,
    isMoreSelected: Boolean,
    onLibraryClick: () -> Unit,
    onMoreClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        // Centre, not the Scaffold's default bottom-end: `PRD.md` §6.1 puts create between
        // the two nav targets, and centring is what makes it read as the third thumb
        // target rather than as an action floating over the content.
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            if (showChrome) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    NavigationBarItem(
                        selected = isLibrarySelected,
                        onClick = onLibraryClick,
                        icon = {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        },
                        label = { Text(stringResource(R.string.nav_library)) },
                    )
                    NavigationBarItem(
                        selected = isMoreSelected,
                        onClick = onMoreClick,
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_horiz),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_more)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (showChrome) {
                FloatingActionButton(
                    onClick = onCreateClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.medium,
                    elevation = FloatingActionButtonDefaults.elevation(
                        // Separation in this palette comes from spacing and outline, not
                        // shadow — design-system.md §5. A floating shadow on a warm stone
                        // ground reads as a different app.
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.nav_create),
                    )
                }
            }
        },
        content = content,
    )
}

@Preview(name = "Shell — light", showBackground = true)
@Preview(name = "Shell — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun AppShellPreview() {
    FujiTheme {
        AppShell(
            showChrome = true,
            isLibrarySelected = true,
            isMoreSelected = false,
            onLibraryClick = {},
            onMoreClick = {},
            onCreateClick = {},
        ) { padding ->
            dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen(
                titleRes = R.string.placeholder_library_title,
                bodyRes = R.string.placeholder_library_body,
                contentPadding = padding,
            )
        }
    }
}
