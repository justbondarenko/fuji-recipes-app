package dev.bondarenko.fujirecipes

import android.app.Application
import dev.bondarenko.fujirecipes.core.AppContainer

/**
 * The Application, and the one place the object graph is built.
 *
 * **No dependency-injection framework.** The graph is a library store, a DataStore-backed
 * preference holder, the camera connection and a repository — four objects with no cycles
 * and no scoping beyond "one of each". A DI framework here buys an annotation processor, a
 * Gradle plugin and a Kotlin/KSP version alignment to keep track of, in exchange for wiring
 * that fits on one screen (`coding-standards.md` P8).
 */
class FujiRecipesApp : Application() {

    /** Built lazily so a test can replace it before anything touches it. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
