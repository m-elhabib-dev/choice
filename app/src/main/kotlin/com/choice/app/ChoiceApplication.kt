package com.choice.app

import android.app.Application
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import com.choice.app.widget.WidgetRefreshCoordinator

class ChoiceApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        WidgetRefreshCoordinator(this, appContainer.coinRepository).start()
    }
}

class AppContainer(context: Application) {
    private val database = ChoiceDatabase.create(context)
    val coinRepository = CoinRepositoryImpl(database.coinDao())
}
