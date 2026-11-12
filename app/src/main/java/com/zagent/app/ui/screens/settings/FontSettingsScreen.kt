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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun FontSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var selectedFamily by remember(viewModel.fontFamilyId) { mutableStateOf(viewModel.fontFamilyId) }
    var size by remember(viewModel.fontScale) { mutableFloatStateOf(viewModel.fontScale) }
    val families = listOf(
        "INTER" to "Inter",
        "SYSTEM" to "Système",
        "SPACE_GROTESK" to "Space Grotesk"
    )
    SettingsPageScaffold(title = stringResource(R.string.ui_police), onBack = onBack) {
        SettingsSectionLabel(stringResource(R.string.ui_famille))
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.ui_police_interface), color = MaterialTheme.colorScheme.onSurface)
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
                    stringResource(R.string.ui_apercu_typo),
                    fontFamily = when (selectedFamily) {
                        "SYSTEM" -> androidx.compose.ui.text.font.FontFamily.Default
                        "SPACE_GROTESK" -> SpaceGrotesk
                        else -> Inter
                    },
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.ui_font_applique),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsSectionLabel(stringResource(R.string.ui_taille_texte))
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.ui_echelle), color = MaterialTheme.colorScheme.onSurface)
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
                    Text(stringResource(R.string.ui_petite), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.ui_normale), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.ui_grande), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // MA83 : tailles de police du code, réglables aussi au pincement.
        SettingsSectionLabel(stringResource(R.string.ui_taille_code))
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.ui_editeur), color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.editorFontSize} sp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = viewModel.editorFontSize.toFloat(),
                    onValueChange = { viewModel.updateEditorFontSize(it.roundToInt()) },
                    valueRange = 10f..22f,
                    steps = 11
                )
                Text(
                    stringResource(R.string.ui_pin_code),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.ui_terminal), color = MaterialTheme.colorScheme.onSurface)
                    Text("${viewModel.terminalFontSize} sp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = viewModel.terminalFontSize.toFloat(),
                    onValueChange = { viewModel.updateTerminalFontSize(it.roundToInt()) },
                    valueRange = 10f..22f,
                    steps = 11
                )
                Text(
                    stringResource(R.string.ui_pin_terminal),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsInfoRow(
            "Aperçu",
            "La taille choisie modifie les styles Material 3 de l’application sans changer la taille système d’Android."
        )
    }
}