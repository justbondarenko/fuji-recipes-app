package dev.bondarenko.fujirecipes.core

import android.content.Context
import dev.bondarenko.fujirecipes.camera.CameraController
import dev.bondarenko.fujirecipes.core.settings.ViewPreferences
import dev.bondarenko.fujirecipes.core.store.ImageStore
import dev.bondarenko.fujirecipes.core.store.LibraryStore
import dev.bondarenko.fujirecipes.data.repo.LocalRecipeRepository
import dev.bondarenko.fujirecipes.data.repo.RecipeRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * The object graph, by hand.
 *
 * Everything long-lived hangs off here and is created lazily, so a screen that never needs
 * the store never opens a file. `FujiRecipesApp` owns the single instance.
 *
 * No DI framework: four objects, no cycles, one scope. Hilt would cost a Gradle plugin, an
 * annotation processor and a Kotlin/KSP version alignment for wiring that fits on a screen
 * (`steering/tech-stack.md` §2).
 */
class AppContainer(context: Context) {

    val applicationContext: Context = context.applicationContext

    val viewPreferences: ViewPreferences by lazy { ViewPreferences(applicationContext) }

    /**
     * The library file, in app-private storage.
     *
     * `filesDir` rather than `cacheDir`, and the difference is the whole point: this is the
     * only copy of the recipes, so the OS must never be free to reclaim it
     * (`steering/architecture.md` §4).
     */
    val libraryStore: LibraryStore by lazy {
        LibraryStore(File(applicationContext.filesDir, LibraryStore.FILE_NAME))
    }

    /**
     * Store for recipe sample/reference photos.
     */
    val imageStore: ImageStore by lazy {
        ImageStore(
            directory = File(applicationContext.filesDir, ImageStore.DIRECTORY_NAME),
            contentResolver = applicationContext.contentResolver,
        )
    }

    /**
     * The camera connection, above the nav graph so it survives navigation
     * (`architecture.md` §3). Lazy like everything else: a launch that never touches the
     * camera never registers a USB receiver.
     */
    val cameraController: CameraController by lazy { CameraController(applicationContext) }

    /**
     * The library.
     *
     * `DemoRecipeRepository` was deleted in this merge rather than carried across. It existed
     * to stand in for the server in a debug build with nothing configured — the case that
     * parked you on the setup form with nothing to look at. There is no setup form and no
     * server now, and an in-memory fixture whose writes vanish on process death would
     * contradict the one thing this repository is for.
     */
    val recipeRepository: RecipeRepository by lazy {
        LocalRecipeRepository(
            store = libraryStore,
            now = ::isoNow,
            newId = ::newId,
        )
    }

    companion object {
        /**
         * ISO-8601 UTC with milliseconds — the format every timestamp in this platform uses.
         * Kept because the export files carry it, and a file this app writes has to be one
         * the web client can read back.
         */
        fun isoNow(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(System.currentTimeMillis())

        /**
         * A recipe id.
         *
         * A random UUID rather than a counter: ids from two devices end up in the same
         * library the moment someone imports an export, and a counter would collide there.
         */
        fun newId(): String = UUID.randomUUID().toString()
    }
}
