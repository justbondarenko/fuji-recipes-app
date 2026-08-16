package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.ui.shell.FloatingToolbarItem
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The camera status, as the third item of the shell's floating toolbar.
 *
 * It was a second FAB in the bottom-left corner until the toolbar gained a FAB slot of its
 * own — M3 specifies one toolbar and one attached FAB, and two FABs flanking a toolbar is not
 * a pattern the spec has.
 *
 * The state still reads at a glance: the icon changes per state (USB, camera, warning,
 * spinning refresh) and carries the state's accent colour. What a corner FAB could show and a
 * 24dp toolbar icon cannot is the determinate write ring; the write sheet owns that progress
 * and is on screen for the whole write anyway.
 */
@Composable
fun CameraToolbarItem(
    state: CameraState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val look = cameraChipLook(state)
    val label = look.modelLabel ?: stringResource(look.labelRes)

    FloatingToolbarItem(
        selected = selected,
        icon = look.icon.painter(),
        contentDescription = stringResource(R.string.camera_item_description, label),
        onClick = onClick,
        tint = look.tone.accent(),
    )
}

/**
 * The state's colour as a single accent, for use on a transparent toolbar.
 *
 * The container/content pair below was built for a filled FAB. An icon on the toolbar has no
 * container of its own, so each tone collapses to the one colour that identified it —
 * `MUTED` deliberately to the ordinary toolbar role, because "no camera" should not shout.
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

@Composable
internal fun CameraChipIcon.painter(): Painter = when (this) {
    CameraChipIcon.USB -> painterResource(R.drawable.ic_usb)
    CameraChipIcon.INFO -> rememberVectorPainter(Icons.Filled.Info)
    CameraChipIcon.CONNECTING -> rememberVectorPainter(Icons.Filled.Refresh)
    CameraChipIcon.CAMERA -> painterResource(R.drawable.ic_photo_camera)
    CameraChipIcon.WARNING -> rememberVectorPainter(Icons.Filled.Warning)
}

@Preview(name = "Camera item — light", showBackground = true)
@Preview(name = "Camera item — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraToolbarItemPreview() {
    FujiTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            CameraToolbarItem(state = CameraState.NoUsbHost, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Disconnected, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Connecting, selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Connected(CameraModels.identify("X100VI")), selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Writing(slot = 3, done = 7, total = 17, current = "Clarity"), selected = false, onClick = {})
            CameraToolbarItem(state = CameraState.Error("Device busy", ptpCode = 0x2019), selected = false, onClick = {})
        }
    }
}
