package com.choice.app.domain

import kotlin.random.Random

fun flipCoin(choices: List<Choice>, random: Random = Random.Default): Choice {
    require(choices.isNotEmpty()) { "Cannot flip a coin with no choices" }
    return choices[random.nextInt(choices.size)]
}
