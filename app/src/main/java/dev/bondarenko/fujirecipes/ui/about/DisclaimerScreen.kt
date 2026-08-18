package dev.bondarenko.fujirecipes.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import dev.bondarenko.fujirecipes.ui.theme.icons.ArrowBack
import dev.bondarenko.fujirecipes.ui.theme.icons.FujiIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.disclaimer_title),
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
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 24.dp + contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.disclaimer_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec1_title),
                body = stringResource(R.string.disclaimer_sec1_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec2_title),
                body = stringResource(R.string.disclaimer_sec2_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec3_title),
                body = stringResource(R.string.disclaimer_sec3_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec4_title),
                body = stringResource(R.string.disclaimer_sec4_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec5_title),
                body = stringResource(R.string.disclaimer_sec5_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec6_title),
                body = stringResource(R.string.disclaimer_sec6_body),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )

            LegalClause(
                title = stringResource(R.string.disclaimer_sec7_title),
                body = stringResource(R.string.disclaimer_sec7_body),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.disclaimer_acknowledgement),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LegalClause(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
    }
}

@Composable
fun DisclaimerRouteContent(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    DisclaimerScreen(
        onBack = onBack,
        contentPadding = contentPadding,
    )
}

@Preview(name = "Disclaimer — light", showBackground = true)
@Preview(name = "Disclaimer — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DisclaimerPreview() {
    FujiTheme {
        DisclaimerScreen(
            onBack = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
