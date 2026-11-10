package com.zagent.app.ui.components.chat

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import com.zagent.app.data.ModelCatalog
import com.zagent.app.data.Capacite
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextOverflow
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.glassSurface
import java.util.Locale
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.GlassDropdownMenu
import com.zagent.app.ui.components.common.GlassModalBottomSheet
import com.zagent.app.ui.components.common.SecondaryButton

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
    val shape = MaterialTheme.shapes.small
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
    onClose: () -> Unit,
    onOpenFull: (() -> Unit)? = null
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    GlassModalBottomSheet(
        onDismissRequest = onClose,
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
                Text(
                    "Changements Git",
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    color = c.text,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenFull != null) {
                    TextButton(onClick = onOpenFull) {
                        Text("Écran complet", color = c.violet)
                    }
                }
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

/**
 * Choix du modèle pour la conversation en cours (le réglage global ne change pas).
 * MàJ 102 — puces de filtre par capacité (Code, Vision, Raisonnement), badges de
 * capacité sur chaque ligne, favoris épinglés en tête.
 * MàJ 104 — section « Mode Duo » : deux modèles ensemble (collaboration ou débat).
 */
@Composable
fun ModelPickerDialog(
    globalModel: String,
    currentChatModel: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    favoris: Set<String> = emptySet(),
    onToggleFavori: (String) -> Unit = {},
    duoMode: String? = null,
    duoModeleB: String? = null,
    onDuoChange: (String?, String?) -> Unit = { _, _ -> }
) {
    val c = Zcolors
    var custom by remember { mutableStateOf("") }
    var filtre by remember { mutableStateOf<Capacite?>(null) }
    var duoB by remember { mutableStateOf(duoModeleB) }

    // Catalogue conversationnel filtré : favoris d'abord, modèle global masqué.
    val connus = ModelCatalog.conversationnels(filtre, favoris).map { it.id }.filter { it != globalModel }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modèle de la conversation", fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // MàJ 102 — puces de filtre par capacité
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    PuceFiltre("Tous", filtre == null) { filtre = null }
                    PuceFiltre(Capacite.CODE.libelle, filtre == Capacite.CODE) { filtre = Capacite.CODE }
                    PuceFiltre(Capacite.VISION.libelle, filtre == Capacite.VISION) { filtre = Capacite.VISION }
                    PuceFiltre(Capacite.RAISONNEMENT.libelle, filtre == Capacite.RAISONNEMENT) { filtre = Capacite.RAISONNEMENT }
                }
                Spacer(Modifier.height(8.dp))
                ModelRow(
                    label = "Réglage global : $globalModel",
                    selected = currentChatModel == null,
                    onClick = { onSelect(null) }
                )
                connus.forEach { id ->
                    val info = ModelCatalog.find(id)
                    LigneModeleCatalogue(
                        info = info,
                        idReserve = id,
                        selected = currentChatModel == id,
                        favori = id in favoris,
                        onClick = { onSelect(id) },
                        onFavori = { onToggleFavori(id) }
                    )
                }
                if (currentChatModel != null && currentChatModel != globalModel && currentChatModel !in connus) {
                    ModelRow(label = currentChatModel, selected = true, onClick = { onSelect(currentChatModel) })
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    placeholder = { Text("autre identifiant de modèle", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text, fontSize = 13.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (custom.isBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ce choix ne vaut que pour cette conversation et revient au réglage global avec « Nouvelle conversation ».",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                        color = c.text2
                    )
                }

                // ── MàJ 104 : Mode Duo ────────────────────────────────────────
                Spacer(Modifier.height(14.dp))
                Text(
                    "Mode Duo — deux modèles ensemble",
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = c.text
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    PuceFiltre("Désactivé", duoMode == null) { duoB = null; onDuoChange(null, null) }
                    PuceFiltre("Collaboration", duoMode == "collaboration") { onDuoChange("collaboration", duoB) }
                    PuceFiltre("Débat", duoMode == "debat") { onDuoChange("debat", duoB) }
                    PuceFiltre("Comparer", duoMode == "compare") { onDuoChange("compare", duoB) } // feat-005
                }
                when (duoMode) {
                    "collaboration" -> Text(
                        "Le modèle choisi plus haut (A) fait la tâche, puis le modèle B relit sa réponse et propose des corrections.",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = c.text2
                    )
                    "debat" -> Text(
                        "A et B alternent à voix haute, deux tours chacun, puis concluent. Écris ton sujet pour lancer le débat.",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = c.text2
                    )
                    "compare" -> Text( // feat-005
                        "Le même prompt part vers A et B, sans outils : les deux réponses s'affichent l'une sous l'autre, étiquetées, pour comparaison.",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = c.text2
                    )
                }
                if (duoMode != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Modèle B du duo :",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.text2
                    )
                    Column(
                        Modifier
                            .heightIn(max = 170.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ModelCatalog.conversationnels(null, favoris)
                            .filter { it.id != (currentChatModel ?: globalModel) }
                            .forEach { info ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { duoB = info.id; onDuoChange(duoMode, info.id) }
                                        .padding(vertical = 7.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(9.dp)
                                            .background(if (duoB == info.id) c.cyan else c.surface3, CircleShape)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            info.label,
                                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                                            color = if (duoB == info.id) c.cyan else c.text
                                        )
                                        Text(
                                            info.capacites.joinToString(" · ") { it.libelle },
                                            style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                                            color = c.text2
                                        )
                                    }
                                }
                            }
                    }
                }
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

/** Puce de filtre (capacités, mode Duo). */
@Composable
private fun PuceFiltre(label: String, active: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) c.cyanBg else c.surface2)
            .border(1.dp, if (active) c.cyan else c.border2, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = if (active) c.cyan else c.text2
        )
    }
}

