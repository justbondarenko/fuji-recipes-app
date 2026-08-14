package dev.bondarenko.fujirecipes.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.bondarenko.fujirecipes.core.net.ApiError
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * The states the list can be in that are not "here are your recipes" — FEAT-001 T-21.
 *
 * Each is a distinct rendering with its own copy, because `coding-standards.md` P5 says a
 * refused token, a sleeping database and no signal have three different remedies. The one
 * that matters most is what is *not* here: **403 never renders as an empty library.**
 */

/** Skeleton rows, so the layout does not jump when the recipes land. */
@Composable
fun LibraryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.medium,
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Bar(width = 36.dp, height = 36.dp, shape = RoundedCornerShape(50))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bar(width = 160.dp, height = 16.dp)
                    Bar(width = 100.dp, height = 12.dp)
                }
            }
        }
    }
}

@Composable
private fun Bar(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {}
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
 * The `when` is exhaustive over `ApiError` on purpose: a new case added to the sealed type
 * becomes a compile error here, which is how P8 stays true as the API grows rather than
 * decaying into a default branch that says "something went wrong".
 */
@Composable
fun LibraryErrorPanel(
    error: ApiError,
    onRetry: () -> Unit,
    onOpenConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title: String
    val body: String
    var primary: String? = stringResource(R.string.action_try_again)
    var onPrimary: () -> Unit = onRetry

    when (error) {
        is ApiError.Forbidden -> {
            title = stringResource(R.string.error_forbidden_title)
            body = error.message ?: stringResource(R.string.error_forbidden_body)
            primary = stringResource(R.string.action_open_connection)
            onPrimary = onOpenConnection
        }

        is ApiError.AccessUnconfigured -> {
            // The server's gate, not the user's token, and not the database. The message
            // names the missing Worker secrets, so it is shown rather than replaced.
            title = stringResource(R.string.error_access_unconfigured_title)
            body = error.message ?: stringResource(R.string.error_access_unconfigured_body)
        }

        is ApiError.StorageUnavailable -> {
            title = stringResource(R.string.error_storage_title)
            body = stringResource(R.string.error_storage_body)
        }

        is ApiError.Network -> {
            title = stringResource(R.string.error_network_title)
            body = stringResource(R.string.error_network_body)
        }

        is ApiError.Internal -> {
            title = stringResource(R.string.error_internal_title)
            body = error.requestId
                ?.let { stringResource(R.string.error_internal_body_with_id, it) }
                ?: stringResource(R.string.error_internal_body)
        }

        is ApiError.Malformed -> {
            title = stringResource(R.string.error_malformed_title)
            body = stringResource(R.string.error_malformed_body)
        }

        is ApiError.NotFound,
        is ApiError.ValidationFailed,
        is ApiError.IdExists,
        is ApiError.BadRequest,
        is ApiError.MethodNotAllowed,
        is ApiError.Unexpected,
        -> {
            title = stringResource(R.string.error_unexpected_title)
            body = error.message ?: stringResource(R.string.error_unexpected_body)
        }
    }

    LibraryPanel(
        title = title,
        body = body,
        modifier = modifier,
        primaryLabel = primary,
        onPrimary = onPrimary,
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
                ApiError.Forbidden("This token was not accepted."),
                onRetry = {},
                onOpenConnection = {},
            )
            LibraryErrorPanel(
                ApiError.AccessUnconfigured(
                    "This deployment has no access gate: CF_ACCESS_POLICY_AUD is not set.",
                ),
                onRetry = {},
                onOpenConnection = {},
            )
            LibraryErrorPanel(ApiError.Network(null), onRetry = {}, onOpenConnection = {})
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
    val formatted = LastWritten.updatedAt(updatedAt) ?: return

    Text(
        text = stringResource(R.string.last_updated, formatted),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
    )
}
