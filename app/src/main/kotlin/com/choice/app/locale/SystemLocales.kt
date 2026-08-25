package com.choice.app.locale

import android.content.res.Resources

/**
 * The device's raw system language preference list, highest priority first, unaffected by any
 * per-app [LocaleApplier] override — read from the shared system [Resources] singleton rather
 * than this app's own (possibly already-overridden) configuration.
 *
 * Shared between [com.choice.app.ChoiceApplication] (process start) and
 * [com.choice.app.ui.settings.SettingsViewModel] (in-app language switch) so both resolve against
 * the same, un-overridden source of truth — contracts/localization-contract.md §2.
 */
fun systemLanguageTags(): List<String> {
    val systemLocales = Resources.getSystem().configuration.locales
    return (0 until systemLocales.size()).map { systemLocales[it].toLanguageTag() }
}
