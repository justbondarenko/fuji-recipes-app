package dev.bondarenko.fujirecipes.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Where the library lives and how to get past the gate — FEAT-001 T-08.
 *
 * Stored in app-private DataStore with `android:allowBackup="false"` on the manifest, which
 * is the bound `steering/architecture.md` §5 states plainly: app-private storage keeps this
 * away from other apps on the device, and nothing here defends against a rooted phone.
 * `androidx.security:security-crypto` is not used because Jetpack deprecated it.
 */
// ponytail: plain app-private DataStore for the service-token secret. Wrap with a Keystore
// key if the threat model ever includes a rooted device or physical seizure.
data class ConnectionConfig(
    val baseUrl: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
) {
    /**
     * Whether a request would even be worth making.
     *
     * All three or none: a base URL with no credentials reaches an Access-gated deployment
     * and gets a 403 every time, which is a confusing way to say "not set up yet".
     */
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()
}

private val Context.connectionDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "connection")

class ConnectionSettings(private val context: Context) {

    private object Keys {
        val BaseUrl = stringPreferencesKey("connection.base_url")
        val ClientId = stringPreferencesKey("connection.client_id")
        val ClientSecret = stringPreferencesKey("connection.client_secret")
    }

    val config: Flow<ConnectionConfig> = context.connectionDataStore.data.map { prefs ->
        ConnectionConfig(
            baseUrl = prefs[Keys.BaseUrl].orEmpty(),
            clientId = prefs[Keys.ClientId].orEmpty(),
            clientSecret = prefs[Keys.ClientSecret].orEmpty(),
        )
    }

    suspend fun current(): ConnectionConfig = config.first()

    suspend fun save(baseUrl: String, clientId: String, clientSecret: String) {
        context.connectionDataStore.edit { prefs ->
            prefs[Keys.BaseUrl] = normaliseBaseUrl(baseUrl)
            prefs[Keys.ClientId] = clientId.trim()
            prefs[Keys.ClientSecret] = clientSecret.trim()
        }
    }

    companion object {
        /**
         * The base URL, in the one form the rest of the app can rely on.
         *
         * Trailing slashes go, because every call site appends `/api/...` and
         * `https://host//api/recipes` is a 404 on some proxies and fine on others — a
         * difference nobody should have to discover. A bare host gets `https://`, since a
         * typed-in `recipes.example.com` means the site, and plain HTTP to an
         * Access-protected origin would not work anyway.
         */
        fun normaliseBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return ""
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }
    }
}
