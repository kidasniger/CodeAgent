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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
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

    LaunchedEffect(Unit) { events = viewModel.activityRepoSnapshot() }

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
                checked = viewModel.settingsManager.allowRead,
                onToggle = { viewModel.settingsManager.allowRead = it },
                icon = Icons.Default.Visibility,
                tint = c.cyan
            )
            ModeRow(
                title = "Mode lecture seule",
                subtitle = "Bloque tout : écritures, suppressions et commandes. L'agent ne peut que lire et analyser.",
                checked = viewModel.settingsManager.lectureSeule,
                onToggle = { viewModel.settingsManager.lectureSeule = it },
                icon = Icons.Default.Lock,
                tint = c.text2
            )
            ModeRow(
                title = "Écrire les fichiers",
                subtitle = "Créations et modifications sans confirmation.",
                checked = viewModel.settingsManager.allowWrite,
                onToggle = { viewModel.settingsManager.allowWrite = it },
                icon = Icons.Default.Bolt,
                tint = c.amber
            )
            ModeRow(
                title = "Supprimer des fichiers",
                subtitle = "Suppressions sans confirmation.",
                checked = viewModel.settingsManager.allowDelete,
                onToggle = { viewModel.settingsManager.allowDelete = it },
                icon = Icons.Default.Delete,
                tint = c.red
            )
            ModeRow(
                title = "Exécuter des commandes",
                subtitle = "Commandes shell sans confirmation (git push, commit et commandes destructrices restent confirmés).",
                checked = viewModel.settingsManager.allowExecute,
                onToggle = { viewModel.settingsManager.allowExecute = it },
                icon = Icons.Default.Terminal,
                tint = c.violet
            )
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
