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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.components.common.StatusBadge
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.chat.GitDiffView
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.SecondaryButton
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlayTabs
import com.zagent.app.ui.components.common.MonoLine
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
        // env-004 — état de l'environnement GitHub (aucun réseau nécessaire).
        viewModel.refreshEnvStatut()
    }

    OverlayScreen(title = stringResource(R.string.ui_git), subtitle = viewModel.folderLabel, onBack = onClose) {
        OverlayTabs(listOf(R.string.onglet_changements, R.string.onglet_historique, R.string.onglet_branches, R.string.onglet_actions), tab) { tab = it }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            0 -> ChangesTab(viewModel)
            1 -> HistoryTab(viewModel)
            2 -> BranchesTab(viewModel)
            else -> ActionsTab(viewModel)
        }
    }
    // env-002 — dialogue de connexion d'un dépôt comme environnement.
    EnvConnexionDialog(viewModel)
}

// ── Onglet Changements ───────────────────────────────────────────────────────

@Composable
private fun ChangesTab(viewModel: MainViewModel) {
    val c = Zcolors
    val changes = viewModel.gitUi.changes
    var commitMsg by remember { mutableStateOf(viewModel.gitCommitMessage) }

    if (changes == null) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.cyan, strokeWidth = 2.dp)
        }
        return
    }
    if (changes.error != null) {
        OverlayEmptyState(Icons.Default.CallMerge, "Pas de dépôt Git", changes.error ?: "")
        Spacer(Modifier.height(10.dp))
        // env-004 — proposition « Connecter un dépôt comme environnement » :
        // le clone complet devient le dossier de travail de l'agent.
        EnvCarte(viewModel)
        Spacer(Modifier.height(10.dp))
        CloneCard(viewModel)
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
        // ux-070 — compteurs par état : modifiés, ajoutés, supprimés.
        val nbModifies = files.count { it.stateLabel.startsWith("modifié") || it.stateLabel.startsWith("renommé") }
        val nbAjoutes = files.count { it.untracked || it.stateLabel.startsWith("ajouté") }
        val nbSupprimes = files.count { it.stateLabel.startsWith("supprimé") }
        if (files.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                if (nbModifies > 0) StatusBadge("$nbModifies modifié${if (nbModifies > 1) "s" else ""}", c.amber)
                if (nbAjoutes > 0) StatusBadge("$nbAjoutes ajouté${if (nbAjoutes > 1) "s" else ""}", c.green)
                if (nbSupprimes > 0) StatusBadge("$nbSupprimes supprimé${if (nbSupprimes > 1) "s" else ""}", c.red)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (files.isEmpty()) {
            Text(
                stringResource(R.string.git_tout_propre),
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
            stringResource(R.string.git_message_commit),
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
            placeholder = { Text(stringResource(R.string.git_message_hint), style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text3) },
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

    // env-004 — carte Environnement GitHub : état avance/retard/modifs/
    // conflits + synchronisation groupée (pull + push), hors ligne respecté.
    EnvCarte(viewModel)
    Spacer(Modifier.height(12.dp))

    // ── feat-085/086 : pull, push, stash + feat-088 : PR ─────────────────────
    var showPrDialog by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            "Pull",
            onClick = { viewModel.gitPull() },
            enabled = !viewModel.gitBranchBusy,
            modifier = Modifier.weight(1f),
            height = 42.dp,
            leading = { Icon(Icons.Default.CloudDownload, null, tint = c.cyan, modifier = Modifier.size(15.dp)) }
        )
        SecondaryButton(
            "Push",
            onClick = { viewModel.gitPush() },
            enabled = !viewModel.gitBranchBusy,
            modifier = Modifier.weight(1f),
            height = 42.dp,
            leading = { Icon(Icons.Default.CloudUpload, null, tint = c.violet, modifier = Modifier.size(15.dp)) }
        )
        SecondaryButton(
            "Stash",
            onClick = { viewModel.gitStashPush() },
            enabled = !viewModel.gitBranchBusy && files.isNotEmpty(),
            modifier = Modifier.weight(1f),
            height = 42.dp,
            leading = { Icon(Icons.Default.ContentCopy, null, tint = c.amber, modifier = Modifier.size(15.dp)) }
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            "Rétablir le stash",
            onClick = { viewModel.gitStashPop() },
            enabled = !viewModel.gitBranchBusy,
            modifier = Modifier.weight(1f),
            height = 42.dp,
            leading = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = c.text2, modifier = Modifier.size(15.dp)) }
        )
        SecondaryButton(
            "Créer une PR",
            onClick = { showPrDialog = true },
            enabled = !viewModel.gitPrBusy,
            modifier = Modifier.weight(1f),
            height = 42.dp,
            leading = { Icon(Icons.Default.CallMerge, null, tint = c.green, modifier = Modifier.size(15.dp)) }
        )
    }
    if (showPrDialog) {
        PrDialog(viewModel, onDismiss = { showPrDialog = false })
    }
    Spacer(Modifier.height(12.dp))

    Text(
        stringResource(R.string.ui_diff_complet),
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

// ── feat-081 : carte de clonage (dépôt absent) ───────────────────────────────

/** Carte « Cloner un dépôt » affichée quand le projet n'est pas un dépôt Git. */
@Composable
private fun CloneCard(viewModel: MainViewModel) {
    val c = Zcolors
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    GlassCard {
        Text(
            stringResource(R.string.git_cloner),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.git_clone_note),
            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 15.sp),
            color = c.text2
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text(stringResource(R.string.git_url_exemple), style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp), color = c.text3) },
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp, color = c.text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = "Cloner ici",
            enabled = url.isNotBlank() && !busy,
            onClick = {
                busy = true
                viewModel.gitClone(url) { ok -> busy = false }
            },
            modifier = Modifier.fillMaxWidth(),
            height = 42.dp,
            leading = { Icon(Icons.Default.CloudDownload, null, tint = c.onGrad, modifier = Modifier.size(15.dp)) }
        )
    }
}

