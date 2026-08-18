package dev.bondarenko.fujirecipes.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.bondarenko.fujirecipes.data.library.SortDirection
import dev.bondarenko.fujirecipes.data.library.SortId
import dev.bondarenko.fujirecipes.data.library.StoredLibraryView
import dev.bondarenko.fujirecipes.data.library.defaultDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The remembered view — FEAT-001 T-09.
 *
 * Filters and sort survive a restart; **the search text does not**. That asymmetry is the
 * point, not an omission: search is a way of getting to one recipe, and a phone reopened
 * tomorrow showing three of forty recipes because of a word typed yesterday reads as a
 * library that lost things.
 *
 * Reading goes through [StoredLibraryView.repair], so a value this build did not write can
 * never put the list into a state the UI cannot explain. The repair itself is pure and
 * tested directly; this class only moves bytes.
 */
private val Context.viewDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "library_view")

class ViewPreferences(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.viewDataStore)

    private object Keys {
        val Sort = stringPreferencesKey("view.sort")
        val SortDirection = stringPreferencesKey("view.sort_direction")
        val MinRating = intPreferencesKey("view.min_rating")
        val MaxRating = intPreferencesKey("view.max_rating")
        val Tags = stringSetPreferencesKey("view.tags")
        val Simulations = stringSetPreferencesKey("view.simulations")
    }

    val view: Flow<StoredLibraryView> = dataStore.data.map { prefs ->
        StoredLibraryView.repair(
            tags = prefs[Keys.Tags],
            minRating = prefs[Keys.MinRating],
            maxRating = prefs[Keys.MaxRating],
            simulations = prefs[Keys.Simulations],
            sort = prefs[Keys.Sort],
            sortDirection = prefs[Keys.SortDirection],
        )
    }

    suspend fun save(view: StoredLibraryView) {
        dataStore.edit { prefs ->
            prefs[Keys.Sort] = view.sort.id
            prefs[Keys.SortDirection] = view.sortDirection.id
            prefs[Keys.MinRating] = view.filters.minRating
            prefs[Keys.MaxRating] = view.filters.maxRating
            prefs[Keys.Tags] = view.filters.tags.toSet()
            prefs[Keys.Simulations] = view.filters.simulations.toSet()
        }
    }

    suspend fun saveSort(sort: SortId) {
        dataStore.edit { prefs ->
            prefs[Keys.Sort] = sort.id
            // When changing sort parameter alone, update to its default direction if no direction exists
            if (prefs[Keys.SortDirection] == null) {
                prefs[Keys.SortDirection] = sort.defaultDirection.id
            }
        }
    }

    suspend fun saveSort(sort: SortId, direction: SortDirection) {
        dataStore.edit { prefs ->
            prefs[Keys.Sort] = sort.id
            prefs[Keys.SortDirection] = direction.id
        }
    }

    suspend fun saveSortDirection(direction: SortDirection) {
        dataStore.edit { prefs ->
            prefs[Keys.SortDirection] = direction.id
        }
    }
}

