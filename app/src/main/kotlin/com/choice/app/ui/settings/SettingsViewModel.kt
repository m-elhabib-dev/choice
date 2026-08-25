package com.choice.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.choice.app.locale.LanguagePreferenceStore
import com.choice.app.locale.LocaleApplier
import com.choice.app.locale.resolveLanguageTag
import com.choice.app.locale.systemLanguageTags
import com.choice.app.ui.theme.AppThemeState
import com.choice.app.ui.theme.ThemeFlavor
import com.choice.app.ui.theme.ThemePreferenceStore
import com.choice.app.ui.theme.resolveThemeFlavor

/**
 * Wraps [LanguagePreferenceStore] + [LocaleApplier], and [ThemePreferenceStore] + [AppThemeState],
 * behind the single actions the Settings screen needs — contracts/navigation-contract.md §2
 * "Selection flow" and contracts/theme-contract.md §4.
 *
 * Deliberately holds no reference to `CoinRepository` or any Glance store: neither a language
 * change nor a theme change may ever touch either (FR-025/SC-004/SN-3 for language;
 * FR-012/SC-006/TA-3 for theme) — that guarantee is structural here, not enforced by a runtime
 * check.
 *
 * [refreshWidgets] is wired to the real `WidgetRefreshCoordinator.refreshAll()` at the
 * `SettingsViewModelFactory` call site in `MainActivity` (US5/T056/T058, reused for theme by
 * T016). It defaults to a no-op here so this class has no compile-time dependency on the widget
 * package.
 */
class SettingsViewModel(
    private val context: Context,
    private val languagePreferenceStore: LanguagePreferenceStore,
    private val themePreferenceStore: ThemePreferenceStore,
    private val themeState: AppThemeState,
    private val systemLanguageTags: () -> List<String> = ::systemLanguageTags,
    private val refreshWidgets: () -> Unit = {},
) : ViewModel() {

    /** The currently active option — `null`/"en"/"ar" — for the selector's initial selection. */
    fun currentLanguageTag(): String? = languagePreferenceStore.read()

    /**
     * Applies [tag] ( `null` = "System default") as the user's explicit language override.
     *
     * Re-selecting the already-active option is a no-op (SN-4): nothing is written, applied, or
     * refreshed. Returns `true` when a real change was made, so the caller knows whether an
     * API 26–32 activity recreate is needed (contracts/localization-contract.md §4).
     */
    fun selectLanguage(tag: String?): Boolean {
        if (languagePreferenceStore.read() == tag) return false

        languagePreferenceStore.write(tag)
        val resolved = resolveLanguageTag(tag, systemLanguageTags())
        LocaleApplier.apply(context, resolved)
        refreshWidgets()
        return true
    }

    /** The currently active flavor — for the Appearance section's initial selection (mirrors [currentLanguageTag]). */
    fun currentThemeFlavor(): ThemeFlavor = resolveThemeFlavor(themePreferenceStore.read())

    /**
     * Applies [flavor] as the user's chosen appearance theme.
     *
     * Re-selecting the already-active flavor is a no-op (TA-2, Principle I): nothing is written,
     * applied, or refreshed. Reads [themePreferenceStore] rather than [themeState] to decide
     * "current" (AS-3) — the ViewModel only ever writes to [themeState], never reads it to drive
     * UI decisions — contracts/theme-contract.md §4.
     */
    fun selectTheme(flavor: ThemeFlavor) {
        if (resolveThemeFlavor(themePreferenceStore.read()) == flavor) return

        themePreferenceStore.write(flavor.id)
        themeState.set(flavor)
        refreshWidgets()
    }
}

class SettingsViewModelFactory(
    context: Context,
    private val languagePreferenceStore: LanguagePreferenceStore,
    private val themePreferenceStore: ThemePreferenceStore,
    private val themeState: AppThemeState,
    private val refreshWidgets: () -> Unit = {},
) : ViewModelProvider.Factory {
    // Application context only — a ViewModel must never hold an Activity reference.
    private val appContext: Context = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SettingsViewModel(
            appContext,
            languagePreferenceStore,
            themePreferenceStore,
            themeState,
            refreshWidgets = refreshWidgets,
        ) as T
    }
}
