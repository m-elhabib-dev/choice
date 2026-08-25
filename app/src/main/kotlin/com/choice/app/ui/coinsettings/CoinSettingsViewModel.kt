package com.choice.app.ui.coinsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.Choice
import com.choice.app.domain.SharedCoin
import com.choice.app.domain.SharedChoice
import com.choice.app.domain.encodeSharedCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Domain code stays free of Android resource dependencies (Principle VI) — the Composable
 * resolves the final localized message via `stringResource` (see CoinSettingsScreen.kt).
 */
enum class CoinSettingsError {
    WEIGHT_NOT_POSITIVE,
}

data class WeightFieldState(
    val choiceId: Long,
    val choiceText: String,
    val weightText: String,
    val error: CoinSettingsError? = null,
)

data class CoinSettingsUiState(
    val coinId: Long = 0,
    val coinName: String = "",
    val weightedEnabled: Boolean = false,
    val avoidLastResultEnabled: Boolean = false,
    val choiceWeights: List<WeightFieldState> = emptyList(),
    val isSaved: Boolean = false,
    val saveError: String? = null,
)

class CoinSettingsViewModel(
    private val coinRepository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinSettingsUiState())
    val uiState: StateFlow<CoinSettingsUiState> = _uiState.asStateFlow()

    init {
        loadCoin()
    }

    private fun loadCoin() {
        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices != null) {
                _uiState.update {
                    CoinSettingsUiState(
                        coinId = coinWithChoices.coin.id,
                        coinName = coinWithChoices.coin.name,
                        weightedEnabled = coinWithChoices.coin.weightedEnabled,
                        avoidLastResultEnabled = coinWithChoices.coin.avoidLastResultEnabled,
                        choiceWeights = coinWithChoices.choices.map { choice ->
                            WeightFieldState(
                                choiceId = choice.id,
                                choiceText = choice.text,
                                weightText = choice.weight?.toString() ?: "",
                            )
                        },
                    )
                }
            }
        }
    }

    fun setWeightedEnabled(enabled: Boolean) {
        _uiState.update { it.copy(weightedEnabled = enabled, saveError = null) }
    }

    fun setAvoidLastResultEnabled(enabled: Boolean) {
        _uiState.update { it.copy(avoidLastResultEnabled = enabled, saveError = null) }
    }

    fun updateWeight(index: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                choiceWeights = state.choiceWeights.mapIndexed { i, field ->
                    if (i == index) field.copy(weightText = text, error = null) else field
                },
                saveError = null,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        var hasErrors = false
        val validatedWeights = state.choiceWeights.map { field ->
            val weight: Int? = if (field.weightText.isBlank()) {
                null
            } else {
                val parsed = field.weightText.toIntOrNull()
                if (parsed == null || parsed < 1) {
                    hasErrors = true
                    null
                } else {
                    parsed
                }
            }
            field.choiceId to weight
        }

        if (hasErrors) {
            _uiState.update { current ->
                current.copy(
                    choiceWeights = current.choiceWeights.map { field ->
                        if (field.weightText.isNotBlank()) {
                            val parsed = field.weightText.toIntOrNull()
                            if (parsed == null || parsed < 1) {
                                field.copy(error = CoinSettingsError.WEIGHT_NOT_POSITIVE)
                            } else {
                                field
                            }
                        } else {
                            field
                        }
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            coinRepository.setWeightedEnabled(coinId, state.weightedEnabled)
            coinRepository.setAvoidLastResultEnabled(coinId, state.avoidLastResultEnabled)
            coinRepository.updateChoiceWeights(coinId, validatedWeights)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun generateSharePayload(): String {
        val state = _uiState.value
        val coin = SharedCoin(
            schemaVersion = 1,
            name = state.coinName,
            choices = state.choiceWeights.map { field ->
                SharedChoice(
                    text = field.choiceText,
                    weight = field.weightText.toIntOrNull(),
                )
            },
            weightedEnabled = state.weightedEnabled,
            avoidLastResultEnabled = state.avoidLastResultEnabled,
        )
        return encodeSharedCoin(coin)
    }
}
