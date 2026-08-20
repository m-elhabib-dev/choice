package com.choice.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.choice.app.data.CoinRepository
import com.choice.app.ui.coinflip.CoinFlipViewModel
import com.choice.app.ui.coinedit.CoinEditViewModel
import com.choice.app.ui.coinsettings.CoinSettingsViewModel
import com.choice.app.ui.history.HistoryViewModel
import com.choice.app.ui.main.MainViewModel
import com.choice.app.ui.statistics.StatisticsViewModel
import com.choice.app.ui.share.ImportCoinViewModel

class CoinViewModelFactory(
    private val coinRepository: CoinRepository,
    private val coinId: Long? = null,
    private val templateName: String? = null,
    private val templateChoices: List<String>? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CoinFlipViewModel::class.java) ->
                CoinFlipViewModel(coinRepository, coinId ?: -1L) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(coinRepository) as T
            modelClass.isAssignableFrom(CoinEditViewModel::class.java) ->
                CoinEditViewModel(coinRepository, coinId, templateName, templateChoices) as T
            modelClass.isAssignableFrom(CoinSettingsViewModel::class.java) ->
                CoinSettingsViewModel(coinRepository, coinId ?: -1L) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(coinRepository, coinId ?: -1L) as T
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) ->
                StatisticsViewModel(coinRepository, coinId ?: -1L) as T
            modelClass.isAssignableFrom(ImportCoinViewModel::class.java) ->
                ImportCoinViewModel(coinRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
