package dev.bondarenko.fujirecipes.core.net

/**
 * Everything that can go wrong on the way to the library — FEAT-001 T-04.
 *
 * Mirrors the server's `ErrorBody` union in
 * `fuji-recipes-book/src/server/utils/errors.ts`, plus the two failures that never reach a
 * route at all ([Network], [Malformed]).
 *
 * **Why a sealed type and not a message string.** `coding-standards.md` P5 forbids
 * rendering a 403, a 503 and a socket timeout the same way, because they have three
 * different remedies: fix your token, wait for the server, check your signal. A string
 * makes that distinction unrepresentable at the point where the UI has to draw it.
 *
 * Each case carries the server's own `message` where there is one. It is written to be read
 * by a person — `accessMisconfigured` names the missing variables in its prose — so the UI
 * shows it rather than inventing a replacement.
 */
sealed interface ApiError {

    /** The human-readable line the server sent, or null for the transport failures. */
    val message: String?

    /**
     * 403 — Cloudflare Access refused the request.
     *
     * The credentials in this app are wrong, missing or expired. It is the **only** 403 the
     * API produces: no route makes an authorization decision, because there is nobody to
     * make one about (`constitution.md` P1). So this always means "go fix the token".
     */
    data class Forbidden(override val message: String?) : ApiError

    /**
     * 503 `access_unconfigured` — the *server* has no Access gate configured.
     *
     * Deliberately not folded into [StorageUnavailable]: the database is fine, and a banner
     * saying otherwise sends the reader to check something that is working. The message
     * names the missing Worker secrets.
     */
    data class AccessUnconfigured(override val message: String?) : ApiError

    /** 503 `storage_unavailable` — D1 is unreachable. Retryable, and nothing is lost. */
    data class StorageUnavailable(override val message: String?) : ApiError

    /** 404, with the id or the path that was not found. */
    data class NotFound(override val message: String?, val id: String?) : ApiError

    /** 422, with the dotted paths that failed — e.g. `settings.clarity`. */
    data class ValidationFailed(
        override val message: String?,
        val fields: List<FieldError>,
    ) : ApiError

    /** 409 — a client-supplied id was already taken. */
    data class IdExists(override val message: String?, val id: String?) : ApiError

    /** 400 — the request itself was malformed. A bug in this client if it ever appears. */
    data class BadRequest(override val message: String?) : ApiError

    /** 405, with the verbs the route does accept. Also a client bug. */
    data class MethodNotAllowed(
        override val message: String?,
        val allowed: List<String>,
    ) : ApiError

    /** 500 — carries the id that ties this response to a line in the server's log. */
    data class Internal(override val message: String?, val requestId: String?) : ApiError

    /**
     * The request never got an answer: no signal, DNS, TLS, timeout.
     *
     * Distinct from every code above, because it is the one failure where the cached
     * snapshot is the right thing to show instead of an error screen.
     */
    data class Network(override val message: String?) : ApiError

    /**
     * An answer arrived and could not be understood.
     *
     * Kept apart from [Internal] because it means something is wrong with *this build's*
     * expectations, or with a proxy in the way — a captive portal serving an HTML login
     * page to a JSON request lands here, and calling that a server error would be wrong.
     */
    data class Malformed(override val message: String?) : ApiError

    /**
     * A status or an `error` code this build has never heard of.
     *
     * The server may grow a code before this client learns it. Carrying the raw values is
     * what lets the UI still say something specific (P8) instead of "unknown error".
     */
    data class Unexpected(
        override val message: String?,
        val status: Int,
        val code: String?,
    ) : ApiError
}

/** One entry of a 422's `fields` array. */
data class FieldError(val path: String, val message: String)
