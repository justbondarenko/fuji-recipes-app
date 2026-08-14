package dev.bondarenko.fujirecipes.ui.library

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The "last written" line, in one place.
 *
 * Port of `fuji-recipes-book/src/utils/last-written.ts`. The slot picker will want the same
 * line per slot in FEAT-004, and a date formatted in two places is two formats the first
 * time either changes.
 *
 * **Absent rather than "Never"** when the recipe has never been written: an absent line
 * scans faster down a long list, and the slot picker is where absence gets a label.
 */
object LastWritten {

    fun line(slot: Int?, at: String?, locale: Locale = Locale.getDefault()): String? {
        if (slot == null) return null
        val label = "C$slot"
        val date = at?.let { formatDate(it, locale) } ?: return "Written to $label"
        return "$label · $date"
    }

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

    /**
     * `12 Aug 2026`, or null if the timestamp cannot be read.
     *
     * A null on a bad date rather than a substituted "now": the server refuses to invent a
     * timestamp it cannot read (`recipe-rows.ts`), and inventing one here would be the same
     * mistake one layer up. The caller degrades to "Written to C3", which is still true.
     */
    private fun formatDate(iso: String, locale: Locale): String? = runCatching {
        val parsed = isoParser().parse(iso) ?: return null
        SimpleDateFormat("d MMM yyyy", locale).format(parsed)
    }.getOrNull()
}
