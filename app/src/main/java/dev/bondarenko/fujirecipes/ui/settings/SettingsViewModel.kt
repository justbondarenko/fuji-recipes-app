package dev.bondarenko.fujirecipes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.bondarenko.fujirecipes.core.AppContainer
import dev.bondarenko.fujirecipes.core.settings.RecipeViewMode
import dev.bondarenko.fujirecipes.core.settings.StoredUiPreferences
import dev.bondarenko.fujirecipes.core.settings.UiPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UiPreferences,
) : ViewModel() {

    val state: StateFlow<StoredUiPreferences> = preferences.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StoredUiPreferences(),
    )

    fun onSelectRecipeViewMode(mode: RecipeViewMode) {
        viewModelScope.launch {
            preferences.setRecipeViewMode(mode)
        }
    }

    fun onToggleShowPhotos(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowPhotos(show)
        }
    }

    fun onToggleShowTags(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowTags(show)
        }
    }

    fun onToggleShowFilmSimulation(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowFilmSimulation(show)
        }
    }

    fun onToggleShowRating(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowRating(show)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container.uiPreferences) as T
            }
    }
}
