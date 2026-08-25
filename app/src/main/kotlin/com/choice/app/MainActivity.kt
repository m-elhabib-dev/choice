package com.choice.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.choice.app.locale.LocaleApplier
import com.choice.app.ui.CoinViewModelFactory
import com.choice.app.ui.coinedit.CoinEditScreen
import com.choice.app.ui.coinflip.CoinFlipScreen
import com.choice.app.ui.coinsettings.CoinSettingsScreen
import com.choice.app.ui.history.HistoryScreen
import com.choice.app.ui.main.MainScreen
import com.choice.app.ui.settings.SettingsScreen
import com.choice.app.ui.settings.SettingsViewModel
import com.choice.app.ui.settings.SettingsViewModelFactory
import com.choice.app.ui.statistics.StatisticsScreen
import com.choice.app.ui.templates.TemplatePickerScreen
import com.choice.app.ui.share.ImportCoinScreen
import com.choice.app.ui.theme.ChoiceTheme
import com.choice.app.widget.OpenCoinInAppAction

class MainActivity : ComponentActivity() {
    // API 26–32 backport path; a no-op on API 33+ (contracts/localization-contract.md §4).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleApplier.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChoiceTheme {
                ChoiceNavHost(
                    initialPayload = intent?.takeIf {
                        it.action == android.content.Intent.ACTION_SEND
                    }?.getStringExtra(android.content.Intent.EXTRA_TEXT),
                    openCoinId = intent?.takeIf {
                        it.hasExtra(OpenCoinInAppAction.EXTRA_OPEN_COIN_ID)
                    }?.getLongExtra(OpenCoinInAppAction.EXTRA_OPEN_COIN_ID, -1L),
                )
            }
        }
    }
}

@Composable
private fun ChoiceNavHost(initialPayload: String? = null, openCoinId: Long? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ChoiceApplication).appContainer

    if (initialPayload != null) {
        LaunchedEffect(Unit) {
            val encodedPayload = java.net.URLEncoder.encode(initialPayload, "UTF-8")
            navController.navigate("import?payload=$encodedPayload")
        }
    }

    if (openCoinId != null) {
        // Mirrors the ACTION_SEND import handling above: a widget's "open in app" action (per
        // contracts/widget-action-contract.md) hands MainActivity a coinId to deep-link straight
        // to, instead of the default "main" coin list start destination.
        LaunchedEffect(Unit) {
            navController.navigate("coinflip/$openCoinId")
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
                onSettings = {
                    navController.navigate("settings")
                },
            )
        }

        composable("settings") {
            val application = context.applicationContext as ChoiceApplication
            val factory = SettingsViewModelFactory(
                context,
                application.languagePreferenceStore,
                refreshWidgets = { application.widgetRefreshCoordinator.refreshAll() },
            )
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
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
            route = "coinedit?coinId={coinId}&templateId={templateId}",
            arguments = listOf(
                navArgument("coinId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val coinIdArg = backStackEntry.arguments?.getLong("coinId") ?: -1L
            val coinId = if (coinIdArg == -1L) null else coinIdArg
            val templateId = backStackEntry.arguments?.getString("templateId")
            // Resolved here, before CoinEditViewModel — the materialization boundary
            // (data-model.md §4, NV-5): once resolved to a plain String/List<String>, a created
            // coin can never re-translate on a later language change.
            val template = templateId?.let { id ->
                com.choice.app.domain.CoinTemplates.templates.find { it.id == id }
            }
            val templateName = template?.let { stringResource(it.nameRes) }
            val templateChoices = template?.let { stringArrayResource(it.choicesRes).toList() }
            val factory = CoinViewModelFactory(
                appContainer.coinRepository,
                coinId,
                templateName,
                templateChoices,
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
                onTemplateSelected = { templateId ->
                    navController.navigate("coinedit?templateId=$templateId")
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
