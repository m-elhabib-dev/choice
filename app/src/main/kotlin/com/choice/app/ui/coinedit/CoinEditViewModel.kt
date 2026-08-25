package com.choice.app.ui.coinedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.data.CoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Domain code stays free of Android resource dependencies (Principle VI) — the Composable
 * resolves the final localized message via `stringResource` (see CoinEditScreen.kt).
 */
enum class CoinEditError {
    NAME_BLANK,
    CHOICE_BLANK,
    TOO_FEW_CHOICES,
}

data class ChoiceFieldState(
    val id: Long? = null,
    val text: String = "",
    val error: CoinEditError? = null,
)

data class CoinEditUiState(
    val coinId: Long? = null,
    val name: String = "",
    val nameError: CoinEditError? = null,
    val choices: List<ChoiceFieldState> = emptyList(),
    val formError: CoinEditError? = null,
    val canSave: Boolean = false,
    val isSaved: Boolean = false,
    val removeError: CoinEditError? = null,
    val orderChanged: Boolean = false,
)

class CoinEditViewModel(
    private val coinRepository: CoinRepository,
    private val coinId: Long?,
    private val templateName: String? = null,
    private val templateChoices: List<String>? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinEditUiState())
    val uiState: StateFlow<CoinEditUiState> = _uiState.asStateFlow()

    private var showErrors = false
    private var originalChoiceIds: List<Long> = emptyList()

    val canRemoveChoice: Boolean
        get() = _uiState.value.choices.size > 2

    init {
        if (coinId == null) {
            if (templateName != null && templateChoices != null) {
                _uiState.update {
                    it.copy(
                        coinId = null,
                        name = templateName,
                        choices = templateChoices.map { text -> ChoiceFieldState(text = text) },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        coinId = null,
                        choices = listOf(ChoiceFieldState(), ChoiceFieldState()),
                    )
                }
            }
            recompute()
        } else {
            loadCoin(coinId)
        }
    }

    private fun loadCoin(coinId: Long) {
        viewModelScope.launch {
            val coinWithChoices = coinRepository.observeCoin(coinId).firstOrNull()
            if (coinWithChoices != null) {
                originalChoiceIds = coinWithChoices.choices.map { it.id }
                _uiState.update {
                    it.copy(
                        coinId = coinWithChoices.coin.id,
                        name = coinWithChoices.coin.name,
                        choices = coinWithChoices.choices.map { choice ->
                            ChoiceFieldState(id = choice.id, text = choice.text)
                        },
                    )
                }
                recompute()
            }
        }
    }

    fun updateName(text: String) {
        _uiState.update { it.copy(name = text) }
        recompute()
    }

    fun addChoice() {
        _uiState.update { it.copy(choices = it.choices + ChoiceFieldState(), removeError = null) }
        recompute()
    }

    fun updateChoice(index: Int, text: String) {
        _uiState.update { state ->
            state.copy(
                choices = state.choices.mapIndexed { i, choice ->
                    if (i == index) choice.copy(text = text) else choice
                },
            )
        }
        recompute()
    }

    fun removeChoice(index: Int) {
        if (_uiState.value.choices.size <= 2) {
            _uiState.update { it.copy(removeError = CoinEditError.TOO_FEW_CHOICES) }
            return
        }
        _uiState.update { state ->
            state.copy(choices = state.choices.filterIndexed { i, _ -> i != index }, removeError = null)
        }
        recompute()
    }

    fun moveChoiceUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val choices = state.choices.toMutableList()
            val temp = choices[index]
            choices[index] = choices[index - 1]
            choices[index - 1] = temp
            state.copy(choices = choices, orderChanged = true)
        }
        recompute()
    }

    fun moveChoiceDown(index: Int) {
        val choices = _uiState.value.choices
        if (index >= choices.size - 1) return
        _uiState.update { state ->
            val mutableChoices = state.choices.toMutableList()
            val temp = mutableChoices[index]
            mutableChoices[index] = mutableChoices[index + 1]
            mutableChoices[index + 1] = temp
            state.copy(choices = mutableChoices, orderChanged = true)
        }
        recompute()
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) {
            showErrors = true
            recompute()
            return
        }
        viewModelScope.launch {
            coinRepository.saveCoin(coinId, state.name, state.choices.map { it.text })
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val nameError = if (showErrors && state.name.isBlank()) CoinEditError.NAME_BLANK else null
        val choices = state.choices.map { choice ->
            val error = if (showErrors && choice.text.isBlank()) CoinEditError.CHOICE_BLANK else null
            choice.copy(error = error)
        }
        val formError = if (state.choices.size < 2) {
            CoinEditError.TOO_FEW_CHOICES
        } else {
            null
        }
        val canSave = state.name.isNotBlank() && state.name.length <= 40 &&
            state.choices.size >= 2 &&
            state.choices.all { it.text.isNotBlank() && it.text.length <= 60 }
        _uiState.update {
            it.copy(
                nameError = nameError,
                choices = choices,
                formError = formError,
                canSave = canSave,
            )
        }
    }
}
