package com.choice.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.choice.app.ChoiceApplication

/**
 * Manifest-declared (not runtime-registered) receiver for `ACTION_LOCALE_CHANGED` —
 * widget-localization-contract.md §4. Must be manifest-declared: the app process may not be alive
 * when the user changes the device's system language, and a runtime receiver would miss the
 * broadcast entirely, leaving widgets stale in the old language (FR-031).
 *
 * Deliberately reads nothing from the broadcast itself and never consults the system locale
 * directly — [WidgetRefreshCoordinator.refreshAll] re-renders through
 * `LocaleApplier.localizedContext`, which resolves the active language (in-app override or
 * system) the same way every other render does. This is what makes WR-6 hold: an override in
 * effect wins over the new system language automatically.
 */
class LocaleChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCALE_CHANGED) return
        val application = context.applicationContext as ChoiceApplication
        application.widgetRefreshCoordinator.refreshAll()
    }
}
