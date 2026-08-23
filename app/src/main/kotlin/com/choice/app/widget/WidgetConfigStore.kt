package com.choice.app.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

/**
 * Per-widget-instance configuration: which coin (Single Coin Widget) or which coins (Quick Coins
 * Widget) a given `GlanceId` represents. Stored via Glance's `PreferencesGlanceStateDefinition`
 * (data-model.md's "New per-widget-instance configuration" section) — never a copy of coin data
 * itself (FR-018).
 */
data class SingleCoinWidgetConfig(
    val coinId: Long?,
)

data class QuickCoinsWidgetConfig(
    val useFavorites: Boolean = true,
    val explicitCoinIds: List<Long> = emptyList(),
)

object WidgetConfigStore {

    private val singleCoinIdKey = longPreferencesKey("single_coin_widget_coin_id")
    private val quickCoinsUseFavoritesKey = booleanPreferencesKey("quick_coins_widget_use_favorites")
    private val quickCoinsExplicitIdsKey = stringPreferencesKey("quick_coins_widget_explicit_coin_ids")

    /**
     * Reads this instance's [SingleCoinWidgetConfig]. Returns `null` for an unset or
     * corrupted/undecodable config — per FR-015 / widget-configuration-contract.md §3, callers map
     * a `null` result to the Unavailable state rather than treating it as an error.
     */
    suspend fun readSingleCoinConfig(context: Context, glanceId: GlanceId): SingleCoinWidgetConfig? {
        val prefs = readPreferences(context, glanceId) ?: return null
        val coinId = prefs[singleCoinIdKey] ?: return null
        return SingleCoinWidgetConfig(coinId = coinId)
    }

    suspend fun writeSingleCoinConfig(
        context: Context,
        glanceId: GlanceId,
        config: SingleCoinWidgetConfig,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            if (config.coinId != null) {
                prefs[singleCoinIdKey] = config.coinId
            } else {
                prefs.remove(singleCoinIdKey)
            }
        }
    }

    /**
     * Reads this instance's [QuickCoinsWidgetConfig]. An unset or corrupted/undecodable config
     * maps to the default (`useFavorites = true`, no explicit IDs) rather than throwing, since a
     * Quick Coins Widget always has a well-defined "no explicit selection" fallback — per FR-015.
     */
    suspend fun readQuickCoinsConfig(context: Context, glanceId: GlanceId): QuickCoinsWidgetConfig {
        val prefs = readPreferences(context, glanceId) ?: return QuickCoinsWidgetConfig()
        val useFavorites = prefs[quickCoinsUseFavoritesKey] ?: true
        val explicitCoinIds = prefs[quickCoinsExplicitIdsKey]
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
        return QuickCoinsWidgetConfig(useFavorites = useFavorites, explicitCoinIds = explicitCoinIds)
    }

    suspend fun writeQuickCoinsConfig(
        context: Context,
        glanceId: GlanceId,
        config: QuickCoinsWidgetConfig,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[quickCoinsUseFavoritesKey] = config.useFavorites
            prefs[quickCoinsExplicitIdsKey] = config.explicitCoinIds.joinToString(",")
        }
    }

    /** Returns `null` (rather than throwing) if the stored state cannot be read/decoded. */
    private suspend fun readPreferences(context: Context, glanceId: GlanceId): Preferences? =
        try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        } catch (e: Exception) {
            null
        }
}
