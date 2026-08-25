package com.choice.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.choice.app.locale.LanguagePreferenceStore
import com.choice.app.locale.LocaleApplier
import com.choice.app.locale.resolveLanguageTag
import com.choice.app.locale.systemLanguageTags

/**
 * Wraps [LanguagePreferenceStore] + [LocaleApplier] behind the single action the Settings screen
 * needs — contracts/navigation-contract.md §2 "Selection flow".
 *
 * Deliberately holds no reference to `CoinRepository` or any Glance store: a language change must
 * never touch either (FR-025, SC-004, SN-3) — that guarantee is structural here, not enforced by
 * a runtime check.
 *
 * [refreshWidgets] is wired to the real `WidgetRefreshCoordinator.refreshAll()` at the
 * `SettingsViewModelFactory` call site in `MainActivity` (US5/T056/T058). It defaults to a no-op
 * here so this class has no compile-time dependency on the widget package.
 */
class SettingsViewModel(
    private val context: Context,
    private val languagePreferenceStore: LanguagePreferenceStore,
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
}

class SettingsViewModelFactory(
    context: Context,
    private val languagePreferenceStore: LanguagePreferenceStore,
    private val refreshWidgets: () -> Unit = {},
) : ViewModelProvider.Factory {
    // Application context only — a ViewModel must never hold an Activity reference.
    private val appContext: Context = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SettingsViewModel(appContext, languagePreferenceStore, refreshWidgets = refreshWidgets) as T
    }
}
