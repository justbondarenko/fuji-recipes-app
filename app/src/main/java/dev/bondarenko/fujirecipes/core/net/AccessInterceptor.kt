package dev.bondarenko.fujirecipes.core.net

import dev.bondarenko.fujirecipes.core.settings.ConnectionConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * The Cloudflare Access service token, on every request — FEAT-001 T-06.
 *
 * Access validates these two headers at the edge, mints the JWT the Worker expects, and
 * `fuji-recipes-book/src/server/middleware/auth.ts` verifies it unchanged. That is the whole
 * reason no server change was needed for a native client (`steering/architecture.md` §5).
 *
 * The config is read through a lambda rather than captured, so a token saved on the
 * connection screen takes effect on the next request instead of the next process.
 */
class AccessInterceptor(private val config: () -> ConnectionConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val current = config()

        /**
         * **Both headers or neither.**
         *
         * A half-set pair is not a partial credential, it is an invalid one, and sending it
         * gets a 403 that reads exactly like a wrong token — sending nothing gets the same
         * 403 and leaves no doubt about what to go and fix.
         */
        val request = if (current.clientId.isNotBlank() && current.clientSecret.isNotBlank()) {
            chain.request().newBuilder()
                .header(CLIENT_ID_HEADER, current.clientId)
                .header(CLIENT_SECRET_HEADER, current.clientSecret)
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }

    companion object {
        const val CLIENT_ID_HEADER = "CF-Access-Client-Id"
        const val CLIENT_SECRET_HEADER = "CF-Access-Client-Secret"
    }
}
