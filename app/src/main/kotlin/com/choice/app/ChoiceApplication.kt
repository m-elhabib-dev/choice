package com.choice.app

import android.app.Application
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import com.choice.app.locale.LocaleApplier
import com.choice.app.locale.LanguagePreferenceStore
import com.choice.app.locale.SharedPreferencesLanguageStore
import com.choice.app.locale.resolveLanguageTag
import com.choice.app.locale.systemLanguageTags
import com.choice.app.ui.theme.AppThemeState
import com.choice.app.ui.theme.SharedPreferencesThemeStore
import com.choice.app.ui.theme.ThemePreferenceStore
import com.choice.app.ui.theme.resolveThemeFlavor
import com.choice.app.widget.WidgetRefreshCoordinator

class ChoiceApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    lateinit var languagePreferenceStore: LanguagePreferenceStore
        private set

    lateinit var themePreferenceStore: ThemePreferenceStore
        private set

    lateinit var themeState: AppThemeState
        private set

    lateinit var widgetRefreshCoordinator: WidgetRefreshCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        languagePreferenceStore = SharedPreferencesLanguageStore(this)
        LocaleApplier.apply(
            this,
            resolveLanguageTag(languagePreferenceStore.read(), systemLanguageTags()),
        )
        themePreferenceStore = SharedPreferencesThemeStore(this)
        themeState = AppThemeState(resolveThemeFlavor(themePreferenceStore.read()))
        appContainer = AppContainer(this)
        widgetRefreshCoordinator = WidgetRefreshCoordinator(this, appContainer.coinRepository)
        widgetRefreshCoordinator.start()
    }
}

class AppContainer(context: Application) {
    private val database = ChoiceDatabase.create(context)
    val coinRepository = CoinRepositoryImpl(database.coinDao())
}
