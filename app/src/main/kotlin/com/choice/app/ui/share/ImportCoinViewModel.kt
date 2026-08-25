package com.choice.app.ui.share

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choice.app.R
import com.choice.app.data.CoinRepository
import com.choice.app.domain.InvalidSharePayloadException
import com.choice.app.domain.SharedCoin
import com.choice.app.domain.SharePayloadError
import com.choice.app.domain.decodeSharedCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Domain code (`SharePayload.kt`) stays free of Android resource dependencies (Principle VI) — the
 * Composable resolves the final localized message via `stringResource` (see ImportCoinScreen.kt),
 * same pattern as `CoinEditError.messageRes()`.
 */
sealed interface ImportCoinError {
    data object EmptyPayload : ImportCoinError
    data class Payload(val reason: SharePayloadError) : ImportCoinError
    data class Unknown(val detail: String?) : ImportCoinError
}

data class ImportCoinUiState(
    val inputText: String = "",
    val isImporting: Boolean = false,
    val error: ImportCoinError? = null,
    val isImported: Boolean = false,
)

class ImportCoinViewModel(
    private val coinRepository: CoinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportCoinUiState())
    val uiState: StateFlow<ImportCoinUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null, isImported = false) }
    }

    fun importCoin() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = ImportCoinError.EmptyPayload) }
            return
        }

        _uiState.update { it.copy(isImporting = true, error = null) }

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
                        error = ImportCoinError.Payload(e.reason),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = ImportCoinError.Unknown(e.message),
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/** [args] are the `formatArgs` to pass alongside [messageRes] to `stringResource`, if any. */
data class LocalizedMessage(@StringRes val messageRes: Int, val args: List<Any> = emptyList())

fun ImportCoinError.toLocalizedMessage(): LocalizedMessage = when (this) {
    is ImportCoinError.EmptyPayload -> LocalizedMessage(R.string.share_error_empty_payload)
    is ImportCoinError.Payload -> reason.toLocalizedMessage()
    is ImportCoinError.Unknown ->
        LocalizedMessage(R.string.share_error_import_failed, listOf(detail ?: ""))
}

private fun SharePayloadError.toLocalizedMessage(): LocalizedMessage = when (this) {
    is SharePayloadError.MalformedJson -> LocalizedMessage(R.string.share_error_invalid_json)
    is SharePayloadError.MissingSchemaVersion -> LocalizedMessage(R.string.share_error_missing_schema_version)
    is SharePayloadError.UnsupportedSchemaVersion ->
        LocalizedMessage(R.string.share_error_unsupported_schema_version, listOf(version))
    is SharePayloadError.NameRequired -> LocalizedMessage(R.string.share_error_name_required)
    is SharePayloadError.NameBlank -> LocalizedMessage(R.string.share_error_name_blank)
    is SharePayloadError.NameTooLong -> LocalizedMessage(R.string.share_error_name_too_long, listOf(maxLength))
    is SharePayloadError.ChoicesRequired -> LocalizedMessage(R.string.share_error_choices_required)
    is SharePayloadError.TooFewChoices -> LocalizedMessage(R.string.share_error_too_few_choices)
    is SharePayloadError.ChoiceTextRequired -> LocalizedMessage(R.string.share_error_choice_text_required)
    is SharePayloadError.ChoiceTextBlank -> LocalizedMessage(R.string.share_error_choice_text_blank)
    is SharePayloadError.ChoiceTextTooLong ->
        LocalizedMessage(R.string.share_error_choice_text_too_long, listOf(maxLength))
    is SharePayloadError.WeightNotANumber -> LocalizedMessage(R.string.share_error_weight_not_a_number)
    is SharePayloadError.WeightNotPositive -> LocalizedMessage(R.string.share_error_weight_not_positive)
    is SharePayloadError.MissingWeightedEnabled ->
        LocalizedMessage(R.string.share_error_missing_weighted_enabled)
    is SharePayloadError.MissingAvoidLastResultEnabled ->
        LocalizedMessage(R.string.share_error_missing_avoid_last_result_enabled)
    is SharePayloadError.InvalidBoolean -> LocalizedMessage(R.string.share_error_invalid_boolean)
}
