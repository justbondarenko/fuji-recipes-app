package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * M3's menu (`m3.material.io/components/menus`), opening above the control that owns it.
 *
 * Two things this adds to `DropdownMenuPopup`:
 *
 * - The anchor position. Every caller is a control at the bottom of the screen — a split
 *   button, a floating toolbar — where a menu dropping downwards has nowhere to go.
 *   `MenuAnchorPosition.Above` still falls back to below when the top is the tighter side.
 * - The grouping. Items arrive as one list of groups, each drawn in its own container with
 *   M3's group spacing between them, so a caller never repeats the spacing itself
 *   (`m3.material.io/components/menus/guidelines`).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiAnchoredMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    groups: List<@Composable ColumnScope.() -> Unit>,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            dropdownMenuAnchorPosition = MenuAnchorPosition.Above,
        ),
    ) {
        // The popup is only the window and the animation; a group is what draws a menu
        // container, so the items need one around them.
        groups.forEachIndexed { index, group ->
            if (index > 0) {
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            }
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                containerColor = MenuDefaults.groupVibrantContainerColor,
                content = group,
            )
        }
    }
}

/**
 * One item of a [FujiAnchoredMenu].
 *
 * Its own composable because the vibrant group container is `tertiaryContainer`, and
 * `DropdownMenuItem`'s default colours are written for a surface: left alone, the label and
 * the icon would be `onSurface` on top of it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
            leadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            trailingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        onClick = onClick,
    )
}
