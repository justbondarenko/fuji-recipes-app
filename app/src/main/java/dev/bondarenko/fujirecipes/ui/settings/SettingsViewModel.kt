package dev.bondarenko.fujirecipes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.settings.ConnectionSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What settings shows about the connection.
 *
 * **The secret is not in here.** Not masked, not truncated — absent. A summary line needs
 * the host and whether credentials exist, and a value that never enters the state cannot be
 * leaked by a careless `toString()` in a log or a crash report.
 */
data class SettingsUiState(
    val host: String = "",
    val isConfigured: Boolean = false,
) {
    /** Reads as a status line: where it points, and whether it can get in. */
    fun summary(): String = when {
        host.isBlank() -> "Not configured"
        isConfigured -> "$host · credentials set"
        else -> "$host · no credentials"
    }
}

class SettingsViewModel(
    private val connectionSettings: ConnectionSettings,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = connectionSettings.config
        .map { config ->
            SettingsUiState(
                // Just the host: the scheme is noise in a summary line.
                host = config.baseUrl.substringAfter("://"),
                isConfigured = config.isConfigured,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun clearCredentials() {
        viewModelScope.launch { connectionSettings.clear() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container.connectionSettings) as T
            }
    }
}
