package dev.bondarenko.fujirecipes.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The chrome every top-level screen sits inside.
 *
 * Implements Material 3 Floating Toolbar specification (https://m3.material.io/components/toolbars/specs):
 * - A pill-shaped floating toolbar centered at the bottom housing top-level navigation items.
 * - Two fixed standalone FABs anchored to the bottom corners (USB status on the left, Create recipe on the right)
 *   so toolbar width changes during tab transitions never shift them.
 *
 * The right FAB is a Material 3 FAB menu (https://m3.material.io/components/fab-menu/overview):
 * there are two ways to start a recipe now — from pasted text, or from an empty form.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppShell(
    showChrome: Boolean,
    isLibrarySelected: Boolean,
    isReadSelected: Boolean,
    isMoreSelected: Boolean,
    onLibraryClick: () -> Unit,
    onReadClick: () -> Unit,
    onMoreClick: () -> Unit,
    onCreateClick: () -> Unit,
    onParseTextClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The camera status, rendered as the third item of the toolbar.
     *
     * A slot rather than a `CameraState` parameter, so the shell keeps knowing nothing about
     * USB — see `CameraToolbarItem`, which builds it out of [FloatingToolbarItem].
     */
    cameraItem: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val systemBars = WindowInsets.navigationBars.asPaddingValues()
    val statusBar = WindowInsets.statusBars.asPaddingValues()

    /**
     * Where the bottom edge of every floating thing lands.
     *
     * The two FABs and the toolbar each subtract whatever bottom inset their own component
     * already reserves, so all three bottoms end up on this line. Measured on device rather
     * than guessed: with one shared padding they sat at 2872 / 2848 / 2824 px — an 8dp
     * staircase, because `HorizontalFloatingToolbar` reserves 8dp under itself and
     * `FloatingActionButtonMenu` reserves 16dp under its button.
     */
    val barBaseline = systemBars.calculateBottomPadding() + BarMargin

    // Deliberately not saved across process death: an open menu is a gesture in progress,
    // and restoring one over a freshly drawn library would read as the app doing something
    // on its own.
    var menuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = menuExpanded) { menuExpanded = false }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Content scrolls under the floating toolbar
        content(
            PaddingValues(
                top = statusBar.calculateTopPadding(),
                bottom = if (showChrome) {
                    BarHeight + BarMargin * 2 + systemBars.calculateBottomPadding()
                } else {
                    systemBars.calculateBottomPadding()
                },
            ),
        )

        if (showChrome) {
            // The FAB menu's scrim. Under the chrome and over the content: what is behind it
            // must be dimmed and untappable, while the menu itself stays lit.
            AnimatedVisibility(
                visible = menuExpanded,
                enter = fadeIn(tween(MenuDurationMs)),
                exit = fadeOut(tween(MenuDurationMs)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpanded = false },
                        ),
                )
            }

            // Gradient status bar scrim to protect system status bar legibility while scrolling
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBar.calculateTopPadding() + 16.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.surface,
                            0.7f to MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )

            /**
             * The toolbar, now carrying the camera status that used to be a second FAB.
             *
             * The create FAB stays a sibling rather than going in the toolbar's own
             * `floatingActionButton` slot: `FloatingActionButtonMenu` re-applies its bottom
             * inset inside that slot too, which put it 12dp above the toolbar's edge. It is
             * a menu that grows upward, not the single FAB the slot is specified for.
             */
            HorizontalFloatingToolbar(
                expanded = true,
                expandedShadowElevation = ToolbarElevation,
                collapsedShadowElevation = ToolbarElevation,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = BarMargin, bottom = barBaseline - ToolbarOwnInset),
            ) {
                FloatingToolbarItem(
                    selected = isLibrarySelected,
                    icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                    label = stringResource(R.string.nav_library),
                    contentDescription = stringResource(R.string.nav_library),
                    onClick = onLibraryClick,
                )
                FloatingToolbarItem(
                    selected = isReadSelected,
                    icon = painterResource(R.drawable.ic_photo_camera),
                    label = stringResource(R.string.nav_read),
                    contentDescription = stringResource(R.string.nav_read),
                    onClick = onReadClick,
                )
                // Third, before settings: the camera is closer to the work than the settings
                // are. The shell still knows nothing about USB — it renders whatever the slot
                // hands it.
                cameraItem?.invoke()
                FloatingToolbarItem(
                    selected = isMoreSelected,
                    icon = rememberVectorPainter(Icons.Filled.Settings),
                    label = stringResource(R.string.nav_more),
                    contentDescription = stringResource(R.string.nav_more),
                    onClick = onMoreClick,
                )
            }

            // The one FAB, on the same baseline as the toolbar — see `barBaseline`.
            CreateFabMenu(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onParseTextClick = {
                    menuExpanded = false
                    onParseTextClick()
                },
                onManualClick = {
                    menuExpanded = false
                    onCreateClick()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = BarMargin - FabMenuOwnInset,
                        bottom = barBaseline - FabMenuOwnInset,
                    ),
            )
        }
    }
}

