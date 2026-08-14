package dev.bondarenko.fujirecipes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.EnumFieldDef
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.NumberField
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.RecipeValidation
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.common.errorMessageFor
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Create and edit — FEAT-003 T-10 to T-13.
 *
 * The controls are driven by the same `RecipeFields` table the view reads, so the two cannot
 * disagree about what a recipe contains — and applicability is live: changing the film
 * simulation to a monochrome one removes the colour control as you watch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    state: EditorUiState,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onSettingChange: (String, kotlinx.serialization.json.JsonElement?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    /**
     * Leaving with unsaved work asks; leaving an untouched form does not.
     *
     * The second half matters: a guard that fires when nothing was typed teaches people to
     * dismiss it without reading, which is how the one that mattered gets dismissed too.
     */
    fun attemptBack() {
        if (state.isDirty) confirmDiscard = true else onBack()
    }

    BackHandler(enabled = true) { attemptBack() }

    Column(modifier = modifier.fillMaxSize()) {
        /**
         * The **small top app bar** for an editing flow (`m3.material.io/components/app-bars`):
         * back cancels, the title says what you are doing, and the confirming action lives in
         * the bar itself.
         *
         * The subtitle is a `Column` in the `title` slot rather than the `subtitle` parameter,
         * because that overload is `internal` at material3 1.4.0 — verified by compiling
         * against it, not assumed.
         */
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(
                            if (state.isNew) R.string.editor_new else R.string.editor_edit,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    // Which recipe, so the bar answers "what am I editing" as well as "what
                    // am I doing". Omitted when there is no name yet to answer with.
                    if (state.name.isNotBlank()) {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = {
                // Back *is* cancel, and it confirms when there is work to lose.
                IconButton(onClick = ::attemptBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_cancel),
                    )
                }
            },
            actions = {
                if (!state.notFound) {
                    FilledIconButton(
                        onClick = onSave,
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(36.dp)
                            .height(48.dp),
                    ) {
                        if (state.isSaving) {
                            FujiLoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.action_save),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        when {
            state.isLoading -> Unit

            state.notFound -> LibraryPanel(
                title = stringResource(R.string.recipe_not_found_title),
                body = stringResource(R.string.recipe_not_found_body),
                primaryLabel = stringResource(R.string.action_back_to_list),
                onPrimary = onBack,
                modifier = Modifier.padding(16.dp),
            )

            else -> EditorBody(
                state, onNameChange, onNotesChange, onRatingChange,
                onTagsChange, onSettingChange, onDuplicate,
                onRequestDelete = { confirmDelete = true },
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text(stringResource(R.string.action_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            // Named, so a confirmation cannot be given for the wrong recipe.
            text = { Text(stringResource(R.string.delete_body, state.name)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun EditorBody(
    state: EditorUiState,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onSettingChange: (String, kotlinx.serialization.json.JsonElement?) -> Unit,
    onDuplicate: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val context = state.fieldContext
    val applicable = RecipeFields.applicable(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    isError = state.problemFor(RecipeValidation.NAME_FIELD) != null,
                    supportingText = state.problemFor(RecipeValidation.NAME_FIELD)
                        ?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                RatingInput(state.rating, onRatingChange)

                TagInput(
                    tags = state.tags,
                    onTagsChange = onTagsChange,
                    error = state.problemFor(RecipeValidation.TAGS_FIELD),
                )
            }
        }

        FieldGroup.entries
            .filter { group -> applicable.any { it.group == group } }
            .forEachIndexed { index, group ->
                item(key = group.id) {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(group.label, showDivider = index > 0)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        applicable.filter { it.group == group }.forEach { field ->
                            when (field) {
                                is EnumFieldDef ->
                                    if (field.id == "filmSimulation") {
                                        FilmSimulationPicker(
                                            value = state.settings.stringOrNull(field.id),
                                            onValueChange = {
                                                onSettingChange(field.id, JsonPrimitive(it))
                                            },
                                        )
                                    } else if (field.options.size <= 4) {
                                        EnumButtonGroup(
                                            field = field,
                                            value = state.settings.stringOrNull(field.id),
                                            onValueChange = {
                                                onSettingChange(field.id, JsonPrimitive(it))
                                            },
                                        )
                                    } else {
                                        EnumDropdown(
                                            field = field,
                                            value = state.settings.stringOrNull(field.id),
                                            onValueChange = {
                                                onSettingChange(field.id, JsonPrimitive(it))
                                            },
                                        )
                                    }

                                is NumberField -> NumberStepper(
                                    field = field,
                                    value = state.settings.numberOrNull(field.id),
                                    onValueChange = {
                                        onSettingChange(field.id, it?.let(::JsonPrimitive))
                                    },
                                    error = state.problemFor(field.id),
                                )
                            }
                        }
                    }
                  }
                }
            }

        item {
            // Last, not first: notes are written about a recipe once its parameters are
            // decided, and a two-line text box above the controls pushed the actual
            // parameters below the fold.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(stringResource(R.string.notes))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    minLines = 3,
                    isError = state.problemFor(RecipeValidation.NOTES_FIELD) != null,
                    supportingText = state.problemFor(RecipeValidation.NOTES_FIELD)
                        ?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.saveError != null) {
            item {
                Text(
                    // Named per ApiError, so "no signal" and "token refused" are not the
                    // same sentence (`coding-standards.md` P5).
                    text = errorMessageFor(state.saveError),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Saving lives in the app bar now. What is left down here is the pair of actions
        // that end a recipe's life — and delete is last, where it cannot be hit on the way
        // to something else.
        if (!state.isNew) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDuplicate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_duplicate))
                    }

                    TextButton(
                        onClick = onRequestDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }
}

private fun kotlinx.serialization.json.JsonObject.stringOrNull(key: String): String? =
    runCatching { (this[key] as? JsonPrimitive)?.content }.getOrNull()

private fun kotlinx.serialization.json.JsonObject.numberOrNull(key: String): Double? =
    runCatching { (this[key] as? JsonPrimitive)?.doubleOrNull }.getOrNull()

@Composable
fun RecipeEditorRouteContent(
    recipeId: String?,
    duplicateOf: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    onDuplicate: (String) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeEditorViewModel = viewModel(
        factory = RecipeEditorViewModel.factory(container, recipeId, duplicateOf),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    RecipeEditorScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onNotesChange = viewModel::onNotesChange,
        onRatingChange = viewModel::onRatingChange,
        onTagsChange = viewModel::onTagsChange,
        onSettingChange = viewModel::onSettingChange,
        onSave = { viewModel.save(onSaved) },
        onDelete = { viewModel.delete(onDeleted) },
        onDuplicate = { recipeId?.let(onDuplicate) },
        onBack = onBack,
    )
}

@Preview(name = "Editor — light", showBackground = true, heightDp = 950)
@Preview(name = "Editor — dark", showBackground = true, uiMode = 0x20, heightDp = 950)
@Composable
private fun RecipeEditorPreview() {
    FujiTheme {
        RecipeEditorScreen(
            state = EditorUiState(
                isLoading = false,
                isNew = true,
                name = "Kodachrome 64",
                rating = 4,
                tags = listOf("street"),
                settings = kotlinx.serialization.json.buildJsonObject {
                    put("filmSimulation", JsonPrimitive("classic-chrome"))
                    put("sharpness", JsonPrimitive(2))
                    put("grainEffect", JsonPrimitive("off"))
                },
            ),
            onNameChange = {}, onNotesChange = {}, onRatingChange = {}, onTagsChange = {},
            onSettingChange = { _, _ -> }, onSave = {}, onDelete = {}, onDuplicate = {},
            onBack = {},
        )
    }
}
