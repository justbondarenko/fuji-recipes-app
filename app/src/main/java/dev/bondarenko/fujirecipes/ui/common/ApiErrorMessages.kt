package dev.bondarenko.fujirecipes.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.net.ApiError

/**
 * The per-`ApiError` copy, in one place.
 *
 * Behind an interface so a ViewModel can be tested on the JVM without a `Context`, and
 * shared because the connection screen and the editor must not describe the same failure
 * two different ways (`coding-standards.md` P5).
 */
interface ApiErrorMessages {
    val incomplete: String
    fun success(count: Int): String
    fun forError(error: ApiError): String

    companion object {
        fun from(context: android.content.Context): ApiErrorMessages =
            object : ApiErrorMessages {
                override val incomplete: String
                    get() = context.getString(R.string.connection_incomplete)

                override fun success(count: Int): String =
                    context.getString(R.string.connection_ok, count)

                override fun forError(error: ApiError): String =
                    when (error) {
                        is ApiError.Forbidden ->
                            error.message ?: context.getString(R.string.error_forbidden_body)
                        is ApiError.AccessUnconfigured ->
                            error.message ?: context.getString(R.string.error_access_unconfigured_body)
                        is ApiError.StorageUnavailable -> context.getString(R.string.error_storage_body)
                        is ApiError.Network -> context.getString(R.string.error_network_body)
                        is ApiError.Malformed -> context.getString(R.string.error_malformed_body)
                        is ApiError.Internal -> error.requestId
                            ?.let { context.getString(R.string.error_internal_body_with_id, it) }
                            ?: context.getString(R.string.error_internal_body)
                        else -> error.message ?: context.getString(R.string.error_unexpected_body)
                    }
            }
    }
}

/** The same copy, for a composable that has no ViewModel to ask. */
@Composable
fun errorMessageFor(error: ApiError): String =
    ApiErrorMessages.from(LocalContext.current).forError(error)
