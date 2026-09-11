package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
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
 * How a screen opens the camera side sheet.
 *
 * The button sits on every top-level screen, and the sheet's open state belongs to the app
 * shell above all of them, so the opener is provided rather than threaded through four layers
 * of screen parameters. The default does nothing, which is what a preview wants.
 */
val LocalCameraSheetOpener = compositionLocalOf<() -> Unit> { {} }

/**
 * The camera status, as the icon button that opens the camera side sheet.
 *
 * The state reads the same way it did in the navigation bar: the icon changes per state (USB,
 * camera, warning, refresh) and an M3 badge (`m3.material.io/components/badges`) carries the
 * colour, so colour is never the only signal. The resting state — a camera simply not plugged
 * in — carries no badge: a dot that is always there says nothing.
 */
@Composable
fun CameraSheetButton(
    state: CameraState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = cameraChipLook(state)
    val label = look.modelLabel ?: stringResource(look.labelRes)

    // The same round tonal button the settings cog is: the two sit side by side in the
    // library's search row, and one plain and one tonal reads as two kinds of control.
    FilledTonalIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
        modifier = modifier,
    ) {
        BadgedBox(
            badge = {
                if (look.tone != CameraChipTone.MUTED) {
                    Badge(containerColor = look.tone.accent())
                }
            },
        ) {
            Icon(
                imageVector = look.icon.imageVector(),
                contentDescription = stringResource(R.string.camera_item_description, label),
            )
        }
    }
}

/**
 * The state's colour as a single accent.
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

@Preview(name = "Camera button — light", showBackground = true)
@Preview(name = "Camera button — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraSheetButtonPreview() {
    FujiTheme {
        androidx.compose.foundation.layout.Row {
            CameraSheetButton(state = CameraState.NoUsbHost, onClick = {})
            CameraSheetButton(state = CameraState.Disconnected, onClick = {})
            CameraSheetButton(state = CameraState.Connecting, onClick = {})
            CameraSheetButton(
                state = CameraState.Connected(CameraModels.identify("X100VI")),
                onClick = {},
            )
        }
    }
}
