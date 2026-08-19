package com.choice.app.domain

import kotlin.math.pow

object QuickAccessScore {
    const val HALF_LIFE_HOURS: Double = 72.0

    fun compute(interactionCount: Int, lastInteractionAt: Long?): Double {
        if (lastInteractionAt == null || interactionCount == 0) return 0.0
        val hoursSinceLastInteraction =
            ((System.currentTimeMillis() - lastInteractionAt) / (1000.0 * 60.0 * 60.0))
        return interactionCount * 2.0.pow(-hoursSinceLastInteraction / HALF_LIFE_HOURS)
    }
}
