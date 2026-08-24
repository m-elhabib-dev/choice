package com.choice.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.choice.app.ChoiceApplication
import com.choice.app.R
import kotlinx.coroutines.flow.firstOrNull

/**
 * Derived, render-only state for a single Single Coin Widget instance (data-model.md's "Derived
 * render states" table). Never persisted — recomputed from [WidgetConfigStore] + the live
 * `CoinRepository` every time this widget instance is rendered.
 */
sealed interface SingleCoinWidgetState {
    data object Unavailable : SingleCoinWidgetState
    data class TooFewChoices(val coinId: Long, val coinName: String) : SingleCoinWidgetState
    data class Ready(val coinId: Long, val coinName: String) : SingleCoinWidgetState
    data class Result(val coinId: Long, val coinName: String, val resultText: String) : SingleCoinWidgetState
}

/**
 * `GlanceAppWidget` for the Single Coin Widget (User Story 1). Resolves its
 * [SingleCoinWidgetConfig] against the live `CoinRepository` on every composition — triggered by a
 * user tap ([FlipSingleCoinAction]) or by [WidgetRefreshCoordinator] reacting to an app-side data
 * change — and renders the matching state. No widget-local decision logic: flips are performed by
 * [FlipSingleCoinAction], which calls the same `selectChoice` the app itself uses.
 */
class SingleCoinWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = resolveState(context, id)
        provideContent {
            SingleCoinWidgetContent(state)
        }
    }

    private suspend fun resolveState(context: Context, id: GlanceId): SingleCoinWidgetState {
        val coinId = WidgetConfigStore.readSingleCoinConfig(context, id)?.coinId
            ?: return SingleCoinWidgetState.Unavailable

        val coinRepository = (context.applicationContext as ChoiceApplication)
            .appContainer.coinRepository
        val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            ?: return SingleCoinWidgetState.Unavailable

        if (coinWithChoices.choices.size < 2) {
            return SingleCoinWidgetState.TooFewChoices(coinId, coinWithChoices.coin.name)
        }

        val lastDecision = coinRepository.getLastDecision(coinId)
        return if (lastDecision != null) {
            SingleCoinWidgetState.Result(
                coinId = coinId,
                coinName = coinWithChoices.coin.name,
                resultText = lastDecision.choiceTextSnapshot,
            )
        } else {
            SingleCoinWidgetState.Ready(coinId = coinId, coinName = coinWithChoices.coin.name)
        }
    }
}

@Composable
private fun SingleCoinWidgetContent(state: SingleCoinWidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetGlanceTheme.background)
            .cornerRadius(16.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            is SingleCoinWidgetState.Unavailable -> UnavailableContent()
            is SingleCoinWidgetState.TooFewChoices -> TooFewChoicesContent(state)
            is SingleCoinWidgetState.Ready -> ReadyContent(state)
            is SingleCoinWidgetState.Result -> ResultContent(state)
        }
    }
}

/** No resolvable coin (deleted, never configured, or an undecodable config) — FR-012. */
@Composable
private fun UnavailableContent() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = context.getString(R.string.widget_state_unavailable),
            style = TextStyle(color = WidgetGlanceTheme.textPrimary, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_action_reconfigure),
            style = TextStyle(color = WidgetGlanceTheme.accent, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier
                .clickable(actionRunCallback<ReconfigureSingleCoinAction>())
                .semantics { contentDescription = context.getString(R.string.widget_action_reconfigure) },
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_action_open_app),
            style = TextStyle(color = WidgetGlanceTheme.textSecondary),
            // No coinId parameter: OpenCoinInAppAction falls back to the app's main coin list.
            modifier = GlanceModifier
                .clickable(actionRunCallback<OpenCoinInAppAction>())
                .semantics { contentDescription = context.getString(R.string.widget_action_open_app) },
        )
    }
}

/** Coin resolves but has fewer than 2 choices — no flip control offered (FR-014). */
@Composable
private fun TooFewChoicesContent(state: SingleCoinWidgetState.TooFewChoices) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CoinNameHeader(coinId = state.coinId, coinName = state.coinName)
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_state_too_few_choices),
            style = TextStyle(color = WidgetGlanceTheme.textSecondary, textAlign = TextAlign.Center),
            modifier = GlanceModifier.semantics {
                contentDescription =
                    "${state.coinName}: ${context.getString(R.string.widget_state_too_few_choices)}"
            },
        )
    }
}

/** Coin resolves, ≥ 2 choices, no decision recorded yet — primary flip control (FR-008). */
@Composable
private fun ReadyContent(state: SingleCoinWidgetState.Ready) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CoinNameHeader(coinId = state.coinId, coinName = state.coinName)
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = context.getString(R.string.widget_state_ready),
            style = TextStyle(color = WidgetGlanceTheme.textSecondary),
            modifier = GlanceModifier.semantics {
                contentDescription = "${state.coinName}: ${context.getString(R.string.widget_state_ready)}"
            },
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        FlipControl(primary = true, coinName = state.coinName)
    }
}

/** A decision exists for the coin — settled result plus a visually secondary flip (FR-009). */
@Composable
private fun ResultContent(state: SingleCoinWidgetState.Result) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.coinName,
            style = TextStyle(color = WidgetGlanceTheme.textSecondary, fontSize = 12.sp),
            modifier = GlanceModifier
                .clickable(
                    actionRunCallback<OpenCoinInAppAction>(
                        actionParametersOf(OpenCoinInAppAction.ParamCoinId to state.coinId),
                    ),
                )
                .semantics {
                    contentDescription =
                        "${state.coinName}, ${context.getString(R.string.widget_action_open_app)}"
                },
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = state.resultText,
            style = TextStyle(
                color = WidgetGlanceTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            ),
            modifier = GlanceModifier.semantics {
                contentDescription = "${state.coinName} result: ${state.resultText}"
            },
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        FlipControl(primary = false, coinName = state.coinName)
    }
}

@Composable
private fun CoinNameHeader(coinId: Long, coinName: String) {
    val context = LocalContext.current
    Text(
        text = coinName,
        style = TextStyle(
            color = WidgetGlanceTheme.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        ),
        modifier = GlanceModifier
            .clickable(
                actionRunCallback<OpenCoinInAppAction>(
                    actionParametersOf(OpenCoinInAppAction.ParamCoinId to coinId),
                ),
            )
            .semantics {
                contentDescription = "$coinName, ${context.getString(R.string.widget_action_open_app)}"
            },
    )
}

/**
 * The flip control. [primary] renders it as the visually primary action (Ready state, FR-008);
 * non-primary renders it visually secondary — smaller, no button chrome — for the Result state's
 * explicit-override re-flip (FR-009). [coinName] is folded into the accessible label (rather than
 * just the visible "Flip") so a screen-reader user always hears which coin the control acts on —
 * FR-020/SC-009.
 */
@Composable
private fun FlipControl(primary: Boolean, coinName: String) {
    val context = LocalContext.current
    val label = context.getString(R.string.widget_action_flip)
    val flipModifier = GlanceModifier
        .clickable(actionRunCallback<FlipSingleCoinAction>())
        .semantics { contentDescription = "$label $coinName" }

    if (primary) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetGlanceTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = flipModifier
                .background(WidgetGlanceTheme.surface)
                .cornerRadius(8.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    } else {
        Text(
            text = label,
            style = TextStyle(color = WidgetGlanceTheme.textSecondary, fontSize = 12.sp),
            modifier = flipModifier,
        )
    }
}
