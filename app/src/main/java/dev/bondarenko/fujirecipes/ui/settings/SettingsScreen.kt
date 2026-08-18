package dev.bondarenko.fujirecipes.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.BuildConfig
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.settings.RecipeViewMode
import dev.bondarenko.fujirecipes.core.settings.StoredUiPreferences
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.FileExport
import dev.bondarenko.fujirecipes.ui.theme.icons.FileSave
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Info
import dev.bondarenko.fujirecipes.ui.theme.icons.KeyboardArrowRight
import dev.bondarenko.fujirecipes.ui.theme.icons.LinkedCamera

/**
 * Settings — UI configuration and backup/restore.
 */
@Composable
fun SettingsScreen(
    preferences: StoredUiPreferences,
    onSelectRecipeViewMode: (RecipeViewMode) -> Unit,
    onToggleShowPhotos: (Boolean) -> Unit,
    onToggleShowTags: (Boolean) -> Unit,
    onToggleShowFilmSimulation: (Boolean) -> Unit,
    onToggleShowRating: (Boolean) -> Unit,
    onOpenImport: () -> Unit,
    onOpenFileImport: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenAbout: () -> Unit,
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

        SectionHeader(stringResource(R.string.settings_section_recipe_view))

        RecipeViewModeSetting(
            mode = preferences.recipeViewMode,
            onSelectMode = onSelectRecipeViewMode,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        SectionHeader(stringResource(R.string.settings_section_library))

        SettingsToggleRow(
            title = stringResource(R.string.settings_photos_title),
            checked = preferences.showPhotos,
            onCheckedChange = onToggleShowPhotos,
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_tags_title),
            checked = preferences.showTags,
            onCheckedChange = onToggleShowTags,
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_film_simulation_title),
            checked = preferences.showFilmSimulation,
            onCheckedChange = onToggleShowFilmSimulation,
        )

        SettingsToggleRow(
            title = stringResource(R.string.settings_rating_title),
            checked = preferences.showRating,
            onCheckedChange = onToggleShowRating,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        SectionHeader(stringResource(R.string.settings_backup_restore))

        SettingsCard(
            title = stringResource(R.string.settings_import_camera_title),
            subtitle = stringResource(R.string.settings_import_camera_subtitle),
            icon = FujiIcons.LinkedCamera,
            onClick = onOpenImport,
            showChevron = true,
        )

        SettingsCard(
            title = stringResource(R.string.file_import_title),
            subtitle = stringResource(R.string.file_import_subtitle),
            icon = FujiIcons.FileSave,
            onClick = onOpenFileImport,
            showChevron = true,
        )

        SettingsCard(
            title = stringResource(R.string.export_title),
            subtitle = stringResource(R.string.export_subtitle),
            icon = FujiIcons.FileExport,
            onClick = onOpenExport,
            showChevron = true,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        SectionHeader(stringResource(R.string.settings_about))

        SettingsCard(
            title = stringResource(R.string.about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            icon = FujiIcons.Info,
            onClick = onOpenAbout,
            showChevron = true,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecipeViewModeSetting(
    mode: RecipeViewMode,
    onSelectMode: (RecipeViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.settings_view_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_view_mode_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = mode == RecipeViewMode.GRID,
                onCheckedChange = { onSelectMode(RecipeViewMode.GRID) },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.settings_view_mode_grid))
            }
            ToggleButton(
                checked = mode == RecipeViewMode.LIST,
                onCheckedChange = { onSelectMode(RecipeViewMode.LIST) },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.settings_view_mode_list))
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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

    val cardColors = CardDefaults.cardColors(containerColor = containerColor)
    val cardBorder = BorderStroke(1.dp, borderColor)
    val cardModifier = modifier.fillMaxWidth()
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
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
                    imageVector = FujiIcons.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = cardColors,
            border = cardBorder,
            content = cardContent,
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = cardColors,
            border = cardBorder,
            content = cardContent,
        )
    }
}

@Composable
fun SettingsRouteContent(
    onOpenImport: () -> Unit,
    onOpenFileImport: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenAbout: () -> Unit,
    contentPadding: PaddingValues,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val preferences by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        preferences = preferences,
        onSelectRecipeViewMode = viewModel::onSelectRecipeViewMode,
        onToggleShowPhotos = viewModel::onToggleShowPhotos,
        onToggleShowTags = viewModel::onToggleShowTags,
        onToggleShowFilmSimulation = viewModel::onToggleShowFilmSimulation,
        onToggleShowRating = viewModel::onToggleShowRating,
        onOpenImport = onOpenImport,
        onOpenFileImport = onOpenFileImport,
        onOpenExport = onOpenExport,
        onOpenAbout = onOpenAbout,
        contentPadding = contentPadding,
    )
}

@Preview(name = "Settings — light", showBackground = true, heightDp = 900)
@Preview(name = "Settings — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun SettingsPreview() {
    FujiTheme {
        SettingsScreen(
            preferences = StoredUiPreferences(),
            onSelectRecipeViewMode = {},
            onToggleShowPhotos = {},
            onToggleShowTags = {},
            onToggleShowFilmSimulation = {},
            onToggleShowRating = {},
            onOpenImport = {},
            onOpenFileImport = {},
            onOpenExport = {},
            onOpenAbout = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
