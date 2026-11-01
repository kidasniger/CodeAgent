package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun ThemeSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    SettingsPageScaffold(title = "Thème", onBack = onBack) {
        SettingsSectionLabel("MODE")
        SettingsCard {
            ThemeMode.entries.forEachIndexed { index, mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = viewModel.themeMode == mode,
                    onClick = { viewModel.updateThemeMode(mode) }
                )
                if (index != ThemeMode.entries.lastIndex) SettingsInsetDivider()
            }
        }
        Spacer(Modifier.height(4.dp))
        SettingsInfoRow(
            title = "Thème de l’application",
            description = "Le mode Système suit automatiquement l’apparence définie dans les réglages Android."
        )
    }
}

@Composable
private fun ThemeModeRow(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    val title = when (mode) {
        ThemeMode.SYSTEM -> "Système"
        ThemeMode.LIGHT -> "Clair"
        ThemeMode.DARK -> "Sombre"
    }
    val subtitle = when (mode) {
        ThemeMode.SYSTEM -> "Suit le réglage de votre appareil"
        ThemeMode.LIGHT -> "Utilise toujours le thème clair"
        ThemeMode.DARK -> "Utilise toujours le thème sombre"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = c.text2, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) c.violet else c.surface3),
            contentAlignment = Alignment.Center
        ) {
            if (selected) androidx.compose.foundation.layout.Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(c.bg2)
            )
        }
    }
}
