package dev.bondarenko.fujirecipes.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Display mode for recipe details screen.
 */
enum class RecipeViewMode(val id: String) {
    GRID("grid"),
    LIST("list");

    companion object {
        fun fromId(id: String?): RecipeViewMode =
            entries.firstOrNull { it.id == id } ?: GRID
    }
}

/**
 * Stored UI settings preserved across app sessions.
 */
data class StoredUiPreferences(
    val recipeViewMode: RecipeViewMode = RecipeViewMode.GRID,
    val showPhotos: Boolean = true,
    val showTags: Boolean = true,
    val showFilmSimulation: Boolean = true,
    val showRating: Boolean = true,
)

private val Context.uiDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "ui_preferences")

class UiPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.uiDataStore)

    private object Keys {
        val RecipeViewMode = stringPreferencesKey("ui.recipe_view_mode")
        val ShowPhotos = booleanPreferencesKey("ui.show_photos")
        val ShowTags = booleanPreferencesKey("ui.show_tags")
        val ShowFilmSimulation = booleanPreferencesKey("ui.show_film_simulation")
        val ShowRating = booleanPreferencesKey("ui.show_rating")
    }

    val preferences: Flow<StoredUiPreferences> = dataStore.data.map { prefs ->
        StoredUiPreferences(
            recipeViewMode = RecipeViewMode.fromId(prefs[Keys.RecipeViewMode]),
            showPhotos = prefs[Keys.ShowPhotos] ?: true,
            showTags = prefs[Keys.ShowTags] ?: true,
            showFilmSimulation = prefs[Keys.ShowFilmSimulation] ?: true,
            showRating = prefs[Keys.ShowRating] ?: true,
        )
    }

    suspend fun setRecipeViewMode(mode: RecipeViewMode) {
        dataStore.edit { prefs ->
            prefs[Keys.RecipeViewMode] = mode.id
        }
    }

    suspend fun setShowPhotos(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ShowPhotos] = show
        }
    }

    suspend fun setShowTags(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ShowTags] = show
        }
    }

    suspend fun setShowFilmSimulation(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ShowFilmSimulation] = show
        }
    }

    suspend fun setShowRating(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ShowRating] = show
        }
    }
}
