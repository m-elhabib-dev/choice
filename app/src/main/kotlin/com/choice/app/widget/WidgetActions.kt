package com.choice.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.choice.app.MainActivity

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
