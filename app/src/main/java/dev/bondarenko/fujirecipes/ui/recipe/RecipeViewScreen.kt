package dev.bondarenko.fujirecipes.ui.recipe

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.canWrite
import dev.bondarenko.fujirecipes.core.share.ShareFile
import dev.bondarenko.fujirecipes.ui.camera.WriteSheetHost
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.common.errorMessageFor
import dev.bondarenko.fujirecipes.ui.editor.RatingInput
import dev.bondarenko.fujirecipes.ui.editor.TagInput
import dev.bondarenko.fujirecipes.ui.library.FilmSimBadge
import dev.bondarenko.fujirecipes.ui.library.LibraryPanel
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures

/**
 * Recipe view presented inside a Material 3 Modal Bottom Sheet.
 *
 * It opens taking up to 80% screen space (partially expanded) and seamlessly expands
 * to full screen as the user scrolls through the Bento Grid parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewBottomSheet(
    recipeId: String,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeViewModel =
        viewModel(factory = RecipeViewModel.factory(container, recipeId), key = recipeId)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        val camera by container.cameraController.state.collectAsStateWithLifecycle()
        var writeOpen by remember { mutableStateOf(false) }

        RecipeViewContent(
            state = state,
            onClose = onDismiss,
            onEdit = { onEdit(recipeId) },
            onChangedOnlyChange = viewModel::onChangedOnlyChange,
            onRatingChange = viewModel::onRatingChange,
            onTagsChange = viewModel::onTagsChange,
            onWriteToCamera = { writeOpen = true },
            canWriteToCamera = camera.canWrite,
            onExportRecipe = {
                viewModel.buildExport { filename, content ->
                    ShareFile.share(context, filename, content)
                }
            },
        )

        if (writeOpen) {
            WriteSheetHost(recipeId = recipeId, onDismiss = { writeOpen = false })
        }
    }
}

/**
 * Full-screen Recipe View Screen (used for deep linking or standalone view routes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeViewScreen(
    state: RecipeViewUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit = {},
    onTagsChange: (List<String>) -> Unit = {},
    onWriteToCamera: () -> Unit = {},
    canWriteToCamera: Boolean = false,
    onExportRecipe: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.action_back_to_list),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        RecipeViewContent(
            state = state,
            onClose = onBack,
            onEdit = onEdit,
            onChangedOnlyChange = onChangedOnlyChange,
            onRatingChange = onRatingChange,
            onTagsChange = onTagsChange,
            onWriteToCamera = onWriteToCamera,
            canWriteToCamera = canWriteToCamera,
            onExportRecipe = onExportRecipe,
        )
    }
}

/**
 * Shared Recipe View Content containing the Hero Card, Bento Grid parameters,
 * and the consolidated floating action toolbar + Write to Camera FAB.
 */
