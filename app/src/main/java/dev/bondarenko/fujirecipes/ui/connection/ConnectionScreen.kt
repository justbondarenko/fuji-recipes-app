package dev.bondarenko.fujirecipes.ui.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
@OptIn(ExperimentalMaterial3Api::class)
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
    onClearCredentials: (() -> Unit)? = null,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.connection_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
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
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onSave,
                    enabled = state.isComplete && !state.isTesting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.connection_save))
                }
                OutlinedButton(
                    onClick = onTest,
                    enabled = state.isComplete && !state.isTesting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
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

            if (state.isConfigured && onClearCredentials != null) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_danger_zone),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )

                TextButton(
                    onClick = { confirmClear = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_clear_credentials),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (confirmClear && onClearCredentials != null) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_title)) },
            text = { Text(stringResource(R.string.settings_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearCredentials()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_confirm),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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
        onClearCredentials = viewModel::clearCredentials,
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
                isConfigured = true,
                result = ConnectionTestResult(false, "Cloudflare Access did not accept this service token."),
            ),
            onBack = {},
            onBaseUrlChange = {}, onClientIdChange = {}, onClientSecretChange = {},
            onToggleSecretVisible = {}, onTest = {}, onSave = {},
            onClearCredentials = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
