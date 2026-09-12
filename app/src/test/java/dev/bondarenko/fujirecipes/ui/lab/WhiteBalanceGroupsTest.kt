package dev.bondarenko.fujirecipes.ui.lab

import dev.bondarenko.fujirecipes.data.fields.RecipeFields
import kotlin.test.Test
import kotlin.test.assertEquals

class WhiteBalanceGroupsTest {

    @Test
    fun `auto, fluorescent and custom variants each get their own group, order kept`() {
        val groups = RecipeFields.WHITE_BALANCE_OPTIONS.whiteBalanceGroups().map { g -> g.map { it.id } }

        assertEquals(
            listOf(
                listOf("auto", "auto-white-priority", "auto-ambience-priority"),
                listOf("daylight", "shade"),
                listOf("fluorescent-1", "fluorescent-2", "fluorescent-3"),
                listOf("incandescent", "underwater", "color-temp"),
                listOf("custom-1", "custom-2", "custom-3"),
            ),
            groups,
        )
    }
}
