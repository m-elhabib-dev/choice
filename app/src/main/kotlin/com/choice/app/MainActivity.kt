package com.choice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.choice.app.ui.CoinViewModelFactory
import com.choice.app.ui.coinedit.CoinEditScreen
import com.choice.app.ui.coinflip.CoinFlipScreen
import com.choice.app.ui.main.MainScreen
import com.choice.app.ui.theme.ChoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChoiceTheme {
                ChoiceNavHost()
            }
        }
    }
}

@Composable
private fun ChoiceNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ChoiceApplication).appContainer

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            val factory = CoinViewModelFactory(appContainer.coinRepository)
            val viewModel: com.choice.app.ui.main.MainViewModel =
                viewModel(factory = factory)
            MainScreen(
                viewModel = viewModel,
                onCoinClick = { coinId ->
                    navController.navigate("coinflip/$coinId")
                },
                onCreateCoin = {
                    navController.navigate("coinedit")
                },
                onEditCoin = { coinId ->
                    navController.navigate("coinedit?coinId=$coinId")
                },
            )
        }

        composable(
            route = "coinflip/{coinId}",
            arguments = listOf(navArgument("coinId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong("coinId") ?: return@composable
            val factory = CoinViewModelFactory(appContainer.coinRepository, coinId)
            val viewModel: com.choice.app.ui.coinflip.CoinFlipViewModel =
                viewModel(factory = factory)
            CoinFlipScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditCoin = { navController.navigate("coinedit/$coinId") },
            )
        }

        composable(
            route = "coinedit?coinId={coinId}",
            arguments = listOf(
                navArgument("coinId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val coinIdArg = backStackEntry.arguments?.getLong("coinId") ?: -1L
            val coinId = if (coinIdArg == -1L) null else coinIdArg
            val factory = CoinViewModelFactory(appContainer.coinRepository, coinId)
            val viewModel: com.choice.app.ui.coinedit.CoinEditViewModel =
                viewModel(factory = factory)
            CoinEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
