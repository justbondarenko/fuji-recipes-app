package dev.bondarenko.fujirecipes.core.result

/**
 * The result of asking the library to do something.
 *
 * No exceptions cross the repository seam (`steering/architecture.md` §3). A failure that
 * arrives as a thrown `IOException` is something a caller can forget to handle and the
 * compiler will not mention it; a `Failure` in the return type is something the caller has
 * to open.
 *
 * This replaced an HTTP result type when the library moved onto the device. The shape did
 * not need to change — what changed is that there are now three ways to fail instead of
 * eleven, because a store on the same filesystem cannot refuse your credentials, be
 * unreachable, or answer with something other than what it was asked.
 */
sealed interface LibraryResult<out T> {
    data class Success<T>(val value: T) : LibraryResult<T>
    data class Failure(val error: LibraryError) : LibraryResult<Nothing>
}

/**
 * Everything that can go wrong between a screen and the recipes on disk.
 *
 * A sealed type rather than a message string, for the reason `coding-standards.md` P5 gives:
 * a full disk, a corrupt file and a recipe that fails validation have three different
 * remedies, and a string makes that distinction unrepresentable where the UI has to draw it.
 */
sealed interface LibraryError {

    /** A line written to be read by a person, or null when there is nothing to add. */
    val message: String?

    /**
     * The library could not be written.
     *
     * **Nothing changed.** Writes replace the whole file through a temporary, so a failure
     * leaves the previous library exactly as it was — which is what makes it safe to tell
     * the user to simply try again.
     */
    data class Storage(override val message: String?) : LibraryError

    /**
     * A library file is on the device and could not be read.
     *
     * Distinct from an empty library and from [Storage], because it is the one failure where
     * the app is holding recipes it cannot show. Never rendered as "no recipes yet".
     */
    data class Unreadable(override val message: String?) : LibraryError

    /** The recipe named by the action is not in the library. */
    data class NotFound(override val message: String?, val id: String?) : LibraryError

    /** A recipe that does not satisfy the field rules, with the paths that failed. */
    data class Invalid(
        override val message: String?,
        val fields: List<FieldProblem> = emptyList(),
    ) : LibraryError
}

/** One failing field, by its dotted path — e.g. `settings.clarity`. */
data class FieldProblem(val path: String, val message: String)
