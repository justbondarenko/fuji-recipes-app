package dev.bondarenko.fujirecipes.ui.recipe

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
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
    val scope = rememberCoroutineScope()

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
            // Let the sheet slide out before the editor arrives. Calling onEdit straight
            // away removes this composable in the same frame, so the sheet vanished rather
            // than closing and the editor appeared to teleport in over the gap.
            onEdit = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onEdit(recipeId)
                }
            },
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
                // Plain text, not a target: the navigation icon beside it is the back
                // affordance, and a bare clickable Text has no ripple, no minimum touch size
                // and no button semantics for TalkBack.
                Text(
                    text = stringResource(R.string.action_back_to_list),
                    style = MaterialTheme.typography.titleMedium,
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
                    painter = painterResource(R.drawable.ic_file_export),
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
 * The recipe's header — no container, just the pieces on the page background.
 *
 * The card, its border, the blurred simulation frame and the frosted scrim are all gone: they
 * framed content that is already the top of its own screen, and the frame was doing more
 * visual work than the recipe was.
 *
 * Stacked rather than side by side — simulation, name, rating, tags — so a long name has the
 * full width and the tag cloud can wrap under it.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecipeHeaderBlock(
    recipe: RecipeHeader,
    onRatingChange: (Int) -> Unit,
    onTagsChange: (List<String>) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        // 💡 HEADER ALIGNMENT — `CenterHorizontally` or `Start`.
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 💡 FILM SIMULATION SIZE — the shaped swatch at the top.
        FilmSimBadge(
            simulationId = recipe.filmSimulationId,
            size = HeaderSwatchSize,
            shape = MaterialShapes.Square.toShape(),
        )

        // 💡 GAP: swatch → name
        Spacer(Modifier.height(SwatchToNameGap))

        // 💡 RECIPE NAME:
        //    - Size: `headlineMediumEmphasized` -> `headlineSmallEmphasized` (smaller) or
        //      `headlineLargeEmphasized` (bigger). The `Emphasized` suffix is M3's heavier,
        //      tighter cut — dropping it gives the plain weight.
        //    - Colour: `primary` follows the device palette on Android 12+, the same way
        //      every other accent in the app does.
        Text(
            text = recipe.name,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        // 💡 GAP: name → rating stars
        Spacer(Modifier.height(NameToRatingGap))

        RatingInput(rating = recipe.rating, onRatingChange = onRatingChange)

        // 💡 GAP: rating stars → tags
        Spacer(Modifier.height(RatingToTagsGap))

        // 💡 TAGS SHOWN BEFORE "+N" — raise to reveal more before the fold.
        TagInput(
            tags = recipe.tags,
            onTagsChange = onTagsChange,
            collapsedLimit = HeaderVisibleTags,
            horizontalAlignment = Alignment.CenterHorizontally,
        )
    }
}

// 💡 HEADER SIZES AND GAPS — every measurement in the header, in one place.
/** The shaped film simulation swatch. */
private val HeaderSwatchSize = 120.dp
/** Swatch to recipe name. */
private val SwatchToNameGap = 14.dp
/** Name to rating stars — deliberately tight, they read as one block. */
private val NameToRatingGap = 4.dp
/** Rating stars to the tag cloud. */
private val RatingToTagsGap = 12.dp

/** 💡 How many tags show before the `+N` chip. */
private const val HeaderVisibleTags = 3

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                // Not bold: at this size the weight was shouting, and the label above it is
                // already the quieter of the two.
                style = MaterialTheme.typography.titleLarge.copy(
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
