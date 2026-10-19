package dev.bondarenko.fujirecipes.ui.cameraphotos

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import dev.bondarenko.fujirecipes.ui.theme.FujiTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraPhotosProgressAndSelectionTest {
    @get:Rule val compose = createComposeRule()
    private val hasProgress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    @Test
    fun cardReadingUsesDeterminateProgressAndUpdatesWithTheReadCount() {
        val current = mutableStateOf(3)
        compose.setContent {
            FujiTheme { CameraPhotosLoadingState(current = current.value, total = 10) }
        }
        compose.onNode(hasProgress)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.3f, 0f..1f))
        compose.runOnIdle { current.value = 7 }
        compose.waitForIdle()
        compose.onNode(hasProgress)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.7f, 0f..1f))
    }

    @Test
    fun beforeTheTotalArrivesProgressRemainsAtZero() {
        compose.setContent {
            FujiTheme { CameraPhotosLoadingState(current = 0, total = 0) }
        }
        compose.onNode(hasProgress)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun dayHeaderExposesPartialAndFullSelectionAndDisablesDuringDownload() {
        val selected = mutableStateOf(0)
        val enabled = mutableStateOf(true)
        var toggles = 0
        compose.setContent {
            FujiTheme {
                CameraPhotoDayHeader(
                    date = LocalDate.of(2026, 9, 9), selectedCount = selected.value,
                    totalCount = 3, enabled = enabled.value, onToggle = { toggles++ },
                )
            }
        }
        val header = compose.onNode(isToggleable())
        header.assertIsOff().performClick()
        compose.runOnIdle {
            assertEquals(1, toggles)
            selected.value = 1
        }
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        compose.runOnIdle { selected.value = 3 }
        header.assertIsOn()
        compose.runOnIdle { enabled.value = false }
        header.assertIsNotEnabled()
    }
}
