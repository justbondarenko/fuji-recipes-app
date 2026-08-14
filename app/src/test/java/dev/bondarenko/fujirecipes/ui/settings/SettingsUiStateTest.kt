package dev.bondarenko.fujirecipes.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The connection summary — FEAT-004 T-05.
 *
 * The assertion that matters is the last one. The secret is not in this state at all: not
 * masked, not truncated, absent. A value that never enters the state cannot be leaked by a
 * careless `toString()` in a log line or a crash report.
 */
class SettingsUiStateTest {

    @Test
    fun `an unconfigured app says so`() {
        assertEquals("Not configured", SettingsUiState().summary())
    }

    @Test
    fun `a configured app names its host and confirms credentials`() {
        val state = SettingsUiState(host = "recipes.example.com", isConfigured = true)
        assertEquals("recipes.example.com · credentials set", state.summary())
    }

    @Test
    fun `a host with no credentials is distinguished from no host at all`() {
        // Different remedies: one needs a token, the other needs setting up from scratch.
        val state = SettingsUiState(host = "recipes.example.com", isConfigured = false)
        assertEquals("recipes.example.com · no credentials", state.summary())
    }

    @Test
    fun `the summary never contains a secret because the state has no field for one`() {
        val fields = SettingsUiState::class.java.declaredFields.map { it.name.lowercase() }

        assertTrue(fields.isNotEmpty())
        assertFalse(fields.any { "secret" in it || "token" in it || "clientid" in it })
    }
}
