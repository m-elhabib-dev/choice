package com.choice.app.domain

import kotlin.random.Random

fun selectChoice(
    choices: List<Choice>,
    weightedEnabled: Boolean,
    avoidLastResultEnabled: Boolean,
    lastChoiceId: Long?,
    random: Random = Random.Default,
): Choice {
    require(choices.isNotEmpty()) { "Cannot select from an empty choice list" }
    val pool = if (avoidLastResultEnabled && lastChoiceId != null) {
        applyAvoidLastResult(choices, lastChoiceId)
    } else {
        choices
    }
    return if (weightedEnabled) {
        selectWeighted(pool, random)
    } else {
        flipCoin(pool, random)
    }
}
