package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.data.ActivityStatus
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySearchField
import com.zagent.app.ui.components.common.OverlayTabs
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// err-097 — Journal d'erreurs dédié : toutes les entrées d'échec de
// l'application réunies en une chronologie unique, filtrable par source
// (Agent / Réseau / Crash), par texte, et partageable sans secrets.
// Sources : activité agent en erreur/refus, journal réseau IA (sans clés),
// rapports de crash persistés dans filesDir/crash.
// ─────────────────────────────────────────────────────────────────────────────

/** Une entrée du journal : horodatage, source, titre, détail, criticité. */
data class EntreeJournalErreurs(
    val ts: Long,
    val source: String, // "Agent" | "Réseau" | "Crash"
    val titre: String,
    val detail: String,
    val critique: Boolean
)

/** Collecte les trois sources du journal — aucune clé API n'y figure jamais. */
fun chargerJournalErreurs(viewModel: MainViewModel, contexte: android.content.Context): List<EntreeJournalErreurs> {
    val entrees = mutableListOf<EntreeJournalErreurs>()

    // 1) Activité agent : outils en erreur ou refusés (le refus est moins critique).
    runCatching {
        viewModel.activityRepoSnapshot().forEach { ev ->
            if (ev.status == ActivityStatus.ERREUR || ev.status == ActivityStatus.REFUSE) {
                entrees += EntreeJournalErreurs(
                    ts = ev.ts,
                    source = "Agent",
                    titre = ev.kind,
                    detail = listOf(ev.label, ev.detail).filter { it.isNotBlank() }.joinToString(" — "),
                    critique = ev.status == ActivityStatus.ERREUR
                )
            }
        }
    }

    // 2) Journal réseau IA : lignes non réussies (HTTP 4xx/5xx, DNS, TLS, E/S),
    //    au format « HH:mm:ss — hôte — événement ». Aucune clé n'est journalisée.
    runCatching {
        val df = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
        val jour = java.util.Calendar.getInstance()
        viewModel.journalReseauIA().forEach { ligne ->
            val morceaux = ligne.split(" — ", limit = 3)
            if (morceaux.size >= 3) {
                val (heure, hote, evenement) = morceaux
                if (evenement.startsWith("HTTP 2")) return@forEach
                jour.set(java.util.Calendar.HOUR_OF_DAY, heure.take(2).toIntOrNull() ?: 0)
                jour.set(java.util.Calendar.MINUTE, heure.drop(3).take(2).toIntOrNull() ?: 0)
                jour.set(java.util.Calendar.SECOND, heure.takeLast(2).toIntOrNull() ?: 0)
                entrees += EntreeJournalErreurs(
                    ts = jour.timeInMillis,
                    source = "Réseau",
                    titre = hote,
                    detail = evenement,
                    critique = evenement.startsWith("HTTP 5") || evenement.startsWith("Erreur")
                )
            } else if (ligne.isNotBlank()) {
                entrees += EntreeJournalErreurs(0L, "Réseau", "Réseau IA", ligne, false)
            }
        }
    }

    // 3) Rapports de crash persistés (err-079) — les plus récents d'abord,
    //    contenu tronqué comme dans le rapport de diagnostic.
    runCatching {
        val dossier = java.io.File(contexte.applicationContext.filesDir, "crash")
        dossier.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.take(10)
            ?.forEach { f ->
                entrees += EntreeJournalErreurs(
                    ts = f.lastModified(),
                    source = "Crash",
                    titre = f.name,
                    detail = runCatching { f.readText().take(2000) }.getOrDefault("(illisible)"),
                    critique = true
                )
            }
    }

    return entrees.sortedByDescending { it.ts }
}

