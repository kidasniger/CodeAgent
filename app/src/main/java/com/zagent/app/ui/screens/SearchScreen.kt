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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.ProjectSearch
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlaySearchField
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
// MA58 — Recherche globale : champ + onglets Conversations / Fichiers / Code.
// Résultats avec extrait surligné, chemin et ligne ; un tap ouvre la conversation,
// l'éditeur à la ligne exacte ou l'explorateur. Historique des dernières recherches.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var codeHits by remember { mutableStateOf<List<ProjectSearch.CodeHit>>(emptyList()) }
    var convHits by remember { mutableStateOf<List<com.zagent.app.data.ConversationHit>>(emptyList()) }

    val history = viewModel.searchHistory

    fun runSearch(q: String) {
        val term = q.trim()
        if (term.isEmpty()) return
        query = term
        viewModel.addSearchQuery(term)
        searching = true
        codeHits = emptyList()
        convHits = emptyList()
        viewModel.runGlobalSearch(term) { convs, files ->
            convHits = convs
            codeHits = files
            searching = false
        }
    }

    OverlayScreen(title = "Recherche", subtitle = viewModel.folderLabel, onBack = onClose) {
        OverlaySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Rechercher partout…"
        )
        Spacer(Modifier.height(10.dp))
        OverlayTabs(listOf("Conversations", "Fichiers", "Code"), tab) { tab = it }
        Spacer(Modifier.height(14.dp))

        if (query.isBlank()) {
            if (history.isNotEmpty()) {
                Text(
                    "Recherches récentes",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.text3,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                history.forEach { h ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { runSearch(h) }
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, null, tint = c.text3, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            h,
                            style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp),
                            color = c.text2
                        )
                    }
                }
            } else {
                OverlayEmptyState(
                    icon = Icons.Default.Search,
                    title = "Recherche globale",
                    text = "Retrouve un message, un fichier ou une ligne de code parmi les conversations et le projet actif."
                )
            }
        } else {
            when (tab) {
                0 -> ConversationResults(viewModel, convHits, searching, query) { convId ->
                    onClose()
                    viewModel.switchToConversation(convId)
                }
                1 -> FileResults(viewModel, query)
                else -> CodeResults(codeHits, searching, query) { path, line ->
                    onClose()
                    viewModel.openFilePath(path, line)
                }
            }
        }
    }
}

/** Onglet Conversations : extraits surlignés depuis les conversations sauvegardées (MA58). */
@Composable
private fun ConversationResults(
    viewModel: MainViewModel,
    hits: List<com.zagent.app.data.ConversationHit>,
    searching: Boolean,
    query: String,
    onOpen: (String) -> Unit
) {
    val c = Zcolors
    if (searching) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.cyan, strokeWidth = 2.dp)
        }
        return
    }
    if (hits.isEmpty()) {
        OverlayEmptyState(Icons.Default.Search, "Aucun message trouvé", "Le texte recherché n'apparaît dans aucune conversation sauvegardée.")
        return
    }
    val df = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
    hits.forEach { hit ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .clickable { onOpen(hit.conversationId) }
                .padding(12.dp)
        ) {
            Text(
                hit.title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Highlighted(hit.snippet, query)
            Spacer(Modifier.height(4.dp))
            MonoLine(df.format(Date(hit.updatedAt)), c.text3)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Onglet Fichiers : correspondances floues dans l'arborescence (MA58). */
@Composable
private fun FileResults(viewModel: MainViewModel, query: String) {
    val c = Zcolors
    val matches = remember(query, viewModel.projectFiles) {
        ProjectSearch.fuzzyFileMatches(viewModel.projectFiles, query.trim(), 30)
    }
    if (matches.isEmpty()) {
        OverlayEmptyState(Icons.Default.FolderOpen, "Aucun fichier", "Aucun chemin du projet ne correspond. Lance une demande pour rafraîchir la liste des fichiers.")
        return
    }
    matches.forEach { path ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.openFilePath(path, null)
                }
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(c.violetBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = c.violet, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            MonoLine(path, c.text, Modifier.weight(1f))
        }
    }
}

/** Onglet Code : lignes de code surlignées, tap = éditeur à la ligne exacte (MA58). */
@Composable
private fun CodeResults(
    hits: List<ProjectSearch.CodeHit>,
    searching: Boolean,
    query: String,
    onOpen: (String, Int) -> Unit
) {
    val c = Zcolors
    if (searching) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.cyan, strokeWidth = 2.dp)
        }
        return
    }
    if (hits.isEmpty()) {
        OverlayEmptyState(Icons.Default.Code, "Aucun résultat de code", "Le texte recherché n'apparaît dans aucun fichier texte du projet (400 fichiers max).")
        return
    }
    hits.forEach { hit ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpen(hit.path, hit.line) }
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null, tint = c.cyan, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                MonoLine(hit.path + " : " + hit.line, c.text2, Modifier.weight(1f))
            }
            Spacer(Modifier.height(3.dp))
            Highlighted(hit.text, query)
        }
    }
}

/** Texte avec la requête surlignée (toutes les occurrences, insensible à la casse). */
@Composable
private fun Highlighted(text: String, query: String) {
    val c = Zcolors
    val annotated = remember(text, query) {
        val q = query.trim()
        if (q.isEmpty()) return@remember AnnotatedString(text)
        val spans = ArrayList<AnnotatedString.Range<SpanStyle>>()
        var idx = text.indexOf(q, ignoreCase = true)
        while (idx >= 0) {
            spans.add(
                AnnotatedString.Range(SpanStyle(background = c.cyanBg, color = c.cyan, fontWeight = FontWeight.SemiBold), idx, idx + q.length)
            )
            idx = text.indexOf(q, idx + q.length, ignoreCase = true)
        }
        AnnotatedString(text, spans)
    }
    Text(
        annotated,
        style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 17.sp),
        color = c.text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