// ── feat-088 : dialogue de création de pull request ──────────────────────────

/** Dialogue PR : titre + description, branche courante → branche par défaut du dépôt. */
@Composable
private fun PrDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val c = Zcolors
    var titre by remember { mutableStateOf(viewModel.gitCommitMessage.ifBlank { "" }) }
    var description by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Text(
                stringResource(R.string.git_creer_pr),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                color = c.text
            )
        },
        text = {
            // resp-09 — contenu scrollable : le champ Description long reste
            // accessible même à grande police ou en paysage.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.git_pr_note),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    placeholder = { Text(stringResource(R.string.ui_titre), color = c.text3, style = TextStyle(fontFamily = Inter, fontSize = 13.sp)) },
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, color = c.text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.git_description_facultative), color = c.text3, style = TextStyle(fontFamily = Inter, fontSize = 13.sp)) },
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 13.sp, color = c.text),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Text(
                stringResource(R.string.action_creer),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = if (titre.isNotBlank() && !viewModel.gitPrBusy) c.cyan else c.text3,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = titre.isNotBlank() && !viewModel.gitPrBusy) {
                        viewModel.createPullRequest(titre, description) { onDismiss() }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                stringResource(R.string.action_annuler),
                style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                color = c.text2,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}

// ── Onglet Historique ────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(viewModel: MainViewModel) {
    val c = Zcolors
    val log = viewModel.gitLog
    if (log.isEmpty()) {
        OverlayEmptyState(Icons.Default.Commit, "Aucun commit", "Le dépôt n'a pas encore d'historique, ou le projet n'est pas un dépôt Git.")
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
        OverlayEmptyState(Icons.Default.CallMerge, "Aucune branche", "Le projet n'est pas un dépôt Git ou n'a pas encore de branche.")
        return
    }
    // feat-084 : création de branche
    var nouvelleBranche by remember { mutableStateOf("") }
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
                    Icon(Icons.Default.Check, stringResource(R.string.git_branche_actuelle), tint = c.cyan, modifier = Modifier.size(16.dp))
                } else {
                    // feat-084 : suppression de branche (long appui = trop discret,
                    // icône visible au bout de la ligne ; la principale est refusée côté VM).
                    Icon(
                        Icons.Default.Delete, "Supprimer la branche " + b.name,
                        tint = c.text3,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(enabled = !viewModel.gitBranchBusy) {
                                viewModel.gitBranchDelete(b.name)
                            }
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    // feat-084 : créer une branche et basculer dessus
    GlassCard {
        Text(
            stringResource(R.string.git_nouvelle_branche),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nouvelleBranche,
                onValueChange = { nouvelleBranche = it },
                placeholder = { Text("ma-fonctionnalite", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp), color = c.text3) },
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp, color = c.text),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.cyan,
                    unfocusedBorderColor = c.border
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.cyanBg)
                    .clickable(enabled = nouvelleBranche.isNotBlank() && !viewModel.gitBranchBusy) {
                        viewModel.gitBranchCreate(nouvelleBranche)
                        nouvelleBranche = ""
                    }
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.git_creer_branche), tint = c.cyan, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (viewModel.gitBranchBusy) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = c.cyan, strokeWidth = 1.6.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.chat_bascule),
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                color = c.text2
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(R.string.git_branches_note),
        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
        color = c.text3
    )
}

