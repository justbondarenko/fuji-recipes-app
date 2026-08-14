package dev.bondarenko.fujirecipes.core

import android.content.Context

/**
 * The object graph, by hand.
 *
 * Everything long-lived hangs off here and is created lazily, so a screen that never needs
 * the HTTP client never builds one. `FujiRecipesApp` owns the single instance; composables
 * reach it through `LocalAppContainer` rather than through a static, so a test or a preview
 * can provide a different one.
 *
 * It is deliberately empty at FEAT-000. Each field arrives with the feature that needs it:
 * FEAT-001 adds the connection settings, the API client, the snapshot cache and the recipe
 * repository. Adding them before then would be scaffolding for later, and later can
 * scaffold for itself.
 */
class AppContainer(private val context: Context) {
    val applicationContext: Context = context.applicationContext
}
