package com.choice.app

import android.app.Application
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase

class ChoiceApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

class AppContainer(context: Application) {
    private val database = ChoiceDatabase.create(context)
    val coinRepository = CoinRepositoryImpl(database.coinDao())
}
