package com.choice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.choice.app.ui.coinsettings.CoinSettingsScreen
import com.choice.app.ui.history.HistoryScreen
import com.choice.app.ui.main.MainScreen
import com.choice.app.ui.statistics.StatisticsScreen
import com.choice.app.ui.templates.TemplatePickerScreen
import com.choice.app.ui.share.ImportCoinScreen
import com.choice.app.ui.theme.ChoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChoiceTheme {
                ChoiceNavHost(
                    initialPayload = intent?.takeIf {
                        it.action == android.content.Intent.ACTION_SEND
                    }?.getStringExtra(android.content.Intent.EXTRA_TEXT),
                )
            }
        }
    }
}

@Composable
private fun ChoiceNavHost(initialPayload: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ChoiceApplication).appContainer

    if (initialPayload != null) {
        LaunchedEffect(Unit) {
            val encodedPayload = java.net.URLEncoder.encode(initialPayload, "UTF-8")
            navController.navigate("import?payload=$encodedPayload")
        }
    }

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
                    navController.navigate("templates")
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
                onSettings = { navController.navigate("coinsettings/$coinId") },
                onHistory = { navController.navigate("history/$coinId") },
            )
        }

        composable(
            route = "coinedit?coinId={coinId}&templateName={templateName}",
            arguments = listOf(
                navArgument("coinId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("templateName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val coinIdArg = backStackEntry.arguments?.getLong("coinId") ?: -1L
            val coinId = if (coinIdArg == -1L) null else coinIdArg
            val templateName = backStackEntry.arguments?.getString("templateName")
            val template = templateName?.let { name ->
                com.choice.app.domain.CoinTemplates.templates.find { it.name == name }
            }
            val factory = CoinViewModelFactory(
                appContainer.coinRepository,
                coinId,
                template?.name,
                template?.choices,
            )
            val viewModel: com.choice.app.ui.coinedit.CoinEditViewModel =
                viewModel(factory = factory)
            CoinEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable("templates") {
            TemplatePickerScreen(
                onTemplateSelected = { name, _ ->
                    navController.navigate("coinedit?templateName=$name")
                },
                onStartBlank = {
                    navController.navigate("coinedit")
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "coinsettings/{coinId}",
            arguments = listOf(navArgument("coinId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong("coinId") ?: return@composable
            val factory = CoinViewModelFactory(appContainer.coinRepository, coinId)
            val viewModel: com.choice.app.ui.coinsettings.CoinSettingsViewModel =
                viewModel(factory = factory)
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onImport = { navController.navigate("import") },
            )
        }

        composable(
            route = "history/{coinId}",
            arguments = listOf(navArgument("coinId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong("coinId") ?: return@composable
            val factory = CoinViewModelFactory(appContainer.coinRepository, coinId)
            val viewModel: com.choice.app.ui.history.HistoryViewModel =
                viewModel(factory = factory)
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStatistics = { navController.navigate("statistics/$coinId") },
            )
        }

        composable(
            route = "statistics/{coinId}",
            arguments = listOf(navArgument("coinId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getLong("coinId") ?: return@composable
            val factory = CoinViewModelFactory(appContainer.coinRepository, coinId)
            val viewModel: com.choice.app.ui.statistics.StatisticsViewModel =
                viewModel(factory = factory)
            StatisticsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "import?payload={payload}",
            arguments = listOf(
                navArgument("payload") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val payload = backStackEntry.arguments?.getString("payload")
            val factory = CoinViewModelFactory(appContainer.coinRepository)
            val viewModel: com.choice.app.ui.share.ImportCoinViewModel =
                viewModel(factory = factory)
            LaunchedEffect(payload) {
                if (payload != null) {
                    viewModel.updateInputText(payload)
                    viewModel.importCoin()
                }
            }
            ImportCoinScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onImportSuccess = { navController.popBackStack() },
            )
        }
    }
}
