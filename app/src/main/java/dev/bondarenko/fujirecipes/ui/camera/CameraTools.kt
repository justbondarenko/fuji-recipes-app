package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.plan.BackupMatch
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The three operations that are about the camera rather than about a recipe.
 *
 * Grouped and put below the slots because none of them is the reason anyone opened the screen:
 * a report is for someone else's benefit, and a backup is for a day that has not happened yet.
 *
 * The state is a plain value and every action is a lambda, so the card renders in a preview and
 * in a UI test with no camera and no file system (`coding-standards.md`, Compose conventions).
 */

/** Which long-running camera operation is in flight, if any. */
enum class CameraTask { REPORT, BACK_UP, RESTORE }

data class CameraToolsState(
    val running: CameraTask? = null,
    /** "Reading property 31 of 68" — whatever the running task last reported. */
    val progress: String? = null,
    /** The outcome of the last finished task, success or failure. */
    val message: String? = null,
    val messageIsError: Boolean = false,
    /** Set once a file has been chosen and checked, and the user must decide. */
    val pendingRestore: PendingRestore? = null,
) {
    val isBusy: Boolean get() = running != null
}

/**
 * A chosen backup file, checked far enough to describe but not yet sent.
 *
 * Everything here goes into the confirmation, because a restore replaces every setting the body
 * holds and the user is entitled to see what this app actually knows before saying yes — which
 * is: how big the file is, and what its *name* claims about where it came from.
 */
data class PendingRestore(
    val filename: String,
    val sizeBytes: Int,
    val match: BackupMatch,
    /** The name the file claims, when it claims one. */
    val claimedModel: String?,
    val connectedModel: String,
)

@Composable
fun CameraToolsCard(
    state: CameraToolsState,
    onShareReport: () -> Unit,
    onBackUp: () -> Unit,
    onChooseRestoreFile: () -> Unit,
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShareReport,
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.camera_tools_report))
                }

                OutlinedButton(
                    onClick = onBackUp,
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.camera_tools_back_up))
                }
            }

            TextButton(
                onClick = onChooseRestoreFile,
                enabled = !state.isBusy,
            ) {
                Text(stringResource(R.string.camera_tools_restore))
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

/**
 * The last thing between a chosen file and every setting on the camera.
 *
 * Not dismissible by tapping outside. Everything else in this app is recoverable by tapping
 * again; this is not, and a dialog that a stray touch can confirm is the wrong shape for it.
 * The confirm button says what will happen rather than "OK".
 */
@Composable
fun RestoreConfirmDialog(
    pending: PendingRestore,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.camera_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(
                        R.string.camera_restore_confirm_body,
                        pending.connectedModel,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    text = stringResource(
                        R.string.camera_restore_confirm_file,
                        pending.filename,
                        pending.sizeBytes,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when (pending.match) {
                    BackupMatch.SAME_MODEL -> Unit

                    BackupMatch.DIFFERENT_MODEL -> Warning(
                        stringResource(
                            R.string.camera_restore_warn_different,
                            pending.claimedModel.orEmpty(),
                            pending.connectedModel,
                        ),
                    )

                    BackupMatch.UNKNOWN_MODEL -> Warning(
                        stringResource(R.string.camera_restore_warn_unknown),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.camera_restore_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.camera_restore_cancel))
            }
        },
    )
}

@Composable
private fun Warning(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(10.dp),
        )
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
                onBackUp = {},
                onChooseRestoreFile = {},
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
                onBackUp = {},
                onChooseRestoreFile = {},
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
                    message = "The camera has no settings backup to give. Check that its USB " +
                        "mode is USB RAW CONV./BACKUP RESTORE, then try again.",
                    messageIsError = true,
                ),
                onShareReport = {},
                onBackUp = {},
                onChooseRestoreFile = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The warning path is the one worth looking at, so the preview shows a mismatched body. */
@Preview(name = "Restore confirmation — different body", showBackground = true)
@Composable
private fun RestoreConfirmPreview() {
    FujiTheme {
        RestoreConfirmDialog(
            pending = PendingRestore(
                filename = "fuji-backup-X-T5-20260908-2117.bin",
                sizeBytes = 38_912,
                match = BackupMatch.DIFFERENT_MODEL,
                claimedModel = "X-T5",
                connectedModel = "X-T50",
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
