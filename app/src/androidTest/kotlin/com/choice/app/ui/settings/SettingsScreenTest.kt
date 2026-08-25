package com.choice.app.ui.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choice.app.R
import com.choice.app.locale.LanguagePreferenceStore
import com.choice.app.locale.LocaleApplier
import com.choice.app.locale.SupportedLanguages
import com.choice.app.ui.theme.AppThemeState
import com.choice.app.ui.theme.ChoiceTheme
import com.choice.app.ui.theme.ThemeFlavor
import com.choice.app.ui.theme.ThemePreferenceStore
import com.choice.app.ui.theme.resolveThemeFlavor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * contracts/navigation-contract.md §2 guarantees SN-2 and SN-4; contracts/theme-contract.md §4/§7
 * guarantee TA-1/TA-2 for theme selection.
 *
 * Uses in-memory [FakeLanguagePreferenceStore]/[FakeThemePreferenceStore] rather than the real
 * `SharedPreferences`-backed stores so persistence-shaped behaviour is proven the same way the
 * rest of this codebase proves it: by asserting the *store* holds the value, then reading it back
 * through a freshly constructed [SettingsViewModel] — exactly what a real process restart would do
 * (read the persisted value before the first screen renders).
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var store: FakeLanguagePreferenceStore
    private lateinit var themeStore: FakeThemePreferenceStore
    private lateinit var themeState: AppThemeState

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = FakeLanguagePreferenceStore()
        themeStore = FakeThemePreferenceStore()
        themeState = AppThemeState(resolveThemeFlavor(themeStore.read()))
    }

    @After
    fun teardown() {
        // Locale changes made through LocaleApplier are process-global; reset so other
        // instrumented tests in this suite don't inherit an Arabic locale.
        LocaleApplier.apply(context, SupportedLanguages.ENGLISH)
    }

    private fun viewModel(refreshWidgets: () -> Unit = {}) = SettingsViewModel(
        context = context,
        languagePreferenceStore = store,
        themePreferenceStore = themeStore,
        themeState = themeState,
        systemLanguageTags = { listOf("en-US") },
        refreshWidgets = refreshWidgets,
    )

    @Test
    fun selectingLanguage_persistsChoice() {
        val viewModel = viewModel()
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_language_arabic))
            .performClick()

        assertEquals(SupportedLanguages.ARABIC, store.read())

        // SN-2: a freshly constructed ViewModel — as onCreate would build after a restart —
        // reads the persisted choice back.
        val afterRestart = viewModel()
        assertEquals(SupportedLanguages.ARABIC, afterRestart.currentLanguageTag())
    }

    @Test
    fun reselectingActiveLanguage_isNoOp() {
        store.write(SupportedLanguages.ARABIC)
        var refreshCount = 0
        val viewModel = viewModel(refreshWidgets = { refreshCount++ })

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_language_arabic))
            .assertIsSelected()
            .performClick()

        assertEquals("re-selecting the active language must not refresh widgets", 0, refreshCount)
        assertEquals(SupportedLanguages.ARABIC, store.read())
    }

    @Test
    fun selectingTheme_updatesColorSchemeImmediately() {
        val viewModel = viewModel()
        var observedBackground: Color? = null

        composeTestRule.setContent {
            val flavor by themeState.flavor.collectAsState()
            ChoiceTheme(flavor) {
                observedBackground = MaterialTheme.colorScheme.background
                SettingsScreen(viewModel = viewModel, onBack = {})
            }
        }

        val initialBackground = observedBackground

        // Starting flavor is MOCHA (dark); LATTE is light, so its background must differ.
        composeTestRule
            .onNodeWithText(context.getString(R.string.theme_latte_name))
            .performClick()

        composeTestRule.waitForIdle()

        assertEquals(ThemeFlavor.LATTE.id, themeStore.read())
        assertNotEquals(
            "selecting a theme must recompose MaterialTheme.colorScheme within the same frame",
            initialBackground,
            observedBackground,
        )
    }

    @Test
    fun selectingTheme_persistsAcrossRestart() {
        val viewModel = viewModel()
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.theme_frappe_name))
            .performClick()

        assertEquals(ThemeFlavor.FRAPPE.id, themeStore.read())

        // US2 Acceptance Scenario 1: a freshly constructed AppThemeState/ViewModel — as
        // ChoiceApplication.onCreate would build after a real process restart — reads the
        // persisted flavor back, exactly mirroring selectingLanguage_persistsChoice above.
        val afterRestartState = AppThemeState(resolveThemeFlavor(themeStore.read()))
        val afterRestart = SettingsViewModel(
            context = context,
            languagePreferenceStore = store,
            themePreferenceStore = themeStore,
            themeState = afterRestartState,
            systemLanguageTags = { listOf("en-US") },
        )
        assertEquals(ThemeFlavor.FRAPPE, afterRestart.currentThemeFlavor())
        assertEquals(ThemeFlavor.FRAPPE, afterRestartState.flavor.value)
    }

    @Test
    fun changingLanguage_leavesThemeSelectionUnchanged() {
        val viewModel = viewModel()
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.theme_macchiato_name))
            .performClick()
        assertEquals(ThemeFlavor.MACCHIATO.id, themeStore.read())

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_language_arabic))
            .performClick()

        assertEquals(SupportedLanguages.ARABIC, store.read())
        // FR-012: a language change must never touch the independent theme preference/state.
        assertEquals(
            "changing the language must not affect the theme selection",
            ThemeFlavor.MACCHIATO.id,
            themeStore.read(),
        )
        assertEquals(ThemeFlavor.MACCHIATO, themeState.flavor.value)
    }

    @Test
    fun reselectingActiveTheme_isNoOp() {
        themeStore.write(ThemeFlavor.LATTE.id)
        themeState.set(ThemeFlavor.LATTE)
        var refreshCount = 0
        val viewModel = viewModel(refreshWidgets = { refreshCount++ })

        composeTestRule.setContent {
            val flavor by themeState.flavor.collectAsState()
            ChoiceTheme(flavor) {
                SettingsScreen(viewModel = viewModel, onBack = {})
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.theme_latte_name))
            .assertIsSelected()
            .performClick()

        assertEquals("re-selecting the active theme must not refresh widgets", 0, refreshCount)
        assertEquals(ThemeFlavor.LATTE.id, themeStore.read())
    }
}

private class FakeLanguagePreferenceStore : LanguagePreferenceStore {
    @Volatile
    private var tag: String? = null

    override fun read(): String? = tag

    override fun write(tag: String?) {
        this.tag = tag
    }
}

private class FakeThemePreferenceStore : ThemePreferenceStore {
    @Volatile
    private var id: String? = null

    override fun read(): String? = id

    override fun write(id: String) {
        this.id = id
    }
}
