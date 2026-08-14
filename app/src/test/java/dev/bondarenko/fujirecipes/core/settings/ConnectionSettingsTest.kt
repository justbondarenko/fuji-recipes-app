package dev.bondarenko.fujirecipes.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Base-URL normalisation and the all-or-nothing rule — FEAT-001 T-08.
 *
 * Both exist so that a typo on the connection screen fails in a way the user can act on,
 * rather than producing a 404 on some proxies and a working app on others.
 */
class ConnectionSettingsTest {

    @Test
    fun `a trailing slash is removed, because every call site appends one`() {
        assertEquals(
            "https://recipes.example.com",
            ConnectionSettings.normaliseBaseUrl("https://recipes.example.com/"),
        )
        assertEquals(
            "https://recipes.example.com",
            ConnectionSettings.normaliseBaseUrl("https://recipes.example.com///"),
        )
    }

    @Test
    fun `a bare host is assumed to be https`() {
        // Plain HTTP to an Access-protected origin would not work anyway.
        assertEquals(
            "https://recipes.example.com",
            ConnectionSettings.normaliseBaseUrl("recipes.example.com"),
        )
    }

    @Test
    fun `an explicit scheme is left alone`() {
        assertEquals(
            "http://localhost:3000",
            ConnectionSettings.normaliseBaseUrl("http://localhost:3000"),
        )
    }

    @Test
    fun `surrounding whitespace from a paste is trimmed`() {
        assertEquals(
            "https://recipes.example.com",
            ConnectionSettings.normaliseBaseUrl("  https://recipes.example.com  "),
        )
    }

    @Test
    fun `empty stays empty rather than becoming a scheme`() {
        assertEquals("", ConnectionSettings.normaliseBaseUrl(""))
        assertEquals("", ConnectionSettings.normaliseBaseUrl("   "))
    }

    @Test
    fun `a configuration needs all three parts`() {
        assertTrue(ConnectionConfig("https://x", "id", "secret").isConfigured)
        assertFalse(ConnectionConfig("https://x", "id", "").isConfigured)
        assertFalse(ConnectionConfig("https://x", "", "secret").isConfigured)
        assertFalse(ConnectionConfig("", "id", "secret").isConfigured)
    }
}
