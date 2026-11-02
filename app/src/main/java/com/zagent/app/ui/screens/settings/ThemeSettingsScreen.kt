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
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import android.os.Build
import androidx.compose.runtime.remember
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
    val settings = viewModel.settingsManager
    var darkTime by remember { mutableStateOf(settings.scheduledDarkTime) }
    var lightTime by remember { mutableStateOf(settings.scheduledLightTime) }
    var oled by remember { mutableStateOf(settings.oledBlack) }
    var dynamic by remember { mutableStateOf(settings.dynamicSystemColors) }
    var radius by remember { mutableStateOf(settings.cornerRadius) }
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
        if (viewModel.themeMode == ThemeMode.SCHEDULED) {
            SettingsSectionLabel("HORAIRES")
            SettingsCard {
                OutlinedTextField(
                    value = darkTime,
                    onValueChange = { value ->
                        if (value.matches(Regex("\\d{0,2}:?\\d{0,2}"))) {
                            darkTime = value
                            if (value.matches(Regex("\\d{2}:\\d{2}")) && value.substring(0, 2).toIntOrNull() in 0..23 && value.substring(3, 5).toIntOrNull() in 0..59) settings.scheduledDarkTime = value
                        }
                    },
                    label = { Text("Passage en sombre (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lightTime,
                    onValueChange = { value ->
                        if (value.matches(Regex("\\d{0,2}:?\\d{0,2}"))) {
                            lightTime = value
                            if (value.matches(Regex("\\d{2}:\\d{2}")) && value.substring(0, 2).toIntOrNull() in 0..23 && value.substring(3, 5).toIntOrNull() in 0..59) settings.scheduledLightTime = value
                        }
                    },
                    label = { Text("Passage en clair (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        SettingsSectionLabel("RAYON DES ANGLES")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                listOf(8 to "Net", 16 to "Doux", 28 to "Très rond").forEach { (value, label) ->
                    FilterChip(
                        selected = radius == value,
                        onClick = { radius = value; settings.cornerRadius = value },
                        label = { Text(label) }
                    )
                }
            }
        }
        $marker
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Noir pur (OLED)", color = Zcolors.text)
                    Text("Fond noir et surfaces profondes en mode sombre", color = Zcolors.text2, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Switch(checked = oled, onCheckedChange = { oled = it; settings.oledBlack = it })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsInsetDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Couleurs du système", color = Zcolors.text)
                        Text("Utiliser la palette dynamique Android", color = Zcolors.text2, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = dynamic, onCheckedChange = { dynamic = it; settings.dynamicSystemColors = it })
                }
            }
        }
        $after
    }
}

@Composable
private fun ThemeModeRow(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    val title = when (mode) {
        ThemeMode.SYSTEM -> "Système"
        ThemeMode.LIGHT -> "Clair"
        ThemeMode.DARK -> "Sombre"
        ThemeMode.SCHEDULED -> "Planifié"
    }
    val subtitle = when (mode) {
        ThemeMode.SYSTEM -> "Suit le réglage de votre appareil"
        ThemeMode.LIGHT -> "Utilise toujours le thème clair"
        ThemeMode.DARK -> "Utilise toujours le thème sombre"
        ThemeMode.SCHEDULED -> "Bascule automatiquement selon les horaires définis"
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
