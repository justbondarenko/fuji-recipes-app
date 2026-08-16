package dev.bondarenko.fujirecipes.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onOpenImport: () -> Unit,
    onOpenFileImport: () -> Unit,
    onOpenExport: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_more),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SectionHeader(stringResource(R.string.settings_connection))

        SettingsCard(
            title = stringResource(R.string.connection_title),
            subtitle = state.summary(),
            icon = painterResource(R.drawable.ic_cloud_sync),
            onClick = onOpenConnection,
            showChevron = true,
        )

        SectionHeader(stringResource(R.string.settings_backup_restore))

        SettingsCard(
            title = stringResource(R.string.settings_import_camera_title),
            subtitle = stringResource(R.string.settings_import_camera_subtitle),
            icon = painterResource(R.drawable.ic_linked_camera),
            onClick = onOpenImport,
            showChevron = true,
        )

        SettingsCard(
            title = stringResource(R.string.file_import_title),
            subtitle = stringResource(R.string.file_import_subtitle),
            icon = painterResource(R.drawable.ic_file_save),
            onClick = onOpenFileImport,
            showChevron = true,
        )

        SettingsCard(
            title = stringResource(R.string.export_title),
            subtitle = stringResource(R.string.export_subtitle),
            icon = painterResource(R.drawable.ic_file_save),
            onClick = onOpenExport,
            showChevron = true,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: (() -> Unit)?,
    showChevron: Boolean = false,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val containerColor = if (danger) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = if (danger) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (danger) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (danger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (danger) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun SettingsRouteContent(
    onOpenConnection: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenFileImport: () -> Unit,
    onOpenExport: () -> Unit,
    contentPadding: PaddingValues,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onOpenConnection = onOpenConnection,
        onOpenImport = onOpenImport,
        onOpenFileImport = onOpenFileImport,
        onOpenExport = onOpenExport,
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
            onOpenImport = {},
            onOpenFileImport = {},
            onOpenExport = {},
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
            onOpenImport = {},
            onOpenFileImport = {},
            onOpenExport = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

