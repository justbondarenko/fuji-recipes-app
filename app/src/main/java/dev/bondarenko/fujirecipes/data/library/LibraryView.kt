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

/** Everything the pipeline needs of a recipe. Structural, so a test can build one. */
interface ViewableRecipe {
    val name: String
    val rating: Int
    val tags: List<String>
    val sortKey: Double
    val createdAt: String
    val updatedAt: String
    val filmSimulationId: String?
}

/**
 * The whole view: what is typed, what is filtered, how it is ordered.
 *
 * `search` is here but is never persisted — see `ViewPreferences`.
 */
data class LibraryView(
    val search: String = "",
    val filters: LibraryFilters = LibraryFilters(),
    val sort: SortId = SortId.Default,
)

private fun fold(value: String): String = value.trim().lowercase()

/** Whitespace-separated terms; the empty search matches everything. */
fun searchTerms(search: String): List<String> =
    fold(search).split(Regex("\\s+")).filter { it.isNotEmpty() }

/**
 * Free text over name and tags.
 *
 * **Every** term has to match somewhere, so typing narrows monotonically; a term may land
 * on the name or on any one tag, which is what makes "acros street" useful. Substring
 * rather than prefix: recipes get named "Kodachrome 64", and the memorable part is not
 * always the first word.
 */
fun matchesSearch(recipe: ViewableRecipe, search: String): Boolean {
    val terms = searchTerms(search)
    if (terms.isEmpty()) return true

    val name = fold(recipe.name)
    val tags = recipe.tags.map(::fold)

    return terms.all { term -> name.contains(term) || tags.any { it.contains(term) } }
}

/** Every axis that is set must agree. */
fun matchesFilters(recipe: ViewableRecipe, filters: LibraryFilters): Boolean {
    if (recipe.rating < filters.minRating) return false

    if (filters.simulations.isNotEmpty() && recipe.filmSimulationId !in filters.simulations) {
        return false
    }

    // Conjunctive: a recipe carries many tags, so requiring each selected one narrows as
    // the user adds them, which is what a filter drawer implies.
    if (filters.tags.isNotEmpty() && !recipe.tags.containsAll(filters.tags)) return false

    return true
}

/**
 * Manual order — `sortKey` ascending, ties broken by `createdAt` ascending.
 *
 * Ported from `fuji-recipes-book/shared/ordering.ts`. Never offered as a sort (the web
 * client does not offer it either), but every comparator below falls back to it, so equal
 * ratings and same-day edits keep the arrangement the user chose rather than depending on
 * the response's incidental order.
 *
 * `createdAt` is compared as a string: ISO-8601 UTC with a fixed width sorts
 * lexicographically in the same order it sorts chronologically, so parsing would add a
 * failure mode and buy nothing.
 */
val ManualOrder: Comparator<ViewableRecipe> =
    compareBy<ViewableRecipe> { it.sortKey }.thenBy { it.createdAt }

/**
 * Names the way people read them: "Portra 2" before "Portra 10", case and accents ignored.
 *
 * The web client gets this from `localeCompare(…, { numeric: true, sensitivity: "base" })`.
 * Java's `Collator` supplies the accent- and case-insensitive half at PRIMARY strength but
 * has no numeric mode, so the digit runs are compared as numbers here and everything else
 * is handed to the collator.
 */
fun compareNamesNaturally(left: String, right: String): Int {
    var i = 0
    var j = 0

    while (i < left.length && j < right.length) {
        val leftDigit = left[i].isDigit()
        val rightDigit = right[j].isDigit()

        if (leftDigit && rightDigit) {
            val leftEnd = left.indexOfFirstNonDigit(i)
            val rightEnd = right.indexOfFirstNonDigit(j)
            // Compared as numbers, so leading zeros do not change the value and "10" is
            // greater than "2" rather than lexicographically smaller.
            val comparison = left.substring(i, leftEnd).trimStart('0').padTo(
                right.substring(j, rightEnd).trimStart('0'),
            )
            if (comparison != 0) return comparison
            i = leftEnd
            j = rightEnd
        } else {
            val leftEnd = if (leftDigit) left.indexOfFirstNonDigit(i) else left.indexOfFirstDigit(i)
            val rightEnd = if (rightDigit) right.indexOfFirstNonDigit(j) else right.indexOfFirstDigit(j)
            val comparison = baseCollator.compare(
                left.substring(i, leftEnd),
                right.substring(j, rightEnd),
            )
            if (comparison != 0) return comparison
            i = leftEnd
            j = rightEnd
        }
    }

    return (left.length - i).compareTo(right.length - j)
}

private val baseCollator: java.text.Collator =
    java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }

private fun String.indexOfFirstNonDigit(from: Int): Int {
    var index = from
    while (index < length && this[index].isDigit()) index++
    return index
}

private fun String.indexOfFirstDigit(from: Int): Int {
    var index = from
    while (index < length && !this[index].isDigit()) index++
    return index
}

/** Numeric comparison of two digit strings that may be longer than a `Long`. */
private fun String.padTo(other: String): Int =
    if (length != other.length) length.compareTo(other.length) else compareTo(other)

/**
 * The comparator for a sort, each falling back to manual order.
 *
 * `written` and `manual` are deliberately absent: the selector does not offer them, and
 * adding them here would be a divergence from the web client rather than a feature.
 */
fun comparatorFor(sort: SortId): Comparator<ViewableRecipe> = when (sort) {
    SortId.NAME -> Comparator<ViewableRecipe> { a, b -> compareNamesNaturally(a.name, b.name) }
        .then(ManualOrder)

    SortId.RATING -> compareByDescending<ViewableRecipe> { it.rating }.then(ManualOrder)

    SortId.UPDATED -> compareByDescending<ViewableRecipe> { it.updatedAt }.then(ManualOrder)
}

/**
 * Search, then filter, then sort — the whole list pipeline.
 *
 * Returns a new list and never touches the input: the caller holds the repository's
 * recipes, and sorting them in place would rewrite manual order as a side effect of
 * choosing "Name A–Z".
 */
fun <T : ViewableRecipe> selectRecipes(recipes: List<T>, view: LibraryView): List<T> =
    recipes
        .filter { matchesSearch(it, view.search) && matchesFilters(it, view.filters) }
        .sortedWith(comparatorFor(view.sort))
