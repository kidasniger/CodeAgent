package com.zagent.app.ui.screens.settings

import androidx.compose.runtime.Composable

@Composable
fun DensitySettingsScreen(onBack: () -> Unit) {
    SettingsPageScaffold(title = "Densité", onBack = onBack) {
        SettingsSectionLabel("MISE EN PAGE")
        SettingsCard {
            SettingsInfoRow("Espacement", "L’espace entre les éléments sera réglable ici.")
            SettingsInsetDivider()
            SettingsInfoRow("Compacité", "La densité des cartes et des lignes sera réglable ici.")
        }
    }
}
