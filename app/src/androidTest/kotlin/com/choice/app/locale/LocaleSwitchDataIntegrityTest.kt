package com.choice.app.locale

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import com.choice.app.ui.settings.SettingsViewModel
import com.choice.app.ui.theme.AppThemeState
import com.choice.app.ui.theme.SharedPreferencesThemeStore
import com.choice.app.ui.theme.resolveThemeFlavor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SC-004 / FR-025: a language change — via the in-app selector *and* via a simulated device
 * system-language change — must never read, write, or invalidate a Room row.
 *
 * data-model.md's guiding invariant ("language is presentation; it never touches persisted user
 * content") is structural here — the language preference lives in a wholly separate
 * `SharedPreferences` file with no reference to [com.choice.app.data.CoinRepository] — but this
 * test proves it end to end rather than by inspection alone.
 */
@RunWith(AndroidJUnit4::class)
class LocaleSwitchDataIntegrityTest {

    private lateinit var context: Context
    private lateinit var database: ChoiceDatabase
    private lateinit var repository: CoinRepositoryImpl
    private lateinit var store: LanguagePreferenceStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChoiceDatabase::class.java).build()
        repository = CoinRepositoryImpl(database.coinDao())
        store = SharedPreferencesLanguageStore(context)
        store.write(null)
    }

    @After
    fun teardown() {
        database.close()
        store.write(null)
        LocaleApplier.apply(context, SupportedLanguages.ENGLISH)
    }

    @Test
    fun switchingLanguage_leavesCoinsChoicesDecisionsAndFavoritesUnchanged() = runBlocking {
        val arabicNamedCoinId = repository.saveCoin(null, "الإفطار", listOf("فول", "بيض"))
        val englishNamedCoinId = repository.saveCoin(null, "Lunch", listOf("Rice", "Pasta"))
        repository.setFavorite(arabicNamedCoinId, true)

        val firstChoiceId = repository.observeCoin(arabicNamedCoinId).first()!!.choices.first().id
        repository.recordDecision(arabicNamedCoinId, firstChoiceId, "فول")

        val beforeArabicCoin = repository.observeCoin(arabicNamedCoinId).first()
        val beforeEnglishCoin = repository.observeCoin(englishNamedCoinId).first()
        val beforeHistory = repository.observeDecisionHistory(arabicNamedCoinId).first()

        // 1. Switch via the in-app selector (SettingsViewModel wraps the exact write+apply path
        //    the real Settings screen uses). The theme store/state are unused by this test's
        //    assertions — they exist only because SettingsViewModel now also owns theme selection.
        val themeStore = SharedPreferencesThemeStore(context)
        val settingsViewModel = SettingsViewModel(
            context = context,
            languagePreferenceStore = store,
            themePreferenceStore = themeStore,
            themeState = AppThemeState(resolveThemeFlavor(themeStore.read())),
            systemLanguageTags = { listOf("en-US") },
        )
        settingsViewModel.selectLanguage(SupportedLanguages.ARABIC)

        // 2. Simulate a device system-language change too — resolving against a different
        //    system list while the stored override is untouched (FR-026: the override wins).
        LocaleApplier.apply(
            context,
            resolveLanguageTag(store.read(), systemLanguageTags = listOf("fr-FR")),
        )

        val afterArabicCoin = repository.observeCoin(arabicNamedCoinId).first()
        val afterEnglishCoin = repository.observeCoin(englishNamedCoinId).first()
        val afterHistory = repository.observeDecisionHistory(arabicNamedCoinId).first()

        assertEquals(beforeArabicCoin, afterArabicCoin)
        assertEquals(beforeEnglishCoin, afterEnglishCoin)
        assertEquals(beforeHistory, afterHistory)
    }
}
