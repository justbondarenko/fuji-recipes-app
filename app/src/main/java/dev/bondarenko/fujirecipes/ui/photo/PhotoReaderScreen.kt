package dev.bondarenko.fujirecipes.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import coil.compose.AsyncImage
import dev.bondarenko.fujirecipes.core.store.ImageStore
import dev.bondarenko.fujirecipes.ui.theme.icons.Add
import dev.bondarenko.fujirecipes.ui.theme.icons.Check
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.ImageSearch
import dev.bondarenko.fujirecipes.ui.theme.icons.KeyboardArrowRight
import dev.bondarenko.fujirecipes.ui.theme.icons.StarRate
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.photo.MatchResult
import dev.bondarenko.fujirecipes.data.photo.PhotoReadFailure
import dev.bondarenko.fujirecipes.data.photo.PhotoRecipe
import dev.bondarenko.fujirecipes.data.photo.RecipeMatch
import dev.bondarenko.fujirecipes.ui.common.FujiIconPanel
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.theme.TabularFigures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read a recipe from a photo — FEAT-009 T-10.
 *
 * One question, one answer: *which of my recipes is this?* Everything on the screen is in
 * service of that, and the settings themselves are shown underneath rather than above,
 * because the name is what was asked for.
 */
@Composable
fun PhotoReaderScreen(
    state: PhotoReaderUiState,
    onChoosePhoto: () -> Unit,
    onSelectPhoto: (Int) -> Unit,
    onAddPhotoToRecipe: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    onReset: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    when (val stage = state.stage) {
        PhotoReaderStage.Empty -> {
            EmptyPhotoReaderState(
                onChoosePhoto = onChoosePhoto,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        PhotoReaderStage.Reading -> {
            ReadingPhotoState(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        is PhotoReaderStage.Failed -> {
            FailedPhotoState(
                reason = stage.reason,
                onChoosePhoto = onChoosePhoto,
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        is PhotoReaderStage.Result -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        top = 12.dp + contentPadding.calculateTopPadding(),
                        bottom = 12.dp + contentPadding.calculateBottomPadding(),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.photo_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = onReset,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_reset),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                val pagerState = rememberPagerState(initialPage = stage.selectedIndex) { stage.photos.size }
                LaunchedEffect(pagerState.currentPage) {
                    onSelectPhoto(pagerState.currentPage)
                }

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(
                        horizontal = if (stage.photos.size > 1) 36.dp else 16.dp,
                        vertical = 8.dp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    val photo = stage.photos[page]
                    val currentUri = photo.uri
                    val isCurrentPhotoAdded = currentUri.isNotEmpty() && state.addedPhotoUris.contains(currentUri)

                    AnalyzedPhotoCard(
                        photo = photo,
                        pageIndex = page,
                        totalPages = stage.photos.size,
                        isAddingPhoto = state.isAddingPhoto,
                        isPhotoAdded = isCurrentPhotoAdded,
                        onAddPhotoToRecipe = onAddPhotoToRecipe,
                        onOpenRecipe = onOpenRecipe,
                        onSaveAsNew = onSaveAsNew,
                        onChoosePhoto = onChoosePhoto,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                                scaleY = lerp(0.88f, 1f, 1f - pageOffset)
                                alpha = lerp(0.65f, 1f, 1f - pageOffset)
                            },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyPhotoReaderState(
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FujiIconPanel(
        // The same glyph as the Read item in the toolbar.
        icon = FujiIcons.ImageSearch,
        shape = MaterialShapes.Pill.toShape(),
        title = stringResource(R.string.photo_title),
        body = stringResource(R.string.photo_intro),
        actionLabel = stringResource(R.string.photo_action_choose),
        onAction = onChoosePhoto,
        modifier = modifier,
    )
}

@Composable
private fun ReadingPhotoState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FujiLoadingIndicator(size = 36.dp)
            Text(
                text = stringResource(R.string.photo_reading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FailedPhotoState(
    reason: PhotoReadFailure,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = reason.message()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onChoosePhoto,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.photo_action_choose))
            }
        }
    }
}

@Composable
private fun AnalyzedPhotoCard(
    photo: AnalyzedPhoto,
    pageIndex: Int,
    totalPages: Int,
    isAddingPhoto: Boolean,
    isPhotoAdded: Boolean,
    onAddPhotoToRecipe: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    onChoosePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        val best = photo.matches.best
        val isExactMatch = best?.isExact == true

        if (isExactMatch) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (photo.uri.isNotEmpty()) {
                    val parsedModel = remember(photo.uri) {
                        runCatching { android.net.Uri.parse(photo.uri) }.getOrDefault(photo.uri)
                    }
                    AsyncImage(
                        model = parsedModel,
                        contentDescription = stringResource(R.string.photo_title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                    )
                }

                // Top-Left: Exact Match Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF163E2B).copy(alpha = 0.90f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = FujiIcons.Check,
                            contentDescription = null,
                            tint = Color(0xFF85E0A3),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.photo_match_badge_exact),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF85E0A3),
                        )
                    }
                }

                // Top-Right: Page indicator
                if (totalPages > 1) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.60f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "${pageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TabularFigures,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // Bottom Gradient Scrim (~30% height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.92f),
                                ),
                            ),
                        ),
                )

                // Bottom Content: Matched Recipe Name & 2 CTAs
                best?.let { match ->
                    val recipe = match.recipe
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRecipe(recipe.id) },
                        ) {
                            // Row with Name and Rating next to it
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = recipe.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (recipe.rating > 0) {
                                    RatingBadge(rating = recipe.rating)
                                }
                            }

                            // View recipe link below title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.photo_action_view_recipe),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                                Icon(
                                    imageVector = FujiIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        // 2 CTA buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (recipe.images.size < ImageStore.MAX_IMAGES_PER_RECIPE || isPhotoAdded) {
                                if (isPhotoAdded) {
                                    FilledTonalButton(
                                        onClick = {},
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            disabledContainerColor = Color.White.copy(alpha = 0.2f),
                                            disabledContentColor = Color.White,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = FujiIcons.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.photo_action_photo_added),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { onAddPhotoToRecipe(recipe.id) },
                                        enabled = !isAddingPhoto,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (isAddingPhoto) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.photo_action_adding_photo),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = FujiIcons.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.photo_action_add_photo),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onSaveAsNew,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            ) {
                                Text(
                                    text = stringResource(R.string.photo_action_save),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (photo.uri.isNotEmpty()) {
                    val parsedModel = remember(photo.uri) {
                        runCatching { android.net.Uri.parse(photo.uri) }.getOrDefault(photo.uri)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    ) {
                        AsyncImage(
                            model = parsedModel,
                            contentDescription = stringResource(R.string.photo_title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (totalPages > 1) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    text = "${pageIndex + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = TabularFigures,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        if (best != null) {
                            MatchedRecipeCard(
                                match = best,
                                isAddingPhoto = isAddingPhoto,
                                isPhotoAdded = isPhotoAdded,
                                onAddPhotoToRecipe = { onAddPhotoToRecipe(best.recipe.id) },
                                onOpenRecipe = onOpenRecipe,
                                onSaveAsNew = onSaveAsNew,
                            )
                        } else {
                            NoMatchCard(
                                matches = photo.matches,
                                onSaveAsNew = onSaveAsNew,
                            )
                        }
                    }

                    item { SectionHeader(stringResource(R.string.photo_settings)) }

                    photo.recipe.cameraModel?.let { model ->
                        item { Body(stringResource(R.string.photo_shot_on, model)) }
                    }

                    items(photo.recipe.rawValues.entries.toList(), key = { it.key }) { (label, value) ->
                        SettingRow(label = label, value = value)
                    }
                }
            }
        }
    }
}

