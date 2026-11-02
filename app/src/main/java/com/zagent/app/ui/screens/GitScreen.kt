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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.GitDiffView
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.Lot5EmptyState
import com.zagent.app.ui.components.Lot5Screen
import com.zagent.app.ui.components.Lot5Tabs
import com.zagent.app.ui.components.MonoLine
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA61 — Écran Git : onglets Changements / Historique / Branches. Cases pour
// indexer (git add / git reset), champ de message de commit avec bouton
// « Générer avec l'IA », liste de commits (auteur + date) avec diff au tap,
// branches avec bascule au tap.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GitScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.refreshGitChanges()
        viewModel.refreshGitLog()
        viewModel.refreshGitBranches()
    }

    Lot5Screen(title = "Git", subtitle = viewModel.folderLabel, onBack = onClose) {
        Lot5Tabs(listOf("Changements", "Historique", "Branches"), tab) { tab = it }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            0 -> ChangesTab(viewModel)
            1 -> HistoryTab(viewModel)
            else -> BranchesTab(viewModel)
        }
    }
}

// ── Onglet Changements ───────────────────────────────────────────────────────

@Composable
private fun ChangesTab(viewModel: MainViewModel) {
    val c = Zcolors
    val changes = viewModel.gitChanges
    var commitMsg by remember { mutableStateOf(viewModel.gitCommitMessage) }

    if (changes == null) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.cyan, strokeWidth = 2.dp)
        }
        return
    }
    if (changes.error != null) {
        Lot5EmptyState(Icons.Default.CallMerge, "Pas de dépôt Git", changes.error ?: "")
        return
    }

    val files = remember(changes.status) { parseStatusLines(changes.status) }

    GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                files.size.toString() + " fichier(s) modifié(s)",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Refresh, "Actualiser",
                tint = c.cyan,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.refreshGitChanges() }
            )
        }
        Spacer(Modifier.height(6.dp))
        if (files.isEmpty()) {
            Text(
                "Aucun changement : tout est propre.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                color = c.text2
            )
        }
        files.forEach { f ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.gitToggleStage(f.path, f.staged) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = f.staged,
                    onCheckedChange = { viewModel.gitToggleStage(f.path, f.staged) },
                    colors = CheckboxDefaults.colors(checkedColor = c.cyan, checkmarkColor = c.onGrad)
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    MonoLine(f.path, c.text)
                    MonoLine(f.stateLabel, if (f.untracked) c.green else c.amber)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    GlassCard {
        Text(
            "Message de commit",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = commitMsg,
            onValueChange = {
                commitMsg = it
                viewModel.gitCommitMessage = it
            },
            placeholder = { Text("Ajoute un message clair et court…", style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text3) },
            textStyle = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, color = c.text),
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.violetBg)
                    .clickable(enabled = !viewModel.isGeneratingCommitMsg && files.isNotEmpty()) {
                        viewModel.generateCommitMessage()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.isGeneratingCommitMsg) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), color = c.violet, strokeWidth = 1.6.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = c.violet, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (viewModel.isGeneratingCommitMsg) "Génération…" else "Générer avec l'IA",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                        color = c.violet
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            GradientButton(
                text = "Commiter",
                enabled = commitMsg.isNotBlank() && files.isNotEmpty() && !viewModel.isGeneratingCommitMsg,
                onClick = { viewModel.gitCommit(commitMsg) { commitMsg = "" } },
                modifier = Modifier.weight(1f),
                height = 44.dp,
                leading = { Icon(Icons.Default.Commit, null, tint = c.onGrad, modifier = Modifier.size(16.dp)) }
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    Text(
        "Diff complet",
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        color = c.text
    )
    Spacer(Modifier.height(6.dp))
    GitDiffView(
        status = changes.status,
        diff = changes.diff,
        error = null,
        maxHeight = 340.dp
    )
}

// ── Onglet Historique ────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(viewModel: MainViewModel) {
    val c = Zcolors
    val log = viewModel.gitLog
    if (log.isEmpty()) {
        Lot5EmptyState(Icons.Default.Commit, "Aucun commit", "Le dépôt n'a pas encore d'historique, ou le projet n'est pas un dépôt Git.")
        return
    }
    val df = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()) }
    val diff = viewModel.gitCommitDiff

    log.forEach { commit ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (viewModel.gitCommitDiffHash == commit.hash) c.cyanBg else c.surface)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .clickable { viewModel.showCommitDiff(commit.hash) }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c.violetBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        commit.author.take(1).uppercase(),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = c.violet
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        commit.subject,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = c.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    MonoLine(commit.hash + " · " + commit.author + " · " + df.format(Date(commit.date)), c.text3)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (diff != null) {
        Spacer(Modifier.height(6.dp))
        Text(
            "Diff de " + viewModel.gitCommitDiffHash,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.height(6.dp))
        GitDiffView(status = "", diff = diff, error = null, maxHeight = 320.dp)
    }
}

// ── Onglet Branches ──────────────────────────────────────────────────────────

@Composable
private fun BranchesTab(viewModel: MainViewModel) {
    val c = Zcolors
    val branches = viewModel.gitBranches
    if (branches.isEmpty()) {
        Lot5EmptyState(Icons.Default.CallMerge, "Aucune branche", "Le projet n'est pas un dépôt Git ou n'a pas encore de branche.")
        return
    }
    GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        branches.forEach { b ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (b.current) c.cyanBg else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(enabled = !b.current && !viewModel.gitBranchBusy) {
                        viewModel.gitCheckout(b.name)
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CallMerge, null,
                    tint = if (b.current) c.cyan else c.text3,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                MonoLine(b.name, if (b.current) c.cyan else c.text, Modifier.weight(1f))
                if (b.current) {
                    Icon(Icons.Default.Check, "Branche actuelle", tint = c.cyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    if (viewModel.gitBranchBusy) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = c.cyan, strokeWidth = 1.6.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Bascule en cours…",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                color = c.text2
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Touche une branche pour basculer dessus (les changements non commités doivent être compatibles).",
        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
        color = c.text3
    )
}

// ── Analyse de git status --short ────────────────────────────────────────────

/** Une ligne de git status --short : chemin, état, indexation (MA61). */
data class GitFileState(
    val path: String,
    val staged: Boolean,
    val untracked: Boolean,
    val stateLabel: String
)

/** Parse « XY chemin » ; X = index (staged), Y = arbre de travail. */
private fun parseStatusLines(status: String): List<GitFileState> {
    val out = ArrayList<GitFileState>()
    for (line in status.lines()) {
        if (line.length < 4) continue
        val x = line[0]
        val y = line[1]
        val path = line.drop(2).trim()
        if (path.isEmpty()) continue
        val untracked = x == '?' && y == '?'
        val staged = !untracked && x != ' ' && x != '?'
        val label = when {
            untracked -> "nouveau fichier (non suivi)"
            x == 'A' -> "ajouté (indexé)"
            x == 'D' || y == 'D' -> "supprimé"
            x == 'R' -> "renommé"
            y == 'M' -> "modifié (non indexé)"
            x == 'M' -> "modifié (indexé)"
            else -> "état " + x + y
        }
        out.add(GitFileState(path, staged, untracked, label))
    }
    return out
}
