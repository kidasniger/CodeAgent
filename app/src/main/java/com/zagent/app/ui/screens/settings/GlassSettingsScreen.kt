package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.GlassPreviewCard
import com.zagent.app.ui.theme.*
import com.zagent.app.ui.viewmodel.MainViewModel

private val glassSwatches = listOf(
    "" to "Aucune", "#D97757" to "Corail", "#8B5CF6" to "Violet", "#34D399" to "Menthe",
    "#F5F5F5" to "Blanc", "#F59E0B" to "Ambre", "#22D3EE" to "Cyan",
    "#3B82F6" to "Bleu", "#EC4899" to "Rose", "#64748B" to "Ardoise"
)

@Composable
fun GlassSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val manager = viewModel.settingsManager
    var editDark by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(manager.loadGlassSettings(false)) }
    var resetDialog by remember { mutableStateOf(false) }

    fun save(value: GlassSettings) {
        current = value
        viewModel.updateGlassSettings(editDark, value)
        manager.markStylePresetCustom()
    }
    fun load(dark: Boolean) {
        editDark = dark
        current = manager.loadGlassSettings(dark)
        viewModel.updateGlassSettings(dark, current)
    }

    SettingsPageScaffold(title = "Verre", onBack = onBack) {
        SettingsSectionLabel("ACTIVATION")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Liquid Glass", color = Zcolors.text, style = MaterialTheme.typography.titleMedium)
                    Text("Surfaces flottantes translucides", color = Zcolors.text2, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = current.enabled, onCheckedChange = { save(current.copy(enabled = it)) })
            }
        }

        SettingsSectionLabel("MODIFIER POUR")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !editDark, onClick = { load(false) }, label = { Text("Clair") })
                FilterChip(selected = editDark, onClick = { load(true) }, label = { Text("Sombre") })
            }
        }

        SettingsSectionLabel("APERÇU")
        GlassPreviewCard(
            modifier = Modifier.fillMaxWidth(),
            material = current.material.label(),
            glassColor = if (current.colour == Color.Unspecified) Zcolors.cyan else current.colour,
            blur = current.frost.value / 24f,
            tint = current.tint,
            film = current.film,
            activeTab = "Chat"
        )

        SettingsSectionLabel("MATÉRIAU")
        SettingsCard {
            Column {
                GlassMaterial.entries.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { save(current.copy(material = item)) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label(), color = Zcolors.text, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when (item) {
                                    GlassMaterial.FASTEST -> "Translucide, sans flou"
                                    GlassMaterial.BALANCED -> "Flou léger, consommation modérée"
                                    GlassMaterial.QUALITY -> "Flou complet, reflet et bordure spéculaire"
                                },
                                color = Zcolors.text2,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (current.material == item) Text("✓", color = Zcolors.cyan, fontSize = 20.sp)
                    }
                    if (index < GlassMaterial.entries.lastIndex) SettingsInsetDivider()
                }
            }
        }

        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Toujours respecter mon réglage", color = Zcolors.text)
                    Text("Ignore l'allègement automatique", color = Zcolors.text2, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = current.alwaysRespectSetting, onCheckedChange = { save(current.copy(alwaysRespectSetting = it)) })
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val power = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        if (!current.alwaysRespectSetting && (android.os.Build.VERSION.SDK_INT < 31 || activity?.isLowRamDevice == true || power?.isPowerSaveMode == true)) {
            Text(
                when {
                    android.os.Build.VERSION.SDK_INT < 31 -> "Verre allégé : flou matériel indisponible avant Android 12"
                    power?.isPowerSaveMode == true -> "Verre allégé : économiseur de batterie actif"
                    else -> "Verre allégé : appareil à mémoire limitée"
                },
                color = Zcolors.text3,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        SettingsSectionLabel("COULEUR")
        SettingsCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Teinte appliquée uniquement aux surfaces flottantes", color = Zcolors.text2, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    glassSwatches.take(5).forEach { (hex, label) ->
                        GlassSwatch(hex, label, current.colour) {
                            save(current.copy(colour = if (hex.isBlank()) Color.Unspecified else Color(android.graphics.Color.parseColor(hex))))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    glassSwatches.drop(5).forEach { (hex, label) ->
                        GlassSwatch(hex, label, current.colour) {
                            save(current.copy(colour = if (hex.isBlank()) Color.Unspecified else Color(android.graphics.Color.parseColor(hex))))
                        }
                    }
                }
            }
        }

        SettingsSectionLabel("EFFETS")
        SettingsCard {
            GlassSliderRow("Couleur", Icons.Default.Opacity, current.tint, 0f..1f, { String.format("%d %%", (it * 100).toInt()) }) {
                save(current.copy(tint = it))
            }
            SettingsInsetDivider()
            GlassSliderRow("Flou", Icons.Default.BlurOn, current.frost.value, 0f..24f, { String.format("%d dp", it.toInt()) }) {
                save(current.copy(frost = it.dp))
            }
            if (current.material == GlassMaterial.QUALITY && current.frost > 16.dp) {
                Text("Peut ralentir les anciens téléphones", color = Zcolors.text3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            }
            SettingsInsetDivider()
            GlassSliderRow("Film", Icons.Default.Tune, current.film, 0f..1f, { String.format("%d %%", (it * 100).toInt()) }) {
                save(current.copy(film = it))
            }
            SettingsInsetDivider()
            GlassSliderRow("Onglet actif", Icons.Default.Tune, current.activeTab, 0f..1f, { String.format("%d %%", (it * 100).toInt()) }) {
                save(current.copy(activeTab = it))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { resetDialog = true }) { Text("Réinitialiser") }
        }
        if (resetDialog) {
            GlassAlertDialog(
                onDismissRequest = { resetDialog = false },
                title = { Text("Réinitialiser le verre ?") },
                text = { Text("Les réglages clair et sombre seront restaurés.") },
                confirmButton = {
                    TextButton(onClick = {
                        val light = GlassSettings(film = 0.60f)
                        val dark = GlassSettings(film = 0.35f)
                        manager.saveGlassSettings(false, light)
                        manager.saveGlassSettings(true, dark)
                        current = if (editDark) dark else light
                        viewModel.updateGlassSettings(editDark, current)
                        resetDialog = false
                    }) { Text("Réinitialiser") }
                },
                dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Annuler") } }
            )
        }
    }
}

@Composable
private fun GlassSwatch(hex: String, label: String, selected: Color, onClick: () -> Unit) {
    val color = if (hex.isBlank()) Color(0xFFE9E5DD) else Color(android.graphics.Color.parseColor(hex))
    val chosen = if (hex.isBlank()) selected == Color.Unspecified else selected == color
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(31.dp).clip(CircleShape).background(color)
                .border(if (chosen) 3.dp else 1.dp, if (chosen) Zcolors.text else Zcolors.border2, CircleShape)
                .clickable(onClick = onClick)
        )
        Text(label, color = Zcolors.text3, fontSize = 9.sp)
    }
}

@Composable
private fun GlassSliderRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF1E7DC)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF9B7058), modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(label, Modifier.weight(1f), color = Zcolors.text, style = MaterialTheme.typography.titleSmall)
            Text(format(value), color = Zcolors.text2, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
