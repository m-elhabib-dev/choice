package com.choice.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.Decision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val decisions: List<Decision> = emptyList(),
    val isEmpty: Boolean = true,
)

class HistoryViewModel(
    private val coinRepository: CoinRepository,
    private val coinId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            coinRepository.observeDecisionHistory(coinId).collect { decisions ->
                _uiState.update {
                    HistoryUiState(
                        decisions = decisions,
                        isEmpty = decisions.isEmpty(),
                    )
                }
            }
        }
    }
}
