package dev.bondarenko.fujirecipes.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.BuildConfig
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * Settings — FEAT-004.
 *
 * Exists because the connection screen was reachable only by provoking a 403 once
 * credentials were saved. A token that has been rotated is an ordinary event; recovering
 * from it should not require breaking the app first.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenConnection: () -> Unit,
    onClearCredentials: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_more),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SectionHeader(stringResource(R.string.settings_connection))

        Card {
            SettingsRow(
                title = stringResource(R.string.connection_title),
                // The host and whether credentials exist — never the secret itself.
                subtitle = state.summary(),
                onClick = onOpenConnection,
            )

            if (state.isConfigured) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    title = stringResource(R.string.settings_clear_credentials),
                    subtitle = stringResource(R.string.settings_clear_credentials_subtitle),
                    onClick = { confirmClear = true },
                    danger = true,
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_about))

        Card {
            SettingsRow(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                onClick = null,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_title)) },
            // Says plainly that this is local. Implying it revoked the token would be worse
            // than not offering the action at all.
            text = { Text(stringResource(R.string.settings_clear_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearCredentials() }) {
                    Text(stringResource(R.string.settings_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) { content() }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (danger) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsRouteContent(onOpenConnection: () -> Unit, contentPadding: PaddingValues) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onOpenConnection = onOpenConnection,
        onClearCredentials = viewModel::clearCredentials,
        contentPadding = contentPadding,
    )
}

@Preview(name = "Settings — light", showBackground = true, heightDp = 700)
@Preview(name = "Settings — dark", showBackground = true, uiMode = 0x20, heightDp = 700)
@Composable
private fun SettingsPreview() {
    FujiTheme {
        SettingsScreen(
            state = SettingsUiState(host = "recipes.example.com", isConfigured = true),
            onOpenConnection = {},
            onClearCredentials = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(name = "Settings — unconfigured", showBackground = true, heightDp = 500)
@Composable
private fun SettingsUnconfiguredPreview() {
    FujiTheme {
        SettingsScreen(
            state = SettingsUiState(),
            onOpenConnection = {},
            onClearCredentials = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