/** Ligne du catalogue : libellé, badges de capacité, étoile favori. */
@Composable
private fun LigneModeleCatalogue(
    info: com.zagent.app.data.ModelInfo?,
    idReserve: String,
    selected: Boolean,
    favori: Boolean,
    onClick: () -> Unit,
    onFavori: () -> Unit
) {
    val c = Zcolors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (selected) c.green else c.surface3, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info?.label ?: idReserve,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp),
                    color = if (selected) c.green else c.text
                )
                if (info?.recommended == true) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.amberBg)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "Recommandé",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                            color = c.amber
                        )
                    }
                }
            }
            Text(
                info?.capacites?.joinToString(" · ") { it.libelle } ?: "Modèle personnalisé",
                style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                color = c.text2
            )
        }
        Icon(
            if (favori) Icons.Default.Star else Icons.Default.StarBorder,
            if (favori) "Retirer des favoris" else "Ajouter aux favoris",
            tint = if (favori) c.amber else c.text3,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onFavori)
                .padding(5.dp)
                .size(15.dp)
        )
    }
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

/** Nom court affiché dans le chip : dernier segment de l'identifiant, tronqué. */
private fun shortModelName(model: String): String =
    model.substringAfterLast('/').take(20)

/**
 * Petit chip en bas à gauche du champ : nom court du modèle actif ; un tap ouvre un
 * menu ancré (popup en verre) listant les modèles avec une description d'une ligne (MA51).
 */
@Composable
fun ModelChipMenu(
    globalModel: String,
    currentChatModel: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    favoris: Set<String> = emptySet()
) {
    val c = Zcolors
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .border(1.dp, c.border2.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Tune, null, tint = c.cyan, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                shortModelName(currentChatModel ?: globalModel),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp)
            )
            Spacer(Modifier.width(3.dp))
            Icon(Icons.Default.ExpandMore, null, tint = c.text3, modifier = Modifier.size(12.dp))
        }
        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            ModelMenuOption(
                name = "Réglage global : " + globalModel,
                description = "Utilise le modèle configuré dans Réglages",
                selected = currentChatModel == null,
                onClick = { onExpandedChange(false); onSelect(null) }
            )
            // MàJ 102 — catalogue complet (favoris en tête), badges de capacité en description.
            val known = ModelCatalog.conversationnels(null, favoris).map { it.id }
            known.forEach { m ->
                val info = ModelCatalog.find(m)
                ModelMenuOption(
                    name = info?.label ?: m,
                    description = info?.capacites?.joinToString(" · ") { it.libelle } ?: "Modèle personnalisé",
                    selected = currentChatModel == m,
                    onClick = { onExpandedChange(false); onSelect(m) }
                )
            }
            if (currentChatModel != null && currentChatModel != globalModel && currentChatModel !in known) {
                ModelMenuOption(
                    name = currentChatModel,
                    description = "Modèle personnalisé de cette conversation",
                    selected = true,
                    onClick = { onExpandedChange(false); onSelect(currentChatModel) }
                )
            }
        }
    }
}

/** Option du menu modèle : identifiant + description d'une ligne. */
@Composable
private fun ModelMenuOption(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    name,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                    color = if (selected) c.green else c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    description,
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = c.text2
                )
            }
        },
        onClick = onClick
    )
}

/** Ligne d'usage en pied de message : tokens + coût, tap = détail entrée/sortie (MA45). */
@Composable
fun UsageLine(
    totalTokens: Int,
    estimated: Boolean,
    cost: Double?,
    promptTokens: Int = -1,
    completionTokens: Int = -1,
    showToggle: Boolean = false,
    usageUnderMessages: Boolean = false,
    onToggleUsageUnderMessages: (() -> Unit)? = null
) {
    if (totalTokens <= 0) return
    val c = Zcolors
    var open by remember { mutableStateOf(false) }
    val tokens = if (totalTokens >= 1000) String.format(Locale.US, "%.1f k", totalTokens / 1000.0) else totalTokens.toString()
    val costText = cost?.let { v ->
        "  ·  " + (if (estimated) "~" else "") + "$" + (if (v < 1.0) String.format(Locale.US, "%.4f", v) else String.format(Locale.US, "%.2f", v))
    }.orEmpty()
    Column {
        Text(
            (if (estimated) "~" else "") + tokens + " tokens" + costText,
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
            color = c.text3,
            modifier = Modifier.clickable { open = !open }
        )
        if (open) {
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(c.surface2)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (promptTokens >= 0) {
                    Text(
                        "Entrée : $promptTokens tokens",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = c.text2
                    )
                    Text(
                        "Sortie : $completionTokens tokens",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = c.text2
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (showToggle && onToggleUsageUnderMessages != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggleUsageUnderMessages() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Afficher l'usage sous les messages",
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                            color = c.text2,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(if (usageUnderMessages) c.cyan else c.surface3, CircleShape)
                        )
                    }
                }
            }
        }
    }
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
    val shape = MaterialTheme.shapes.medium
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
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}