@Composable
fun JournalErreursScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    val ctx = LocalContext.current
    var entrees by remember { mutableStateOf<List<EntreeJournalErreurs>>(emptyList()) }
    var filtre by remember { mutableStateOf(0) } // 0 = tout, 1 = Agent, 2 = Réseau, 3 = Crash
    var requete by remember { mutableStateOf("") }
    var charge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entrees = runCatching { chargerJournalErreurs(viewModel, ctx) }.getOrDefault(emptyList())
        charge = true
    }

    val filtrees = remember(entrees, filtre, requete) {
        val parSource = when (filtre) {
            1 -> entrees.filter { it.source == "Agent" }
            2 -> entrees.filter { it.source == "Réseau" }
            3 -> entrees.filter { it.source == "Crash" }
            else -> entrees
        }
        if (requete.isBlank()) parSource
        else parSource.filter {
            it.titre.contains(requete, ignoreCase = true) || it.detail.contains(requete, ignoreCase = true)
        }
    }

    OverlayScreen(
        title = stringResource(R.string.journal_titre),
        subtitle = filtrees.size.toString() + " entrée" + if (filtrees.size > 1) "s" else "",
        onBack = onBack
    ) {
        OverlayTabs(listOf(R.string.onglet_tout, R.string.onglet_agent, R.string.onglet_reseau, R.string.onglet_crash), filtre) { filtre = it }
        Spacer(Modifier.height(10.dp))
        OverlaySearchField(requete, { requete = it }, stringResource(R.string.journal_recherche))
        Spacer(Modifier.height(10.dp))

        // Export partageable (sans secrets) — même principe que le rapport de diagnostic.
        val ctxPartage = LocalContext.current
        Text(
            stringResource(R.string.journal_partager),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
            color = c.cyan,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .clickable {
                    val partage = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            filtrees.joinToString(",\n", prefix = "[", postfix = "]") { e ->
                                "{" +
                                    "\"ts\":" + e.ts + "," +
                                    "\"source\":\"" + e.source.replace("\"", "'") + "\"," +
                                    "\"titre\":\"" + e.titre.replace("\"", "'").replace("\n", " ") + "\"," +
                                    "\"detail\":\"" + e.detail.replace("\"", "'").replace("\n", " ").take(600) + "\"" +
                                    "}"
                            }
                        )
                        putExtra(android.content.Intent.EXTRA_TITLE, "journal-erreurs-zagent.json")
                    }
                    ctxPartage.startActivity(android.content.Intent.createChooser(partage, "Partager le journal"))
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Spacer(Modifier.height(10.dp))

        if (charge && filtrees.isEmpty()) {
            OverlayEmptyState(
                Icons.Default.BugReport,
                stringResource(R.string.journal_vide_titre),
                stringResource(R.string.journal_vide_texte)
            )
            return@OverlayScreen
        }

        val df = remember { SimpleDateFormat("d MMM · HH:mm:ss", Locale.getDefault()) }
        Column {
            filtrees.take(200).forEach { e ->
                // resp-07 — IntrinsicSize.Min : le connecteur s'étire avec le
                // contenu (échelle de police 2.0) au lieu d'une hauteur figée.
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Colonne temporelle : point coloré + ligne de liaison
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight()
                            .padding(bottom = 14.dp)
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        e.source == "Crash" -> c.red
                                        e.source == "Réseau" -> if (e.critique) c.red else c.amber
                                        else -> if (e.critique) c.red else c.amber
                                    }
                                )
                        )
                        Box(
                            Modifier
                                .width(1.5.dp)
                                .weight(1f)
                                .background(c.border)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(bottom = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                e.source,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = if (e.source == "Crash") c.red else if (e.source == "Réseau") c.amber else c.cyan
                            )
                            Spacer(Modifier.width(8.dp))
                            if (e.critique) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(c.red.copy(alpha = 0.14f))
                                        .border(1.dp, c.red.copy(alpha = 0.35f), RoundedCornerShape(50))
                                        .padding(horizontal = 7.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.journal_critique),
                                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                        color = c.red
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            if (e.ts > 0) {
                                Text(
                                    df.format(Date(e.ts)),
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                    color = c.text3
                                )
                            }
                        }
                        Text(
                            e.titre,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp),
                            color = c.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (e.detail.isNotBlank()) {
                            Text(
                                e.detail,
                                style = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 15.sp),
                                color = c.text2,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (filtrees.size > 200) {
                GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Text(
                        stringResource(R.string.journal_tronque),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = c.text2
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
