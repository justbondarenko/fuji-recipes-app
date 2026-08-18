package dev.bondarenko.fujirecipes.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import dev.bondarenko.fujirecipes.ui.theme.icons.Add
import dev.bondarenko.fujirecipes.ui.theme.icons.ContentPaste
import dev.bondarenko.fujirecipes.ui.theme.icons.Edit
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.editor.PasteRecipeSheet

/**
 * Starting a recipe, from wherever the offer was made.
 *
 * The whole two-step gesture in one place: the dialog that offers the two ways in, and — when
 * the answer is "from text" — the sheet that parses it. Callers get one callback and open the
 * editor with what it hands them.
 *
 * It lives here rather than in `AppShell` because the shell is no longer the only place that
 * asks. An export screen with an empty library has the same thing to offer and must offer it
 * the same way; a second copy of the dialog is how two entry points start to differ.
 *
 * Nothing here is saved across process death, deliberately: a half-made choice is a gesture
 * in progress, and restoring the dialog over a freshly drawn screen would read as the app
 * doing something on its own.
 */
@Composable
fun CreateRecipeFlow(
    visible: Boolean,
    onDismiss: () -> Unit,
    /**
     * Open the editor. [prefill] is the parsed `settings` object as JSON and [prefillName] the
     * name the text carried — both null for a create that starts from nothing.
     */
    onCreate: (prefill: String?, prefillName: String?) -> Unit,
) {
    // Survives the dialog closing, because choosing "from text" is what opens it.
    var pasting by remember { mutableStateOf(false) }

    if (visible && !pasting) {
        CreateRecipeDialog(
            onDismiss = onDismiss,
            onParseTextClick = { pasting = true },
            onManualClick = {
                onDismiss()
                onCreate(null, null)
            },
        )
    }

    if (pasting) {
        PasteRecipeSheet(
            onDismiss = {
                pasting = false
                onDismiss()
            },
            onImport = { parsed ->
                pasting = false
                onDismiss()
                onCreate(parsed.settings.toString(), parsed.name)
            },
        )
    }
}

/**
 * Two ways to start a recipe, as a dialog.
 *
 * This was a `FloatingActionButtonMenu` behind a `ToggleFloatingActionButton`, which meant the
 * bar carried a component that expands upward, brings its own scrim and reserves its own
 * insets — for a choice between two things. A plain FAB and a dialog say the same thing, and
 * let the toolbar own its FAB slot the way M3 specifies.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateRecipeDialog(
    onDismiss: () -> Unit,
    onParseTextClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The hero icon slot from `m3.material.io/components/dialogs/specs` — centred above
        // the headline, and the reason the headline centres with it.
        icon = {
            Box(
                modifier = Modifier
                    .size(DialogHeroSize)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FujiIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(DialogHeroIconSize),
                )
            }
        },
        title = { Text(stringResource(R.string.nav_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.create_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CreateOption(
                    icon = FujiIcons.ContentPaste,
                    title = stringResource(R.string.create_from_text),
                    body = stringResource(R.string.create_from_text_body),
                    shape = MaterialShapes.Pill.toShape(),
                    onClick = onParseTextClick,
                )
                CreateOption(
                    icon = FujiIcons.Edit,
                    title = stringResource(R.string.create_manually),
                    body = stringResource(R.string.create_manually_body),
                    shape = MaterialShapes.Square.toShape(),
                    onClick = onManualClick,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * One way to start a recipe.
 *
 * A card rather than a list row: the two are a choice, not a menu, and each carries a line
 * saying what it does. The shape on the icon is the same device the pages use — a glyph in an
 * M3 shape rather than a bare icon in a circle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    shape: Shape,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(DialogOptionPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(DialogOptionIconSlot)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// 💡 CREATE DIALOG SIZES.
/** The hero shape above the headline. */
private val DialogHeroSize = 56.dp
private val DialogHeroIconSize = 28.dp

/** The shaped glyph on each option. */
private val DialogOptionIconSlot = 44.dp
private val DialogOptionPadding = 14.dp
