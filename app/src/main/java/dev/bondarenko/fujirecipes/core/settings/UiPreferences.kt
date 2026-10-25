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
 * Destinations pinned independently to the bottom bar (4) and sidebar (8).
 * Unpinned destinations appear in More for the current layout.
 */
enum class NavDestinationId(val id: String) {
    LIBRARY("library"),
    ANALYZE("analyze"),
    LAB("lab"),
    PHOTOS("photos"),
    RAW_EDITOR("raw_editor"),
    CLEANUP("cleanup"),
    IMPORT_CAMERA("import_camera"),
    IMPORT_FILE("import_file"),
    EXPORT("export");

    companion object {
        val DEFAULT_PINNED: List<NavDestinationId> = listOf(LIBRARY, ANALYZE, LAB, PHOTOS)
        const val MAX_PINNED = 4
        const val MAX_RAIL_PINNED = 8

        fun fromId(id: String?): NavDestinationId? =
            entries.firstOrNull { it.id == id }

        fun fromStoredString(raw: String?, maxPinned: Int = MAX_PINNED): List<NavDestinationId> {
            if (raw.isNullOrBlank()) return DEFAULT_PINNED
            val parsed = raw.split(",")
                .mapNotNull { fromId(it.trim()) }
                .distinct()
                .take(maxPinned)
            return parsed.ifEmpty { DEFAULT_PINNED }
        }
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
    val pinnedNavIds: List<NavDestinationId> = NavDestinationId.DEFAULT_PINNED,
    val railPinnedNavIds: List<NavDestinationId> = pinnedNavIds,
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
        val PinnedNavIds = stringPreferencesKey("ui.pinned_nav_ids")
        val RailPinnedNavIds = stringPreferencesKey("ui.rail_pinned_nav_ids")
    }

    val preferences: Flow<StoredUiPreferences> = dataStore.data.map { prefs ->
        StoredUiPreferences(
            recipeViewMode = RecipeViewMode.fromId(prefs[Keys.RecipeViewMode]),
            showPhotos = prefs[Keys.ShowPhotos] ?: true,
            showTags = prefs[Keys.ShowTags] ?: true,
            showFilmSimulation = prefs[Keys.ShowFilmSimulation] ?: true,
            showRating = prefs[Keys.ShowRating] ?: true,
            pinnedNavIds = NavDestinationId.fromStoredString(prefs[Keys.PinnedNavIds]),
            railPinnedNavIds = NavDestinationId.fromStoredString(
                prefs[Keys.RailPinnedNavIds] ?: prefs[Keys.PinnedNavIds], NavDestinationId.MAX_RAIL_PINNED,
            ),
        )
    }

    suspend fun setPinnedNavIds(ids: List<NavDestinationId>, forRail: Boolean = false) {
        val normalized = ids.distinct().take(if (forRail) NavDestinationId.MAX_RAIL_PINNED else NavDestinationId.MAX_PINNED)
        if (normalized.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[if (forRail) Keys.RailPinnedNavIds else Keys.PinnedNavIds] = normalized.joinToString(",") { it.id }
        }
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
