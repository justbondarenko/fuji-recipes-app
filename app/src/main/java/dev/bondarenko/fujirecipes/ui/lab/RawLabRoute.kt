package dev.bondarenko.fujirecipes.ui.lab

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import dev.bondarenko.fujirecipes.data.model.Recipe

/**
 * The lab, wired up.
 *
 * Owns the things a stateless screen cannot: the document picker, the share sheet, the recipe
 * chooser, and the question asked before back throws away unsaved work.
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
    var showLeaveConfirm by remember { mutableStateOf(false) }

    // Back is how a development session ends: it discards the RAF and returns to the empty lab,
    // asking first only when that would lose work.
    val onBack = {
        if (state.lab.hasUnsavedChanges) showLeaveConfirm = true else viewModel.discard()
    }
    BackHandler(enabled = state.lab.hasRaf, onBack = onBack)

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
        onSettingChange = viewModel::onSettingChange,
        onRenderPreview = viewModel::retryPreview,
        onConnect = viewModel::connect,
        onSave = viewModel::requestJpegSave,
        onBack = onBack,
        onShareProfile = { filename, profile -> ShareFile.share(context, filename, profile) },
        contentPadding = contentPadding,
    )

    // The document picker opens on the ticket rather than on the tap: Save may have to render
    // a full-resolution frame first, and there is nothing to write until that lands.
    LaunchedEffect(state.saveTicket) {
        if (state.saveTicket != null) {
            val name = state.lab.rafName.substringBeforeLast('.').ifEmpty { "developed" }
            saveJpeg.launch("$name.jpg")
            viewModel.clearSaveTicket()
        }
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

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(stringResource(R.string.lab_leave_title)) },
            text = { Text(stringResource(R.string.lab_leave_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    viewModel.discard()
                }) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.action_no))
                }
            },
        )
    }
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
