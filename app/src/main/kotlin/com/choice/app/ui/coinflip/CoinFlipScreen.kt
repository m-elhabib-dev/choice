package com.choice.app.ui.coinflip

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(
    viewModel: CoinFlipViewModel,
    onBack: () -> Unit,
    onEditCoin: (Long) -> Unit,
    onSettings: (Long) -> Unit,
    onHistory: (Long) -> Unit,
    onShare: (Long) -> Unit = {},
    onImport: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.coinName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val payload = viewModel.generateSharePayload()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share coin"))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share coin",
                        )
                    }
                    IconButton(onClick = { onHistory(uiState.coinId) }) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Decision history",
                        )
                    }
                    IconButton(onClick = { onSettings(uiState.coinId) }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Coin settings",
                        )
                    }
                    IconButton(onClick = { onEditCoin(uiState.coinId) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit coin",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (uiState.result == null) {
                Button(
                    onClick = { viewModel.flip() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Flip",
                        fontSize = 20.sp,
                    )
                }
            } else {
                Text(
                    text = if (uiState.accepted) "Decision accepted" else "Your decision:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.result!!,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                if (!uiState.accepted) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { viewModel.accept() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Accept",
                            fontSize = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.flipAgain() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Flip again",
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
