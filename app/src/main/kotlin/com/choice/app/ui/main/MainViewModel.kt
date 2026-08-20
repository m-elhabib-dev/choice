package com.choice.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.CoinSummary
import com.choice.app.domain.matchesSearchQuery
import com.choice.app.ui.components.DeleteCoinConfirmationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val quickCoins: List<CoinSummary> = emptyList(),
    val allCoins: List<CoinSummary> = emptyList(),
    val filteredAllCoins: List<CoinSummary> = emptyList(),
    val isEmpty: Boolean = false,
    val isNoResults: Boolean = false,
    val searchQuery: String = "",
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
                coinRepository.observeQuickCoins(),
                coinRepository.observeCoins(),
            ) { quickCoins, all ->
                val allSummaries = all.map { coinWithChoices ->
                    CoinSummary(
                        id = coinWithChoices.coin.id,
                        name = coinWithChoices.coin.name,
                        choiceCount = coinWithChoices.choices.size,
                        isFavorite = coinWithChoices.coin.isFavorite,
                    )
                }
                MainScreenUiState(
                    quickCoins = quickCoins.map { coinWithChoices ->
                        CoinSummary(
                            id = coinWithChoices.coin.id,
                            name = coinWithChoices.coin.name,
                            choiceCount = coinWithChoices.choices.size,
                            isFavorite = true,
                        )
                    },
                    allCoins = allSummaries,
                    isEmpty = all.isEmpty(),
                )
            }.collect { state ->
                val query = _uiState.value.searchQuery
                val filtered = if (query.isBlank()) {
                    state.allCoins
                } else {
                    state.allCoins.filter { matchesSearchQuery(it.name, query) }
                }
                _uiState.update { it.copy(
                    quickCoins = state.quickCoins,
                    allCoins = state.allCoins,
                    filteredAllCoins = filtered,
                    isEmpty = state.isEmpty,
                    isNoResults = filtered.isEmpty() && !state.isEmpty,
                ) }
            }
        }
    }

    fun openCoin(coinId: Long) {
        viewModelScope.launch {
            coinRepository.recordInteraction(coinId)
        }
    }

    fun toggleFavorite(coinId: Long) {
        viewModelScope.launch {
            val isCurrentlyFavorite = _uiState.value.quickCoins.any { it.id == coinId }
            coinRepository.setFavorite(coinId, !isCurrentlyFavorite)
        }
    }

    fun updateSearchQuery(query: String) {
        val filtered = if (query.isBlank()) {
            _uiState.value.allCoins
        } else {
            _uiState.value.allCoins.filter { matchesSearchQuery(it.name, query) }
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredAllCoins = filtered,
                isNoResults = filtered.isEmpty() && !_uiState.value.isEmpty,
            )
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
