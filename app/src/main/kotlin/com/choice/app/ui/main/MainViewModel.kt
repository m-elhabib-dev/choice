package com.choice.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.CoinSummary
import com.choice.app.ui.components.DeleteCoinConfirmationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val quickAccessCoins: List<CoinSummary> = emptyList(),
    val allCoins: List<CoinSummary> = emptyList(),
    val isEmpty: Boolean = false,
    val deleteConfirmation: DeleteCoinConfirmationState? = null,
)

class MainViewModel(
    private val coinRepository: CoinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                coinRepository.observeQuickAccessCoins(),
                coinRepository.observeCoins(),
            ) { quickAccess, all ->
                MainScreenUiState(
                    quickAccessCoins = quickAccess.map { coinWithChoices ->
                        CoinSummary(
                            id = coinWithChoices.coin.id,
                            name = coinWithChoices.coin.name,
                            choiceCount = coinWithChoices.choices.size,
                        )
                    },
                    allCoins = all.map { coinWithChoices ->
                        CoinSummary(
                            id = coinWithChoices.coin.id,
                            name = coinWithChoices.coin.name,
                            choiceCount = coinWithChoices.choices.size,
                        )
                    },
                    isEmpty = all.isEmpty(),
                )
            }.collect { state ->
                _uiState.update { it.copy(
                    quickAccessCoins = state.quickAccessCoins,
                    allCoins = state.allCoins,
                    isEmpty = state.isEmpty,
                ) }
            }
        }
    }

    fun openCoin(coinId: Long) {
        viewModelScope.launch {
            coinRepository.recordInteraction(coinId)
        }
    }

    fun requestDelete(coinId: Long) {
        val coin = _uiState.value.allCoins.find { it.id == coinId }
        if (coin != null) {
            _uiState.update {
                it.copy(
                    deleteConfirmation = DeleteCoinConfirmationState(
                        coinId = coinId,
                        coinName = coin.name,
                    ),
                )
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmation = null) }
    }

    fun confirmDelete() {
        val confirmation = _uiState.value.deleteConfirmation ?: return
        viewModelScope.launch {
            coinRepository.deleteCoin(confirmation.coinId)
            _uiState.update { it.copy(deleteConfirmation = null) }
        }
    }
}
