package com.choice.app.ui.coinflip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.flipCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoinFlipUiState(
    val coinId: Long = 0,
    val coinName: String = "",
    val choices: List<String> = emptyList(),
    val result: String? = null,
    val canRemoveChoice: Boolean = true,
)

class CoinFlipViewModel(
    private val coinRepository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinFlipUiState())
    val uiState: StateFlow<CoinFlipUiState> = _uiState.asStateFlow()

    init {
        loadCoin()
    }

    fun loadCoin() {
        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices != null) {
                _uiState.update {
                    CoinFlipUiState(
                        coinId = coinWithChoices.coin.id,
                        coinName = coinWithChoices.coin.name,
                        choices = coinWithChoices.choices.map { it.text },
                        result = null,
                        canRemoveChoice = coinWithChoices.choices.size > 2,
                    )
                }
            }
        }
    }

    fun flip() {
        val state = _uiState.value
        if (state.result != null) return

        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices != null) {
                val selectedChoice = flipCoin(coinWithChoices.choices)
                _uiState.update {
                    it.copy(result = selectedChoice.text)
                }
                coinRepository.recordInteraction(coinId)
            }
        }
    }
}
