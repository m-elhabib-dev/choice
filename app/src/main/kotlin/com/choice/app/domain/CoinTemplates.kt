package com.choice.app.domain

data class CoinTemplate(
    val name: String,
    val choices: List<String>,
)

object CoinTemplates {
    val templates: List<CoinTemplate> = listOf(
        CoinTemplate(
            name = "Breakfast",
            choices = listOf("Ful", "Eggs", "Falafel", "Cheese"),
        ),
        CoinTemplate(
            name = "Lunch",
            choices = listOf("Rice", "Pasta", "Chicken", "Lentils"),
        ),
        CoinTemplate(
            name = "Workout",
            choices = listOf("Chest", "Back", "Legs", "Full Body"),
        ),
        CoinTemplate(
            name = "Movie",
            choices = listOf("Interstellar", "Dune", "Batman", "The Matrix"),
        ),
    )
}
