package com.choice.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.choice.app.locale.LocaleApplier
import com.choice.app.ui.theme.ChoiceTheme
import com.choice.app.ui.theme.SharedPreferencesThemeStore
import com.choice.app.ui.theme.resolveThemeFlavor
import kotlinx.coroutines.launch

/**
 * Configuration handshake for the Quick Coins Widget, per widget-configuration-contract.md §§1–2
 * — the same shared contract [SingleCoinWidgetConfigActivity] implements, but rendering a
 * multi-select coin list (defaulting to the current favorites, pre-checked) plus a "use my
 * favorites (auto-updating)" toggle instead of a single-select picker.
 */
class QuickCoinsWidgetConfigActivity : ComponentActivity() {

    // API 26–32 backport path; a no-op on API 33+ (contracts/localization-contract.md §4, WL-4).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleApplier.localizedContext(newBase))
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            // One-shot, synchronous read — this configuration UI is short-lived and does not
            // need to observe a theme change made elsewhere in the same session
            // (contracts/theme-contract.md §4 table).
            val flavor = resolveThemeFlavor(SharedPreferencesThemeStore(this).read())
            ChoiceTheme(flavor) {
                val coroutineScope = rememberCoroutineScope()
                val coins by coinRepository.observeCoins()
                    .collectAsStateWithLifecycle(initialValue = null)

                QuickCoinsWidgetConfigScreen(
                    coins = coins,
                    onConfirm = { useFavorites, explicitCoinIds ->
                        coroutineScope.launch {
                            val glanceId = GlanceAppWidgetManager(this@QuickCoinsWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)
                            WidgetConfigStore.writeQuickCoinsConfig(
                                context = this@QuickCoinsWidgetConfigActivity,
                                glanceId = glanceId,
                                config = QuickCoinsWidgetConfig(
                                    useFavorites = useFavorites,
                                    explicitCoinIds = explicitCoinIds,
                                ),
                            )
                            QuickCoinsWidget().update(this@QuickCoinsWidgetConfigActivity, glanceId)
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                    onOpenApp = {
                        startActivity(Intent(this@QuickCoinsWidgetConfigActivity, MainActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickCoinsWidgetConfigScreen(
    coins: List<CoinWithChoices>?,
    onConfirm: (useFavorites: Boolean, explicitCoinIds: List<Long>) -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        when {
            coins == null -> LoadingContent(padding)
            coins.isEmpty() -> NoCoinsYetContent(padding, onOpenApp)
            else -> CoinMultiSelectContent(padding, coins, onConfirm)
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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
private fun CoinMultiSelectContent(
    padding: PaddingValues,
    coins: List<CoinWithChoices>,
    onConfirm: (useFavorites: Boolean, explicitCoinIds: List<Long>) -> Unit,
) {
    // Defaults per widget-configuration-contract.md §1: the toggle starts on (live favorites),
    // and the list is pre-checked to the current favorites — matching a never-configured
    // instance's default `QuickCoinsWidgetConfig()`.
    var useFavorites by remember { mutableStateOf(true) }
    var selectedIds by remember {
        mutableStateOf(coins.filter { it.coin.isFavorite }.map { it.coin.id }.toSet())
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.widget_config_use_favorites_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.widget_config_use_favorites_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = useFavorites, onCheckedChange = { useFavorites = it })
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(coins, key = { it.coin.id }) { coinWithChoices ->
                val coinId = coinWithChoices.coin.id
                val checked = if (useFavorites) coinWithChoices.coin.isFavorite else coinId in selectedIds
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !useFavorites) {
                            selectedIds = if (coinId in selectedIds) {
                                selectedIds - coinId
                            } else {
                                selectedIds + coinId
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = !useFavorites)
                        Text(
                            text = coinWithChoices.coin.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onClick = {
                onConfirm(useFavorites, coins.map { it.coin.id }.filter { it in selectedIds })
            },
        ) {
            Text(stringResource(R.string.widget_config_confirm_selection))
        }
    }
}
