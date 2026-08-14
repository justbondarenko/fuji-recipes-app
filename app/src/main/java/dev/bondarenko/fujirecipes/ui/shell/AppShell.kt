package dev.bondarenko.fujirecipes.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * A **floating bar**, not an edge-to-edge `NavigationBar`: a rounded container that hovers
 * over the content with the page visible around it, plus a separate circular button for the
 * one action that is not a destination.
 *
 * Two rules from the reference design, and both are the point of it:
 *
 * - **Only the selected item shows an icon.** The others are labels. Selection is carried by
 *   one strong signal — an icon inside a filled pill — rather than by a colour difference
 *   between five identical glyphs, which is the thing that makes a conventional nav bar hard
 *   to read at a glance.
 * - **Create is a circle beside the bar, not an item in it.** It is an action, not a place;
 *   a "create" tab that never holds a selected state is a lie about what tabs mean.
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
    // The top inset travels with the bottom one. Dropping `Scaffold` for the floating bar
    // also dropped its inset handling, and a screen title behind the clock is the result.
    val statusBar = WindowInsets.statusBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Content scrolls *under* the bar; the bottom inset is what lets the last row clear
        // it rather than sitting behind it for ever.
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
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = systemBars.calculateBottomPadding() + BarMargin)
                    .padding(horizontal = BarMargin),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(barContainer())
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(
                        selected = isLibrarySelected,
                        icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                        label = stringResource(R.string.nav_library),
                        onClick = onLibraryClick,
                    )
                    NavItem(
                        selected = isMoreSelected,
                        icon = painterResource(R.drawable.ic_more_horiz),
                        label = stringResource(R.string.nav_more),
                        onClick = onMoreClick,
                    )
                }

                // The action, kept out of the bar so it never competes for "selected".
                Box(
                    modifier = Modifier
                        .size(BarHeight)
                        .clip(CircleShape)
                        .background(barContainer())
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onCreateClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.nav_create),
                        tint = barContent(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    icon: Painter,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                // The filled pill is the whole selection signal, alongside the icon.
                if (selected) barContent().copy(alpha = 0.16f) else Color.Transparent,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // No icon when unselected: the reference's rule, and it is what makes the selected
        // item findable without reading.
        if (selected) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = barContent(),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) barContent() else barContent().copy(alpha = 0.72f),
        )
    }
}

/**
 * The bar's own colour pair.
 *
 * Deliberately not `primary`: in dark mode that is near-white, and a glaring white slab
 * floating over a stone-900 page is not what the reference shows. This picks the tone that
 * *contrasts with the page* in each scheme — dark bar on a light page, lighter bar on a dark
 * one — which is the effect being copied rather than the literal colour.
 */
@Composable
private fun barContainer(): Color =
    if (MaterialTheme.colorScheme.surface.isLight()) {
        MaterialTheme.colorScheme.inverseSurface
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

@Composable
private fun barContent(): Color =
    if (MaterialTheme.colorScheme.surface.isLight()) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

private fun Color.isLight(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) > 0.5f

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
