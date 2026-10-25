package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Check

/**
 * M3's menu (`m3.material.io/components/menus`), opening above the control that owns it.
 *
 * Shared behavior over `DropdownMenuPopup`:
 *
 * - The anchor position favors space above split buttons and floating toolbars. Recipe-row
 *   context menus use the same positioning: `MenuAnchorPosition.Above` falls back to below
 *   when the top is the tighter side.
 * - The grouping. Items arrive as one list of groups, each drawn in its own container with
 *   M3's group spacing between them, so a caller never repeats the spacing itself
 *   (`m3.material.io/components/menus/guidelines`).
 * - Bounded vertical scrolling, including long simulation lists on compact screens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiAnchoredMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    groups: List<@Composable ColumnScope.() -> Unit>,
    modifier: Modifier = Modifier,
) {
    DropdownMenuPopup(
        expanded = expanded,
        // Scrolling clips its viewport. Keep the elevated surfaces inside it, including
        // their shadows, so the bottom corners and shadow aren't sliced off.
        modifier = modifier.verticalScroll(rememberScrollState())
            .padding(MenuDefaults.ShadowElevation * 4),
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
                shapes = MenuDefaults.groupShape(index, groups.size),
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
fun FujiMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
    destructive: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text)
                if (supportingText != null) {
                    Text(supportingText, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = MenuDefaults.itemColors(
            textColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer,
            leadingIconColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer,
            trailingIconColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        onClick = onClick,
        enabled = enabled,
    )
}

/** A checked choice with the same vibrant colors and item shapes as the crop menu. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FujiSelectionMenuItem(
    text: String,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
    icon: ImageVector? = null,
) {
    DropdownMenuItem(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        text = {
            Column {
                Text(text)
                if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingIcon = if (icon != null) ({ Icon(icon, null) }) else null,
        selectedLeadingIcon = { Icon(FujiIcons.Check, null) },
        shapes = MenuDefaults.itemShape(index, count),
        colors = MenuDefaults.selectableItemVibrantColors(),
    )
}
