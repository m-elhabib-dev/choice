package com.choice.app.domain

data class ChoiceCount(
    val choice: Choice,
    val count: Int,
)

data class CoinStatistics(
    val totalDecisions: Int,
    val perChoiceCounts: List<ChoiceCount>,
    val mostFrequent: ChoiceCount?,
    val leastFrequent: ChoiceCount?,
    val mostRecentDecision: Decision?,
)

fun computeStatistics(choices: List<Choice>, decisions: List<Decision>): CoinStatistics {
    if (decisions.isEmpty()) {
        return CoinStatistics(
            totalDecisions = 0,
            perChoiceCounts = choices.map { ChoiceCount(choice = it, count = 0) },
            mostFrequent = null,
            leastFrequent = null,
            mostRecentDecision = null,
        )
    }

    val choiceIds = choices.map { it.id }.toSet()

    val decisionsByChoiceId = decisions.filter { it.choiceId != null && it.choiceId in choiceIds }
        .groupingBy { it.choiceId!! }
        .eachCount()

    val orphanedDecisions = decisions.filter { it.choiceId == null || it.choiceId !in choiceIds }
    val orphanedSnapshotCounts = orphanedDecisions.groupingBy { it.choiceTextSnapshot }.eachCount()

    val perChoiceCounts = choices.map { choice ->
        val count = (decisionsByChoiceId[choice.id] ?: 0) +
            (orphanedSnapshotCounts[choice.text] ?: 0)
        ChoiceCount(choice = choice, count = count)
    }

    val totalDecisions = decisions.size
    val mostRecentDecision = decisions.maxByOrNull { it.decidedAt }

    val maxCount = perChoiceCounts.maxOf { it.count }
    val minCount = perChoiceCounts.minOf { it.count }

    val tiedMostFrequent = perChoiceCounts.filter { it.count == maxCount }
    val mostFrequent = tiedMostFrequent.minByOrNull { it.choice.position }

    val tiedLeastFrequent = perChoiceCounts.filter { it.count == minCount }
    val leastFrequent = tiedLeastFrequent.minByOrNull { it.choice.position }

    return CoinStatistics(
        totalDecisions = totalDecisions,
        perChoiceCounts = perChoiceCounts,
        mostFrequent = mostFrequent,
        leastFrequent = leastFrequent,
        mostRecentDecision = mostRecentDecision,
    )
}
