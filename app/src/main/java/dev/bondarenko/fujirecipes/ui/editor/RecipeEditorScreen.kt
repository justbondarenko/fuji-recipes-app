package dev.bondarenko.fujirecipes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
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
import androidx.compose.ui.Modifier
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
                    EditorActions(
                        isSaving = state.isSaving,
                        // A new recipe has nothing to duplicate and nothing to delete, so it
                        // gets a plain button rather than a split one with an empty menu.
                        hasSecondaryActions = !state.isNew,
                        onSave = onSave,
                        onDuplicate = onDuplicate,
                        onDelete = { confirmDelete = true },
                        modifier = Modifier.padding(end = 8.dp),
                    )
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
                TextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
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
) {
    val context = state.fieldContext
    val applicable = RecipeFields.applicable(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                TagInput(
                    tags = state.tags,
                    onTagsChange = onTagsChange,
                    error = state.problemFor(RecipeValidation.TAGS_FIELD),
                )
            }
        }

        FieldGroup.entries
            .filter { group -> applicable.any { it.group == group } }
            .forEach { group ->
                item(key = group.id) {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(group.label)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    // Named per LibraryError, so "out of space" and "value out of range"
                    // are not the same sentence (`coding-standards.md` P5).
                    text = errorMessageFor(state.saveError),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

    }
}

/**
 * Save, duplicate and delete as one M3 split button.
 *
 * They were three controls in two places — save in the app bar, duplicate and delete adrift at
 * the bottom of a long scrolling form, where reaching them meant scrolling past every field.
 * A split button is the component for exactly this: one action you will almost always want,
 * and its less common relatives behind the same control.
 *
 * Delete keeps its confirmation dialog, so the menu item is a request rather than the deed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorActions(
    isSaving: Boolean,
    hasSecondaryActions: Boolean,
    onSave: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    // 💡 SAVE LABEL — icon plus text; drop the Text for an icon-only leading button.
    val saveContent: @Composable RowScope.() -> Unit = {
        if (isSaving) {
            FujiLoadingIndicator(
                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.action_save))
        }
    }

    if (!hasSecondaryActions) {
        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = modifier,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            content = saveContent,
        )
        return
    }

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = onSave,
                    enabled = !isSaving,
                    content = saveContent,
                )
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = menuOpen,
                    onCheckedChange = { menuOpen = it },
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_more),
                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                    )
                }
            },
        )

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            // 💡 SECONDARY ACTION STYLE — each item's icon and colours are set here.
            //    `MenuDefaults.itemColors` colours the label and the icon together, so a
            //    destructive item reads as destructive before it is read at all.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_duplicate)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_content_copy), contentDescription = null)
                },
                onClick = { menuOpen = false; onDuplicate() },
            )
            // Last, and in the error colour: the one item here that cannot be undone.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = { menuOpen = false; onDelete() },
            )
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
    prefill: String? = null,
    prefillName: String? = null,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    onDuplicate: (String) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeEditorViewModel = viewModel(
        factory = RecipeEditorViewModel.factory(
            container,
            recipeId,
            duplicateOf,
            prefill,
            prefillName,
        ),
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
