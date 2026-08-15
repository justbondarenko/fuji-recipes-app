package dev.bondarenko.fujirecipes.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme

/**
 * Where the library is and how to get past the gate — FEAT-001 T-12.
 *
 * The one thing worth knowing about this screen: **Test connection issues
 * `GET /api/recipes`, never `GET /api/health`.** Health is in the server's `UNGATED_PATHS`,
 * so it answers 200 with no credentials at all — testing against it would report a revoked
 * token as a working connection, which is the exact failure this screen exists to catch.
 */
@Composable
fun ConnectionScreen(
    state: ConnectionUiState,
    // Null on first run: there is nothing behind setup to go back to.
    onBack: (() -> Unit)? = null,
    onBaseUrlChange: (String) -> Unit,
    onClientIdChange: (String) -> Unit,
    onClientSecretChange: (String) -> Unit,
    onToggleSecretVisible: () -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                Text(stringResource(R.string.action_back))
            }
        }

        Text(
            text = stringResource(R.string.connection_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.connection_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text(stringResource(R.string.connection_base_url)) },
            placeholder = { Text(stringResource(R.string.connection_base_url_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.clientId,
            onValueChange = onClientIdChange,
            label = { Text(stringResource(R.string.connection_client_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.clientSecret,
            onValueChange = onClientSecretChange,
            label = { Text(stringResource(R.string.connection_client_secret)) },
            singleLine = true,
            visualTransformation = if (state.secretVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                TextButton(onClick = onToggleSecretVisible) {
                    Text(
                        stringResource(
                            if (state.secretVisible) {
                                R.string.connection_hide_secret
                            } else {
                                R.string.connection_show_secret
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave, enabled = state.isComplete && !state.isTesting) {
                Text(stringResource(R.string.connection_save))
            }
            OutlinedButton(onClick = onTest, enabled = state.isComplete && !state.isTesting) {
                Text(
                    stringResource(
                        if (state.isTesting) R.string.connection_testing else R.string.connection_test,
                    ),
                )
            }
        }

        state.result?.let { result ->
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.isSuccess) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
fun ConnectionRouteContent(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    contentPadding: PaddingValues,
) {
    val container = (LocalContext.current.applicationContext as FujiRecipesApp).container
    val viewModel: ConnectionViewModel =
        viewModel(factory = ConnectionViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ConnectionScreen(
        state = state,
        onBack = onBack.takeIf { showBack },
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onClientIdChange = viewModel::onClientIdChange,
        onClientSecretChange = viewModel::onClientSecretChange,
        onToggleSecretVisible = viewModel::onToggleSecretVisible,
        onTest = viewModel::onTest,
        onSave = { viewModel.onSave(onSaved) },
        contentPadding = contentPadding,
    )
}

@Preview(name = "Connection — light", showBackground = true, heightDp = 800)
@Preview(name = "Connection — dark", showBackground = true, uiMode = 0x20, heightDp = 800)
@Composable
private fun ConnectionScreenPreview() {
    FujiTheme {
        ConnectionScreen(
            state = ConnectionUiState(
                baseUrl = "recipes.example.com",
                clientId = "abc123.access",
                clientSecret = "secret",
                result = ConnectionTestResult(false, "Cloudflare Access did not accept this service token."),
            ),
            onBaseUrlChange = {}, onClientIdChange = {}, onClientSecretChange = {},
            onToggleSecretVisible = {}, onTest = {}, onSave = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
