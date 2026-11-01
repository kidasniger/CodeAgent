package com.zagent.app.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import java.util.Locale

private const val MAX_DIFF_ROWS = 500

/** Vue colorée des changements Git : état des fichiers puis diff (lignes ajoutées en vert, supprimées en rouge). */
@Composable
fun GitDiffView(
    status: String,
    diff: String,
    error: String?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 320.dp
) {
    val c = Zcolors
    val shape = RoundedCornerShape(12.dp)
    val rows = remember(status, diff) {
        val out = ArrayList<Pair<Boolean, String>>()
        status.lines().filter { it.isNotBlank() }.forEach { out.add(true to it) }
        if (diff.isNotBlank()) {
            if (out.isNotEmpty()) out.add(true to "")
            diff.lines().forEach { out.add(false to it) }
        }
        out
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.codeBg)
            .border(1.dp, c.codeBorder, shape)
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
    ) {
        if (error != null) {
            Text(
                error,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                color = c.amber,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else if (rows.isEmpty()) {
            Text(
                "Aucun changement.",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.text2,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else {
            val shown = rows.take(MAX_DIFF_ROWS)
            shown.forEach { (isStatus, line) ->
                var fg: Color = c.text2
                var bg: Color = Color.Transparent
                var bold = false
                if (isStatus) {
                    val code = line.take(2)
                    fg = when {
                        code.contains('D') -> c.red
                        code.contains('?') || code.contains('A') -> c.green
                        else -> c.amber
                    }
                } else when {
                    line.startsWith("diff --git") -> { fg = c.amber; bg = c.amberBg; bold = true }
                    line.startsWith("@@") -> fg = c.cyan
                    line.startsWith("+++") || line.startsWith("---") || line.startsWith("index ") ||
                        line.startsWith("new file") || line.startsWith("deleted file") -> fg = c.text3
                    line.startsWith("+") -> { fg = c.green; bg = c.greenBg }
                    line.startsWith("-") -> { fg = c.red; bg = c.redBg }
                }
                val text = if (!isStatus && line.startsWith("diff --git ")) line.removePrefix("diff --git ") else line
                Text(
                    text.ifEmpty { " " },
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = fg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(horizontal = 10.dp, vertical = 1.dp)
                )
            }
            if (rows.size > MAX_DIFF_ROWS) {
                Text(
                    "… " + (rows.size - MAX_DIFF_ROWS) + " lignes de plus non affichées",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                    color = c.text3,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Feuille « Changements Git » ouverte par l'utilisateur depuis le menu du chat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitChangesSheet(
    status: String,
    diff: String,
    error: String?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = c.bg2,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Changements Git",
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    color = c.text,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh, enabled = !loading) {
                    Text(if (loading) "Chargement…" else "Actualiser", color = c.cyan)
                }
            }
            Spacer(Modifier.height(10.dp))
            GitDiffView(status = status, diff = diff, error = error, maxHeight = 480.dp)
            Spacer(Modifier.height(14.dp))
            SecondaryButton("Fermer", onClick = onClose, modifier = Modifier.fillMaxWidth(), height = 48.dp)
        }
    }
}

/** Choix du modèle pour la conversation en cours (le réglage global ne change pas). */
@Composable
fun ModelPickerDialog(
    globalModel: String,
    currentChatModel: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val c = Zcolors
    var custom by remember { mutableStateOf("") }
    val known = listOf("glm-4.5-flash", "glm-4.7-flash").filter { it != globalModel }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bg2,
        title = { Text("Modèle de la conversation", fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                ModelRow(
                    label = "Réglage global : $globalModel",
                    selected = currentChatModel == null,
                    onClick = { onSelect(null) }
                )
                known.forEach { m ->
                    ModelRow(label = m, selected = currentChatModel == m, onClick = { onSelect(m) })
                }
                if (currentChatModel != null && currentChatModel != globalModel && currentChatModel !in known) {
                    ModelRow(label = currentChatModel, selected = true, onClick = { onSelect(currentChatModel) })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    placeholder = { Text("autre identifiant de modèle", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text, fontSize = 13.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ce choix ne vaut que pour cette conversation et revient au réglage global avec « Nouvelle conversation ».",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (custom.isNotBlank()) onSelect(custom.trim()) else onDismiss() }) {
                Text(if (custom.isNotBlank()) "Appliquer" else "Fermer", color = c.cyan)
            }
        },
        dismissButton = {
            if (custom.isNotBlank()) {
                TextButton(onClick = onDismiss) { Text("Annuler", color = c.text2) }
            }
        }
    )
}

@Composable
private fun ModelRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (selected) c.green else c.surface3, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp),
            color = if (selected) c.green else c.text
        )
    }
}

/** Compteur discret sous le titre : tokens de la conversation et coût estimé. */
@Composable
fun UsageLine(totalTokens: Int, estimated: Boolean, cost: Double?) {
    if (totalTokens <= 0) return
    val c = Zcolors
    val tokens = if (totalTokens >= 1000) String.format(Locale.US, "%.1f k", totalTokens / 1000.0) else totalTokens.toString()
    val costText = cost?.let { v ->
        "  ·  " + (if (estimated) "~" else "") + "$" + (if (v < 1.0) String.format(Locale.US, "%.4f", v) else String.format(Locale.US, "%.2f", v))
    }.orEmpty()
    Text(
        (if (estimated) "~" else "") + tokens + " tokens" + costText,
        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
        color = c.text3
    )
}

/** Barre affichée tant qu'un serveur de développement tourne : ouvrir l'aperçu, journal, arrêter. */
@Composable
fun DevServerBar(
    url: String?,
    log: String,
    onOpen: () -> Unit,
    onStop: () -> Unit
) {
    val c = Zcolors
    var showLog by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (url != null) c.green else c.amber, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Serveur de développement",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = c.text
                )
                Text(
                    url?.removePrefix("http://") ?: "démarrage…",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                    color = c.text2,
                    maxLines = 1
                )
            }
            if (url != null) {
                BarAction("Ouvrir l'aperçu", c.cyan, c.cyanBg, onOpen)
                Spacer(Modifier.width(6.dp))
            }
            BarAction("Journal", c.text2, c.surface2) { showLog = !showLog }
            Spacer(Modifier.width(6.dp))
            BarAction("Arrêter", c.red, c.redBg, onStop)
        }
        if (showLog) {
            Spacer(Modifier.height(6.dp))
            Text(
                log.takeLast(1200).ifBlank { "Aucune sortie pour l'instant." },
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp, lineHeight = 14.sp),
                color = c.text2,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun BarAction(label: String, color: Color, bg: Color, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        fontSize = 11.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}
