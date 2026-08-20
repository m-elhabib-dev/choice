package com.choice.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.CoinStatistics
import com.choice.app.domain.computeStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val statistics: CoinStatistics? = null,
    val isEmpty: Boolean = true,
)

class StatisticsViewModel(
    private val coinRepository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices == null) {
                _uiState.update { StatisticsUiState(isEmpty = true) }
                return@launch
            }

            coinRepository.observeDecisionHistory(coinId).collect { decisions ->
                val stats = computeStatistics(coinWithChoices.choices, decisions)
                _uiState.update {
                    StatisticsUiState(
                        statistics = stats,
                        isEmpty = stats.totalDecisions == 0,
                    )
                }
            }
        }
    }
}
