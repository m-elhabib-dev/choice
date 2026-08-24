package com.choice.app.ui.coinflip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.SharedCoin
import com.choice.app.domain.SharedChoice
import com.choice.app.domain.encodeSharedCoin
import com.choice.app.domain.selectChoice
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
    val resultChoiceId: Long? = null,
    val accepted: Boolean = false,
    val canRemoveChoice: Boolean = true,
    val weightedEnabled: Boolean = false,
    val avoidLastResultEnabled: Boolean = false,
    val notFound: Boolean = false,
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
                        resultChoiceId = null,
                        accepted = false,
                        canRemoveChoice = coinWithChoices.choices.size > 2,
                        weightedEnabled = coinWithChoices.coin.weightedEnabled,
                        avoidLastResultEnabled = coinWithChoices.coin.avoidLastResultEnabled,
                    )
                }
            } else {
                // Per contracts/widget-action-contract.md's OpenCoinInAppAction: a coinId can
                // arrive here from a widget's deep link for a coin that was deleted between the
                // widget's last render and this tap (US5 AS3). No coinId ever resolved in-app
                // before this feature (navigation only ever came from an existing coin's own row),
                // so no not-found handling existed yet; `notFound` lets the screen fall back
                // (pop back to the coin list) instead of rendering a blank/inert flip screen.
                _uiState.update { it.copy(notFound = true) }
            }
        }
    }

    fun flip() {
        val state = _uiState.value
        if (state.result != null) return

        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices != null) {
                val coin = coinWithChoices.coin
                val lastDecision = coinRepository.getLastDecision(coinId)
                val selectedChoice = selectChoice(
                    choices = coinWithChoices.choices,
                    weightedEnabled = coin.weightedEnabled,
                    avoidLastResultEnabled = coin.avoidLastResultEnabled,
                    lastChoiceId = lastDecision?.choiceId,
                )
                _uiState.update {
                    it.copy(
                        result = selectedChoice.text,
                        resultChoiceId = selectedChoice.id,
                        accepted = false,
                    )
                }
                coinRepository.recordInteraction(coinId)
            }
        }
    }

    fun accept() {
        val state = _uiState.value
        val choiceId = state.resultChoiceId ?: return
        val resultText = state.result ?: return

        viewModelScope.launch {
            coinRepository.recordDecision(coinId, choiceId, resultText)
            _uiState.update { it.copy(accepted = true) }
        }
    }

    fun flipAgain() {
        _uiState.update { it.copy(result = null, resultChoiceId = null, accepted = false) }
        flip()
    }

    fun generateSharePayload(): String {
        val state = _uiState.value
        val coin = SharedCoin(
            schemaVersion = 1,
            name = state.coinName,
            choices = state.choices.map { SharedChoice(text = it, weight = null) },
            weightedEnabled = state.weightedEnabled,
            avoidLastResultEnabled = state.avoidLastResultEnabled,
        )
        return encodeSharedCoin(coin)
    }
}
