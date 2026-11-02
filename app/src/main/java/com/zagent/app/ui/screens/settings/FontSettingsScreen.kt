package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun FontSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var selectedFamily by remember(viewModel.fontFamilyId) { mutableStateOf(viewModel.fontFamilyId) }
    var size by remember(viewModel.fontScale) { mutableFloatStateOf(viewModel.fontScale) }
    val families = listOf(
        "INTER" to "Inter",
        "SYSTEM" to "Système",
        "SPACE_GROTESK" to "Space Grotesk"
    )
    SettingsPageScaffold(title = "Police", onBack = onBack) {
        SettingsSectionLabel("FAMILLE")
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Police de l’interface", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    families.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedFamily == id,
                            onClick = {
                                selectedFamily = id
                                viewModel.updateFontFamily(id)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aperçu de la typographie",
                    fontFamily = when (selectedFamily) {
                        "SYSTEM" -> androidx.compose.ui.text.font.FontFamily.Default
                        "SPACE_GROTESK" -> SpaceGrotesk
                        else -> Inter
                    },
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Les changements s’appliquent immédiatement à toute l’application.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsSectionLabel("TAILLE DU TEXTE")
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Échelle", color = MaterialTheme.colorScheme.onSurface)
                    Text("${(size * 100).toInt()} %", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = size,
                    onValueChange = {
                        size = it
                        viewModel.updateFontScale(it)
                    },
                    valueRange = 0.85f..1.25f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Petite", style = MaterialTheme.typography.labelSmall)
                    Text("Normale", style = MaterialTheme.typography.labelSmall)
                    Text("Grande", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsInfoRow(
            "Aperçu",
            "La taille choisie modifie les styles Material 3 de l’application sans changer la taille système d’Android."
        )
    }
}