/**
 * The Material 3 FAB menu (https://m3.material.io/components/fab-menu/overview).
 *
 * The stagger, the item pills and the plus-to-close morph are all component behaviour —
 * `ToggleFloatingActionButton` hands its own animation progress to the icon, so the rotation
 * tracks the container morph instead of running on a parallel timer.
 *
 * The scrim stays in `AppShell`: the component dims nothing, and what is behind an open menu
 * must be untappable.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onParseTextClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    initialColor = MaterialTheme.colorScheme.primaryContainer,
                    finalColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(
                        if (expanded) R.string.create_menu_close else R.string.nav_create,
                    ),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.rotate(checkedProgress * 45f),
                )
            }
        },
    ) {
        // Declaration order is bottom-up: the last item sits nearest the FAB, which is the
        // one nearest the thumb.
        FloatingActionButtonMenuItem(
            onClick = onParseTextClick,
            icon = { Icon(painterResource(R.drawable.ic_content_paste), contentDescription = null) },
            text = { Text(stringResource(R.string.create_from_text)) },
        )
        FloatingActionButtonMenuItem(
            onClick = onManualClick,
            icon = { Icon(rememberVectorPainter(Icons.Filled.Edit), contentDescription = null) },
            text = { Text(stringResource(R.string.create_manually)) },
        )
    }
}

/**
 * Individual item inside the Material 3 Floating Toolbar.
 *
 * A thin adapter over `ToggleButton` — the shape morph on selection, the press physics and
 * the container/content colour roles all come from M3 Expressive now. The only thing left
 * here is the icon-plus-label-when-selected arrangement, which is this app's choice.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FloatingToolbarItem(
    selected: Boolean,
    icon: Painter,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    /**
     * Overrides the unselected content colour, for an item that carries state as well as a
     * destination. Selected still wins: a selected item reads as selected first.
     */
    tint: Color? = null,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
        )
        if (selected) {
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val BarHeight = 56.dp
private val BarMargin = 16.dp

/// Bottom inset `HorizontalFloatingToolbar` reserves inside its own bounds.
private val ToolbarOwnInset = 8.dp

/// Bottom inset `FloatingActionButtonMenu` reserves under its button.
private val FabMenuOwnInset = 16.dp

// The toolbar floats over scrolling content, so it needs to read as above it rather than
// printed on it. M3's default is 3.dp.
private val ToolbarElevation = 6.dp

private const val MenuDurationMs = 180

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
            onParseTextClick = {},
        ) { padding ->
            dev.bondarenko.fujirecipes.ui.common.PlaceholderScreen(
                titleRes = R.string.placeholder_more_title,
                bodyRes = R.string.placeholder_more_body,
                contentPadding = padding,
            )
        }
    }
}
