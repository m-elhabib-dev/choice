package com.choice.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.choice.app.data.CoinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps every active widget instance in sync with app-side coin changes (research.md §3), so a
 * rename/edit/favorite-toggle/delete in the app is reflected by widgets without the user removing
 * and re-adding them (FR-011/FR-012/SC-005/SC-006).
 *
 * Started exactly once, from `ChoiceApplication.onCreate`, and lives for the process's lifetime.
 * It collects the existing [CoinRepository.observeCoins] Flow — already used by `MainViewModel` —
 * and, on every emission, re-renders every active instance of both widget types. There is no
 * periodic/background refresh timer; [CoinRepository.observeCoins] and the explicit [refreshAll]
 * call sites (an in-app language change, a device system-language change — FR-031) are the only
 * non-user-initiated triggers (plan.md Constraints, widget-localization-contract.md §4).
 */
class WidgetRefreshCoordinator(
    private val context: Context,
    private val coinRepository: CoinRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            coinRepository.observeCoins().collect {
                refreshAll()
            }
        }
    }

    /**
     * Re-renders every active instance of both widget types. Idempotent, safe to call from any
     * thread. A cheap no-op when zero instances of a widget type are placed (WR-5). Never writes
     * `WidgetConfigStore` — this re-renders, it never re-configures (WR-3).
     */
    fun refreshAll() {
        scope.launch {
            SingleCoinWidget().updateAll(context)
            QuickCoinsWidget().updateAll(context)
        }
    }
}
