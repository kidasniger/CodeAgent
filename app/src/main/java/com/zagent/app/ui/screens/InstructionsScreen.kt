package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MA63 — Écran Instructions personnalisées : éditeur plein écran avec compteur
// de caractères, 5 modèles prêts à l'emploi et instructions dédiées au projet
// actif (prioritaires sur le texte global).
// ─────────────────────────────────────────────────────────────────────────────

private data class InstrTemplate(val label: String, val text: String)

private val instrTemplates = listOf(
    InstrTemplate(
        "Concis",
        "Réponds de façon très concise : 5 lignes maximum par réponse, va droit au but, pas de résumé inutile, pas d'explication non demandée. Le code d'abord, les mots ensuite."
    ),
    InstrTemplate(
        "Pédagogue",
        "Explique chaque changement simplement, comme à un développeur junior : une phrase sur le pourquoi, une sur le comment. Ajoute un mini exemple quand c'est utile."
    ),
    InstrTemplate(
        "Revue de code",
        "Avant toute modification, pointe les problèmes du code existant (bugs, sécurité, performance, lisibilité) sous forme de liste courte, puis propose un correctif minimal et justifie-le."
    ),
    InstrTemplate(
        "Mobile-first",
        "Conçois d'abord pour les petits écrans : mises en page fluides, cibles tactiles d'au moins 44 px, textes lisibles sans zoom, puis améliore pour les grands écrans."
    ),
    InstrTemplate(
        "Accessibilité",
        "Respecte l'accessibilité : contrastes suffisants, textes alternatifs, navigation au clavier, focus visible, sémantique correcte, jamais d'information portée par la couleur seule."
    )
)

@Composable
fun InstructionsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    var global by remember { mutableStateOf(viewModel.customInstructionsInput) }
    var project by remember { mutableStateOf(viewModel.projectInstructionsText()) }

    Lot5Screen(title = "Instructions personnalisées", subtitle = "Ajoutées au prompt de l'agent", onBack = onBack) {
        // ── Instructions globales ────────────────────────────────────────────
        Lot5Section("Globales — toutes les conversations")
        GlassCard {
            EditorBox(
                value = global,
                onChange = {
                    global = it
                    viewModel.setCustomInstructions(it)
                },
                placeholder = "Ex : réponds en français, privilégie les solutions simples, commente le code…"
            )
            Spacer(Modifier.height(6.dp))
            Text(
                global.length.toString() + " caractères",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                color = c.text3,
                modifier = Modifier.align(Alignment.End)
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Instructions du projet actif ─────────────────────────────────────
        Lot5Section("Projet actif — " + viewModel.folderLabel)
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = c.violet, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Prioritaires sur le texte global pour ce projet.",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text2
                )
            }
            Spacer(Modifier.height(8.dp))
            EditorBox(
                value = project,
                onChange = {
                    project = it
                    viewModel.setProjectInstructionsForCurrent(it)
                },
                placeholder = "Ex : ce projet utilise Kotlin + Compose, ne touche jamais au dossier build…"
            )
            Spacer(Modifier.height(6.dp))
            Text(
                project.length.toString() + " caractères",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                color = c.text3,
                modifier = Modifier.align(Alignment.End)
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Modèles prêts à l'emploi ─────────────────────────────────────────
        Lot5Section("Modèles prêts à l'emploi")
        Text(
            "Un tap remplace le texte global par le modèle choisi.",
            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
            color = c.text3
        )
        Spacer(Modifier.height(8.dp))
        instrTemplates.forEach { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(14.dp))
                    .clickable {
                        global = t.text
                        viewModel.setCustomInstructions(t.text)
                    }
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Description, null, tint = c.cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.label,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                        color = c.text
                    )
                    Text(
                        t.text,
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = c.text2,
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditorBox(value: String, onChange: (String) -> Unit, placeholder: String) {
    val c = Zcolors
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text3) },
        textStyle = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 19.sp, color = c.text),
        minLines = 4,
        maxLines = 10,
        modifier = Modifier.fillMaxWidth()
    )
}
