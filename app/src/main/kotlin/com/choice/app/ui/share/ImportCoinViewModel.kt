package com.choice.app.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import com.choice.app.domain.InvalidSharePayloadException
import com.choice.app.domain.SharedCoin
import com.choice.app.domain.decodeSharedCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportCoinUiState(
    val inputText: String = "",
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val isImported: Boolean = false,
)

class ImportCoinViewModel(
    private val coinRepository: CoinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportCoinUiState())
    val uiState: StateFlow<ImportCoinUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null, isImported = false) }
    }

    fun importCoin() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please paste a coin payload") }
            return
        }

        _uiState.update { it.copy(isImporting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val payload: SharedCoin = decodeSharedCoin(text)
                coinRepository.importSharedCoin(payload)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        isImported = true,
                        inputText = "",
                    )
                }
            } catch (e: InvalidSharePayloadException) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = e.message ?: "Invalid payload",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Import failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
