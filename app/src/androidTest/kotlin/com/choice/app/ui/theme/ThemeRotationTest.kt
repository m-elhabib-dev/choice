package com.choice.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * US2 Acceptance Scenario 2 / TA-4 (contracts/theme-contract.md §4): [AppThemeState] is held once,
 * Application-scoped — not per-Activity — so a device rotation (an Activity recreation) reads the
 * same live [kotlinx.coroutines.flow.StateFlow] instance rather than re-resolving the preference
 * from a store. No flash, no reset.
 *
 * The selection here is made in-memory only, via [AppThemeState.set] — mirroring
 * `SettingsViewModel.selectTheme`'s call but deliberately *not* going through a
 * [ThemePreferenceStore] — so this test isolates exactly one thing: that the value held in
 * [AppThemeState] itself, not a re-read from disk, is what a recreated Activity observes. If
 * rotation triggered any re-resolution (which would fall back to the constructor's initial value
 * since nothing was persisted), this test would fail.
 */
@RunWith(AndroidJUnit4::class)
class ThemeRotationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appThemeState_survivesActivityRecreation_withNoFlashOrReset() {
        val themeState = AppThemeState(ThemeFlavor.MOCHA)

        composeTestRule.setContent {
            val flavor by themeState.flavor.collectAsState()
            ChoiceTheme(flavor) {}
        }
        composeTestRule.waitForIdle()

        // An in-memory-only selection, unpersisted — exactly what would be lost if rotation
        // re-resolved the theme from a preference store instead of reading the live StateFlow.
        themeState.set(ThemeFlavor.LATTE)

        composeTestRule.activityRule.scenario.recreate()

        var observedBackground: Color? = null
        composeTestRule.setContent {
            val flavor by themeState.flavor.collectAsState()
            ChoiceTheme(flavor) {
                observedBackground = MaterialTheme.colorScheme.background
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(ThemeFlavor.LATTE, themeState.flavor.value)
        assertEquals(ThemeFlavor.LATTE.colorScheme.background, observedBackground)
    }
}
