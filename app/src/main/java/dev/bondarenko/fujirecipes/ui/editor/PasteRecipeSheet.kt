package dev.bondarenko.fujirecipes.ui.editor

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_REVIEW_LINES = 3

/**
 * Create a recipe from pasted text — FEAT-011.
 *
 * A full destination, not a sheet: pasted recipes can be lengthy, and the recognition result
 * deserves enough vertical room to be reviewed before opening the editor. Its in-content title
 * intentionally matches Settings rather than introducing a separate app-bar treatment.
 */
@Composable
fun PasteRecipeScreen(
    onBack: () -> Unit,
    onImport: (ParsedRecipeText) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    PasteRecipeContent(
        onImport = onImport,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun PasteRecipeContent(
    onImport: (ParsedRecipeText) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var clipboardWasEmpty by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pure and cheap, so it runs on every keystroke rather than behind a button: what the
    // parser will do is the thing the user needs to see while they are still able to fix it.
    val parsed = remember(text) { parseRecipeText(text) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.paste_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.paste_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
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
        ) {
            Text(stringResource(R.string.paste_from_clipboard))
        }

        if (clipboardWasEmpty) {
            Text(
                text = stringResource(R.string.paste_clipboard_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

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
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 260.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detectionSummary(parsed),
                style = MaterialTheme.typography.bodySmall,
                color = if (parsed.fieldsFound > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            TextButton(onClick = {
                text = SampleText
                clipboardWasEmpty = false
            }) {
                Text(stringResource(R.string.paste_sample))
            }
        }

        ParsedSettingsPreview(parsed)

        if (parsed.linesNeedingReview.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.paste_review_lines,
                            parsed.linesNeedingReview.size,
                            parsed.linesNeedingReview.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
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

        Button(
            onClick = { onImport(parsed) },
            enabled = !parsed.isEmpty,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.paste_action))
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
