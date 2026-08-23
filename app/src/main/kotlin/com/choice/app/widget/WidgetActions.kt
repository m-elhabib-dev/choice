package com.choice.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.choice.app.ChoiceApplication
import com.choice.app.MainActivity
import com.choice.app.domain.selectChoice
import kotlinx.coroutines.flow.firstOrNull

/**
 * Opens the app directly to a coin, per contracts/widget-action-contract.md's
 * `OpenCoinInAppAction`. Used as the tap target on a widget's coin name/header (both widget
 * types) and as the "open app" affordance on an Unavailable-state widget.
 *
 * When [ParamCoinId] resolves to a non-null value, `MainActivity` is started with an explicit
 * [EXTRA_OPEN_COIN_ID] extra so it can deep-link straight to that coin (FR-... / US5). When absent
 * (e.g. an Unavailable widget with no configured coin), `MainActivity` is started with no extra,
 * falling back to its default `main` (coin list) destination.
 */
class OpenCoinInAppAction : ActionCallback {

    companion object {
        val ParamCoinId = ActionParameters.Key<Long>("com.choice.app.widget.PARAM_COIN_ID")
        const val EXTRA_OPEN_COIN_ID = "com.choice.app.widget.EXTRA_OPEN_COIN_ID"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val coinId = parameters[ParamCoinId]
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (coinId != null) {
                putExtra(EXTRA_OPEN_COIN_ID, coinId)
            }
        }
        context.startActivity(intent)
    }
}

/**
 * A Single Coin Widget instance's flip control, per contracts/widget-action-contract.md's
 * `FlipSingleCoinAction`. Guarded by [WidgetFlipGuard] keyed on this [GlanceId] alone, so a second
 * tap arriving while the first tap's select+record is still in flight is dropped rather than
 * queued or double-recorded (research.md §6).
 *
 * Resolves the instance's configured coin against the live [com.choice.app.data.CoinRepository];
 * an unresolvable coin or one with fewer than 2 choices just re-renders the widget (so it shows/
 * confirms its Unavailable or Too Few Choices state) with no selection attempted. Otherwise it
 * calls the same [selectChoice] the app itself uses (User Story 3 parity) and records the result
 * through [com.choice.app.data.CoinRepository.recordDecision]/`recordInteraction`, exactly as
 * [com.choice.app.ui.coinflip.CoinFlipViewModel] does today — no widget-local selection logic.
 */
class FlipSingleCoinAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetFlipGuard.tryFlip(glanceId) {
            val coinRepository = (context.applicationContext as ChoiceApplication)
                .appContainer.coinRepository

            val coinId = WidgetConfigStore.readSingleCoinConfig(context, glanceId)?.coinId
            val coinWithChoices = coinId?.let { coinRepository.observeCoin(it).firstOrNull() }

            if (coinId != null && coinWithChoices != null && coinWithChoices.choices.size >= 2) {
                val lastDecision = coinRepository.getLastDecision(coinId)
                val choice = selectChoice(
                    choices = coinWithChoices.choices,
                    weightedEnabled = coinWithChoices.coin.weightedEnabled,
                    avoidLastResultEnabled = coinWithChoices.coin.avoidLastResultEnabled,
                    lastChoiceId = lastDecision?.choiceId,
                )
                coinRepository.recordDecision(coinId, choice.id, choice.text)
                coinRepository.recordInteraction(coinId)
            }

            SingleCoinWidget().update(context, glanceId)
        }
    }
}

/**
 * Starts [SingleCoinWidgetConfigActivity] as an app-initiated reconfiguration, per
 * widget-configuration-contract.md §2 — the "reconfigure" affordance shown on an Unavailable
 * Single Coin Widget. Reuses the exact same read/save code path the system's own
 * `APPWIDGET_CONFIGURE` launch uses (this is a normal `Intent` start, not the system callback, so
 * no `setResult` is required afterward).
 */
class ReconfigureSingleCoinAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val intent = Intent(context, SingleCoinWidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        context.startActivity(intent)
    }
}

/**
 * One row's flip control within a Quick Coins Widget instance, per
 * contracts/widget-action-contract.md's `FlipQuickCoinAction`. Identical resolve/select/record
 * logic to [FlipSingleCoinAction], but guarded by [WidgetFlipGuard] keyed on `(glanceId, coinId)`
 * so different coins within the same instance can flip concurrently while the same coin cannot
 * double-fire — flipping one row only ever updates that row's coin; other rows in the same
 * instance are untouched.
 */
class FlipQuickCoinAction : ActionCallback {

    companion object {
        val ParamCoinId = ActionParameters.Key<Long>("com.choice.app.widget.QUICK_COINS_PARAM_COIN_ID")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val coinId = parameters[ParamCoinId] ?: return

        WidgetFlipGuard.tryFlip(glanceId to coinId) {
            val coinRepository = (context.applicationContext as ChoiceApplication)
                .appContainer.coinRepository

            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()

            if (coinWithChoices != null && coinWithChoices.choices.size >= 2) {
                val lastDecision = coinRepository.getLastDecision(coinId)
                val choice = selectChoice(
                    choices = coinWithChoices.choices,
                    weightedEnabled = coinWithChoices.coin.weightedEnabled,
                    avoidLastResultEnabled = coinWithChoices.coin.avoidLastResultEnabled,
                    lastChoiceId = lastDecision?.choiceId,
                )
                coinRepository.recordDecision(coinId, choice.id, choice.text)
                coinRepository.recordInteraction(coinId)
            }

            QuickCoinsWidget().update(context, glanceId)
        }
    }
}

/**
 * Starts [QuickCoinsWidgetConfigActivity] as an app-initiated reconfiguration, per
 * widget-configuration-contract.md §2 — the "reconfigure"/"pick coins" affordance shown on a
 * No-Coins-Selected Quick Coins Widget. See [ReconfigureSingleCoinAction] for the shared contract.
 */
class ReconfigureQuickCoinsAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val intent = Intent(context, QuickCoinsWidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        context.startActivity(intent)
    }
}
