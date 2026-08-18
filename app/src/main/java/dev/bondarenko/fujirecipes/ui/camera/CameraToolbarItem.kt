package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.Cable
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Info
import dev.bondarenko.fujirecipes.ui.theme.icons.PhotoCamera
import dev.bondarenko.fujirecipes.ui.theme.icons.Refresh
import dev.bondarenko.fujirecipes.ui.theme.icons.Warning

/**
 * The camera status, as an item of the shell's navigation bar.
 *
 * The state still reads at a glance: the icon changes per state (USB, camera, warning,
 * spinning refresh) and carries the state's accent colour.
 */
@Composable
fun RowScope.CameraToolbarItem(
    state: CameraState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val look = cameraChipLook(state)
    val label = look.modelLabel ?: stringResource(look.labelRes)

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = look.icon.imageVector(),
                contentDescription = stringResource(R.string.camera_item_description, label),
            )
        },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            unselectedIconColor = look.tone.accent(),
        ),
    )
}

/**
 * The state's colour as a single accent, for use on a navigation bar.
 */
@Composable
internal fun CameraChipTone.accent(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        CameraChipTone.NEUTRAL -> if (dark) Color(0xFFFDE68A) else Color(0xFF92400E)
        CameraChipTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
        CameraChipTone.WAITING -> MaterialTheme.colorScheme.tertiary
        CameraChipTone.READY -> if (dark) Color(0xFF6ABF69) else Color(0xFF2E7D32)
        CameraChipTone.ALERT -> MaterialTheme.colorScheme.error
    }
}

internal fun CameraChipIcon.imageVector(): ImageVector = when (this) {
    CameraChipIcon.USB -> FujiIcons.Cable
    CameraChipIcon.INFO -> FujiIcons.Info
    CameraChipIcon.CONNECTING -> FujiIcons.Refresh
    CameraChipIcon.CAMERA -> FujiIcons.PhotoCamera
    CameraChipIcon.WARNING -> FujiIcons.Warning
}

@Preview(name = "Camera item — light", showBackground = true)
@Preview(name = "Camera item — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraToolbarItemPreview() {
    FujiTheme {
        NavigationBar {
            CameraToolbarItem(state = CameraState.NoUsbHost, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Disconnected, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Connecting, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Connected(CameraModels.identify("X100VI")), selected = true, onClick = {})
        }
    }
}
