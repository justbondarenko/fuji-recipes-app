package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Settings

/**
 * The way into settings, wherever a page has room for it.
 *
 * A round tonal M3 icon button (`m3.material.io/components/icon-buttons`). One composable so
 * every page's button is the same button: settings left the navigation bar, and a control
 * that changes shape from page to page would not read as the same door.
 */
@Composable
fun FujiSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
        modifier = modifier,
    ) {
        Icon(
            imageVector = FujiIcons.Settings,
            contentDescription = stringResource(R.string.nav_settings),
        )
    }
}
