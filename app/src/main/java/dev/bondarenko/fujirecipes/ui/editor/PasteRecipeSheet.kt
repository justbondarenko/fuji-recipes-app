package dev.bondarenko.fujirecipes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.data.text.ParsedRecipeText
import dev.bondarenko.fujirecipes.data.text.parseRecipeText
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * Create a recipe from pasted text — FEAT-011.
 *
 * Mirrors the web client's import dialog (`fuji-recipes-book/src/pages/recipe/new.vue`): paste,
 * see how much was recognised **before** committing to it, then open the form pre-filled. The
 * count is shown live because the failure mode this guards against is a paste that parses to
 * almost nothing and silently opens an empty form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteRecipeSheet(
    onDismiss: () -> Unit,
    onImport: (ParsedRecipeText) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PasteRecipeSheetContent(onImport = onImport)
    }
}

@Composable
fun PasteRecipeSheetContent(
    onImport: (ParsedRecipeText) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    // Pure and cheap, so it runs on every keystroke rather than behind a button: what the
    // parser will do is the thing the user needs to see while they are still able to fix it.
    val parsed = remember(text) { parseRecipeText(text) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.paste_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.paste_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
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
            TextButton(onClick = { text = SampleText }) {
                Text(stringResource(R.string.paste_sample))
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
private fun PasteRecipeSheetPreview() {
    FujiTheme {
        PasteRecipeSheetContent(onImport = {})
    }
}
