package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.ActivityEvent
import com.zagent.app.data.ActivityStatus
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA64 — Écran Permissions & sécurité : chemins « toujours autorisés » avec
// suppression, interrupteurs par type d'action (lire, écrire, supprimer,
// exécuter), mode Auto/Demander, historique des commandes approuvées avec date.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PermissionsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    var allowedPaths by remember { mutableStateOf(viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()) }
    var events by remember { mutableStateOf<List<ActivityEvent>>(emptyList()) }

    // corr-096 — Interrupteurs en état Compose : les préférences chiffrées
    // n'émettent rien d'observable, donc sans miroir local le commutateur ne
    // se redessinerait pas et l'écran pourrait montrer un état périmé au
    // retour des réglages système.
    var lecture by remember { mutableStateOf(viewModel.settingsManager.allowRead) }
    var ecriture by remember { mutableStateOf(viewModel.settingsManager.allowWrite) }
    var suppression by remember { mutableStateOf(viewModel.settingsManager.allowDelete) }
    var execution by remember { mutableStateOf(viewModel.settingsManager.allowExecute) }
    var lectureSeule by remember { mutableStateOf(viewModel.settingsManager.lectureSeule) }
    var rechercheWeb by remember { mutableStateOf(viewModel.settingsManager.rechercheWebActive) } // feat-032
    var repliAuto by remember { mutableStateOf(viewModel.settingsManager.repliAutoActif) } // feat-025

    LaunchedEffect(Unit) {
        events = viewModel.activityRepoSnapshot()
        // Ré-synchronisation à chaque ouverture : reflète l'état réel.
        lecture = viewModel.settingsManager.allowRead
        ecriture = viewModel.settingsManager.allowWrite
        suppression = viewModel.settingsManager.allowDelete
        execution = viewModel.settingsManager.allowExecute
        lectureSeule = viewModel.settingsManager.lectureSeule
        rechercheWeb = viewModel.settingsManager.rechercheWebActive // feat-032
        allowedPaths = viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()
    }

    OverlayScreen(title = "Permissions & sécurité", subtitle = "Ce que l'agent peut faire sans te demander", onBack = onBack) {
        // ── Mode général ─────────────────────────────────────────────────────
        OverlaySection("Mode de l'agent")
        GlassCard {
            ModeRow(
                title = "Mode Auto",
                subtitle = "L'agent enchaîne sans confirmation (git push, commit et commandes destructrices restent confirmés).",
                checked = viewModel.autoModeInput,
                onToggle = { viewModel.autoModeInput = it; viewModel.settingsManager.autoMode = it }
            )
            ModeRow(
                title = "Mode Intelligent",
                subtitle = "Nouveaux fichiers et modifications légères sans confirmation, écrasements et suppressions confirmés.",
                checked = viewModel.smartModeInput,
                onToggle = { viewModel.setSmartMode(it) }
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Interrupteurs par type d'action ──────────────────────────────────
        OverlaySection("Toujours autoriser")
        GlassCard {
            ModeRow(
                title = "Lire les fichiers",
                subtitle = "L'agent peut explorer et lire le projet.",
                checked = lecture,
                onToggle = { viewModel.settingsManager.allowRead = it; lecture = it },
                icon = Icons.Default.Visibility,
                tint = c.cyan
            )
            ModeRow(
                title = "Mode lecture seule",
                subtitle = "Bloque tout : écritures, suppressions et commandes. L'agent ne peut que lire et analyser.",
                checked = lectureSeule,
                onToggle = { viewModel.settingsManager.lectureSeule = it; lectureSeule = it },
                icon = Icons.Default.Lock,
                tint = c.text2
            )
            ModeRow(
                title = "Écrire les fichiers",
                subtitle = "Créations et modifications sans confirmation.",
                checked = ecriture,
                onToggle = { viewModel.settingsManager.allowWrite = it; ecriture = it },
                icon = Icons.Default.Bolt,
                tint = c.amber
            )
            ModeRow(
                title = "Supprimer des fichiers",
                subtitle = "Suppressions sans confirmation.",
                checked = suppression,
                onToggle = { viewModel.settingsManager.allowDelete = it; suppression = it },
                icon = Icons.Default.Delete,
                tint = c.red
            )
            ModeRow(
                title = "Exécuter des commandes",
                subtitle = "Commandes shell sans confirmation (git push, commit et commandes destructrices restent confirmés).",
                checked = execution,
                onToggle = { viewModel.settingsManager.allowExecute = it; execution = it },
                icon = Icons.Default.Terminal,
                tint = c.violet
            )
            ModeRow(
                title = "Recherche web", // feat-032
                subtitle = "L'agent peut chercher sur le web (DuckDuckGo) et lire des pages avec fetch_url pour la documentation.",
                checked = rechercheWeb,
                onToggle = { viewModel.settingsManager.rechercheWebActive = it; rechercheWeb = it },
                icon = Icons.Default.Language,
                tint = c.cyan
            )
            ModeRow(
                title = "Modèle de secours automatique", // feat-025
                subtitle = "En cas de surcharge ou de clé refusée, bascule tout seul vers un modèle disponible (désactive pour rester sur ton modèle).",
                checked = repliAuto,
                onToggle = { viewModel.settingsManager.repliAutoActif = it; repliAuto = it },
                icon = Icons.Default.SwapHoriz,
                tint = c.amber
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── feat-045 — Permissions par outil : autoriser / demander / refuser ──
        OverlaySection("Permissions par outil")
        GlassCard {
            Text(
                "Réglage fin : chaque outil de l'agent peut être autorisé sans confirmation, laissé en demande, ou interdit.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
                color = c.text2,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            var perms by remember { mutableStateOf(viewModel.settingsManager.permissionsOutils) }
            // ux-020 — variante par projet : « toujours autoriser » peut être
            // restreint au dossier ouvert, sans toucher au réglage global.
            val nomProjetCourant = viewModel.workingFolderUriString?.let { java.io.File(it).name }.orEmpty()
            var permsProjet by remember(nomProjetCourant) {
                mutableStateOf(viewModel.settingsManager.permissionsOutilsParProjet[nomProjetCourant] ?: emptyMap())
            }
            if (nomProjetCourant.isNotBlank()) {
                Text(
                    "Projet ouvert : $nomProjetCourant — les puces « Projet » ne concernent que ce dossier.",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            com.zagent.app.data.AgentTools.nomsOutils.forEach { nom ->
                val courant = perms[nom].orEmpty()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLine(nom, c.text, Modifier.weight(1f))
                    PuceOutil("Auto", courant == "autoriser", c.green) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, if (courant == "autoriser") "" else "autoriser")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    PuceOutil("Dem.", courant == "" , c.cyan) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, "")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    PuceOutil("Refus", courant == "refuser", c.red) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, if (courant == "refuser") "" else "refuser")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    // ux-020 — autorisation restreinte à ce projet (si un dossier est ouvert).
                    if (nomProjetCourant.isNotBlank()) {
                        val proj = permsProjet[nom].orEmpty()
                        PuceOutil("Projet", proj == "autoriser", c.violet) {
                            viewModel.settingsManager.reglerPermissionOutilProjet(
                                nomProjetCourant, nom, if (proj == "autoriser") "" else "autoriser"
                            )
                            permsProjet = viewModel.settingsManager.permissionsOutilsParProjet[nomProjetCourant] ?: emptyMap()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Chemins toujours autorisés ───────────────────────────────────────
        OverlaySection("Fichiers toujours autorisés")
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            if (allowedPaths.isEmpty()) {
                Text(
                    "Aucun chemin pour l'instant. Quand l'agent propose une écriture, choisis « Toujours autoriser » pour l'ajouter ici.",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = c.text2
                )
            }
            allowedPaths.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = c.green, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(10.dp))
                    MonoLine(p, c.text, Modifier.weight(1f))
                    Text(
                        "Retirer",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.red,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                viewModel.removeAlwaysAllowedPath(p)
                                allowedPaths = viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Historique des commandes approuvées ──────────────────────────────
        OverlaySection("Commandes approuvées récemment")
        val approved = remember(events) {
            events.filter { it.kind == "commande" && (it.status == ActivityStatus.OK || it.status == ActivityStatus.ERREUR) }.take(15)
        }
        if (approved.isEmpty()) {
            OverlayEmptyState(
                Icons.Default.Security,
                "Aucune commande approuvée",
                "Les commandes que tu valides (ou que le mode Auto exécute) apparaîtront ici avec leur date."
            )
        } else {
            val df = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                approved.forEach { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            MonoLine(e.label, c.text)
                            Text(
                                df.format(Date(e.ts)) + " · " + e.detail.ifBlank { e.status },
                                style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                                color = c.text3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            e.status,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (e.status == ActivityStatus.OK) c.green else c.amber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = Zcolors.cyan
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = c.text
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                color = c.text2
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.cyan,
                checkedThumbColor = c.onGrad,
                uncheckedTrackColor = c.surface3,
                uncheckedThumbColor = c.text3
            )
        )
    }
}

/** feat-045 — Petit choix d'état de permission (Auto / Dem. / Refus) par outil. */
@Composable
private fun PuceOutil(label: String, activee: Boolean, teinte: Color, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        color = if (activee) c.bg else c.text2,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (activee) teinte else c.surface3)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}

