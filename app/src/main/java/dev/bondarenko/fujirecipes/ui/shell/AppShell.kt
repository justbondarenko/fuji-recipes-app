package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Implements the Material 3 floating toolbar
 * (https://m3.material.io/components/toolbars/guidelines): a standard toolbar of icon-only
 * items owning a vibrant FAB through its own slot, and the pair centred as a unit. The colour
 * carries the emphasis — the bar recedes, the one action on it does not.
 *
 * Creating a recipe asks which way first — from pasted text, or from an empty form — in a
 * dialog rather than a FAB menu.
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
     * Where the bar's bottom edge lands.
     *
     * `HorizontalFloatingToolbar` reserves 8dp under itself, so the padding makes that up to
     * leave a true [BarMargin] of air. One constant now that the toolbar owns the FAB —
     * there is no second element with an inset of its own to reconcile.
     */
    val barBaseline = systemBars.calculateBottomPadding() + BarMargin - ToolbarOwnInset

    // Deliberately not saved across process death: a half-made choice is a gesture in
    // progress, and restoring the dialog over a freshly drawn library would read as the app
    // doing something on its own.
    var createOpen by remember { mutableStateOf(false) }

    if (createOpen) {
        CreateRecipeDialog(
            onDismiss = { createOpen = false },
            onParseTextClick = {
                createOpen = false
                onParseTextClick()
            },
            onManualClick = {
                createOpen = false
                onCreateClick()
            },
        )
    }

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
             * The bar: one toolbar that owns its FAB, centred as a unit.
             *
             * `floatingActionButton` is the slot M3 specifies for exactly this, so the gap
             * between toolbar and FAB, and their shared baseline, are the component's business
             * rather than a pair of hand-measured inset constants. The toolbar reads slightly
             * left of centre because what is centred is the whole bar.
             *
             * Standard container, vibrant FAB: the bar is mostly wayfinding and should recede,
             * so the emphasis goes to the one action on it.
             */
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                expandedShadowElevation = ToolbarElevation,
                collapsedShadowElevation = ToolbarElevation,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { createOpen = true },
                        shape = FabShape,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.nav_create),
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = barBaseline),
            ) {
                FloatingToolbarItem(
                    selected = isLibrarySelected,
                    icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                    contentDescription = stringResource(R.string.nav_library),
                    onClick = onLibraryClick,
                )
                FloatingToolbarItem(
                    selected = isReadSelected,
                    icon = painterResource(R.drawable.ic_photo_camera),
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
                    contentDescription = stringResource(R.string.nav_more),
                    onClick = onMoreClick,
                )
            }
        }
    }
}

/**
 * Two ways to start a recipe, as a dialog.
 *
 * This was a `FloatingActionButtonMenu` behind a `ToggleFloatingActionButton`, which meant the
 * bar carried a component that expands upward, brings its own scrim and reserves its own
 * insets — for a choice between two things. A plain FAB and a dialog say the same thing, and
 * let the toolbar own its FAB slot the way M3 specifies.
 */
@Composable
private fun CreateRecipeDialog(
    onDismiss: () -> Unit,
    onParseTextClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nav_create)) },
        text = {
            Column {
                ListItem(
                    onClick = onParseTextClick,
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_content_paste), contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(stringResource(R.string.create_from_text))
                }
                ListItem(
                    onClick = onManualClick,
                    leadingContent = {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(stringResource(R.string.create_manually))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Individual item inside the Material 3 Floating Toolbar.
 *
 * Icon only: `m3.material.io/components/toolbars/guidelines` shows toolbars as rows of icons,
 * and the label that used to appear on the selected item widened the bar every time you
 * changed tab. The name survives as the content description, so it still reaches TalkBack.
 *
 * A thin adapter over `IconToggleButton` rather than `ToggleButton`: with the label gone this
 * is an icon-only control, and `ToggleButton` is a text button carrying text-button metrics.
 * The shape morph on selection, the press physics and the colour roles come from M3.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FloatingToolbarItem(
    selected: Boolean,
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    /**
     * Overrides the unselected content colour, for an item that carries state as well as a
     * destination. Selected still wins: a selected item reads as selected first.
     */
    tint: Color? = null,
) {
    IconToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = tint ?: LocalContentColor.current,
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(painter = icon, contentDescription = contentDescription)
    }
}

private val BarHeight = 56.dp
private val BarMargin = 16.dp

/// The FAB is a squircle, not a circle: M3 shapes the toolbar's FAB as a rounded square.
private val FabShape = RoundedCornerShape(16.dp)

/// Bottom inset `HorizontalFloatingToolbar` reserves inside its own bounds.
private val ToolbarOwnInset = 8.dp

// The toolbar floats over scrolling content, so it needs to read as above it rather than
// printed on it. M3's default is 3.dp.
private val ToolbarElevation = 6.dp

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
