package dev.bondarenko.fujirecipes.data.library

import dev.bondarenko.fujirecipes.data.fields.FilmSimulations

/**
 * How the library is being looked at — the pure half.
 *
 * **No Compose, no Android imports, on purpose** (`coding-standards.md` P7). The decisions
 * live here where a test can drive them with plain lists; the reactivity lives in the
 * ViewModel and the storage in `ViewPreferences`. It is the port of
 * `fuji-recipes-book/src/utils/library-view.ts`, and keeping it pure is what makes parity
 * with the web client a checked claim rather than an asserted one.
 *
 * FEAT-001 T-09 needs the types and the repair rules; the search/filter/sort pipeline
 * arrives in T-14 and lands in this same file.
 *
 * It sits under `data/` rather than `ui/` so that `core/settings` can depend on it without
 * a layer inversion.
 */

/**
 * The sorts the selector offers, in its order.
 *
 * Exactly three, matching the web client's `SORT_OPTIONS`. `manual` and `written` exist as
 * comparators there too but are **not offered**, and adding them here would be a divergence
 * rather than a feature.
 */
enum class SortId(val id: String) {
    NAME("name"),
    RATING("rating"),
    UPDATED("updated");

    companion object {
        val Default = NAME
        fun fromId(id: String?): SortId? = entries.firstOrNull { it.id == id }
    }
}

data class LibraryFilters(
    /**
     * **All** selected tags must be present. A recipe carries many tags, so requiring each
     * one narrows as the user adds them, which is what a filter drawer implies.
     */
    val tags: List<String> = emptyList(),
    /** `0` means any, and includes unrated recipes. */
    val minRating: Int = 0,
    val simulations: List<String> = emptyList(),
) {
    /** How many axes are set — the number on the filter button's badge. Axes, not values. */
    val activeCount: Int
        get() = (if (tags.isNotEmpty()) 1 else 0) +
            (if (minRating > 0) 1 else 0) +
            (if (simulations.isNotEmpty()) 1 else 0)

    val isEmpty: Boolean get() = activeCount == 0
}

/** What survives the session. Filters and sort only — never the search text. */
data class StoredLibraryView(
    val filters: LibraryFilters = LibraryFilters(),
    val sort: SortId = SortId.Default,
) {
    companion object {
        const val MIN_RATING = 0
        const val MAX_RATING = 5

        /**
         * A stored view, made valid field by field rather than thrown away.
         *
         * Everything here defends against a value this build did not write — an older
         * format, a hand-edited entry, a sort since removed from the selector. Each field
         * falls back on its own, because losing a remembered sort over an unrecognised tag
         * is a worse outcome than either problem.
         *
         * The four rules, matching `parseStoredView` in the web client:
         *
         * - **Unknown film simulation → dropped.** It can never match, and offering it in
         *   the UI would name a simulation that does not exist.
         * - **Rating out of range → clamped**, not discarded. The intent ("only good ones")
         *   is legible even when the number is not.
         * - **Unknown sort → the default.** Keeping it would leave the control blank with
         *   no way to read what the list is ordered by.
         * - **Unrecognised tag → kept.** Deliberately the odd one out: the library has not
         *   loaded when this runs, and a tag that is momentarily absent — mid-import on the
         *   other client — is not a reason to silently drop a filter. It narrows to the
         *   "no recipes match" panel, which says so and offers the way out.
         */
        fun repair(
            tags: Collection<String>?,
            minRating: Int?,
            simulations: Collection<String>?,
            sort: String?,
        ): StoredLibraryView = StoredLibraryView(
            filters = LibraryFilters(
                tags = tags.orEmpty().filter { it.isNotBlank() }.distinct(),
                minRating = (minRating ?: MIN_RATING).coerceIn(MIN_RATING, MAX_RATING),
                simulations = simulations.orEmpty()
                    .filter { FilmSimulations.byId(it) != null }
                    .distinct(),
            ),
            sort = SortId.fromId(sort) ?: SortId.Default,
        )
    }
}
