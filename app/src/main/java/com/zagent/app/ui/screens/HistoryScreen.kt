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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.Lot5SearchField
import com.zagent.app.ui.components.ZEmptyKind
import com.zagent.app.ui.components.ZEmptyState
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

    LaunchedEffect(Unit) { viewModel.refreshConversations() }

    val filtered = remember(all, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) all
        else all.filter {
            it.title.lowercase().contains(q) || it.preview.lowercase().contains(q)
        }
    }

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
                    .width(320.dp)
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
                Lot5SearchField(query, { query = it }, "Rechercher une conversation…")
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
                        listOf(
                            "Aujourd'hui" to 0,
                            "Hier" to 1,
                            "7 derniers jours" to 7,
                            "Plus ancien" to Int.MAX_VALUE
                        ).forEach { (label, _) ->
                            val group = filtered.filter { it.pinned == false && groupOf(it.updatedAt) == label }
                            if (group.isNotEmpty()) {
                                GroupHeader(label)
                                group.forEach { meta -> HistoryRow(viewModel, meta, onClose) }
                            }
                        }
                        val pinned = filtered.filter { it.pinned }
                        if (pinned.isNotEmpty()) {
                            GroupHeader("Épinglées")
                            pinned.forEach { meta -> HistoryRow(viewModel, meta, onClose) }
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
