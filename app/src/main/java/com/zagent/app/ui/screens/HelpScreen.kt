package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.Lot5Screen
import com.zagent.app.ui.components.Lot5Section
import com.zagent.app.ui.components.MonoLine
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MA67 — Écran Raccourcis & gestes : gestes (glisser pour l'historique,
// long-press), commandes slash, raccourcis clavier externe, carte « Astuce du
// jour » en haut.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HelpScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    val tips = remember {
        listOf(
            "Tape « / » dans le champ pour lancer une commande, « @ » pour joindre un fichier du projet.",
            "Maintiens ton doigt sur un message pour le citer, le modifier ou le supprimer.",
            "Le bouton d'envoi se transforme en carré pendant la génération : un tap stoppe l'agent.",
            "Écris le chemin d'un fichier dans ta demande : l'agent le lit directement.",
            "Le mode Intelligent n'autorise que les nouveaux fichiers sans confirmation.",
            "Un appui long sur un bloc de code du chat permet de l'appliquer à un fichier."
        )
    }
    val tip = remember { tips.random() }

    Lot5Screen(title = "Raccourcis & gestes", subtitle = "Tout ce que ZAgent sait faire au doigt", onBack = onBack) {
        // ── Astuce du jour ───────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.cyanBg)
                .border(1.dp, c.cyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = c.cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Astuce du jour",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.cyan
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    tip,
                    style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp),
                    color = c.text
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Gestes ───────────────────────────────────────────────────────────
        Lot5Section("Gestes")
        GestureRow(Icons.Default.Swipe, "Tiroir Historique", "Menu ☰ dans la barre du chat : conversations passées, glisse une ligne pour l'épingler ou la supprimer.")
        GestureRow(Icons.Default.TouchApp, "Long-press sur un message", "Cite, copie, modifie ou supprime n'importe quel message du fil.")
        GestureRow(Icons.Default.Bolt, "Champ de saisie", "Le champ s'étend jusqu'à 8 lignes ; le bouton devient Stop pendant la génération.")

        Spacer(Modifier.height(16.dp))

        // ── Commandes slash ──────────────────────────────────────────────────
        Lot5Section("Commandes slash")
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            listOf(
                "/nouveau" to "Démarrer une nouvelle conversation",
                "/modèle" to "Choisir le modèle de la conversation",
                "/diff" to "Voir les changements Git du projet",
                "/tester" to "Lancer les tests du projet et corriger les échecs",
                "/effacer" to "Effacer les messages du fil",
                "/aide" to "Afficher l'aide directement dans le chat"
            ).forEach { (cmd, desc) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.cyanBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            cmd,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = c.cyan
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        desc,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                        color = c.text2
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Mentions @ ───────────────────────────────────────────────────────
        Lot5Section("Joindre des fichiers avec @")
        GlassCard {
            Text(
                "Tape « @ » puis quelques lettres du nom : l'autocomplétion propose les fichiers du projet. Le contenu du fichier cité est joint au message.",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp),
                color = c.text
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Clavier externe ──────────────────────────────────────────────────
        Lot5Section("Clavier externe (Bluetooth / OTG)")
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            listOf(
                "Entrée" to "Envoyer le message",
                "Maj + Entrée" to "Retour à la ligne dans le champ",
                "Échap" to "Fermer les menus et feuilles ouverts"
            ).forEach { (key, desc) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.surface3)
                            .border(1.dp, c.border2, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            key,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                            color = c.text
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        desc,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                        color = c.text2
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GestureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(c.violetBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.violet, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                color = c.text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp),
                color = c.text2
            )
        }
    }
}
