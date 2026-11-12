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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zagent.app.R
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun DensitySettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var density by remember(viewModel.densityScale) { mutableFloatStateOf(viewModel.densityScale) }
    val presets = listOf(
        0.85f to R.string.density_compacte,
        1.00f to R.string.density_equilibree,
        1.10f to R.string.density_aeree
    )
    SettingsPageScaffold(title = stringResource(R.string.ui_densite), onBack = onBack) {
        SettingsSectionLabel(stringResource(R.string.ui_mise_en_page))
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(stringResource(R.string.ui_compacite_interface), color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (value, labelRes) ->
                        FilterChip(
                            selected = kotlin.math.abs(density - value) < 0.015f,
                            onClick = {
                                density = value
                                viewModel.updateDensityScale(value)
                            },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.ui_espacement), color = MaterialTheme.colorScheme.onSurface)
                    Text("${(density * 100).toInt()} %", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = density,
                    onValueChange = {
                        density = it
                        viewModel.updateDensityScale(it)
                    },
                    valueRange = 0.80f..1.12f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.ui_dense), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.ui_confortable), style = MaterialTheme.typography.labelSmall)
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