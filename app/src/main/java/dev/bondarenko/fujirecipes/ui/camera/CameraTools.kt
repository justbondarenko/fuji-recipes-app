package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The operations that are about the camera rather than about a recipe.
 *
 * Grouped and put below the slots because none of them is the reason anyone opened the screen:
 * a report is for someone else's benefit.
 *
 * The state is a plain value and every action is a lambda, so the card renders in a preview and
 * in a UI test with no camera and no file system (`coding-standards.md`, Compose conventions).
 */

/** Which long-running camera operation is in flight, if any. */
enum class CameraTask { REPORT }

data class CameraToolsState(
    val running: CameraTask? = null,
    /** "Reading property 31 of 68" — whatever the running task last reported. */
    val progress: String? = null,
    /** The outcome of the last finished task, success or failure. */
    val message: String? = null,
    val messageIsError: Boolean = false,
) {
    val isBusy: Boolean get() = running != null
}

@Composable
fun CameraToolsCard(
    state: CameraToolsState,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.camera_tools_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.camera_tools_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = onShareReport,
                enabled = !state.isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.camera_tools_report))
            }

            if (state.isBusy) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FujiLoadingIndicator(size = 20.dp)
                    Text(
                        text = state.progress
                            ?: stringResource(R.string.camera_tools_working),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.message?.let { message ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.messageIsError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@Preview(name = "Camera tools — idle", showBackground = true)
@Preview(name = "Camera tools — idle (Dark)", showBackground = true, uiMode = 0x20)
@Composable
private fun CameraToolsIdlePreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraToolsCard(
                state = CameraToolsState(),
                onShareReport = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Camera tools — running", showBackground = true)
@Composable
private fun CameraToolsRunningPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraToolsCard(
                state = CameraToolsState(
                    running = CameraTask.REPORT,
                    progress = "Reading property 31 of 68…",
                ),
                onShareReport = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Camera tools — failed", showBackground = true)
@Composable
private fun CameraToolsErrorPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CameraToolsCard(
                state = CameraToolsState(
                    message = "The camera stopped answering partway through the report. " +
                        "Check the cable, then try again.",
                    messageIsError = true,
                ),
                onShareReport = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
