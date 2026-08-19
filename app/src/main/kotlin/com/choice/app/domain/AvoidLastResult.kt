package com.choice.app.domain

fun applyAvoidLastResult(choices: List<Choice>, lastChoiceId: Long?): List<Choice> {
    if (lastChoiceId == null) return choices
    val filtered = choices.filter { it.id != lastChoiceId }
    return if (filtered.isEmpty()) choices else filtered
}
