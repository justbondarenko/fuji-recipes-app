package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.result.LibraryError
import dev.bondarenko.fujirecipes.ui.common.FujiCenteredLoading
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The states the list can be in that are not "here are your recipes" — FEAT-001 T-21.
 *
 * Each is a distinct rendering with its own copy, because `coding-standards.md` P5 says a
 * library file that will not parse, a phone with no room left and a value out of range have
 * three different remedies. The one that matters most is what is *not* here: **a library
 * that could not be read never renders as an empty one.**
 */

/**
 * The first load, before there is anything to show.
 *
 * M3's contained loading indicator in the middle of the screen, rather than the five skeleton
 * rows this used to draw. Skeletons are worth their complexity when they trace the real layout
 * closely enough to stop it jumping; five identical grey rows never matched a library of
 * varying tag counts, so they bought the jump anyway and cost a fake row to maintain.
 */
@Composable
fun LibraryLoading(modifier: Modifier = Modifier) {
    FujiCenteredLoading(modifier = modifier)
}


/**
 * A panel: an icon-free headline, a line of body, and up to two actions.
 *
 * Every non-list state is one of these, which is what keeps them consistent — and is also
 * why there is no illustration. `PRD.md` §7.1: set on generous empty space, do not fill it.
 */
@Composable
fun LibraryPanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (primaryLabel != null || secondaryLabel != null) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (primaryLabel != null && onPrimary != null) {
                    Button(onClick = onPrimary) { Text(primaryLabel) }
                }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
                }
            }
        }
    }
}

/**
 * A failure, named.
 *
 * The `when` is exhaustive over `LibraryError` on purpose: a new case added to the sealed
 * type becomes a compile error here, which is how P8 stays true as the store grows rather
 * than decaying into a default branch that says "something went wrong".
 */
@Composable
fun LibraryErrorPanel(
    error: LibraryError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title: String
    val body: String

    when (error) {
        is LibraryError.Unreadable -> {
            // The one failure where the app is holding recipes it cannot show. It must never
            // read as an empty library — nothing has been lost, and nothing will be
            // overwritten while this is true.
            title = stringResource(R.string.error_unreadable_title)
            body = error.message
                ?.let { stringResource(R.string.error_unreadable_body_with_reason, it) }
                ?: stringResource(R.string.error_unreadable_body)
        }

        is LibraryError.Storage -> {
            title = stringResource(R.string.error_storage_title)
            body = error.message
                ?.let { stringResource(R.string.error_storage_body_with_reason, it) }
                ?: stringResource(R.string.error_storage_body)
        }

        is LibraryError.NotFound -> {
            title = stringResource(R.string.error_not_found_title)
            body = stringResource(R.string.error_not_found_body)
        }

        is LibraryError.Invalid -> {
            title = stringResource(R.string.error_invalid_title)
            body = error.message ?: stringResource(R.string.error_invalid_body)
        }
    }

    LibraryPanel(
        title = title,
        body = body,
        modifier = modifier,
        primaryLabel = stringResource(R.string.action_try_again),
        onPrimary = onRetry,
    )
}

@Preview(name = "States — light", showBackground = true, heightDp = 900)
@Preview(name = "States — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun LibraryStatesPreview() {
    FujiTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LibraryErrorPanel(
                LibraryError.Unreadable("Unexpected character at offset 412."),
                onRetry = {},
            )
            LibraryErrorPanel(LibraryError.Storage(null), onRetry = {})
            LibraryLoading()
        }
    }
}

/**
 * When the library was last fetched — the last line of the list.
 *
 * **Quiet on purpose.** This replaced a tertiary-coloured banner at the top of the screen,
 * which used alarm styling to report that everything was fine: a cached library is the
 * normal state in the field, not a warning. A date at the end of the list is where you look
 * when you are already wondering how fresh this is, and invisible when you are not.
 */
@Composable
fun LastUpdatedFooter(updatedAt: String, modifier: Modifier = Modifier) {
    val formatted = Timestamps.updatedAt(updatedAt) ?: return

    Text(
        text = stringResource(R.string.last_updated, formatted),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
    )
}
