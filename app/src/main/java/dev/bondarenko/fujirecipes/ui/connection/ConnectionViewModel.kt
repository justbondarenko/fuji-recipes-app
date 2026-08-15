package dev.bondarenko.fujirecipes.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.net.ApiClient
import dev.bondarenko.fujirecipes.core.net.ApiResult
import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import dev.bondarenko.fujirecipes.core.settings.ConnectionSettings
import dev.bondarenko.fujirecipes.ui.common.ApiErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionTestResult(val isSuccess: Boolean, val message: String)

data class ConnectionUiState(
    val baseUrl: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val secretVisible: Boolean = false,
    val isTesting: Boolean = false,
    val result: ConnectionTestResult? = null,
) {
    /** All three or none — a half-set credential only ever produces a confusing 403. */
    val isComplete: Boolean
        get() = baseUrl.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()
}

/**
 * FEAT-001 T-12.
 *
 * [test] is the interesting part, and it takes a [ApiClient] factory rather than the
 * container's shared client: the whole point is to try credentials that have **not** been
 * saved yet, and the shared client reads the saved ones.
 */
class ConnectionViewModel(
    private val settings: ConnectionSettings,
    private val clientFor: (ConnectionConfig) -> ApiClient,
    private val messages: ApiErrorMessages,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionUiState())
    val state: StateFlow<ConnectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.current()
            _state.update {
                it.copy(
                    baseUrl = current.baseUrl,
                    clientId = current.clientId,
                    clientSecret = current.clientSecret,
                )
            }
        }
    }

    fun onBaseUrlChange(value: String) = _state.update { it.copy(baseUrl = value, result = null) }
    fun onClientIdChange(value: String) = _state.update { it.copy(clientId = value, result = null) }
    fun onClientSecretChange(value: String) =
        _state.update { it.copy(clientSecret = value, result = null) }

    fun onToggleSecretVisible() = _state.update { it.copy(secretVisible = !it.secretVisible) }

    /**
     * Try the credentials as typed.
     *
     * **Against `GET /api/recipes`, never `GET /api/health`.** Health sits in the server's
     * `UNGATED_PATHS`, so it answers 200 to an anonymous request — a test against it would
     * call a revoked token healthy, which is precisely the failure this button exists to
     * catch.
     */
    fun onTest() {
        val current = _state.value
        if (!current.isComplete) {
            _state.update { it.copy(result = ConnectionTestResult(false, messages.incomplete)) }
            return
        }

        _state.update { it.copy(isTesting = true, result = null) }

        viewModelScope.launch {
            val config = ConnectionConfig(
                baseUrl = ConnectionSettings.normaliseBaseUrl(current.baseUrl),
                clientId = current.clientId.trim(),
                clientSecret = current.clientSecret.trim(),
            )

            val result = when (val response = clientFor(config).listRecipes()) {
                is ApiResult.Success ->
                    ConnectionTestResult(true, messages.success(response.value.recipes.size))
                is ApiResult.Failure ->
                    ConnectionTestResult(false, messages.forError(response.error))
            }

            _state.update { it.copy(isTesting = false, result = result) }
        }
    }

    fun onSave(onSaved: () -> Unit) {
        val current = _state.value
        viewModelScope.launch {
            settings.save(current.baseUrl, current.clientId, current.clientSecret)
            onSaved()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConnectionViewModel(
                        settings = container.connectionSettings,
                        clientFor = { config -> ApiClient(config = { config }) },
                        messages = ApiErrorMessages.from(container.applicationContext),
                    ) as T
            }
    }
}
