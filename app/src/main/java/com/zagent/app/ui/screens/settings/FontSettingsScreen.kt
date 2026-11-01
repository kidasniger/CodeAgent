package com.zagent.app.ui.screens.settings

import androidx.compose.runtime.Composable

@Composable
fun FontSettingsScreen(onBack: () -> Unit) {
    SettingsPageScaffold(title = "Police", onBack = onBack) {
        SettingsSectionLabel("TYPOGRAPHIE")
        SettingsCard {
            SettingsInfoRow("Famille", "Le choix de la police de l’interface sera disponible ici.")
            SettingsInsetDivider()
            SettingsInfoRow("Taille", "La taille des textes pourra être ajustée ici.")
        }
    }
}
