package dev.bondarenko.fujirecipes.core.net

/**
 * The result of an API call — FEAT-001 T-05.
 *
 * No exceptions cross the repository seam (`steering/architecture.md` §3). A failure that
 * arrives as a thrown `IOException` is something a caller can forget to handle and the
 * compiler will not mention it; a `Failure` in the return type is something the caller has
 * to open.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

val <T> ApiResult<T>.valueOrNull: T?
    get() = (this as? ApiResult.Success)?.value

val ApiResult<*>.errorOrNull: ApiError?
    get() = (this as? ApiResult.Failure)?.error
