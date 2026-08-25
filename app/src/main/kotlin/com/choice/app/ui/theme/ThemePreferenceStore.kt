package com.choice.app.ui.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user's selected [ThemeFlavor.id].
 *
 * Structurally identical to `LanguagePreferenceStore`, storing a new key (`"theme_flavor"`) in
 * the same `choice_app_prefs` `SharedPreferences` file the language preference already uses —
 * independent of `ChoiceDatabase` and `WidgetConfigStore` (FR-012) —
 * contracts/theme-contract.md §3.
 */
interface ThemePreferenceStore {
    /** Synchronous. Safe to call before the first Activity attaches. Null = never set. */
    fun read(): String?

    /** Synchronous commit. */
    fun write(id: String)
}

class SharedPreferencesThemeStore(context: Context) : ThemePreferenceStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_THEME_FLAVOR, null)

    override fun write(id: String) {
        prefs.edit().putString(KEY_THEME_FLAVOR, id).apply()
    }

    companion object {
        private const val PREFS_NAME = "choice_app_prefs"
        private const val KEY_THEME_FLAVOR = "theme_flavor"
    }
}
