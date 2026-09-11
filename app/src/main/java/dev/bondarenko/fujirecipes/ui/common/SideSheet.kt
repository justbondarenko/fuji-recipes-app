package dev.bondarenko.fujirecipes.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A modal side sheet (`m3.material.io/components/side-sheets`).
 *
 * Material 3 for Compose ships no side sheet, so this is the spec's own anatomy: a scrim over
 * the page, and a container pinned to the end edge that slides in from it and takes the full
 * height. Modal, so it is dismissed by the scrim, by back, and by whatever the content offers.
 *
 * Drawn inside the app's own layout rather than in a `Dialog` window: a dialog cannot animate
 * itself out, and a sheet that appears with a slide and vanishes with a blink reads as a bug.
 * Being in the layout is also what lets it cover the navigation bar, which a modal should.
 */
@Composable
fun FujiModalSideSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismissRequest)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    // No ripple and no role: the scrim is a way out, not a control.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally { width -> width },
            exit = slideOutHorizontally { width -> width },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier
                    // The status bar and the gesture bar are not the sheet's to draw under:
                    // the scrim covers them, the sheet stops at them.
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxHeight()
                    // Never the whole screen: a modal sheet has to leave enough scrim showing
                    // that the page behind it is still visibly there.
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 400.dp),
            ) {
                content()
            }
        }
    }
}
