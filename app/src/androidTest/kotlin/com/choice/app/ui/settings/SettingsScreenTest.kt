package com.choice.app.ui.settings

import android.content.Context
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * contracts/navigation-contract.md §2 guarantees SN-2 and SN-4.
 *
 * Uses an in-memory [FakeLanguagePreferenceStore] rather than the real
 * `SharedPreferencesLanguageStore` so SN-2 ("survives restart") is proven the same way the rest of
 * this codebase proves persistence-shaped behaviour: by asserting the *store* holds the value,
 * then reading it back through a freshly constructed [SettingsViewModel] — exactly what a real
 * process restart would do (read the persisted tag before the first screen renders).
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var store: FakeLanguagePreferenceStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = FakeLanguagePreferenceStore()
    }

    @After
    fun teardown() {
        // Locale changes made through LocaleApplier are process-global; reset so other
        // instrumented tests in this suite don't inherit an Arabic locale.
        LocaleApplier.apply(context, SupportedLanguages.ENGLISH)
    }

    @Test
    fun selectingLanguage_persistsChoice() {
        val viewModel = SettingsViewModel(context, store, systemLanguageTags = { listOf("en-US") })
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_language_arabic))
            .performClick()

        assertEquals(SupportedLanguages.ARABIC, store.read())

        // SN-2: a freshly constructed ViewModel — as onCreate would build after a restart —
        // reads the persisted choice back.
        val afterRestart = SettingsViewModel(context, store, systemLanguageTags = { listOf("en-US") })
        assertEquals(SupportedLanguages.ARABIC, afterRestart.currentLanguageTag())
    }

    @Test
    fun reselectingActiveLanguage_isNoOp() {
        store.write(SupportedLanguages.ARABIC)
        var refreshCount = 0
        val viewModel = SettingsViewModel(
            context = context,
            languagePreferenceStore = store,
            systemLanguageTags = { listOf("en-US") },
            refreshWidgets = { refreshCount++ },
        )

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
}

private class FakeLanguagePreferenceStore : LanguagePreferenceStore {
    @Volatile
    private var tag: String? = null

    override fun read(): String? = tag

    override fun write(tag: String?) {
        this.tag = tag
    }
}
