package com.choice.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.choice.app.ChoiceApplication
import com.choice.app.R
import com.choice.app.domain.CoinWithChoices
import kotlinx.coroutines.flow.firstOrNull

/**
 * Derived, render-only state for one row of a Quick Coins Widget instance — a per-coin analogue
 * of [SingleCoinWidgetState], minus Unavailable (a coin that no longer resolves is dropped from
 * the resolved list entirely, per data-model.md, rather than shown as a broken row).
 */
sealed interface QuickCoinRowState {
    data class TooFewChoices(val coinId: Long, val coinName: String) : QuickCoinRowState
    data class Ready(val coinId: Long, val coinName: String) : QuickCoinRowState
    data class Result(val coinId: Long, val coinName: String, val resultText: String) : QuickCoinRowState
}

/** Derived, render-only state for a Quick Coins Widget instance as a whole. */
sealed interface QuickCoinsWidgetState {
    data object NoCoinsSelected : QuickCoinsWidgetState
    data class Coins(val rows: List<QuickCoinRowState>) : QuickCoinsWidgetState
}

/**
 * Resolves which coins a Quick Coins Widget instance should show, per data-model.md's
 * `QuickCoinsWidgetConfig` resolution rules: `useFavorites` resolves against the live favorites
 * list; otherwise each of [QuickCoinsWidgetConfig.explicitCoinIds] is resolved against [allCoins]
 * in that stored order, silently dropping any ID that no longer resolves (deleted coin) rather
 * than showing it as a broken row. `internal` so [QuickCoinsDefaultSelectionTest] can exercise it
 * directly with plain in-memory lists, without needing a live `CoinRepository`/Glance state.
 */
internal fun resolveQuickCoinsCoins(
    config: QuickCoinsWidgetConfig,
    favorites: List<CoinWithChoices>,
    allCoins: List<CoinWithChoices>,
): List<CoinWithChoices> {
    if (config.useFavorites) {
        return favorites
    }
    val byId = allCoins.associateBy { it.coin.id }
    return config.explicitCoinIds.mapNotNull { byId[it] }
}

/**
 * `GlanceAppWidget` for the Quick Coins Widget (User Story 2). Resolves its
 * [QuickCoinsWidgetConfig] against the live `CoinRepository` — favorites via `observeQuickCoins()`
 * or an explicit subset via `observeCoins()` — on every composition, and renders one row per
 * resolved coin, each with its own flip action ([FlipQuickCoinAction]). The number of visible rows
 * adapts to the widget's current [LocalSize] so it shows as many coins as fit without
 * clipping/overlap (FR-005).
 */
class QuickCoinsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = resolveState(context, id)
        provideContent {
            QuickCoinsWidgetContent(state)
        }
    }

    private suspend fun resolveState(context: Context, id: GlanceId): QuickCoinsWidgetState {
        val config = WidgetConfigStore.readQuickCoinsConfig(context, id)
        val coinRepository = (context.applicationContext as ChoiceApplication)
            .appContainer.coinRepository

        val favorites = if (config.useFavorites) {
            coinRepository.observeQuickCoins().firstOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val allCoins = if (!config.useFavorites) {
            coinRepository.observeCoins().firstOrNull() ?: emptyList()
        } else {
            emptyList()
        }

        val resolved = resolveQuickCoinsCoins(config, favorites, allCoins)
        if (resolved.isEmpty()) {
            return QuickCoinsWidgetState.NoCoinsSelected
        }

        val rows = resolved.map { coinWithChoices ->
            val coinId = coinWithChoices.coin.id
            when {
                coinWithChoices.choices.size < 2 ->
                    QuickCoinRowState.TooFewChoices(coinId, coinWithChoices.coin.name)
                else -> {
                    val lastDecision = coinRepository.getLastDecision(coinId)
                    if (lastDecision != null) {
                        QuickCoinRowState.Result(
                            coinId = coinId,
                            coinName = coinWithChoices.coin.name,
                            resultText = lastDecision.choiceTextSnapshot,
                        )
                    } else {
                        QuickCoinRowState.Ready(coinId = coinId, coinName = coinWithChoices.coin.name)
                    }
                }
            }
        }
        return QuickCoinsWidgetState.Coins(rows)
    }
}

private val RowHeight = 52.dp

/** How many [RowHeight]-tall rows fit in [availableHeight], always showing at least one. */
private fun maxVisibleRows(availableHeight: Dp): Int =
    (availableHeight.value / RowHeight.value).toInt().coerceAtLeast(1)

@Composable
private fun QuickCoinsWidgetContent(state: QuickCoinsWidgetState) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetGlanceTheme.background)
            .cornerRadius(16.dp)
            .padding(8.dp),
    ) {
        when (state) {
            is QuickCoinsWidgetState.NoCoinsSelected -> NoCoinsSelectedContent()
            is QuickCoinsWidgetState.Coins -> {
                val visibleRows = state.rows.take(maxVisibleRows(size.height))
                visibleRows.forEachIndexed { index, row ->
                    QuickCoinRow(row)
                    if (index != visibleRows.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/** Resolved coin set (favorites or explicit) is empty — a Quick Coins Widget analogue of FR-002. */
@Composable
private fun NoCoinsSelectedContent() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_state_no_coins_selected),
            style = TextStyle(color = WidgetGlanceTheme.textPrimary, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_action_reconfigure),
            style = TextStyle(color = WidgetGlanceTheme.accent, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.clickable(actionRunCallback<ReconfigureQuickCoinsAction>()),
        )
    }
}

@Composable
private fun QuickCoinRow(row: QuickCoinRowState) {
    val context = LocalContext.current
    val coinId = when (row) {
        is QuickCoinRowState.TooFewChoices -> row.coinId
        is QuickCoinRowState.Ready -> row.coinId
        is QuickCoinRowState.Result -> row.coinId
    }
    val coinName = when (row) {
        is QuickCoinRowState.TooFewChoices -> row.coinName
        is QuickCoinRowState.Ready -> row.coinName
        is QuickCoinRowState.Result -> row.coinName
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetGlanceTheme.surface)
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(
                    actionRunCallback<OpenCoinInAppAction>(
                        actionParametersOf(OpenCoinInAppAction.ParamCoinId to coinId),
                    ),
                ),
        ) {
            Text(
                text = coinName,
                style = TextStyle(
                    color = WidgetGlanceTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                maxLines = 1,
            )
            when (row) {
                is QuickCoinRowState.TooFewChoices -> Text(
                    text = context.getString(R.string.widget_state_too_few_choices),
                    style = TextStyle(
                        color = WidgetGlanceTheme.textSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                    ),
                    maxLines = 1,
                )
                is QuickCoinRowState.Ready -> Text(
                    text = context.getString(R.string.widget_state_ready),
                    style = TextStyle(color = WidgetGlanceTheme.textSecondary, fontSize = 11.sp),
                    maxLines = 1,
                )
                is QuickCoinRowState.Result -> Text(
                    text = row.resultText,
                    style = TextStyle(color = WidgetGlanceTheme.textSecondary, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }

        if (row !is QuickCoinRowState.TooFewChoices) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.widget_action_flip),
                style = TextStyle(
                    color = WidgetGlanceTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                modifier = GlanceModifier
                    .background(WidgetGlanceTheme.surfaceVariant)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(
                        actionRunCallback<FlipQuickCoinAction>(
                            actionParametersOf(FlipQuickCoinAction.ParamCoinId to coinId),
                        ),
                    ),
            )
        }
    }
}
