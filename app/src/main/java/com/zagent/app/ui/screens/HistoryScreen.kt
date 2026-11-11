package com.zagent.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.ConversationMeta
import com.zagent.app.ui.components.common.GradientButton
import androidx.compose.material3.TextButton
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.OverlaySearchField
import com.zagent.app.ui.components.common.ZEmptyKind
import com.zagent.app.ui.components.common.ZEmptyState
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableIntStateOf

// ─────────────────────────────────────────────────────────────────────────────
// MA56 — Tiroir Historique des conversations : ouvert depuis l'icône menu de la
// barre du chat. Groupes Aujourd'hui / Hier / 7 derniers jours / Plus ancien,
// recherche en haut, glisser → épingler (vers la droite) ou supprimer (vers la gauche).
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDrawer(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    var query by remember { mutableStateOf("") }
    val all = viewModel.conversationList
    val scope = rememberCoroutineScope()
    // MA87 : indicateur du tirer-pour-actualiser de l'historique.
    var refreshing by remember { mutableStateOf(false) }
    // feat-020 : filtre par étiquette / dossier ("" = toutes).
    var filtreEtiquette by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refreshConversations() }

    val filtered = remember(all, query, filtreEtiquette) {
        val q = query.trim().lowercase()
        all
            .filter { filtreEtiquette.isBlank() || it.etiquette == filtreEtiquette }
            .filter {
                if (q.isEmpty()) true
                else it.title.lowercase().contains(q) || it.preview.lowercase().contains(q) ||
                    it.etiquette.lowercase().contains(q)
            }
    }
    // corr-069 — pagination du rendu : la fenêtre affichée grandit par pas de
    // 60 lignes via « Afficher plus » ; l'index reste chargé en entier (léger :
    // métadonnées seules), ce sont les lignes composées qui sont bornées.
    var lignesAffichees by rememberSaveable { mutableIntStateOf(60) }

    Box(Modifier.fillMaxSize()) {
        // Voile : un tap ferme le tiroir
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(onClick = onClose)
        )

        // Panneau de gauche
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    // ui-074 — jamais plus large que 320 dp, mais se réduit sur
                    // les petits écrans (320 dp de largeur d'écran testés).
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.85f)
                    .background(c.bg2)
                    .border(1.dp, c.border, RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp))
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = c.cyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Historique",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
                        color = c.text,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        filtered.size.toString() + " conv.",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text3
                    )
                }
                Spacer(Modifier.height(12.dp))
                OverlaySearchField(query, { query = it }, "Rechercher une conversation…")
                Spacer(Modifier.height(8.dp))
                // feat-020 : puces de filtrage par étiquette / dossier
                val etiquettes = viewModel.etiquettesConversations
                if (etiquettes.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EtiquetteChip(
                            label = "Toutes",
                            active = filtreEtiquette.isBlank(),
                            onClick = { filtreEtiquette = "" }
                        )
                        etiquettes.forEach { e ->
                            EtiquetteChip(
                                label = e,
                                active = filtreEtiquette == e,
                                onClick = { filtreEtiquette = if (filtreEtiquette == e) "" else e }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(10.dp))
                GradientButton(
                    text = "Nouvelle conversation",
                    onClick = {
                        onClose()
                        viewModel.clearConversation()
                    },
                    height = 44.dp,
                    leading = { Icon(Icons.Default.Add, null, tint = c.onGrad, modifier = Modifier.size(17.dp)) }
                )
                Spacer(Modifier.height(10.dp))
                // feat-019 — importer une conversation exportée en JSON.
                val importJson = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        viewModel.importerConversationJson(uri) { message ->
                            viewModel.refreshConversations()
                        }
                        onClose()
                    }
                }
                Text(
                    "Importer une conversation (.json)",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.cyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { importJson.launch("application/json") }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    // MA87 : état vide illustré (aucune conversation).
                    ZEmptyState(
                        kind = ZEmptyKind.CONVERSATION,
                        title = if (query.isBlank()) "Aucune conversation" else "Aucun résultat",
                        body = if (query.isBlank())
                            "Tes conversations apparaîtront ici, groupées par jour. Glisse une ligne pour l'épingler ou la supprimer."
                        else "Essaie un autre mot-clé."
                    )
                } else {
                    // MA87 : tirer pour actualiser l'historique.
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            viewModel.refreshConversations()
                            scope.launch {
                                delay(600)
                                refreshing = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        val visibles = filtered.take(lignesAffichees)
                        listOf(
                            "Aujourd'hui" to 0,
                            "Hier" to 1,
                            "7 derniers jours" to 7,
                            "Plus ancien" to Int.MAX_VALUE
                        ).forEach { (label, _) ->
                            val group = visibles.filter { it.pinned == false && groupOf(it.updatedAt) == label }
                            if (group.isNotEmpty()) {
                                GroupHeader(label)
                                group.forEach { meta -> HistoryRow(viewModel, meta, onClose) }
                            }
                        }
                        val pinned = visibles.filter { it.pinned }
                        if (pinned.isNotEmpty()) {
                            GroupHeader("Épinglées")
                            pinned.forEach { meta -> HistoryRow(viewModel, meta, onClose) }
                        }
                        if (filtered.size > lignesAffichees) {
                            // corr-069 : page suivante de l'historique.
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { lignesAffichees += 60 }
                                    .padding(vertical = 14.dp)
                            ) {
                                Text(
                                    "Afficher plus (" + (filtered.size - lignesAffichees) + " restantes)",
                                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                                    color = c.cyan
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
        color = c.text3,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

/** Groupe temporel d'une conversation (MA56). */
private fun groupOf(ts: Long): String {
    val now = java.time.LocalDate.now()
    val day = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return when {
        day == now -> "Aujourd'hui"
        day == now.minusDays(1) -> "Hier"
        day.isAfter(now.minusDays(7)) -> "7 derniers jours"
        else -> "Plus ancien"
    }
}

@Composable
private fun HistoryRow(viewModel: MainViewModel, meta: ConversationMeta, onClose: () -> Unit) {
    val c = Zcolors
    val df = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
    // feat-020 : dialogue d'étiquette
    var etiquetteOuverte by remember { mutableStateOf(false) }
    if (etiquetteOuverte) {
        EtiquetteDialog(viewModel, meta) { etiquetteOuverte = false }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    viewModel.deleteConversation(meta.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    viewModel.toggleConversationPin(meta.id)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val deleting = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val pinning = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val bg = when {
                deleting -> c.redBg
                pinning -> c.cyanBg
                else -> androidx.compose.ui.graphics.Color.Transparent
            }
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (deleting) Arrangement.End else Arrangement.Start
            ) {
                if (pinning) {
                    Icon(Icons.Default.PushPin, null, tint = c.cyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Épingler", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = c.cyan)
                } else if (deleting) {
                    Icon(Icons.Default.DeleteOutline, null, tint = c.red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer", style = TextStyle(fontFamily = Inter, fontSize = 12.sp), color = c.red)
                }
            }
        }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (meta.id == viewModel.conversationId) c.cyanBg else c.surface)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .clickable {
                    onClose()
                    viewModel.switchToConversation(meta.id)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (meta.pinned) {
                        Icon(
                            Icons.Outlined.PushPin, null,
                            tint = c.cyan, modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        meta.title,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (meta.preview.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        meta.preview,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = c.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // feat-020 : pastille d'étiquette, tap = attribuer / modifier
                if (meta.etiquette.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        meta.etiquette,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = c.cyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.cyanBg)
                            .clickable { etiquetteOuverte = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    df.format(Date(meta.updatedAt)) + " · " + meta.messageCount + " messages",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                    color = c.text3
                )
            }
        }
    }
}

/** feat-020 — Puce de filtre par étiquette / dossier de l'historique. */
@Composable
private fun EtiquetteChip(label: String, active: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
        color = if (active) c.onGrad else c.text2,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) c.cyan else c.surface2)
            .border(1.dp, if (active) c.cyan else c.border, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

/** feat-020 — Dialogue d'attribution d'étiquette (dossier) à une conversation. */
@Composable
private fun EtiquetteDialog(
    viewModel: MainViewModel,
    meta: ConversationMeta,
    onDismiss: () -> Unit
) {
    val c = Zcolors
    var valeur by remember { mutableStateOf(meta.etiquette) }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dossier / étiquette", fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column {
                Text(
                    "Classe la conversation (ex. « travail », « projet X »). Vide = sans dossier.",
                    fontFamily = Inter, fontSize = 12.sp, color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                OverlaySearchField(valeur, { valeur = it }, "Étiquette…")
                Spacer(Modifier.height(8.dp))
                val existantes = viewModel.etiquettesConversations
                if (existantes.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existantes.forEach { e ->
                            EtiquetteChip(
                                label = e,
                                active = valeur == e,
                                onClick = { valeur = if (valeur == e) "" else e }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.setConversationEtiquette(meta.id, valeur)
                onDismiss()
            }) { Text("Enregistrer", color = c.cyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = c.text2) }
        }
    )
}
