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
 * periodic/background refresh timer; this reactive Flow collection is the only non-user-initiated
 * trigger (plan.md Constraints).
 */
class WidgetRefreshCoordinator(
    private val context: Context,
    private val coinRepository: CoinRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            coinRepository.observeCoins().collect {
                // Cheap no-ops when there are zero active instances of a given widget type.
                SingleCoinWidget().updateAll(context)
                QuickCoinsWidget().updateAll(context)
            }
        }
    }
}
