package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.zagent.app.R
import com.zagent.app.data.ActivityEvent
import com.zagent.app.data.ActivityStatus
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlayTabs
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA65 — Journal d'activité de l'agent : chronologie verticale de tout ce que
// l'agent a fait (outil, fichier, durée, résultat), filtrable par statut, avec
// bouton « Annuler » pour les écritures de fichiers récentes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActivityScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    var events by remember { mutableStateOf<List<ActivityEvent>>(emptyList()) }
    var filter by remember { mutableStateOf(0) } // 0 = tout, 1 = OK, 2 = Erreur/Refusé, 3 = Annulé
    var busyId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { events = viewModel.activityRepoSnapshot() }

    val filtered = remember(events, filter) {
        when (filter) {
            1 -> events.filter { it.status == ActivityStatus.OK }
            2 -> events.filter { it.status == ActivityStatus.ERREUR || it.status == ActivityStatus.REFUSE }
            3 -> events.filter { it.status == ActivityStatus.ANNULE }
            else -> events
        }
    }

    OverlayScreen(title = stringResource(R.string.ui_activite_agent), subtitle = events.size.toString() + " événements", onBack = onBack) {
        OverlayTabs(listOf(R.string.onglet_tout, R.string.onglet_reussi, R.string.onglet_probleme, R.string.onglet_annule), filter) { filter = it }
        Spacer(Modifier.height(10.dp))
        // feat-099 — export du journal d'activité (JSON partageable).
        val ctx = LocalContext.current
        Text(
            stringResource(R.string.journal_exporter),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
            color = c.cyan,
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(c.surface2)
                .clickable {
                    val partage = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            events.joinToString(",\n", prefix = "[", postfix = "]") { ev ->
                                "{" +
                                    "\"ts\":" + ev.ts + "," +
                                    "\"kind\":\"" + ev.kind.replace("\"", "'") + "\"," +
                                    "\"status\":\"" + ev.status + "\"," +
                                    "\"detail\":\"" + (ev.detail ?: "").replace("\"", "'").replace("\n", " ") + "\"" +
                                    "}"
                            }
                        )
                        putExtra(android.content.Intent.EXTRA_TITLE, "journal-zagent.json")
                    }
                    ctx.startActivity(android.content.Intent.createChooser(partage, "Exporter le journal"))
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            OverlayEmptyState(
                Icons.Default.History,
                "Aucun événement",
                "Chaque outil utilisé par l'agent (lecture, écriture, commande…) sera consigné ici avec sa durée et son résultat."
            )
            return@OverlayScreen
        }

        val df = remember { SimpleDateFormat("d MMM · HH:mm:ss", Locale.getDefault()) }
        // ia-036 — vue « par tour » : un entête sépare les paquets d'événements
        // espacés de plus de 90 s (chaque tour de l'agent).
        var parTour by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (parTour) c.cyanBg else c.surface2)
                .clickable { parTour = !parTour }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                stringResource(R.string.outils_par_tour),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = if (parTour) c.cyan else c.text2
            )
        }
        Spacer(Modifier.height(10.dp))
        Column {
            filtered.forEachIndexed { i, e ->
                val nouveauTour = parTour && (i == 0 || e.ts - filtered[i - 1].ts > 90_000)
                if (nouveauTour) {
                    Text(
                        "Tour · " + df.format(Date(e.ts)),
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                        color = c.text3,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                // resp-07 — IntrinsicSize.Min : le connecteur suit la hauteur
                // réelle de la ligne (échelle de police agrandie incluse).
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Colonne temporelle : point + ligne
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
                                    when (e.status) {
                                        ActivityStatus.OK -> c.green
                                        ActivityStatus.ERREUR -> c.red
                                        ActivityStatus.REFUSE -> c.amber
                                        else -> c.text3
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
                                e.kind,
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = c.text2
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                e.status,
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = when (e.status) {
                                    ActivityStatus.OK -> c.green
                                    ActivityStatus.ERREUR -> c.red
                                    ActivityStatus.REFUSE -> c.amber
                                    else -> c.text3
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                df.format(Date(e.ts)),
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                color = c.text3
                            )
                        }
                        Text(
                            e.label,
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
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (e.durationMs / 1000.0).let { String.format(Locale.US, "%.1f s", it) },
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                                color = c.text3
                            )
                            if (e.undoPath != null && e.status == ActivityStatus.OK) {
                                Spacer(Modifier.width(10.dp))
                                if (busyId == e.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = c.cyan, strokeWidth = 1.6.dp)
                                } else {
                                    Text(
                                        stringResource(R.string.chat_annuler_ecriture),
                                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                        color = c.amber,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                busyId = e.id
                                                viewModel.undoActivityEntry(e) {
                                                    events = viewModel.activityRepoSnapshot()
                                                    busyId = null
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Icon(Icons.Default.Undo, null, tint = c.amber, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}