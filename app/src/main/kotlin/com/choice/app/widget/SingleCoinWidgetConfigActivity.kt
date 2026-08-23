package com.choice.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choice.app.ChoiceApplication
import com.choice.app.MainActivity
import com.choice.app.R
import com.choice.app.domain.CoinWithChoices
import com.choice.app.ui.theme.ChoiceTheme
import kotlinx.coroutines.launch

/**
 * System- and app-initiated configuration handshake for the Single Coin Widget, per
 * widget-configuration-contract.md §§1–2. Started either by the system when the widget is first
 * added (`android:configure`), or directly by [ReconfigureSingleCoinAction] when the user taps
 * "reconfigure" on an already-placed Unavailable widget — both paths share this same read/save
 * logic; only the system path needs `setResult`.
 */
class SingleCoinWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default to canceled per platform convention: back-press or process death before a
        // successful save cleanly removes the never-configured widget instead of leaving it
        // broken (FR-015).
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val coinRepository = (application as ChoiceApplication).appContainer.coinRepository

        setContent {
            ChoiceTheme {
                val coroutineScope = rememberCoroutineScope()
                val coins by coinRepository.observeCoins()
                    .collectAsStateWithLifecycle(initialValue = null)

                SingleCoinWidgetConfigScreen(
                    coins = coins,
                    onCoinSelected = { coinId ->
                        coroutineScope.launch {
                            val glanceId = GlanceAppWidgetManager(this@SingleCoinWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)
                            WidgetConfigStore.writeSingleCoinConfig(
                                context = this@SingleCoinWidgetConfigActivity,
                                glanceId = glanceId,
                                config = SingleCoinWidgetConfig(coinId = coinId),
                            )
                            SingleCoinWidget().update(this@SingleCoinWidgetConfigActivity, glanceId)
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                    onOpenApp = {
                        startActivity(Intent(this@SingleCoinWidgetConfigActivity, MainActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun SingleCoinWidgetConfigScreen(
    coins: List<CoinWithChoices>?,
    onCoinSelected: (Long) -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        when {
            coins == null -> LoadingContent(padding)
            coins.isEmpty() -> NoCoinsYetContent(padding, onOpenApp)
            else -> CoinPickerContent(padding, coins, onCoinSelected)
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** No saved coins at all — FR-013: an open-app action to create one first, widget left unconfigured. */
@Composable
private fun NoCoinsYetContent(padding: PaddingValues, onOpenApp: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.widget_state_no_coins_at_all),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenApp) {
            Text(stringResource(R.string.widget_action_create_coin))
        }
    }
}

@Composable
private fun CoinPickerContent(
    padding: PaddingValues,
    coins: List<CoinWithChoices>,
    onCoinSelected: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.widget_config_choose_coin_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(coins, key = { it.coin.id }) { coinWithChoices ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCoinSelected(coinWithChoices.coin.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = coinWithChoices.coin.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${coinWithChoices.choices.size} choices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
