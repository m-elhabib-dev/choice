package com.choice.app.ui.coinedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.choice.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinEditScreen(
    viewModel: CoinEditViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.coinId == null) R.string.coin_edit_title_create else R.string.coin_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { if (it.length <= 40) viewModel.updateName(it) },
                label = { Text(stringResource(R.string.coin_edit_name_label)) },
                isError = uiState.nameError != null,
                supportingText = {
                    uiState.nameError?.let { Text(stringResource(it.messageRes())) }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name"),
            )

            uiState.choices.forEachIndexed { index, choice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = { viewModel.moveChoiceUp(index) },
                        enabled = index > 0,
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.cd_move_choice_up)
                        },
                    ) {
                        Text(
                            text = "\u25B2",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.moveChoiceDown(index) },
                        enabled = index < uiState.choices.size - 1,
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.cd_move_choice_down)
                        },
                    ) {
                        Text(
                            text = "\u25BC",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    OutlinedTextField(
                        value = choice.text,
                        onValueChange = { if (it.length <= 60) viewModel.updateChoice(index, it) },
                        label = { Text(stringResource(R.string.coin_edit_choice_label, index + 1)) },
                        isError = choice.error != null,
                        supportingText = {
                            choice.error?.let { Text(stringResource(it.messageRes())) }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice"),
                    )
                    IconButton(
                        onClick = { viewModel.removeChoice(index) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_remove_choice),
                        )
                    }
                }
            }

            TextButton(onClick = { viewModel.addChoice() }) {
                Text(stringResource(R.string.coin_edit_add_choice))
            }

            if (uiState.formError != null) {
                Text(
                    text = stringResource(uiState.formError!!.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState.removeError != null) {
                Text(
                    text = stringResource(uiState.removeError!!.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.coin_edit_save))
            }
        }
    }
}

private fun CoinEditError.messageRes(): Int = when (this) {
    CoinEditError.NAME_BLANK -> R.string.coin_edit_error_name_blank
    CoinEditError.CHOICE_BLANK -> R.string.coin_edit_error_choice_blank
    CoinEditError.TOO_FEW_CHOICES -> R.string.coin_edit_error_too_few_choices
}
