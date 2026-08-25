package com.choice.app.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList

/**
 * The only place that touches platform locale APIs — contracts/localization-contract.md §4.
 *
 * - API 33+: [android.app.LocaleManager] is system-wide for the app; it recreates activities and
 *   re-renders widgets automatically, and [localizedContext] is a behaviour-identical no-op.
 * - API 26–32: there is no per-app system mechanism, so [apply] records the tag in-process and
 *   [localizedContext] wraps a `Context` with a [android.content.Context.createConfigurationContext]
 *   built from it. The caller is responsible for persisting the tag (see [LanguagePreferenceStore])
 *   and for recreating the visible activity after a change.
 */
object LocaleApplier {

    // API 26-32 only: the tag the most recent [apply] call recorded, read back by
    // [localizedContext]. Set at process start (ChoiceApplication.onCreate) before any Activity
    // attaches, so it is never left uninitialized.
    @Volatile
    private var currentTag: String = SupportedLanguages.DEFAULT

    /** Called from ChoiceApplication.onCreate and after every preference change. */
    fun apply(context: Context, tag: String) {
        currentTag = tag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(tag)
        }
        // API 26-32: no system-wide mechanism exists. currentTag now reflects the new
        // language; the caller must recreate the visible activity for it to take effect.
    }

    /**
     * Returns a Context whose resources resolve against the app's language.
     *
     * On API 33+ this is a no-op — the platform already applies the locale everywhere.
     * On API 26–32 it wraps [context] with a configuration carrying the last-applied locale.
     * Idempotent: wrapping an already-wrapped context behaves identically to wrapping once.
     */
    fun localizedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(uiLocale(currentTag)))
        return context.createConfigurationContext(configuration)
    }
}
