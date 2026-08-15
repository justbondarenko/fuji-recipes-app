package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraModels
import dev.bondarenko.fujirecipes.camera.CameraState
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.LocalReducedMotion

/**
 * The camera indicator FAB — sitting on the bottom-left of the shell chrome.
 */
@Composable
fun CameraFab(
    state: CameraState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = cameraChipLook(state)
    val label = look.modelLabel ?: stringResource(look.labelRes)
    val colors = look.tone.colors()

    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.container,
        contentColor = colors.content,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
        ),
        modifier = modifier
            .size(56.dp)
            .semantics {
                contentDescription = "Camera: $label"
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = look.icon.painter(),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .then(if (look.spinning) Modifier.spin() else Modifier),
            )

            // Circular progress indicator when writing to the camera
            look.progress?.let { fraction ->
                CircularProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    color = colors.content,
                    strokeWidth = 3.dp,
                    trackColor = colors.content.copy(alpha = 0.24f),
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

private data class ChipColors(val container: Color, val content: Color)

@Composable
private fun CameraChipTone.colors(): ChipColors {
    val dark = isSystemInDarkTheme()
    return when (this) {
        CameraChipTone.NEUTRAL -> ChipColors(
            container = if (dark) Color(0xFF451A03) else Color(0xFFFEF3C7),
            content = if (dark) Color(0xFFFDE68A) else Color(0xFF92400E),
        )

        CameraChipTone.MUTED -> ChipColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CameraChipTone.WAITING -> ChipColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )

        CameraChipTone.READY -> ChipColors(
            container = if (dark) Color(0xFF1B5E20) else Color(0xFF2E7D32),
            content = Color(0xFFFFFFFF),
        )

        CameraChipTone.ALERT -> ChipColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun CameraChipIcon.painter(): Painter = when (this) {
    CameraChipIcon.USB -> painterResource(R.drawable.ic_usb)
    CameraChipIcon.INFO -> rememberVectorPainter(Icons.Filled.Info)
    CameraChipIcon.CONNECTING -> rememberVectorPainter(Icons.Filled.Refresh)
    CameraChipIcon.CAMERA -> painterResource(R.drawable.ic_photo_camera)
    CameraChipIcon.WARNING -> rememberVectorPainter(Icons.Filled.Warning)
}

/**
 * A plain rotation for the waiting states.
 */
@Composable
private fun Modifier.spin(): Modifier {
    if (LocalReducedMotion.current) return this

    val transition = rememberInfiniteTransition(label = "camera-chip-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "camera-chip-angle",
    )
    return rotate(angle)
}

@Preview(name = "Camera FAB — light", showBackground = true)
@Preview(name = "Camera FAB — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraFabPreview() {
    FujiTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            CameraFab(CameraState.NoUsbHost, {})
            CameraFab(CameraState.Disconnected, {})
            CameraFab(CameraState.Connecting, {})
            CameraFab(CameraState.Connected(CameraModels.identify("X100VI")), {})
            CameraFab(CameraState.Writing(slot = 3, done = 7, total = 17, current = "Clarity"), {})
            CameraFab(CameraState.Error("Device busy", ptpCode = 0x2019), {})
        }
    }
}
