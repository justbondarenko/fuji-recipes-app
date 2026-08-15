package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * The camera indicator — `PRD.md` §7.2.
 *
 * A pill in the shell, tappable, opening the camera sheet. It renders exactly what
 * [cameraChipLook] returns: the words, the icon and the tone are decided there, where they can
 * be tested.
 *
 * It lives in the shell rather than on one screen because the connection is global: the state
 * it shows survives navigation, and so should the thing showing it.
 */
@Composable
fun CameraChip(
    state: CameraState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = cameraChipLook(state)
    val label = look.modelLabel ?: stringResource(look.labelRes)
    val colors = look.tone.colors()

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .animateContentSize()
            .semantics {
                contentDescription = "Camera: $label"
            },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = look.icon.painter(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (look.spinning) Modifier.spin() else Modifier),
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }

            // The write's progress, as the one state that has something to show over time.
            look.progress?.let { fraction ->
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(colors.content.copy(alpha = 0.24f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(colors.content),
                    )
                }
            }
        }
    }
}

private data class ChipColors(val container: Color, val content: Color)

@Composable
private fun CameraChipTone.colors(): ChipColors = when (this) {
    CameraChipTone.NEUTRAL -> ChipColors(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )

    CameraChipTone.MUTED -> ChipColors(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.onSurface,
    )

    CameraChipTone.WAITING -> ChipColors(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
    )

    CameraChipTone.READY -> ChipColors(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
    )

    CameraChipTone.ALERT -> ChipColors(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )
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
 *
 * `LoadingIndicator` — the Expressive shape-morphing loader `PRD.md` §7.2 asks for — is
 * `internal` at material3 1.4.0 (`tech-stack.md` §6). This is the honest stand-in until the
 * toolchain moves; the swap is one composable.
 *
 * **Honours reduced motion** (`design-system.md` §5), and this is the first continuously
 * animating thing in the app, so it is the first place `LocalReducedMotion` earns its keep.
 * Nothing is lost when it is off: the state still has its own icon and its own words, which
 * is the rule the chip is built on.
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

@Preview(name = "Camera chip — light", showBackground = true)
@Preview(name = "Camera chip — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraChipPreview() {
    FujiTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp).width(220.dp),
        ) {
            CameraChip(CameraState.NoUsbHost, {})
            CameraChip(CameraState.Disconnected, {})
            CameraChip(CameraState.Connecting, {})
            CameraChip(CameraState.Connected(CameraModels.identify("X100VI")), {})
            CameraChip(CameraState.Writing(slot = 3, done = 7, total = 17, current = "Clarity"), {})
            CameraChip(CameraState.Error("Device busy", ptpCode = 0x2019), {})
        }
    }
}
