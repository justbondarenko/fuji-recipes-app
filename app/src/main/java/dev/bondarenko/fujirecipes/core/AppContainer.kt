package dev.bondarenko.fujirecipes.core

import android.content.Context
import dev.bondarenko.fujirecipes.core.cache.SnapshotCache
import dev.bondarenko.fujirecipes.core.net.ApiClient
import dev.bondarenko.fujirecipes.core.settings.ConnectionSettings
import dev.bondarenko.fujirecipes.core.settings.ViewPreferences
import dev.bondarenko.fujirecipes.data.repo.NetworkRecipeRepository
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The object graph, by hand.
 *
 * Everything long-lived hangs off here and is created lazily, so a screen that never needs
 * the HTTP client never builds one. `FujiRecipesApp` owns the single instance.
 *
 * No DI framework: seven objects, no cycles, one scope. Hilt would cost a Gradle plugin, an
 * annotation processor and a Kotlin/KSP version alignment for wiring that fits on a screen
 * (`steering/tech-stack.md` §2).
 */
class AppContainer(context: Context) {

    val applicationContext: Context = context.applicationContext

    val connectionSettings: ConnectionSettings by lazy { ConnectionSettings(applicationContext) }

    val viewPreferences: ViewPreferences by lazy { ViewPreferences(applicationContext) }

    /**
     * The config, read synchronously for the interceptor.
     *
     * OkHttp interceptors are not suspending, so the token has to be readable from a plain
     * function. `runBlocking` on a DataStore read that is already in memory after the first
     * call is cheap, and it happens on OkHttp's own thread rather than the main one.
     */
    // ponytail: runBlocking per request against an in-memory DataStore. Cache the config in
    // a StateFlow if it ever shows up in a trace.
    val apiClient: ApiClient by lazy {
        ApiClient(config = { runBlocking { connectionSettings.current() } })
    }

    val snapshotCache: SnapshotCache by lazy {
        SnapshotCache(File(applicationContext.filesDir, SnapshotCache.FILE_NAME))
    }

    val recipeRepository: RecipeRepository by lazy {
        NetworkRecipeRepository(
            api = apiClient,
            cache = snapshotCache,
            config = { connectionSettings.current() },
            now = ::isoNow,
        )
    }

    companion object {
        /**
         * ISO-8601 UTC with milliseconds — the format every timestamp in this platform uses
         * (`contracts.md`). Only ever a display value here: it stamps the snapshot so the
         * offline banner can say when the copy was taken.
         */
        fun isoNow(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(System.currentTimeMillis())
    }
}
