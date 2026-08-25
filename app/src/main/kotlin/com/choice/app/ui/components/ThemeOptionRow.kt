package com.choice.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.choice.app.ui.theme.ThemeFlavor

/**
 * One selectable row in Settings' "Appearance" section — name, description, a three-swatch color
 * preview (FR-002), and a `RadioButton` indicating the currently active flavor (FR-003).
 *
 * Mirrors [SettingsScreen]'s existing language-row `Modifier.selectable` pattern exactly, so the
 * Appearance section behaves identically to the Language section (click target, semantics role,
 * spacing) — contracts/theme-contract.md §7.
 */
@Composable
fun ThemeOptionRow(
    flavor: ThemeFlavor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp),
        ) {
            Text(
                text = stringResource(flavor.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(flavor.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            flavor.swatchColors.forEach { swatch ->
                Column(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                ) {}
            }
        }
    }
}
