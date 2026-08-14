package dev.bondarenko.fujirecipes.ui.library

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Timestamps, formatted the one way this app formats them.
 *
 * Only the library's own freshness is left here. The "last written to slot C3" line this
 * file used to own is gone: the Android client does not track when a recipe reached a
 * camera (see `data/model/Recipe.kt`).
 */
object Timestamps {

    /**
     * `14 Aug 2026, 10:43`, for the library's own freshness.
     *
     * Date *and* time, unlike a slot write: a refresh is usually minutes old, and a bare
     * date on something from ten seconds ago reads as staler than it is.
     */
    fun updatedAt(iso: String, locale: Locale = Locale.getDefault()): String? = runCatching {
        val parsed = isoParser().parse(iso) ?: return null
        SimpleDateFormat("d MMM yyyy, HH:mm", locale).format(parsed)
    }.getOrNull()

    private fun isoParser() =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

}
