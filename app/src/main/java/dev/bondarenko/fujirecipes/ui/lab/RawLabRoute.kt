package dev.bondarenko.fujirecipes.ui.lab

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.share.ShareFile
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.model.Recipe

/**
 * The lab, wired up.
 *
 * Owns the things a stateless screen cannot: the document picker, the share sheet, the recipe
 * chooser and the two dialogs that stand between a render and something permanent — naming a
 * new recipe, and confirming an overwrite of an existing one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawLabRouteContent(
    recipeId: String?,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: RawLabViewModel = viewModel(factory = RawLabViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Arriving from a recipe seeds the lab with it, once: coming back to the tab later should
    // find the session as it was left, not reset to whatever recipe opened it originally.
    LaunchedEffect(recipeId) {
        if (recipeId != null && state.lab.appliedRecipeId != recipeId) {
            viewModel.applyRecipeById(recipeId)
        }
    }

    state.message?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    var showRecipePicker by remember { mutableStateOf(false) }
    var showSaveMenu by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }

    val chooseRaf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.selectRaf(it.toString()) }
    }
    val saveJpeg = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { uri ->
        uri?.let(viewModel::saveJpeg)
    }

    RawLabScreen(
        state = state,
        onChooseRaf = { chooseRaf.launch(arrayOf("*/*")) },
        onChooseAnotherRaf = viewModel::chooseAnotherRaf,
        onApplyRecipe = { showRecipePicker = true },
        onStartFromDefaults = viewModel::startFromDefaults,
        onSettingChange = viewModel::onSettingChange,
        onRender = { quality -> viewModel.render(quality) },
        onAutoPreviewChange = viewModel::setAutoPreview,
        onConnect = viewModel::connect,
        onSave = { showSaveMenu = true },
        onDiscard = viewModel::discard,
        onShareProfile = { filename, profile -> ShareFile.share(context, filename, profile) },
        contentPadding = contentPadding,
    )

    if (showSaveMenu) {
        SaveSheet(
            state = state,
            onDismiss = { showSaveMenu = false },
            onSaveJpeg = {
                showSaveMenu = false
                val name = state.lab.rafName.substringBeforeLast('.').ifEmpty { "developed" }
                saveJpeg.launch("$name.jpg")
            },
            onSaveAsNew = {
                showSaveMenu = false
                showNameDialog = true
            },
            onUpdateRecipe = {
                showSaveMenu = false
                showUpdateConfirm = true
            },
        )
    }

    if (showRecipePicker) {
        RecipePickerSheet(
            recipes = state.recipes,
            onPick = { recipe ->
                viewModel.applyRecipe(recipe)
                showRecipePicker = false
            },
            onStartFromDefaults = {
                viewModel.startFromDefaults()
                showRecipePicker = false
            },
            onDismiss = { showRecipePicker = false },
        )
    }

    if (showNameDialog) {
        NameRecipeDialog(
            initialName = state.lab.appliedRecipeName?.let { "$it variant" }.orEmpty(),
            onConfirm = { name ->
                showNameDialog = false
                viewModel.saveAsNewRecipe(name)
            },
            onDismiss = { showNameDialog = false },
        )
    }

    if (showUpdateConfirm) {
        val name = state.lab.appliedRecipeName.orEmpty()
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            title = { Text(stringResource(R.string.lab_update_title, name)) },
            text = { Text(stringResource(R.string.lab_update_body, changedFieldSummary(state))) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateConfirm = false
                    viewModel.updateAppliedRecipe()
                }) {
                    Text(stringResource(R.string.lab_action_update_recipe))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The fields that differ from the recipe as stored — what an overwrite would actually do. */
private fun changedFieldSummary(state: RawLabUiState): String {
    val lab = state.lab
    val changed = (lab.settings.keys + lab.baseline.keys)
        .filter { lab.settings[it] != lab.baseline[it] }
        // Labels, not keys: "Highlight tone" is what the person changed; `highlightTone` is
        // what this build calls it.
        .map { RecipeFields.byId(it)?.label ?: it }
        .sorted()
    return if (changed.isEmpty()) "—" else changed.joinToString(", ")
}

/**
 * Where a render can go: a file, a new recipe, or over the recipe it started from.
 *
 * A sheet rather than a menu because the third option needs a sentence, not a word — and
 * because a preview-quality file has to say so before it is written under a JPEG's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveSheet(
    state: RawLabUiState,
    onDismiss: () -> Unit,
    onSaveJpeg: () -> Unit,
    onSaveAsNew: () -> Unit,
    onUpdateRecipe: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.lab_save_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            val preview = state.lab.preview
            if (preview != null) {
                SheetRow(
                    label = if (preview.isFullResolution) {
                        stringResource(R.string.raw_action_save_jpeg)
                    } else {
                        stringResource(R.string.lab_action_save_preview_jpeg)
                    },
                    onClick = onSaveJpeg,
                )
            }

            SheetRow(
                label = stringResource(R.string.lab_action_save_as_new),
                onClick = onSaveAsNew,
            )

            if (state.lab.canUpdateAppliedRecipe) {
                SheetRow(
                    label = stringResource(
                        R.string.lab_action_update_recipe_named,
                        state.lab.appliedRecipeName.orEmpty(),
                    ),
                    onClick = onUpdateRecipe,
                )
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun NameRecipeDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lab_save_as_new_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The library, as somewhere to start from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerSheet(
    recipes: List<Recipe>,
    onPick: (Recipe) -> Unit,
    onStartFromDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.lab_pick_recipe_title),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onStartFromDefaults) {
                Text(stringResource(R.string.lab_action_from_defaults))
            }
            if (recipes.isEmpty()) {
                Text(
                    text = stringResource(R.string.lab_pick_recipe_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(recipe) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
