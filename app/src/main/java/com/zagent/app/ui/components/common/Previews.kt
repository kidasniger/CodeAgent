package com.zagent.app.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.theme.GlassMaterial
import com.zagent.app.ui.theme.GlassSettings
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.AppScreen
import dev.chrisbanes.haze.HazeState

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 100 — aperçus Android Studio des composants partagés, déclinés
// Clair / Sombre / Verre Rapide / Verre Qualité. Les écrans complets dépendent
// de MainViewModel (état applicatif) : on prévisualise les blocs d'interface
// pure. Le verre est simulé en fournissant un [GlassSettings] différent via
// LocalGlass — le même mécanisme que l'application réelle.
// ─────────────────────────────────────────────────────────────────────────────

/** Aperçu thématique : thème clair/sombre + matériau de verre fourni. */
@Composable
private fun ApercuTheme(
    sombre: Boolean,
    verre: GlassSettings = GlassSettings(enabled = false),
    contenu: @Composable () -> Unit
) {
    val haze = remember { HazeState() }
    ZAgentTheme(darkTheme = sombre) {
        CompositionLocalProvider(
            LocalGlass provides verre,
            LocalHazeState provides haze
        ) {
            Surface(color = Zcolors.bg) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    contenu()
                }
            }
        }
    }
}

@Preview(name = "Boutons · Clair", showBackground = true)
@Preview(name = "Boutons · Sombre", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApercuBoutons() {
    ApercuTheme(sombre = false) {
        GradientButton("Réessayer", onClick = {}, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Annuler", onClick = {}, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(Icons.Default.Search, "Rechercher", onClick = {})
            CircleIconButton(Icons.Default.Close, "Fermer", onClick = {}, iconTint = Zcolors.red)
        }
        TypingPill("L'agent travaille", onStop = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge("Valide", Zcolors.green)
            StatusBadge("Agent", Zcolors.cyan)
            StatusBadge("Erreur", Zcolors.red)
        }
    }
}

@Preview(name = "Cartes · Verre rapide · Clair", showBackground = true)
@Composable
private fun ApercuCartesVerreRapide() {
    ApercuTheme(
        sombre = false,
        verre = GlassSettings(enabled = true, material = GlassMaterial.FASTEST, film = 0.20f)
    ) {
        GlassCard {
            Text("Carte verre rapide", color = Zcolors.text)
            Text("Film 0,20 — repli sans flou (API 26-31 et économies d'énergie).", color = Zcolors.text2)
        }
    }
}

@Preview(name = "Cartes · Verre qualité · Sombre", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApercuCartesVerreQualite() {
    ApercuTheme(
        sombre = true,
        verre = GlassSettings(enabled = true, material = GlassMaterial.QUALITY, film = 0.45f, frost = 18.dp)
    ) {
        GlassCard {
            Text("Carte verre qualité", color = Zcolors.text)
            Text("Film 0,45 — flou 18 dp (API 31+).", color = Zcolors.text2)
        }
    }
}

@Preview(name = "Onglets · Clair", showBackground = true)
@Preview(name = "Onglets · Sombre", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApercuBarreOnglets() {
    ApercuTheme(
        sombre = false,
        verre = GlassSettings(enabled = true, material = GlassMaterial.BALANCED)
    ) {
        FloatingTabBar(
            currentScreen = AppScreen.CHAT,
            onNavigate = {},
            chatBadge = 3
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// corr-017 — une preview par composant clé, avec données factices.
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "État vide · Clair", showBackground = true)
@Preview(name = "État vide · Sombre", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApercuEtatVide() {
    ApercuTheme(sombre = false) {
        ZEmptyState(
            kind = ZEmptyKind.FOLDER,
            title = "Aucun projet choisi",
            body = "Crée ou choisis un dossier pour que l'agent puisse lire et écrire les fichiers.",
            actionText = "Choisir un projet",
            onAction = {}
        )
        EmptyState(
            icon = Icons.Default.Search,
            tint = Zcolors.cyan,
            title = "Aucun résultat",
            body = "Aucune conversation ne correspond à « cache ».",
            primaryText = "Réinitialiser",
            onPrimary = {}
        )
    }
}

@Preview(name = "Menu · Clair", showBackground = true)
@Composable
private fun ApercuMenu() {
    ApercuTheme(sombre = false) {
        ZMenu(expanded = true, onDismiss = {}) {
            ZMenuItem("Épingler", Icons.Default.Close, onClick = {})
            ZMenuItem("Renommer", Icons.Default.Search, onClick = {})
            ZMenuSeparator()
            ZMenuItem("Supprimer", Icons.Default.Close, onClick = {})
        }
    }
}

@Preview(name = "Puces modèles · Sombre", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApercuPucesModeles() {
    ApercuTheme(sombre = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelChip("glm-4.7", selected = true, onClick = {})
            ModelChip("nvidia/llama-3.1-70b", selected = false, onClick = {})
            MonoChip("2 048 tokens")
            GradientProgress(0.42f)
        }
    }
}

@Preview(name = "Squelettes · Clair", showBackground = true)
@Composable
private fun ApercuSquelettes() {
    ApercuTheme(sombre = false) {
        RectangleSquelette(Modifier.fillMaxWidth())
        SkeletonListe(lignes = 3)
        SkeletonCartesModeles(cartes = 2)
    }
}

@Preview(name = "Réglage curseur · Clair", showBackground = true)
@Composable
private fun ApercuCurseur() {
    ApercuTheme(sombre = false) {
        ZSliderRow(
            "Budget d'étapes",
            20f,
            5f..100f,
            { "20 étapes" },
            onChange = {}
        )
    }
}
