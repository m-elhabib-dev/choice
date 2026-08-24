package com.choice.app.widget

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [WidgetConfigStore]'s pure decode/apply logic for [SingleCoinWidgetConfig] directly
 * against in-memory `Preferences` (no Android `Context`/`GlanceId` needed), per T010: write/read a
 * config, read an unset/never-written config, and read a corrupted/undecodable value — asserting
 * each maps to "no config" (`null`) rather than throwing (widget-configuration-contract.md §3).
 */
class WidgetConfigStoreTest {

    @Test
    fun writeThenRead_roundTripsTheCoinId() {
        val prefs = mutablePreferencesOf()

        WidgetConfigStore.applySingleCoinConfig(prefs, SingleCoinWidgetConfig(coinId = 42L))
        val result = WidgetConfigStore.decodeSingleCoinConfig(prefs)

        assertEquals(SingleCoinWidgetConfig(coinId = 42L), result)
    }

    @Test
    fun unsetConfig_mapsToNull() {
        val result = WidgetConfigStore.decodeSingleCoinConfig(emptyPreferences())

        assertNull(result)
    }

    @Test
    fun corruptedOrUnreadableConfig_mapsToNull() {
        // Mirrors what WidgetConfigStore's private readPreferences() passes decodeSingleCoinConfig
        // when the underlying Preferences blob can't be read at all (corrupted/undecodable state,
        // e.g. after a backup/restore App Widget ID mismatch) — a `null` input, never an exception.
        val result = WidgetConfigStore.decodeSingleCoinConfig(null)

        assertNull(result)
    }

    @Test
    fun applyWithNullCoinId_removesAnyPreviouslyStoredValue() {
        val prefs = mutablePreferencesOf()
        WidgetConfigStore.applySingleCoinConfig(prefs, SingleCoinWidgetConfig(coinId = 7L))

        WidgetConfigStore.applySingleCoinConfig(prefs, SingleCoinWidgetConfig(coinId = null))

        assertNull(WidgetConfigStore.decodeSingleCoinConfig(prefs))
    }

    /**
     * T033 (US6): each widget instance's config lives in its own `Preferences` blob, keyed by its
     * own `GlanceId` at the real `getAppWidgetState`/`updateAppWidgetState` layer — this test pins
     * that per-instance isolation at the pure decode/apply layer by modeling two instances as two
     * independent in-memory `Preferences` and asserting a write to one never leaks into the other,
     * regardless of write order.
     */
    @Test
    fun twoInstances_eachKeepsItsOwnCoinIdIndependently() {
        val instanceAPrefs = mutablePreferencesOf()
        val instanceBPrefs = mutablePreferencesOf()

        WidgetConfigStore.applySingleCoinConfig(instanceAPrefs, SingleCoinWidgetConfig(coinId = 1L))
        WidgetConfigStore.applySingleCoinConfig(instanceBPrefs, SingleCoinWidgetConfig(coinId = 2L))

        assertEquals(
            SingleCoinWidgetConfig(coinId = 1L),
            WidgetConfigStore.decodeSingleCoinConfig(instanceAPrefs),
        )
        assertEquals(
            SingleCoinWidgetConfig(coinId = 2L),
            WidgetConfigStore.decodeSingleCoinConfig(instanceBPrefs),
        )

        // Reconfiguring instance A must not affect instance B's already-read value.
        WidgetConfigStore.applySingleCoinConfig(instanceAPrefs, SingleCoinWidgetConfig(coinId = 99L))

        assertEquals(
            SingleCoinWidgetConfig(coinId = 99L),
            WidgetConfigStore.decodeSingleCoinConfig(instanceAPrefs),
        )
        assertEquals(
            SingleCoinWidgetConfig(coinId = 2L),
            WidgetConfigStore.decodeSingleCoinConfig(instanceBPrefs),
        )
    }
}
