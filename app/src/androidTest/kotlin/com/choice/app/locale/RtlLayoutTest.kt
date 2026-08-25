package com.choice.app.locale

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import com.choice.app.ui.coinedit.CoinEditScreen
import com.choice.app.ui.coinedit.CoinEditViewModel
import com.choice.app.ui.coinflip.CoinFlipScreen
import com.choice.app.ui.coinflip.CoinFlipViewModel
import com.choice.app.ui.history.HistoryScreen
import com.choice.app.ui.history.HistoryViewModel
import com.choice.app.ui.main.MainScreen
import com.choice.app.ui.main.MainViewModel
import com.choice.app.ui.settings.SettingsScreen
import com.choice.app.ui.settings.SettingsViewModel
import com.choice.app.ui.statistics.StatisticsScreen
import com.choice.app.ui.statistics.StatisticsViewModel
import com.choice.app.ui.theme.AppThemeState
import com.choice.app.ui.theme.SharedPreferencesThemeStore
import com.choice.app.ui.theme.resolveThemeFlavor
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SC-002 / FR-005 / FR-007: layout direction must be derived by the platform from the Arabic
 * locale, never set manually (LD-1), and every screen must render correctly at a narrow width.
 *
 * [ArabicActivity] forces Arabic on its base [Context] at every API level — independent of
 * [LocaleApplier]'s API-33+/26-32 branching (already covered by [LocaleApplierTest]) — so this
 * test isolates the one thing it needs to prove: that Compose resolves [LocalLayoutDirection] to
 * RTL for these five screens when the active locale is Arabic.
 *
 * The ~320dp-wide [Box] around each screen exercises the narrow-width pass alongside the
 * direction assertion; full manual verification of visual clipping/overflow at that width is
 * covered by quickstart.md's US4 block (T049), the same split already used for T025/T034/T041.
 */
@RunWith(AndroidJUnit4::class)
class RtlLayoutTest {

    class ArabicActivity : ComponentActivity() {
        override fun attachBaseContext(newBase: Context) {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(Locale.forLanguageTag("ar"))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ArabicActivity>()

    private lateinit var database: ChoiceDatabase
    private lateinit var repository: CoinRepositoryImpl
    private var coinId: Long = -1L

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            composeTestRule.activity,
            ChoiceDatabase::class.java,
        ).build()
        repository = CoinRepositoryImpl(database.coinDao())
        coinId = runBlocking {
            repository.saveCoin(null, "الإفطار", listOf("فول", "بيض"))
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    /** Renders [content] at a ~320dp-wide viewport and reports the resolved layout direction. */
    private fun renderNarrowAndCaptureDirection(content: @Composable () -> Unit): LayoutDirection {
        var resolved: LayoutDirection? = null
        composeTestRule.setContent {
            resolved = LocalLayoutDirection.current
            Box(modifier = Modifier.width(320.dp)) {
                content()
            }
        }
        composeTestRule.waitForIdle()
        return resolved!!
    }

    @Test
    fun mainScreen_resolvesRtlUnderArabic() {
        val direction = renderNarrowAndCaptureDirection {
            MainScreen(
                viewModel = MainViewModel(repository),
                onCoinClick = {},
                onCreateCoin = {},
                onEditCoin = {},
                onSettings = {},
            )
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun historyScreen_resolvesRtlUnderArabic() {
        val direction = renderNarrowAndCaptureDirection {
            HistoryScreen(
                viewModel = HistoryViewModel(repository, coinId),
                onBack = {},
                onStatistics = {},
            )
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun statisticsScreen_resolvesRtlUnderArabic() {
        val direction = renderNarrowAndCaptureDirection {
            StatisticsScreen(
                viewModel = StatisticsViewModel(repository, coinId),
                onBack = {},
            )
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun coinEditScreen_resolvesRtlUnderArabic() {
        val direction = renderNarrowAndCaptureDirection {
            CoinEditScreen(
                viewModel = CoinEditViewModel(repository, coinId),
                onBack = {},
            )
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun coinFlipScreen_resolvesRtlUnderArabic() {
        val direction = renderNarrowAndCaptureDirection {
            CoinFlipScreen(
                viewModel = CoinFlipViewModel(repository, coinId),
                onBack = {},
                onEditCoin = {},
                onSettings = {},
                onHistory = {},
            )
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }

    /** SC-008: the "Appearance" section added by 005 must mirror correctly, same as every other row here. */
    @Test
    fun settingsScreen_appearanceSection_resolvesRtlUnderArabic() {
        val activity = composeTestRule.activity
        val themeStore = SharedPreferencesThemeStore(activity)
        val themeState = AppThemeState(resolveThemeFlavor(themeStore.read()))
        val viewModel = SettingsViewModel(
            context = activity,
            languagePreferenceStore = SharedPreferencesLanguageStore(activity),
            themePreferenceStore = themeStore,
            themeState = themeState,
        )
        val direction = renderNarrowAndCaptureDirection {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        assertEquals(LayoutDirection.Rtl, direction)
    }
}
