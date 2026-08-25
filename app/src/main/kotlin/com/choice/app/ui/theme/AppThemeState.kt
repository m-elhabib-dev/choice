package com.choice.app.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The in-memory, app-wide "currently active" flavor — data-model.md §6.
 *
 * Held once on `ChoiceApplication` (same lifecycle pattern as `WidgetRefreshCoordinator`), not
 * per-Activity, so a config-change-driven Activity re-creation (device rotation) reads the same
 * live [StateFlow] instance rather than re-resolving the preference — no flash, no re-resolution
 * (US2 Acceptance Scenario 2).
 */
class AppThemeState(initial: ThemeFlavor) {
    private val _flavor = MutableStateFlow(initial)
    val flavor: StateFlow<ThemeFlavor> = _flavor.asStateFlow()

    /** A plain value replace — the last call wins if two selections happen in quick succession. */
    fun set(flavor: ThemeFlavor) {
        _flavor.value = flavor
    }
}
