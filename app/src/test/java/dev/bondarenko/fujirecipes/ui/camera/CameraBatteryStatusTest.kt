package dev.bondarenko.fujirecipes.ui.camera

import dev.bondarenko.fujirecipes.camera.plan.BatteryLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraBatteryStatusTest {
    @Test
    fun `charge bands include the documented boundaries`() {
        assertEquals(BatteryChargeBand.LOW, band(1, 10))
        assertEquals(BatteryChargeBand.MEDIUM, band(2, 10))
        assertEquals(BatteryChargeBand.MEDIUM, band(5, 10))
        assertEquals(BatteryChargeBand.HIGH, band(6, 10))
    }

    @Test
    fun `charge bands scale to a percentage reading`() {
        assertEquals(BatteryChargeBand.LOW, band(19, 100))
        assertEquals(BatteryChargeBand.MEDIUM, band(20, 100))
        assertEquals(BatteryChargeBand.MEDIUM, band(50, 100))
        assertEquals(BatteryChargeBand.HIGH, band(51, 100))
    }

    private fun band(value: Int, max: Int) = batteryChargeBand(
        BatteryLevel(value = value, max = max, maxDeclared = true),
    )
}