/**
 * The recipe that matched what the photo says — FEAT-009 T-10.
 *
 * Full recipe card styling matching the library view, including tags and star rating.
 */
@Composable
private fun MatchedRecipeCard(
    match: RecipeMatch,
    isAddingPhoto: Boolean,
    isPhotoAdded: Boolean,
    onAddPhotoToRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = match.recipe
    val context = LocalContext.current
    val imageStore = remember(context) { (context.applicationContext as FujiRecipesApp).container.imageStore }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row: Match Pill & Quick View Action
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val exactContainer = if (isDark) androidx.compose.ui.graphics.Color(0xFF163E2B) else androidx.compose.ui.graphics.Color(0xFFE6F4EA)
            val exactContent = if (isDark) androidx.compose.ui.graphics.Color(0xFF85E0A3) else androidx.compose.ui.graphics.Color(0xFF137333)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Match badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (match.isExact) exactContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (match.isExact) {
                        Icon(
                            imageVector = FujiIcons.Check,
                            contentDescription = null,
                            tint = exactContent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.photo_match_badge_exact),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = exactContent,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.photo_match_badge_near, match.percentage),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                TextButton(
                    onClick = { onOpenRecipe(recipe.id) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.photo_action_view_recipe),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = FujiIcons.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Recipe row (Thumbnail + Name + Sim + Rating)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenRecipe(recipe.id) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail preview if recipe has an image
                val firstImage = recipe.images.firstOrNull()
                if (firstImage != null) {
                    val imageFile = remember(firstImage) { imageStore.getFile(firstImage) }
                    AsyncImage(
                        model = imageFile,
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = FilmSimulations.labelFor(recipe.filmSimulationId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (recipe.rating > 0) {
                            RatingBadge(rating = recipe.rating)
                        }
                    }
                }
            }

            // Mismatches / Differences section if not exact
            if (!match.isExact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    match.mismatches.forEach { diff ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = diff.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${diff.photoValue} vs ${diff.savedValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (recipe.images.size < ImageStore.MAX_IMAGES_PER_RECIPE || isPhotoAdded) {
                    if (isPhotoAdded) {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = FujiIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.photo_action_photo_added),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Button(
                            onClick = onAddPhotoToRecipe,
                            enabled = !isAddingPhoto,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isAddingPhoto) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.photo_action_adding_photo),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Icon(
                                    imageVector = FujiIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.photo_action_add_photo),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSaveAsNew,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.photo_action_save),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoMatchCard(
    matches: MatchResult,
    onSaveAsNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.photo_match_none_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (matches.recipesChecked == 0) {
                    stringResource(R.string.photo_match_empty_body)
                } else {
                    stringResource(R.string.photo_match_none_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSaveAsNew,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.photo_action_save))
            }
        }
    }
}

@Composable
private fun RatingBadge(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = TabularFigures,
                ),
            )
            Icon(
                imageVector = FujiIcons.StarRate,
                contentDescription = stringResource(R.string.rating_of_five, rating),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PhotoReadFailure.message(): Pair<String, String> = when (this) {
    PhotoReadFailure.NOT_JPEG -> stringResource(R.string.photo_error_not_jpeg_title) to
        stringResource(R.string.photo_error_not_jpeg_body)

    PhotoReadFailure.TOO_LARGE -> stringResource(R.string.photo_error_too_large_title) to
        stringResource(R.string.photo_error_too_large_body)

    PhotoReadFailure.NO_EXIF -> stringResource(R.string.photo_error_no_exif_title) to
        stringResource(R.string.photo_error_no_exif_body)

    PhotoReadFailure.NOT_FUJIFILM -> stringResource(R.string.photo_error_not_fuji_title) to
        stringResource(R.string.photo_error_not_fuji_body)

    PhotoReadFailure.UNREADABLE -> stringResource(R.string.photo_error_unreadable_title) to
        stringResource(R.string.photo_error_unreadable_body)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun PhotoReaderRouteContent(
    initialUri: String? = null,
    onOpenRecipe: (String) -> Unit,
    onSaveAsNew: (prefill: String, name: String) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as FujiRecipesApp).container

    val viewModel: PhotoReaderViewModel = viewModel(
        factory = PhotoReaderViewModel.factory(container) { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                        ?.use { it.readBytes() }
                }.getOrNull()
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            viewModel.read(initialUri)
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.read(uris.map { it.toString() })
        }
    }

    PhotoReaderScreen(
        state = state,
        onChoosePhoto = {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onSelectPhoto = viewModel::selectPhoto,
        onAddPhotoToRecipe = viewModel::addPhotoToRecipe,
        onOpenRecipe = onOpenRecipe,
        onSaveAsNew = {
            viewModel.prefillJson()?.let { onSaveAsNew(it, viewModel.suggestedName()) }
        },
        onReset = viewModel::reset,
        contentPadding = contentPadding,
    )
}
