package dev.bondarenko.fujirecipes.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothBackup.Match
import dev.bondarenko.fujirecipes.camera.bluetooth.BluetoothRecoveryStatus
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

@Composable
fun BluetoothRecoveryCard(
    status: BluetoothRecoveryStatus,
    canInspect: Boolean,
    busy: Boolean,
    message: String?,
    onInspect: () -> Unit,
    onExport: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.camera_recovery_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.camera_recovery_blocked), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(if (status.accepted) R.string.camera_recovery_accepted else R.string.camera_recovery_acceptance_unknown),
                style = MaterialTheme.typography.bodySmall)
            Text(stringResource(when {
                status.damaged -> R.string.camera_recovery_damaged
                status.original == Match.COMPLETE || status.target == Match.COMPLETE -> R.string.camera_recovery_saved_complete
                status.target == Match.RECIPES_ONLY -> R.string.camera_recovery_saved_target_recipes
                status.original == Match.RECIPES_ONLY -> R.string.camera_recovery_saved_original_recipes
                status.original == null -> R.string.camera_recovery_no_readback
                else -> R.string.camera_recovery_saved_mismatch
            }), style = MaterialTheme.typography.bodySmall)
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onInspect, enabled = canInspect && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (busy) R.string.camera_recovery_working else R.string.camera_recovery_check))
            }
            if (!canInspect) Text(stringResource(R.string.camera_recovery_connect), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onExport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.camera_recovery_export))
            }
            Text(stringResource(R.string.camera_recovery_export_help), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoveryPreview() = FujiTheme {
    BluetoothRecoveryCard(BluetoothRecoveryStatus(target = Match.RECIPES_ONLY), true, false, null, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecoveryDamagedPreview() = FujiTheme {
    BluetoothRecoveryCard(BluetoothRecoveryStatus(damaged = true), false, false, null, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecoveryNoReadbackPreview() = FujiTheme {
    BluetoothRecoveryCard(BluetoothRecoveryStatus(), false, false, null, {}, {})
}
