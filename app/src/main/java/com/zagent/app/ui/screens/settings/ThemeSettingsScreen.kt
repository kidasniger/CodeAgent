package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import android.os.Build
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.Zdimens
import com.zagent.app.ui.theme.Zshapes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun ThemeSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings = viewModel.settingsManager
    var darkTime by remember { mutableStateOf(viewModel.scheduledDarkTime) }
    var lightTime by remember { mutableStateOf(viewModel.scheduledLightTime) }
    var oled by remember { mutableStateOf(viewModel.oledBlack) }
    var dynamic by remember { mutableStateOf(viewModel.dynamicSystemColors) }
    var radius by remember { mutableStateOf(viewModel.cornerRadius) }
    SettingsPageScaffold(title = "Thème", onBack = onBack) {
        SettingsSectionLabel("MODE")
        SettingsCard {
            ThemeMode.entries.forEachIndexed { index, mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = viewModel.themeMode == mode,
                    onClick = { viewModel.updateThemeMode(mode); settings.markStylePresetCustom() }
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
                            if (value.matches(Regex("\\d{2}:\\d{2}")) && value.substring(0, 2).toIntOrNull()?.let { it in 0..23 } == true && value.substring(3, 5).toIntOrNull()?.let { it in 0..59 } == true) {
                                viewModel.updateScheduledDarkTime(value)
                                settings.markStylePresetCustom()
                            }
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
                            if (value.matches(Regex("\\d{2}:\\d{2}")) && value.substring(0, 2).toIntOrNull() in 0..23 && value.substring(3, 5).toIntOrNull() in 0..59) {
                                viewModel.updateScheduledLightTime(value)
                                settings.markStylePresetCustom()
                            }
                        }
                    },
                    label = { Text("Passage en clair (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // ui-019 — aperçu du thème en direct : les couleurs de Zcolors sont
        // animées (tween 300) dans ZAgentTheme, donc tout changement ci-dessus
        // (mode, OLED, rayon) se répercute immédiatement sur cette carte.
        SettingsSectionLabel("APERÇU")
        ThemePreviewCard()
        SettingsSectionLabel("RAYON DES ANGLES")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                listOf(8 to "Net", 16 to "Doux", 28 to "Très rond").forEach { (value, label) ->
                    FilterChip(
                        selected = radius == value,
                        onClick = { radius = value; viewModel.updateCornerRadius(value); settings.markStylePresetCustom() },
                        label = { Text(label) }
                    )
                }
            }
        }
        SettingsSectionLabel("OPTIONS D’AFFICHAGE")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Noir pur (OLED)", color = Zcolors.text)
                    Text("Fond noir et surfaces profondes en mode sombre", color = Zcolors.text2, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Switch(checked = oled, onCheckedChange = { oled = it; viewModel.updateOledBlack(it); settings.markStylePresetCustom() })
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
                    Switch(checked = dynamic, onCheckedChange = { dynamic = it; viewModel.updateDynamicSystemColors(it); settings.markStylePresetCustom() })
                }
            }
        }
        SettingsInfoRow(
            title = "Thème de l’application",
            description = "Le mode Système suit automatiquement l’apparence définie dans les réglages Android."
        )
    }
}

/**
 * ui-019 — Aperçu en direct du thème : une bulle agent, un bouton d'action,
 * une chip mono et les quatre accents. Se met à jour sans quitter l'écran.
 */
@Composable
private fun ThemePreviewCard() {
    val c = Zcolors
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Bulle agent
            Row {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(c.violet)
                ) {
                    Icon(
                        Icons.Default.Check, contentDescription = null,
                        tint = c.bg2, modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.width(Zdimens.s))
                Box(
                    Modifier
                        .clip(Zshapes.card)
                        .background(c.surface2)
                        .border(1.dp, c.border, Zshapes.card)
                        .padding(horizontal = Zdimens.m, vertical = Zdimens.s)
                ) {
                    Text(
                        "Voici le rendu des messages.",
                        style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                        color = c.text
                    )
                }
            }
            Spacer(Modifier.height(Zdimens.m))
            // Ligne de composants : bouton, chip mono, pastille
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(Zshapes.pill)
                        .background(c.gradBtn)
                        .padding(horizontal = Zdimens.l, vertical = 8.dp)
                ) {
                    Text(
                        "Action",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = c.onGrad
                    )
                }
                Spacer(Modifier.width(Zdimens.s))
                Box(
                    Modifier
                        .clip(Zshapes.pill)
                        .background(c.cyanBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "glm-4.7",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = c.cyan
                    )
                }
                Spacer(Modifier.width(Zdimens.s))
                Box(Modifier.size(14.dp).clip(CircleShape).background(c.green))
                Spacer(Modifier.width(Zdimens.xs))
                Box(Modifier.size(14.dp).clip(CircleShape).background(c.amber))
                Spacer(Modifier.width(Zdimens.xs))
                Box(Modifier.size(14.dp).clip(CircleShape).background(c.red))
            }
        }
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
        ThemeMode.SYSTEM -> "Suit le réglage de l'appareil"
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
