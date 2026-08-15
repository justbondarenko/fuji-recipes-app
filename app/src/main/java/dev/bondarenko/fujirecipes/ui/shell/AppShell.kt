package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * - A pill-shaped floating toolbar housing top-level navigation items with clear active indicators.
 * - A standalone separate Main Action FAB positioned beside the toolbar.
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
    val systemBars = WindowInsets.navigationBars.asPaddingValues()
    val statusBar = WindowInsets.statusBars.asPaddingValues()

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

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = systemBars.calculateBottomPadding() + BarMargin)
                    .padding(horizontal = BarMargin),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Material 3 Floating Toolbar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .animateContentSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            selected = isMoreSelected,
                            icon = rememberVectorPainter(Icons.Filled.Settings),
                            label = stringResource(R.string.nav_more),
                            contentDescription = stringResource(R.string.nav_more),
                            onClick = onMoreClick,
                        )
                    }
                }

                // Main Action FAB, positioned outside the toolbar per M3 specs
                FloatingActionButton(
                    onClick = onCreateClick,
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
                        contentDescription = stringResource(R.string.nav_create),
                        modifier = Modifier.size(24.dp),
                    )
                }
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

@Preview(name = "Shell — light", showBackground = true, heightDp = 400)
@Preview(name = "Shell — dark", showBackground = true, uiMode = 0x20, heightDp = 400)
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
                titleRes = R.string.placeholder_more_title,
                bodyRes = R.string.placeholder_more_body,
                contentPadding = padding,
            )
        }
    }
}
