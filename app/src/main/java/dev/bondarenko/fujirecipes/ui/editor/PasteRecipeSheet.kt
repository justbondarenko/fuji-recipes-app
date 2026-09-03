package dev.bondarenko.fujirecipes.ui.editor

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import dev.bondarenko.fujirecipes.data.text.ParsedRecipeText
import dev.bondarenko.fujirecipes.data.text.parseRecipeText
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.Check
import dev.bondarenko.fujirecipes.ui.theme.icons.CloseSmall
import dev.bondarenko.fujirecipes.ui.theme.icons.ContentPaste
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import dev.bondarenko.fujirecipes.ui.theme.icons.Info
import dev.bondarenko.fujirecipes.ui.theme.icons.Tune
import dev.bondarenko.fujirecipes.ui.theme.icons.Warning
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_REVIEW_LINES = 3

/**
 * Create a recipe from pasted text — FEAT-011.
 */
@Composable
fun PasteRecipeScreen(
    onBack: () -> Unit,
    onImport: (ParsedRecipeText) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    PasteRecipeContent(
        onBack = onBack,
        onImport = onImport,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteRecipeContent(
    onBack: () -> Unit,
    onImport: (ParsedRecipeText) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var clipboardWasEmpty by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val parsed = remember(text) { parseRecipeText(text) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.paste_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = FujiIcons.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp + contentPadding.calculateBottomPadding(),
                )
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.paste_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    clipboardWasEmpty = false
                },
                label = { Text(stringResource(R.string.paste_label)) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.paste_placeholder),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            text = ""
                            clipboardWasEmpty = false
                        }) {
                            Icon(
                                imageVector = FujiIcons.CloseSmall,
                                contentDescription = stringResource(R.string.paste_clear),
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 240.dp),
            )

            // Recognition review card when text is entered
            if (text.isNotBlank()) {
                Surface(
                    color = if (parsed.fieldsFound > 0) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (parsed.fieldsFound > 0) FujiIcons.Tune else FujiIcons.Info,
                                contentDescription = null,
                                tint = if (parsed.fieldsFound > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = detectionSummary(parsed),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (parsed.fieldsFound > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }

                        ParsedSettingsPreview(parsed)
                    }
                }
            }

            if (parsed.linesNeedingReview.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = FujiIcons.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.paste_review_lines,
                                    parsed.linesNeedingReview.size,
                                    parsed.linesNeedingReview.size,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = stringResource(R.string.paste_review_lines_body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        parsed.linesNeedingReview.take(MAX_VISIBLE_REVIEW_LINES).forEach { line ->
                            Text(
                                text = "• $line",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        val remainingCount = parsed.linesNeedingReview.size - MAX_VISIBLE_REVIEW_LINES
                        if (remainingCount > 0) {
                            Text(
                                text = stringResource(R.string.paste_review_lines_more, remainingCount),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (clipboardWasEmpty) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        imageVector = FujiIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.paste_clipboard_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Vertically stacked buttons: Paste from clipboard -> Analyze -> Try an example
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val clipboardText = clipboard.getClipEntry()
                            ?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                        if (clipboardText.isNullOrBlank()) {
                            clipboardWasEmpty = true
                        } else {
                            text = clipboardText
                            clipboardWasEmpty = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = FujiIcons.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.paste_from_clipboard),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Button(
                onClick = { onImport(parsed) },
                enabled = !parsed.isEmpty,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(
                    imageVector = FujiIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.paste_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            TextButton(
                onClick = {
                    text = SampleText
                    clipboardWasEmpty = false
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.paste_sample),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ParsedSettingsPreview(parsed: ParsedRecipeText) {
    if (parsed.fieldsFound == 0) return

    val labels = parsed.settings.keys.mapNotNull(RecipeFields::byId)
        .joinToString(separator = " · ") { it.label }
    if (labels.isBlank()) return

    Text(
        text = stringResource(R.string.paste_will_fill, labels),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun detectionSummary(parsed: ParsedRecipeText): String {
    if (parsed.fieldsFound == 0 && parsed.name == null) {
        return stringResource(R.string.paste_detected_none)
    }

    val counted = pluralStringResource(
        R.plurals.paste_detected,
        parsed.fieldsFound,
        parsed.fieldsFound,
    )
    return parsed.name?.let { stringResource(R.string.paste_detected_name, counted, it) } ?: counted
}

/** The same example the web client offers, so the two clients teach the same format. */
private val SampleText = """Classic Chrome
Film Simulation: Classic Chrome
Dynamic Range: DR400
Grain Effect: Strong, Small
Color Chrome Effect: Strong
Color Chrome FX Blue: Weak
White Balance: Auto, R: +2, B: -3
Highlight Tone: -1
Shadow Tone: +1.5
Color: +2
Sharpness: 0
High ISO NR: -2
Clarity: -2"""

@Preview(name = "Paste — light", showBackground = true)
@Preview(name = "Paste — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun PasteRecipeScreenPreview() {
    FujiTheme {
        PasteRecipeScreen(onBack = {}, onImport = {})
    }
}