// ── feat-089 : onglet Actions (statut GitHub Actions) ────────────────────────

@Composable
private fun ActionsTab(viewModel: MainViewModel) {
    val c = Zcolors
    LaunchedEffect(Unit) {
        viewModel.refreshGitActions()
        viewModel.listerClesSsh()
    }
    val runs = viewModel.gitActionsRuns

    if (viewModel.gitActionsLoading) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = c.cyan, strokeWidth = 2.dp)
        }
        return
    }
    if (runs.isEmpty()) {
        OverlayEmptyState(
            Icons.Default.Bolt,
            "Aucun run récent",
            "Le dépôt n'a pas de workflow GitHub Actions, ou l'origine n'est pas un dépôt GitHub."
        )
    } else {
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
            runs.forEach { run ->
                val (label, couleur) = when (run.libelle) {
                    "succès" -> "Succès" to c.green
                    "échec" -> "Échec" to c.red
                    "en cours" -> "En cours…" to c.cyan
                    "annulé" -> "Annulé" to c.amber
                    else -> run.libelle to c.text2
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Bolt, null,
                        tint = couleur,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        MonoLine(run.name, c.text)
                        MonoLine(run.branch + " · n°" + run.runNumber + " · " + run.createdAt, c.text3)
                    }
                    Text(
                        label,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = couleur
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    // ── feat-058 : clés SSH pour git ────────────────────────────────────────
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    GlassCard {
        Text(
            stringResource(R.string.git_cles_ssh),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.height(6.dp))
        if (viewModel.sshClePublique.isBlank()) {
            Text(
                stringResource(R.string.git_ssh_vide),
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 15.sp),
                color = c.text2
            )
        } else {
            Text(
                viewModel.sshClePublique,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp, lineHeight = 14.sp),
                color = c.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface2)
                    .padding(8.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.action_copier_cle_publique),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        clipboard.setText(AnnotatedString(viewModel.sshClePublique))
                        com.zagent.app.ui.components.common.ZEphemeral.show("Clé publique copiée")
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = "Générer une clé ed25519",
            enabled = !viewModel.sshOccupe,
            onClick = { viewModel.genererCleSsh() },
            modifier = Modifier.fillMaxWidth(),
            height = 42.dp,
            leading = { Icon(Icons.Default.Add, null, tint = c.onGrad, modifier = Modifier.size(15.dp)) }
        )
    }
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