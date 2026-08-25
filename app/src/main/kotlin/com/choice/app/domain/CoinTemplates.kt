package com.choice.app.domain

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.choice.app.R

/**
 * An application-provided starting point for creating a coin (data-model.md §3).
 *
 * [id] is the only cross-language identity: stable, never localized, never rendered, and used as
 * the navigation/lookup key (contracts/navigation-contract.md §1, NV-1). [nameRes]/[choicesRes]
 * are resolved to plain text in the UI layer, before [com.choice.app.ui.coinedit.CoinEditViewModel]
 * — the materialization boundary that makes a created coin structurally incapable of
 * re-translating later (FR-011, NV-5).
 */
data class CoinTemplate(
    val id: String,
    @StringRes val nameRes: Int,
    @ArrayRes val choicesRes: Int,
)

object CoinTemplates {
    val templates: List<CoinTemplate> = listOf(
        CoinTemplate(
            id = "breakfast",
            nameRes = R.string.template_breakfast_name,
            choicesRes = R.array.template_breakfast_choices,
        ),
        CoinTemplate(
            id = "lunch",
            nameRes = R.string.template_lunch_name,
            choicesRes = R.array.template_lunch_choices,
        ),
        CoinTemplate(
            id = "workout",
            nameRes = R.string.template_workout_name,
            choicesRes = R.array.template_workout_choices,
        ),
        CoinTemplate(
            id = "movie",
            nameRes = R.string.template_movie_name,
            choicesRes = R.array.template_movie_choices,
        ),
    )
}
