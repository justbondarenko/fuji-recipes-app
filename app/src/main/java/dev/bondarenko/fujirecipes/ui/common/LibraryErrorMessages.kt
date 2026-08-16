package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.result.LibraryError

/**
 * The per-[LibraryError] copy, in one place.
 *
 * Shared so that two screens cannot describe the same failure two different ways
 * (`coding-standards.md` P5). The `when` is exhaustive on purpose: a case added to the
 * sealed type becomes a compile error here rather than decaying into "something went wrong".
 */
@Composable
fun errorMessageFor(error: LibraryError): String = when (error) {
    is LibraryError.Storage -> error.message
        ?.let { stringResource(R.string.error_storage_body_with_reason, it) }
        ?: stringResource(R.string.error_storage_body)

    is LibraryError.Unreadable -> error.message
        ?.let { stringResource(R.string.error_unreadable_body_with_reason, it) }
        ?: stringResource(R.string.error_unreadable_body)

    is LibraryError.NotFound -> stringResource(R.string.error_not_found_body)

    is LibraryError.Invalid -> error.message ?: stringResource(R.string.error_invalid_body)
}
