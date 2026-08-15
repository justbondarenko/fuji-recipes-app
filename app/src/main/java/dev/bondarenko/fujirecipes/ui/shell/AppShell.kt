package dev.bondarenko.fujirecipes.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
     * The camera FAB indicator, anchored to the bottom-left corner of the chrome.
     */
    cameraChip: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val systemBars = WindowInsets.navigationBars.asPaddingValues()
    val statusBar = WindowInsets.statusBars.asPaddingValues()

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

            // Left FAB: USB Connection status (fixed in bottom-left corner)
            if (cameraChip != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = BarMargin,
                            bottom = systemBars.calculateBottomPadding() + BarMargin,
                        ),
                ) {
                    cameraChip()
                }
            }

            // Center: Material 3 Floating Navigation Toolbar (fixed at bottom-center)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = systemBars.calculateBottomPadding() + BarMargin)
                    .height(56.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .animateContentSize()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                    FloatingToolbarItem(
                        selected = isMoreSelected,
                        icon = rememberVectorPainter(Icons.Filled.Settings),
                        label = stringResource(R.string.nav_more),
                        contentDescription = stringResource(R.string.nav_more),
                        onClick = onMoreClick,
                    )
                }
            }

            // Right FAB: the create menu (fixed in bottom-right corner)
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
                        end = BarMargin,
                        bottom = systemBars.calculateBottomPadding() + BarMargin,
                    ),
            )
        }
    }
}

/**
 * The Material 3 FAB menu (https://m3.material.io/components/fab-menu/overview).
 *
 * Hand-built rather than `FloatingActionButtonMenu`: that component arrived in material3
 * 1.5.0-alpha, which pulls compose-foundation 1.12 and with it the AGP 9 / compileSdk 37
 * move that `gradle/libs.versions.toml` is pinned away from. Two items and a scrim are not
 * worth that migration; when the pins move, this becomes a delete.
 */
@Composable
private fun CreateFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onParseTextClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The plus turns into a close by rotating: one icon, and the motion says what happened.
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(MenuDurationMs),
        label = "fabIconRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Items are listed bottom-up, the way they read from the FAB outwards, so the last
        // one in the column is the one nearest the thumb.
        CreateFabMenuItem(
            visible = expanded,
            // The far item leads, so the pair unfolds away from the FAB rather than at once.
            delayMillis = MenuStaggerMs,
            icon = painterResource(R.drawable.ic_content_paste),
            label = stringResource(R.string.create_from_text),
            onClick = onParseTextClick,
        )
        CreateFabMenuItem(
            visible = expanded,
            delayMillis = 0,
            icon = rememberVectorPainter(Icons.Filled.Edit),
            label = stringResource(R.string.create_manually),
            onClick = onManualClick,
        )

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp,
            ),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(
                    if (expanded) R.string.create_menu_close else R.string.nav_create,
                ),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(iconRotation),
            )
        }
    }
}

/** One row of the menu: a labelled pill, sized to its own text. */
@Composable
private fun CreateFabMenuItem(
    visible: Boolean,
    delayMillis: Int,
    icon: Painter,
    label: String,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MenuDurationMs, delayMillis)) +
            scaleIn(tween(MenuDurationMs, delayMillis), initialScale = 0.8f),
        exit = fadeOut(tween(MenuDurationMs)) + scaleOut(tween(MenuDurationMs), targetScale = 0.8f),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.height(56.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Individual item inside the Material 3 Floating Toolbar.
 * When selected, expands into a pill displaying both icon and text label with secondaryContainer tonal background.
 */
@Composable
private fun FloatingToolbarItem(
    selected: Boolean,
    icon: Painter,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .defaultMinSize(minWidth = 44.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 16.dp else 11.dp)
            .animateContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
            if (selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private val BarHeight = 56.dp
private val BarMargin = 16.dp

private const val MenuDurationMs = 180
private const val MenuStaggerMs = 40

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
