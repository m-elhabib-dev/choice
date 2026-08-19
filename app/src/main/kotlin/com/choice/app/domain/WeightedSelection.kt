package com.choice.app.domain

import kotlin.random.Random

fun selectWeighted(choices: List<Choice>, random: Random = Random.Default): Choice {
    require(choices.isNotEmpty()) { "Cannot select from an empty choice list" }
    val totalWeight = choices.sumOf { (it.weight ?: 1).toLong() }
    var roll = random.nextLong(totalWeight)
    for (choice in choices) {
        val w = (choice.weight ?: 1).toLong()
        if (roll < w) return choice
        roll -= w
    }
    return choices.last()
}
