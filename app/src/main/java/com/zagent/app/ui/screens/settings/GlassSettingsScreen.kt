package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.components.GlassPreviewCard
import com.zagent.app.ui.theme.Zcolors

@Composable
fun GlassSettingsScreen(onBack: () -> Unit) {
    SettingsPageScaffold(title = "Verre", onBack = onBack) {
        GlassPreviewCard(
            modifier = Modifier.fillMaxWidth(),
            material = "Liquid",
            glassColor = Zcolors.cyan,
            blur = 0.55f,
            tint = 0.35f,
            film = 0.22f,
            activeTab = "Chat"
        )

        SettingsSectionLabel("MATÉRIAU")
        SettingsCard {
            SettingsInfoRow("Matériau", "Les choix de surface vitrée seront configurés ici.")
        }
        SettingsSectionLabel("COULEUR")
        SettingsCard {
            SettingsInfoRow("Couleur", "Les teintes et accents du verre seront réglables ici.")
        }
        SettingsSectionLabel("EFFETS")
        SettingsCard {
            SettingsInfoRow("Flou", "Le niveau de flou et son intensité seront réglables ici.")
            SettingsInsetDivider()
            SettingsInfoRow("Teinte", "La teinte appliquée aux surfaces sera réglable ici.")
            SettingsInsetDivider()
            SettingsInfoRow("Film", "Le voile coloré et sa transparence seront réglables ici.")
            SettingsInsetDivider()
            SettingsInfoRow("Onglet actif", "Le traitement visuel de l’onglet sélectionné sera réglable ici.")
        }
    }
}
