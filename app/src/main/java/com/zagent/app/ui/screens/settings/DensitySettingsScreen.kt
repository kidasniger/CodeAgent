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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun DensitySettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var density by remember(viewModel.densityScale) { mutableFloatStateOf(viewModel.densityScale) }
    val presets = listOf(
        0.90f to "Compacte",
        1.00f to "Équilibrée",
        1.10f to "Aérée"
    )
    SettingsPageScaffold(title = "Densité", onBack = onBack) {
        SettingsSectionLabel("MISE EN PAGE")
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Compacité de l’interface", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (value, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(density - value) < 0.015f,
                            onClick = {
                                density = value
                                viewModel.updateDensityScale(value)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Espacement", color = MaterialTheme.colorScheme.onSurface)
                    Text("${(density * 100).toInt()} %", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = density,
                    onValueChange = {
                        density = it
                        viewModel.updateDensityScale(it)
                    },
                    valueRange = 0.88f..1.12f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dense", style = MaterialTheme.typography.labelSmall)
                    Text("Confortable", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsInfoRow(
            "Application immédiate",
            "La densité ajuste les dimensions dp des composants et les espacements de l’interface. Le texte conserve son échelle indépendante."
        )
    }
}
