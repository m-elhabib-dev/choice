package com.choice.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.choice.app.data.CoinRepository
import com.choice.app.ui.coinflip.CoinFlipViewModel
import com.choice.app.ui.coinedit.CoinEditViewModel
import com.choice.app.ui.coinsettings.CoinSettingsViewModel
import com.choice.app.ui.main.MainViewModel

class CoinViewModelFactory(
    private val coinRepository: CoinRepository,
    private val coinId: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CoinFlipViewModel::class.java) ->
                CoinFlipViewModel(coinRepository, coinId ?: -1L) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(coinRepository) as T
            modelClass.isAssignableFrom(CoinEditViewModel::class.java) ->
                CoinEditViewModel(coinRepository, coinId) as T
            modelClass.isAssignableFrom(CoinSettingsViewModel::class.java) ->
                CoinSettingsViewModel(coinRepository, coinId ?: -1L) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
