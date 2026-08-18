package dev.bondarenko.fujirecipes.ui.camera

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.camera.ModelIdentity
import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import dev.bondarenko.fujirecipes.data.fields.FieldContext
import dev.bondarenko.fujirecipes.data.fields.FieldFormatting
import dev.bondarenko.fujirecipes.data.fields.FieldGroup
import dev.bondarenko.fujirecipes.data.fields.FilmSimulations
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.fields.SensorGeneration
import dev.bondarenko.fujirecipes.ui.common.FujiLoadingIndicator
import dev.bondarenko.fujirecipes.ui.common.SectionHeader
import dev.bondarenko.fujirecipes.ui.recipe.BentoGroupGrid
import dev.bondarenko.fujirecipes.ui.recipe.RecipeHeader
import dev.bondarenko.fujirecipes.ui.recipe.RecipeTextFormatter
import dev.bondarenko.fujirecipes.ui.recipe.SettingsGroup
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Bottom sheet displaying the full configuration parameters for a single camera slot (C1–C7).
 * Arranges parameters into Bento grid tiles matching the Recipe View.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotDetailBottomSheet(
    slotNumber: Int,
    cameraIdentity: ModelIdentity?,
    controller: CameraController,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as FujiRecipesApp).container.recipeRepository }
    val coroutineScope = rememberCoroutineScope()

    var slotRecipe by remember { mutableStateOf<SlotRecipe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadSlot() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            runCatching { controller.readSlotRecipe(slotNumber) }
                .onSuccess { recipe ->
                    slotRecipe = recipe
                    isLoading = false
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to read slot parameters from camera."
                    isLoading = false
                }
        }
    }

    LaunchedEffect(slotNumber) {
        loadSlot()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        SlotDetailContent(
            slotNumber = slotNumber,
            cameraIdentity = cameraIdentity,
            slotRecipe = slotRecipe,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onRetry = ::loadSlot,
            onSaveToLibrary = { recipe ->
                coroutineScope.launch {
                    val body = buildJsonObject {
                        put("name", recipe.name)
                        put("settings", mapToJsonObject(recipe.settings))
                    }
                    repository.create(body)
                    Toast.makeText(context, "Saved “${recipe.name}” to your library", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

/**
 * Content of the Slot Detail view.
 */
@Composable
fun SlotDetailContent(
    slotNumber: Int,
    cameraIdentity: ModelIdentity?,
    slotRecipe: SlotRecipe?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSaveToLibrary: ((SlotRecipe) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dark = isSystemInDarkTheme()
    val greenAccent = if (dark) Color(0xFF6ABF69) else Color(0xFF2E7D32)

    var changedOnly by rememberSaveable { mutableStateOf(false) }

    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FujiLoadingIndicator(size = 48.dp, color = greenAccent)
                    Text(
                        text = "Reading C$slotNumber configuration…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        errorMessage != null -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.camera_action_retry))
                }
            }
        }

        slotRecipe != null -> {
            val simulationId = (slotRecipe.settings["filmSimulation"] as? String) ?: "provia"
            val simulationLabel = FilmSimulations.labelFor(simulationId)

            val fieldContext = FieldContext(
                generation = cameraIdentity?.generation ?: RecipeFields.generationOf(null),
                filmSimulationId = simulationId,
                grainEffectOff = (slotRecipe.settings["grainEffect"] as? String).let { it == null || it == "off" },
                whiteBalanceId = (slotRecipe.settings["whiteBalance"] as? String) ?: "auto",
            )

            val settingsJson = remember(slotRecipe.settings) {
                mapToJsonObject(slotRecipe.settings)
            }

            val allRows = remember(settingsJson, fieldContext) {
                FieldFormatting.rowsFor(settingsJson, fieldContext)
            }

            val displayedRows = remember(allRows, changedOnly) {
                if (changedOnly) allRows.filter { !it.isDefault } else allRows
            }

            val groups = remember(displayedRows) {
                groupRows(displayedRows)
            }

            val nothingChanged = changedOnly && displayedRows.isEmpty()

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ─── Header Block ───────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = greenAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "C$slotNumber",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = greenAccent,
                                )
                            }
                        }

                        Text(
                            text = slotRecipe.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = simulationLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }

                            if (cameraIdentity != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Text(
                                        text = cameraIdentity.model.ifBlank { cameraIdentity.label },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Action Buttons (Copy / Save) ───────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dummyHeader = RecipeHeader(
                                    id = "",
                                    name = slotRecipe.name,
                                    filmSimulationId = simulationId,
                                    filmSimulationLabel = simulationLabel,
                                    rating = 0,
                                    tags = emptyList(),
                                    notes = "",
                                    images = emptyList(),
                                )
                                val text = RecipeTextFormatter.format(dummyHeader, groupRows(allRows))
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, context.getString(R.string.recipe_copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_copy_recipe))
                        }

                        if (onSaveToLibrary != null) {
                            Button(
                                onClick = { onSaveToLibrary(slotRecipe) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_save),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Save to library")
                            }
                        }
                    }
                }

                // ─── Changed-only Filter ────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.changed_only),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Switch(
                                checked = changedOnly,
                                onCheckedChange = { changedOnly = it },
                            )
                        }
                    }
                }

                if (nothingChanged) {
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

                // ─── Bento Grid Sections ────────────────────────────────────
                groups.forEach { group ->
                    item(key = group.group.id) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(group.group.label)
                            BentoGroupGrid(group)
                        }
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No slot data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Helper converting Map<String, Any> to JsonObject */
private fun mapToJsonObject(map: Map<String, Any>): JsonObject = buildJsonObject {
    map.forEach { (k, v) ->
        when (v) {
            is String -> put(k, v)
            is Number -> put(k, v)
            is Boolean -> put(k, v)
            else -> put(k, v.toString())
        }
    }
}

/** Groups formatted rows into SettingsGroup matching FieldGroup order. */
private fun groupRows(rows: List<FieldFormatting.Row>): List<SettingsGroup> =
    rows.groupBy { row ->
        RecipeFields.byId(row.fieldId)?.group ?: FieldGroup.WHITE_BALANCE
    }.toList()
        .sortedBy { (group, _) -> group.ordinal }
        .map { (group, groupRows) -> SettingsGroup(group, groupRows) }

// ─── Previews ───────────────────────────────────────────────────────────────

private val previewSlotRecipe = SlotRecipe(
    slot = 2,
    name = "Portra 400",
    settings = mapOf(
        "filmSimulation" to "classic_chrome",
        "dynamicRange" to 400,
        "grainEffect" to "strong",
        "grainSize" to "large",
        "colorChromeEffect" to "strong",
        "colorChromeFxBlue" to "weak",
        "whiteBalance" to "auto",
        "wbShiftRed" to 3,
        "wbShiftBlue" to -4,
        "highlightTone" to -1.0,
        "shadowTone" to 1.5,
        "color" to 2,
        "sharpness" to -1,
        "noiseReduction" to -4,
        "clarity" to -2,
    ),
)

@Preview(name = "Slot Detail — Loaded", showBackground = true)
@Preview(name = "Slot Detail — Loaded (Dark)", showBackground = true, uiMode = 0x20)
@Composable
private fun SlotDetailLoadedPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            SlotDetailContent(
                slotNumber = 2,
                cameraIdentity = ModelIdentity("X100VI", SensorGeneration.XTRANS_V, true, "X-Trans V", true, null),
                slotRecipe = previewSlotRecipe,
                isLoading = false,
                errorMessage = null,
                onRetry = {},
                onSaveToLibrary = {},
            )
        }
    }
}

@Preview(name = "Slot Detail — Loading", showBackground = true)
@Composable
private fun SlotDetailLoadingPreview() {
    FujiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            SlotDetailContent(
                slotNumber = 3,
                cameraIdentity = ModelIdentity("X-T5", SensorGeneration.XTRANS_V, true, "X-Trans V", true, null),
                slotRecipe = null,
                isLoading = true,
                errorMessage = null,
                onRetry = {},
                onSaveToLibrary = {},
            )
        }
    }
}