@Composable
fun RecipeViewContent(
    state: RecipeViewUiState,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit = {},
    onTagsChange: (List<String>) -> Unit = {},
    onWriteToCamera: () -> Unit = {},
    canWriteToCamera: Boolean = false,
    onExportRecipe: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center,
            ) {
                FujiLoadingIndicator()
            }
        }

        state.isNotFound -> {
            LibraryPanel(
                title = stringResource(R.string.recipe_not_found_title),
                body = stringResource(R.string.recipe_not_found_body),
                primaryLabel = stringResource(R.string.action_back_to_list),
                onPrimary = onClose,
                modifier = modifier.padding(16.dp),
            )
        }

        else -> {
            val recipe = state.recipe
            Box(modifier = modifier.fillMaxSize()) {
                RecipeBentoBody(
                    state = state,
                    onChangedOnlyChange = onChangedOnlyChange,
                    onRatingChange = onRatingChange,
                    onTagsChange = onTagsChange,
                    modifier = Modifier.fillMaxSize(),
                )

                if (recipe != null) {
                    RecipeFloatingToolbar(
                        recipe = recipe,
                        groups = state.groups,
                        onEdit = onEdit,
                        onExportRecipe = onExportRecipe,
                        onWriteToCamera = onWriteToCamera,
                        canWriteToCamera = canWriteToCamera,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/**
 * Consolidated floating actions: Floating Toolbar (Edit, Copy as text, Export)
 * and adjacent FAB (Write to camera).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecipeFloatingToolbar(
    recipe: RecipeHeader,
    groups: List<SettingsGroup>,
    onEdit: () -> Unit,
    onExportRecipe: () -> Unit,
    onWriteToCamera: () -> Unit,
    canWriteToCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.recipe_copied)
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalFloatingToolbar(expanded = true) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                )
            }

            IconButton(
                onClick = {
                    val text = RecipeTextFormatter.format(recipe, groups)
                    clipboardManager.setText(AnnotatedString(text))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_content_copy),
                    contentDescription = stringResource(R.string.action_copy_recipe),
                )
            }

            IconButton(onClick = onExportRecipe) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_sync),
                    contentDescription = stringResource(R.string.action_export_recipe),
                )
            }
        }

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(R.string.camera_not_connected_tooltip))
                }
            },
            state = tooltipState,
        ) {
            FloatingActionButton(
                onClick = {
                    if (canWriteToCamera) {
                        onWriteToCamera()
                    } else {
                        coroutineScope.launch {
                            tooltipState.show()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (canWriteToCamera) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (canWriteToCamera) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp,
                ),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.action_write_to_camera),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Bento Grid body for the recipe view.
 */
@Composable
private fun RecipeBentoBody(
    state: RecipeViewUiState,
    onChangedOnlyChange: (Boolean) -> Unit,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Header Card
        item {
            RecipeHeaderBlock(
                recipe = recipe,
                onRatingChange = onRatingChange,
                onTagsChange = onTagsChange,
            )
        }

        // Changed settings toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.changed_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(checked = state.changedOnly, onCheckedChange = onChangedOnlyChange)
            }
        }

        if (state.saveError != null) {
            item {
                Text(
                    text = errorMessageFor(state.saveError),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        if (state.nothingChanged) {
            item {
                Text(
                    text = stringResource(R.string.nothing_changed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        }

        // Bento Grid Sections
        state.groups.forEach { group ->
            item(key = group.group.id) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(group.group.label)
                    BentoGroupGrid(group)
                }
            }
        }

        if (recipe.notes.isNotBlank()) {
            item {
                BentoNotesCard(recipe.notes)
            }
        }
    }
}

/**
 * Header card containing recipe thumbnail, name, simulation, rating, and tags
 * with a blurred film simulation frosted glass background.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeHeaderBlock(
    recipe: RecipeHeader,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
) {
    val sim = FilmSimulations.byId(recipe.filmSimulationId)
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Blurred film simulation background image
            if (sim?.image != null) {
                Image(
                    painter = painterResource(sim.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(24.dp)
                        .graphicsLayer(scaleX = 1.15f, scaleY = 1.15f),
                )
            } else if (recipe.filmSimulationId != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(FilmSimulations.swatchFor(recipe.filmSimulationId).copy(alpha = 0.35f)),
                )
            }

            // Frosted glass translucent scrim overlay to ensure crisp contrast and readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilmSimBadge(
                        simulationId = recipe.filmSimulationId,
                        size = 56.dp,
                        shape = CircleShape,
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = recipe.filmSimulationLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                RatingInput(rating = recipe.rating, onRatingChange = onRatingChange)

                TagInput(tags = recipe.tags, onTagsChange = onTagsChange)
            }
        }
    }
}

/**
 * 2-column Bento Grid for section parameters.
 */
@Composable
private fun BentoGroupGrid(group: SettingsGroup) {
    val rows = group.rows
    val pairs = rows.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pairs.forEach { pair ->
            if (pair.size == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BentoParameterTile(
                        row = pair[0],
                        modifier = Modifier.weight(1f),
                    )
                    BentoParameterTile(
                        row = pair[1],
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                BentoParameterTile(
                    row = pair[0],
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Individual Bento Grid parameter card.
 */
@Composable
private fun BentoParameterTile(
    row: FieldFormatting.Row,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isDefault) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Null for the fields with no glyph of their own — ISO bounds and the two
                // monochromatic shifts — which then read as label-only tiles rather than
                // borrowing a neighbour's icon.
                fieldIcon(row.fieldId)?.let { icon ->
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = row.value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TabularFigures,
                ),
                color = if (row.isDefault) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Full-width Bento Card for recipe notes with auto-detected clickable URLs.
 */
/**
 * The glyph for a parameter, keyed by the field id from `RecipeFields`.
 *
 * Keyed by id rather than label so a copy change cannot silently drop an icon. Null is a
 * legitimate answer: the ISO bounds and the two monochromatic shifts have no glyph that says
 * anything a nearby one does not, and a wrong icon is worse than none.
 */
@DrawableRes
private fun fieldIcon(id: String): Int? = when (id) {
    "filmSimulation" -> R.drawable.ic_camera_roll
    "dynamicRange", "dRangePriority" -> R.drawable.ic_contrast
    "highlightTone" -> R.drawable.ic_tonality
    "shadowTone" -> R.drawable.ic_tonality_2
    "color" -> R.drawable.ic_palette
    "sharpness" -> R.drawable.ic_details
    "highIsoNR" -> R.drawable.ic_deblur
    "clarity" -> R.drawable.ic_diamond
    "grainEffect" -> R.drawable.ic_grain
    "grainSize" -> R.drawable.ic_transition_dissolve
    "colorChromeEffect", "colorChromeFxBlue" -> R.drawable.ic_colors
    "whiteBalance", "colorTemperature" -> R.drawable.ic_wb_auto
    "wbShiftRed", "wbShiftBlue" -> R.drawable.ic_discover_tune
    "exposureCompensation" -> R.drawable.ic_exposure
    else -> null
}

@Composable
private fun BentoNotesCard(notes: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current

    val annotatedNotes = remember(notes, primaryColor, uriHandler) {
        buildAnnotatedString {
            val urlRegex = Regex("""https?://[^\s<>"{}|\\^`]+""")
            var lastIndex = 0
            for (match in urlRegex.findAll(notes)) {
                val start = match.range.first
                val end = match.range.last + 1
                if (start > lastIndex) {
                    append(notes.substring(lastIndex, start))
                }
                val rawUrl = match.value
                val cleanUrl = rawUrl.trimEnd('.', ',', ';', '!', '?', ')')
                val trailingPunctuation = rawUrl.substring(cleanUrl.length)

                val link = LinkAnnotation.Url(
                    url = cleanUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = {
                        try {
                            uriHandler.openUri(cleanUrl)
                        } catch (_: Exception) {
                            // Ignored if no browser/handler installed
                        }
                    },
                )
                withLink(link) {
                    append(cleanUrl)
                }
                if (trailingPunctuation.isNotEmpty()) {
                    append(trailingPunctuation)
                }
                lastIndex = end
            }
            if (lastIndex < notes.length) {
                append(notes.substring(lastIndex))
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = annotatedNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun RecipeViewRouteContent(
    recipeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container
    val viewModel: RecipeViewModel =
        viewModel(factory = RecipeViewModel.factory(container, recipeId))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val camera by container.cameraController.state.collectAsStateWithLifecycle()
    var writeOpen by remember { mutableStateOf(false) }

    RecipeViewScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onChangedOnlyChange = viewModel::onChangedOnlyChange,
        onRatingChange = viewModel::onRatingChange,
        onTagsChange = viewModel::onTagsChange,
        onWriteToCamera = { writeOpen = true },
        canWriteToCamera = camera.canWrite,
        onExportRecipe = {
            viewModel.buildExport { filename, content ->
                ShareFile.share(context, filename, content)
            }
        },
    )

    if (writeOpen) {
        WriteSheetHost(recipeId = recipeId, onDismiss = { writeOpen = false })
    }
}

// --- previews ---

private val sampleHeader = RecipeHeader(
    id = "a",
    name = "Kodachrome 64",
    filmSimulationId = "classic-chrome",
    filmSimulationLabel = "Classic Chrome",
    rating = 5,
    tags = listOf("street", "warm"),
    notes = "Warm midday light. ETERNA is too flat for this.",
)

private fun row(id: String, label: String, value: String, default: Boolean = false) =
    FieldFormatting.Row(id, label, value, default, advisory = false)

private val sampleGroups = listOf(
    SettingsGroup(
        FieldGroup.SIMULATION,
        listOf(
            row("filmSimulation", "Film simulation", "Classic Chrome"),
            row("dynamicRange", "Dynamic range", "DR400"),
        ),
    ),
    SettingsGroup(
        FieldGroup.TONE,
        listOf(
            row("highlightTone", "Highlight tone", "+1.5"),
            row("shadowTone", "Shadow tone", "0", default = true),
            row("color", "Color", "+2"),
            row("sharpness", "Sharpness", "0", default = true),
        ),
    ),
    SettingsGroup(
        FieldGroup.WHITE_BALANCE,
        listOf(
            row("whiteBalance", "White balance", "Colour temperature"),
            row("colorTemperature", "Colour temperature", "5500K"),
            row(FieldFormatting.WB_SHIFT_ROW_ID, "WB shift", "R +3 / B -2"),
        ),
    ),
)

@Preview(name = "Recipe — light", showBackground = true, heightDp = 950)
@Preview(name = "Recipe — dark", showBackground = true, uiMode = 0x20, heightDp = 950)
@Composable
private fun RecipeViewPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(
                isLoading = false,
                recipe = sampleHeader,
                groups = sampleGroups,
            ),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}

@Preview(name = "Recipe — nothing changed", showBackground = true, heightDp = 700)
@Composable
private fun RecipeNothingChangedPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(
                isLoading = false,
                recipe = sampleHeader.copy(rating = 0, tags = emptyList(), notes = ""),
                groups = emptyList(),
                changedOnly = true,
                nothingChanged = true,
            ),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}

@Preview(name = "Recipe — not found", showBackground = true, heightDp = 600)
@Composable
private fun RecipeNotFoundPreview() {
    FujiTheme {
        RecipeViewScreen(
            state = RecipeViewUiState(isLoading = false, recipe = null),
            onBack = {}, onEdit = {}, onChangedOnlyChange = {},
        )
    }
}
