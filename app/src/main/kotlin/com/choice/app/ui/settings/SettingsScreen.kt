package com.choice.app.ui.settings

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.choice.app.R
import com.choice.app.locale.SupportedLanguages

/**
 * Choice's only app-level settings screen — language, and nothing else (Principle I, Principle
 * VIII; contracts/navigation-contract.md §2). Reachable from a settings icon in `MainScreen`'s
 * `TopAppBar`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(viewModel.currentLanguageTag()) }

    val options = listOf(
        null to stringResource(R.string.settings_language_system),
        SupportedLanguages.ENGLISH to stringResource(R.string.settings_language_english),
        SupportedLanguages.ARABIC to stringResource(R.string.settings_language_arabic),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            options.forEach { (tag, label) ->
                val isSelected = tag == selectedTag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                // SN-4: re-selecting the already-active option is a no-op — no
                                // write, no LocaleApplier.apply, no recreate, no widget refresh.
                                if (isSelected) return@selectable
                                selectedTag = tag
                                val changed = viewModel.selectLanguage(tag)
                                // API 33+ (LocaleManager) recreates automatically; the API 26–32
                                // backport needs the caller to recreate the visible activity —
                                // contracts/localization-contract.md §4.
                                if (changed && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    (context as? Activity)?.recreate()
                                }
                            },
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
