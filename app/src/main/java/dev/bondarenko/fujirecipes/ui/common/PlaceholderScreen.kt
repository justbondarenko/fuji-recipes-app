package dev.bondarenko.fujirecipes.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * A destination that exists so the shell can be navigated before its feature is built.
 *
 * Every one of these is deleted by the feature that owns the route. It says which feature
 * that is, so an unfinished build is legible rather than mysterious.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Placeholder — light", showBackground = true)
@Preview(name = "Placeholder — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun PlaceholderScreenPreview() {
    FujiTheme {
        PlaceholderScreen(
            titleRes = R.string.placeholder_more_title,
            bodyRes = R.string.placeholder_more_body,
            contentPadding = PaddingValues(0.dp),
        )
    }
}
