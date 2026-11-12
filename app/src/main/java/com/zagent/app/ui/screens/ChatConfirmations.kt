package com.zagent.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.GlassModalBottomSheet
import com.zagent.app.ui.components.chat.GitDiffView
import com.zagent.app.ui.components.chat.DiffPanel
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.IconBadge
import com.zagent.app.ui.components.common.SecondaryButton
import com.zagent.app.ui.components.common.StatusBadge
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 99 — découpe de ChatScreen : dialogue et feuilles de
// confirmation (application de code, commande, suppression, écriture).
// Anciennement fonctions privées de ChatScreen.kt, mêmes signatures
// (visibilité internal, même paquet) : aucun changement d'appel nécessaire.
// MàJ 96 — rôles sémantiques sur les interrupteurs et boutons.
// ─────────────────────────────────────────────────────────────────────────────

/** MA32 : appliquer un bloc de code à un fichier du projet. */
@Composable
internal fun ApplyCodeDialog(code: String, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Zcolors
    var path by remember { mutableStateOf("") }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_appliquer_fichier), fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.chat_bloc_note),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    placeholder = { Text("chemin/du/fichier.kt", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text, fontSize = 13.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    code.lines().size.toString() + " lignes · " + code.length + " caractères",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3
                )
            }
        },
        confirmButton = {
            TextButton(enabled = path.isNotBlank(), onClick = { onApply(path.trim()) }) {
                Text(stringResource(R.string.action_appliquer), color = if (path.isNotBlank()) c.cyan else c.text3)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_annuler), color = c.text2) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation d'une commande
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommandConfirmSheet(
    pending: MainViewModel.PendingCommand,
    onResolve: (Boolean) -> Unit,
    /** ux-019 — autorise la demande en cours et toutes les suivantes du tour. */
    onAllowAllTurn: () -> Unit = {}
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Terminal, c.cyan, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.securite_commande_agent),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        stringResource(R.string.chat_executer_commande_q),
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            if (pending.command.contains("sudo", ignoreCase = true) || pending.command.contains("rm -rf", ignoreCase = true)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.chat_commande_sensible),
                    color = c.red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(c.red.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    pending.command,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text,
                    maxLines = 8
                )
            }

            if (pending.note != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.amberBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        pending.note,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 17.sp),
                        color = c.amber
                    )
                }
            }
            pending.changes?.let { ch ->
                Spacer(Modifier.height(10.dp))
                GitDiffView(status = ch.status, diff = ch.diff, error = ch.error, maxHeight = 260.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                pending.footer ?: if (pending.note != null) "Un git push est toujours confirmé, même en mode Auto."
                else "Elle s'exécute dans le dossier du projet, avec le terminal de l'appli. Active le mode Auto dans les réglages pour ne plus avoir à confirmer.",
                style = MaterialTheme.typography.bodySmall,
                color = c.text2
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Exécuter",
                    onClick = { onResolve(true) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
            }
            // ux-019 — quand plusieurs confirmations se suivent dans le même
            // tour, un seul tap passe le reste du tour en exécution automatique.
            Text(
                stringResource(R.string.chat_tout_autoriser_tour),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable { onAllowAllTurn() }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation de suppression
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteConfirmSheet(
    pending: MainViewModel.PendingDelete,
    onResolve: (Boolean) -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Shield, c.red, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.ui_confirmation_suppression),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        stringResource(R.string.ui_supprimer_cet_element_q),
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.surface2)
                    .border(1.dp, c.border, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    pending.path,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.chat_agent_suppression_def),
                style = MaterialTheme.typography.bodySmall,
                color = c.text2
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Supprimer",
                    onClick = { onResolve(true) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet de confirmation d'écriture (avec diff)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WriteConfirmSheet(
    pending: MainViewModel.PendingWrite,
    onResolve: (Boolean, Boolean) -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var alwaysAllow by remember(pending.toolCallId) { mutableStateOf(false) }

    GlassModalBottomSheet(
        onDismissRequest = { onResolve(false, false) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Shield, c.violet, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.ui_confirmation_ecriture),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = c.text2
                    )
                    Text(
                        stringResource(R.string.chat_autoriser_ecriture_q),
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = c.text
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Chemin + compteur de lignes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .border(1.dp, c.border, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .weight(1f, fill = false)
                ) {
                    Text(
                        pending.path,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (pending.diff.added > 0) StatusBadge("+${pending.diff.added} ligne${if (pending.diff.added > 1) "s" else ""}", c.green)
                if (pending.diff.removed > 0) {
                    Spacer(Modifier.width(6.dp))
                    StatusBadge("-${pending.diff.removed}", c.red)
                }
            }

            Spacer(Modifier.height(12.dp))
            DiffPanel(pending.diff.lines)

            Spacer(Modifier.height(14.dp))

            // Toujours autoriser
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.chat_toujours_fichier),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = c.text
                    )
                    Text(
                        stringResource(R.string.chat_ecritures_auto_fichier),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text2
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = alwaysAllow,
                    onCheckedChange = { alwaysAllow = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = c.green,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = c.surface3,
                        uncheckedThumbColor = c.bg2,
                        uncheckedBorderColor = c.border2
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Refuser",
                    onClick = { onResolve(false, false) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp
                )
                GradientButton(
                    "Autoriser",
                    onClick = { onResolve(true, alwaysAllow) },
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                    leading = {
                        Icon(Icons.Default.Check, null, tint = c.onGrad, modifier = Modifier.size(17.dp))
                    }
                )
            }
        }
    }
}