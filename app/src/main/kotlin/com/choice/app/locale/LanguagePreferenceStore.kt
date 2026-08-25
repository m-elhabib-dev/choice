package com.choice.app.locale

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user's explicit language override.
 *
 * Deliberately a separate `SharedPreferences` file from `ChoiceDatabase` and from
 * `WidgetConfigStore`, so it is structurally impossible for a language change to touch user
 * content or widget configuration (FR-025, SC-004) — contracts/localization-contract.md §3.
 */
interface LanguagePreferenceStore {
    /** Synchronous. Safe to call before the first Activity attaches. Null = follow system. */
    fun read(): String?

    /** Synchronous commit. Passing null clears the override (back to "follow system"). */
    fun write(tag: String?)
}

class SharedPreferencesLanguageStore(context: Context) : LanguagePreferenceStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_LANGUAGE_TAG, null)

    override fun write(tag: String?) {
        prefs.edit().putString(KEY_LANGUAGE_TAG, tag).apply()
    }

    companion object {
        private const val PREFS_NAME = "choice_app_prefs"
        private const val KEY_LANGUAGE_TAG = "language_tag"
    }
}
