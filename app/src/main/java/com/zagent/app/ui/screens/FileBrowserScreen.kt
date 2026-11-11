package com.zagent.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.FileRepository
import com.zagent.app.ui.components.common.CircleIconButton
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.ZEphemeral
import com.zagent.app.ui.components.common.StatusBadge
import com.zagent.app.ui.components.common.ZBreadcrumb
import com.zagent.app.ui.components.common.ZEmptyKind
import com.zagent.app.ui.components.common.ZEmptyState
import com.zagent.app.ui.components.common.ZMenu
import com.zagent.app.ui.components.common.ZMenuItem
import com.zagent.app.ui.components.common.ZMenuSeparator
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Explorateur : arbre du projet, recherche, badges « Agent » (MA85) ; fil
// d'Ariane cliquable (MA86) ; états vides illustrés et tirer-pour-actualiser
// (MA87) ; icônes par type, tri, vue grille, glisser latéral et sélection
// multiple (MA85).
// ─────────────────────────────────────────────────────────────────────────────

/** Critères de tri de l'explorateur (MA85). */
private enum class SortMode(val label: String) {
    NAME_ASC("Nom (A → Z)"),
    NAME_DESC("Nom (Z → A)"),
    DATE_DESC("Date (récent d'abord)"),
    DATE_ASC("Date (ancien d'abord)"),
    SIZE_DESC("Taille (décroissante)"),
    SIZE_ASC("Taille (croissante)")
}

/** Trie des entrées selon le mode choisi ; les dossiers restent en premier. */
private fun sortEntries(entries: List<FileRepository.Entry>, mode: SortMode): List<FileRepository.Entry> {
    val byCriterion = when (mode) {
        SortMode.NAME_ASC -> entries.sortedBy { it.name.lowercase() }
        SortMode.NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
        SortMode.DATE_DESC -> entries.sortedByDescending { it.lastModified }
        SortMode.DATE_ASC -> entries.sortedBy { it.lastModified }
        SortMode.SIZE_DESC -> entries.sortedByDescending { it.size }
        SortMode.SIZE_ASC -> entries.sortedBy { it.size }
    }
    // Tri stable : le critère est conservé au sein des deux groupes.
    return byCriterion.sortedByDescending { it.isDir }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 o"
    val ko = bytes / 1024.0
    return if (ko < 1000) {
        String.format(Locale.FRANCE, "%.1f Ko", ko)
    } else {
        String.format(Locale.FRANCE, "%.1f Mo", ko / 1024.0)
    }
}

private fun formatDate(ts: Long): String =
    if (ts <= 0L) "" else SimpleDateFormat("d MMM yyyy", Locale.FRANCE).format(Date(ts))

// ─────────────────────────────────────────────────────────────────────────────
// feat-072 — Glisser-déposer : appui long sur une ligne puis glisser vers un
// dossier pour déplacer le fichier. La cible survolée est mise en évidence.
// ─────────────────────────────────────────────────────────────────────────────

/** État du glisser en cours (un seul à la fois, propre à l'écran). */
class FileDragState {
    var draggingPath by mutableStateOf<String?>(null)
    var position by mutableStateOf(Offset.Zero)
    var hoverDir by mutableStateOf<String?>(null)

    val actif: Boolean get() = draggingPath != null

    fun reset() {
        draggingPath = null
        hoverDir = null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val c = Zcolors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var createKind by rememberSaveable { mutableStateOf<String?>(null) }
    var createPrefix by rememberSaveable { mutableStateOf("") }
    var menuNew by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileRepository.Entry?>(null) }
    var deleteTarget by remember { mutableStateOf<FileRepository.Entry?>(null) }

    // ── feat-071/073/074/075/079 : déplacer, zipper, importer, exporter, corbeille ──
    var moveDialogOpen by remember { mutableStateOf(false) }
    var zipDialogOpen by remember { mutableStateOf(false) }
    var trashSheetOpen by remember { mutableStateOf(false) }
    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.importProjectZip(uri) }

    // ── feat-072 : glisser-déposer vers un dossier ──
    val drag = remember { FileDragState() }
    val dropBounds = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }

    // ── MA85 : tri, vue grille, sélection multiple ──
    var sortMode by rememberSaveable { mutableStateOf(SortMode.NAME_ASC) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var gridMode by rememberSaveable { mutableStateOf(false) }
    var gridDir by rememberSaveable { mutableStateOf(".") }
    val selected = remember { mutableStateListOf<String>() }
    val selectionMode = selected.isNotEmpty()
    var confirmDeleteSelected by remember { mutableStateOf(false) }

    // Recharge à l'ouverture, après chaque sauvegarde de l'éditeur et après chaque écriture de l'agent.
    LaunchedEffect(viewModel.editor.saveCount, viewModel.modifiedPaths.size) { viewModel.refreshBrowser() }

    fun toggleSelection(path: String) {
        if (selected.contains(path)) selected.remove(path) else selected.add(path)
    }

    fun openEntry(e: FileRepository.Entry) {
        if (selectionMode) {
            toggleSelection(e.path)
        } else if (e.isDir) {
            if (gridMode) gridDir = e.path else viewModel.toggleDir(e.path)
        } else {
            viewModel.openBrowserFile(e.path)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Barre supérieure
        Column(
            Modifier
                .fillMaxWidth()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(24.dp), LocalContext.current)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Explorateur", style = MaterialTheme.typography.headlineSmall, color = c.text)
                    Text(
                        "${viewModel.folderLabel} · ${viewModel.browserCount} fichiers",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text2
                    )
                }
                if (viewModel.browserLoading || viewModel.browserFileLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = c.cyan)
                    Spacer(Modifier.width(10.dp))
                }
                // MA85 : tri par nom / date / taille.
                Box {
                    CircleIconButton(
                        Icons.AutoMirrored.Filled.Sort, "Trier",
                        onClick = { sortMenuOpen = true },
                        tintBackground = if (sortMode != SortMode.NAME_ASC) c.cyanBg else c.surface2
                    )
                    ZMenu(expanded = sortMenuOpen, onDismiss = { sortMenuOpen = false }) {
                        SortMode.entries.forEach { mode ->
                            ZMenuItem(
                                label = mode.label,
                                icon = if (mode == sortMode) Icons.Default.Check else Icons.AutoMirrored.Filled.Sort,
                                onClick = { sortMode = mode; sortMenuOpen = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                CircleIconButton(Icons.Default.Refresh, "Rafraîchir", onClick = { viewModel.refreshBrowser() })
                Spacer(Modifier.width(10.dp))
                // MA85 : bascule liste ↔ grille.
                CircleIconButton(
                    if (gridMode) Icons.Default.ViewList else Icons.Default.GridView,
                    if (gridMode) "Vue en liste" else "Vue en grille",
                    onClick = { gridMode = !gridMode }
                )
                Spacer(Modifier.width(10.dp))
                Box {
                    CircleIconButton(Icons.Default.Add, "Nouveau", onClick = { menuNew = true })
                    ZMenu(
                        expanded = menuNew,
                        onDismiss = { menuNew = false }
                    ) {
                        ZMenuItem(
                            label = "Nouveau fichier",
                            icon = Icons.Default.InsertDriveFile,
                            onClick = { menuNew = false; createPrefix = ""; createKind = "file" }
                        )
                        ZMenuItem(
                            label = "Nouveau dossier",
                            icon = Icons.Default.CreateNewFolder,
                            onClick = { menuNew = false; createPrefix = ""; createKind = "folder" }
                        )
                        // feat-078 : nouveaux fichiers depuis un modèle
                        ZMenuSeparator()
                        FichierModele("index.html — page web", "index.html", TemplatesFichiers.HTML) { p, contenu ->
                            menuNew = false; viewModel.createBrowserFileAvecModele(p, contenu)
                        }
                        FichierModele("main.py — script Python", "main.py", TemplatesFichiers.PYTHON) { p, contenu ->
                            menuNew = false; viewModel.createBrowserFileAvecModele(p, contenu)
                        }
                        FichierModele("Main.kt — point d'entrée Kotlin", "Main.kt", TemplatesFichiers.KOTLIN) { p, contenu ->
                            menuNew = false; viewModel.createBrowserFileAvecModele(p, contenu)
                        }
                        FichierModele("README.md — documentation", "README.md", TemplatesFichiers.README) { p, contenu ->
                            menuNew = false; viewModel.createBrowserFileAvecModele(p, contenu)
                        }
                        // feat-074/075 : import et export du projet
                        ZMenuSeparator()
                        ZMenuItem(
                            label = "Importer une archive (.zip)",
                            icon = Icons.Default.Unarchive,
                            onClick = { menuNew = false; importZipLauncher.launch("application/zip") }
                        )
                        ZMenuItem(
                            label = "Exporter le projet (.zip)",
                            icon = Icons.Default.Archive,
                            onClick = { menuNew = false; viewModel.exportProjectZip() }
                        )
                        ZMenuItem(
                            label = "Corbeille",
                            icon = Icons.Default.DeleteSweep,
                            onClick = { menuNew = false; trashSheetOpen = true }
                        )
                    }
                }
            }
            // Accès limité au dossier choisi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = c.text3, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Accès limité au dossier choisi",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = c.text3
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── MA86 : fil d'Ariane cliquable (projet › app › src › main) ──
        val deepestExpanded = viewModel.expandedDirs.keys.maxByOrNull {
            it.count { ch -> ch == '/' } + 1
        } ?: "."
        val focusPath = if (gridMode) gridDir else deepestExpanded
        val focusParts = focusPath.split('/').filter { it.isNotBlank() && it != "." }
        val segments = remember(focusPath, viewModel.folderLabel) {
            listOf(viewModel.folderLabel) + focusParts
        }

        fun pathForSegment(i: Int): String =
            if (i <= 0) "." else focusParts.take(i).joinToString("/")

        ZBreadcrumb(
            segments = segments,
            modifier = Modifier.padding(horizontal = 20.dp),
            onSegmentClick = { i ->
                if (gridMode) {
                    gridDir = pathForSegment(i)
                } else if (i == 0) {
                    // Racine : referme toute l'arborescence.
                    viewModel.expandedDirs.keys.toList().forEach { viewModel.expandedDirs.remove(it) }
                } else {
                    // Referme les dossiers strictement plus profonds que le segment choisi.
                    val target = pathForSegment(i)
                    val targetDepth = i
                    viewModel.expandedDirs.keys.toList().forEach { p ->
                        val depth = p.count { ch -> ch == '/' } + 1
                        val under = p == target || p.startsWith("$target/")
                        if (under && depth > targetDepth) viewModel.expandedDirs.remove(p)
                    }
                }
            }
        )

        // ── MA85 : barre de sélection multiple ──
        if (selectionMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.cyanBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selected.size == 1) "1 sélectionné" else "${selected.size} sélectionnés",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = c.text,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Copier",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            clipboard.setText(AnnotatedString(selected.joinToString("\n")))
                            // ui-091 — message éphémère stylé au lieu du Toast système.
                            ZEphemeral.show(
                                if (selected.size == 1) "Chemin copié" else "${selected.size} chemins copiés"
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(6.dp))
                // feat-071 : déplacer la sélection vers un dossier
                Text(
                    "Déplacer",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.violet,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { moveDialogOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(6.dp))
                // feat-071/073 : zipper la sélection
                Text(
                    "Zipper",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.amber,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { zipDialogOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Supprimer",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.red,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { confirmDeleteSelected = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.Close, "Quitter la sélection",
                    tint = c.text2, modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { selected.clear() }
                        .padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Recherche
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .border(1.dp, c.border2, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = c.text3, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text("Rechercher un fichier…", style = TextStyle(fontFamily = Inter, fontSize = 14.sp), color = c.text3)
                },
                textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = c.text),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── feat-076 : favoris et fichiers récents ──
        if (viewModel.browserFavorites.isNotEmpty() || viewModel.browserRecents.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (viewModel.browserFavorites.isNotEmpty()) {
                    Text(
                        "Favoris",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                        color = c.text3
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.browserFavorites.forEach { path ->
                            ChipFavoriRecent(
                                path = path,
                                couleur = c.amber,
                                icone = Icons.Default.Star,
                                onOpen = { viewModel.openBrowserFile(path) },
                                onRemove = { viewModel.toggleFavorite(path) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (viewModel.browserRecents.isNotEmpty()) {
                    Text(
                        "Récents",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                        color = c.text3
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.browserRecents.forEach { path ->
                            ChipFavoriRecent(
                                path = path,
                                couleur = c.cyan,
                                icone = Icons.Default.InsertDriveFile,
                                onOpen = { viewModel.openBrowserFile(path) },
                                onRemove = null
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Contenu : arbre (liste) ou grille du dossier courant ──
        if (gridMode) {
            LaunchedEffect(gridDir) { viewModel.ensureLoaded(gridDir) }
            val gridEntries = remember(viewModel.browserChildren[gridDir], sortMode, query) {
                sortEntries(
                    (viewModel.browserChildren[gridDir] ?: emptyList()).filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true)
                    },
                    sortMode
                )
            }
            // MA87 : tirer pour actualiser la liste de fichiers.
            PullToRefreshBox(
                isRefreshing = viewModel.browserLoading,
                onRefresh = { viewModel.refreshBrowser() },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (gridEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BrowserEmptyState(
                            queryBlank = query.isBlank(),
                            error = viewModel.browserError,
                            onNewFile = { createPrefix = ""; createKind = "file" }
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 104.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        gridItems(gridEntries, key = { it.path }) { entry ->
                            GridCell(
                                entry = entry,
                                isModified = viewModel.modifiedPaths.containsKey(entry.path),
                                selected = selected.contains(entry.path),
                                selectionMode = selectionMode,
                                onClick = { openEntry(entry) },
                                onLongClick = { toggleSelection(entry.path) }
                            )
                        }
                    }
                }
            }
        } else if (!viewModel.browserChildren.containsKey(".") && viewModel.browserLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.cyan)
            }
        } else {
            val rows = rememberBrowserRows(viewModel, query, sortMode)
            // MA87 : tirer pour actualiser la liste de fichiers.
            PullToRefreshBox(
                isRefreshing = viewModel.browserLoading,
                onRefresh = { viewModel.refreshBrowser() },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (rows.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BrowserEmptyState(
                                queryBlank = query.isBlank(),
                                error = viewModel.browserError,
                                onNewFile = { createPrefix = ""; createKind = "file" }
                            )
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 104.dp)
                        ) {
                            items(rows, key = { "${it.entry.path}:${it.depth}" }) { row ->
                                BrowserRow(
                                    row = row,
                                    viewModel = viewModel,
                                    selected = selected.contains(row.entry.path),
                                    selectionMode = selectionMode,
                                    drag = drag,
                                    dropBounds = dropBounds,
                                    onOpen = { openEntry(it) },
                                    onLongClick = { toggleSelection(it.path) }
                                ) { action, e ->
                                    val prefix = if (e.isDir) e.path + "/" else ""
                                    when (action) {
                                        "newfile" -> { createPrefix = prefix; createKind = "file" }
                                        "newfolder" -> { createPrefix = prefix; createKind = "folder" }
                                        "rename" -> renameTarget = e
                                        "delete" -> deleteTarget = e
                                        "copypath" -> {
                                            clipboard.setText(AnnotatedString(e.path))
                                            ZEphemeral.show("Chemin copié") // ui-091
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.browserError?.let { error ->
            Text(
                error,
                color = c.red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        // Synthèse en bas
        if (viewModel.modifiedPaths.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.violetBg)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${viewModel.modifiedPaths.size} fichier${if (viewModel.modifiedPaths.size > 1) "s" else ""} modifié${if (viewModel.modifiedPaths.size > 1) "s" else ""} par l'agent",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                        color = c.violet
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "dernière sync · à l'instant",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text3
                )
            }
        }
    }

    // ── Dialogues ───────────────────────────────────────────────────────────

    // MA85 : confirmation de suppression de la sélection multiple.
    if (confirmDeleteSelected) {
        GlassAlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("Supprimer ${selected.size} élément${if (selected.size > 1) "s" else ""} ?", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Text(
                    "Les dossiers sélectionnés et tout leur contenu seront supprimés.",
                    fontFamily = Inter, fontSize = 13.sp, color = c.text2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // feat-079 : suppression annulable — la sélection part en corbeille.
                    selected.toList().forEach { viewModel.trashBrowserPath(it) }
                    selected.clear()
                    confirmDeleteSelected = false
                }) { Text("Supprimer", color = c.red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelected = false }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    // ── feat-071 : dialogue « Déplacer vers… » (liste des dossiers du projet) ──
    if (moveDialogOpen) {
        val dossiers = remember(viewModel.browserChildren) {
            val list = mutableListOf(".")
            viewModel.browserChildren.forEach { (dir, entries) ->
                entries.filter { it.isDir }.forEach { list.add(it.path) }
                if (dir != "." && !list.contains(dir)) list.add(dir)
            }
            list.distinct().sorted()
        }
        GlassAlertDialog(
            onDismissRequest = { moveDialogOpen = false },
            title = { Text("Déplacer ${selected.size} élément(s) vers…", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    if (dossiers.isEmpty()) {
                        Text("Aucun dossier connu — déplie d'abord l'arborescence.", color = c.text2, fontFamily = Inter, fontSize = 12.sp)
                    }
                    dossiers.forEach { d ->
                        Text(
                            if (d == ".") "Racine du projet" else d,
                            fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.cyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.moveSelected(selected.toList(), d)
                                    selected.clear()
                                    moveDialogOpen = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveDialogOpen = false }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    // ── feat-071/073 : dialogue de nom d'archive ──
    if (zipDialogOpen) {
        var nomZip by remember { mutableStateOf("archive") }
        GlassAlertDialog(
            onDismissRequest = { zipDialogOpen = false },
            title = { Text("Créer une archive", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Column {
                    Text(
                        selected.joinToString("\n") { it },
                        fontFamily = JetBrainsMono, fontSize = 11.5.sp, color = c.text2,
                        maxLines = 4
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nomZip,
                        onValueChange = { nomZip = it },
                        textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.zipSelected(selected.toList(), nomZip.trim())
                    selected.clear()
                    zipDialogOpen = false
                }) { Text("Zipper", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { zipDialogOpen = false }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    // ── feat-079 : feuille corbeille (restauration sous 7 jours) ──
    if (trashSheetOpen) {
        var entries by remember { mutableStateOf<List<FileRepository.TrashEntry>?>(null) }
        LaunchedEffect(Unit) { entries = viewModel.loadTrashEntries() }
        GlassAlertDialog(
            onDismissRequest = { trashSheetOpen = false },
            title = { Text("Corbeille du projet", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Les éléments sont conservés 7 jours, puis supprimés automatiquement.",
                        fontFamily = Inter, fontSize = 11.5.sp, color = c.text3
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        entries == null -> Text("Chargement…", color = c.text2, fontFamily = Inter, fontSize = 12.sp)
                        entries.orEmpty().isEmpty() -> Text("La corbeille est vide.", color = c.text2, fontFamily = Inter, fontSize = 12.sp)
                        else -> entries.orEmpty().forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text)
                                    Text(
                                        formatSize(entry.bytes) + " · " + formatDate(entry.trashedAt),
                                        fontFamily = Inter, fontSize = 10.5.sp, color = c.text3
                                    )
                                }
                                Icon(
                                    Icons.Default.RestoreFromTrash, "Restaurer " + entry.name,
                                    tint = c.cyan,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { viewModel.restoreTrashEntry(entry.id) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    trashSheetOpen = false
                }) { Text("Vider", color = c.red) }
            },
            dismissButton = {
                TextButton(onClick = { trashSheetOpen = false }) { Text("Fermer", color = c.text2) }
            }
        )
    }

    viewModel.browserFilePreview?.let { preview ->
        GlassAlertDialog(
            onDismissRequest = { viewModel.closeBrowserFilePreview() },
            title = { Text(preview.path, color = c.text, fontFamily = JetBrainsMono, maxLines = 2) },
            text = {
                Column(Modifier.fillMaxWidth().height(360.dp).verticalScroll(rememberScrollState())) {
                    Text(preview.content, color = c.text2, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeBrowserFilePreview() }) {
                    Text("Fermer", color = c.cyan)
                }
            }
        )
    }

    createKind?.let { kind ->
        var name by remember(kind, createPrefix) { mutableStateOf(createPrefix) }
        GlassAlertDialog(
            onDismissRequest = { createKind = null },
            title = {
                Text(
                    if (kind == "file") "Nouveau fichier" else "Nouveau dossier",
                    fontFamily = SpaceGrotesk, color = c.text
                )
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            if (kind == "file") "src/index.html" else "src/utils",
                            fontFamily = JetBrainsMono, color = c.text3
                        )
                    },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim().trim('/')
                    if (n.isNotEmpty()) {
                        if (kind == "file") viewModel.createBrowserFile(n) else viewModel.createBrowserFolder(n)
                    }
                    createKind = null
                }) { Text("Créer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { createKind = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    renameTarget?.let { e ->
        var name by remember(e.path) { mutableStateOf(e.name) }
        GlassAlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renommer", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameBrowserPath(e.path, name.trim())
                    renameTarget = null
                }) { Text("Renommer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    deleteTarget?.let { e ->
        GlassAlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Supprimer ?", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Text(
                    e.path + if (e.isDir) "\n(le dossier et tout son contenu)" else "",
                    fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // feat-079 : la corbeille remplace la suppression définitive.
                    viewModel.trashBrowserPath(e.path)
                    deleteTarget = null
                }) { Text("Supprimer", color = c.red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    // ── feat-072 : fantôme du fichier glissé + dépôt sur un dossier ──
    if (drag.actif) {
        Box(
            Modifier
                .offset { IntOffset((drag.position.x - 40).roundToInt(), (drag.position.y - 20).roundToInt()) }
                .clip(RoundedCornerShape(50))
                .background(c.cyanBg)
                .border(1.dp, c.cyan, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "→ " + (drag.hoverDir?.let { if (it == ".") "racine" else it.substringAfterLast('/') } ?: "déplacer"),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = c.cyan
            )
        }
    }
    } // fin Box feat-072
}

// ─────────────────────────────────────────────────────────────────────────────
// MA87 — État vide illustré de l'explorateur : dossier vide, aucun résultat,
// ou dossier inaccessible (hors-ligne).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrowserEmptyState(queryBlank: Boolean, error: String?, onNewFile: () -> Unit) {
    if (!queryBlank) {
        ZEmptyState(
            kind = ZEmptyKind.RESULTS,
            title = "Aucun résultat",
            body = "Aucun fichier ne correspond à ta recherche. Essaie un autre mot-clé."
        )
    } else if (error != null) {
        ZEmptyState(
            kind = ZEmptyKind.OFFLINE,
            title = "Dossier inaccessible",
            body = error
        )
    } else {
        ZEmptyState(
            kind = ZEmptyKind.FOLDER,
            title = "Ce dossier est vide",
            body = "Crée un premier fichier pour démarrer le projet.",
            actionText = "Nouveau fichier",
            onAction = onNewFile
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Construction de l'arbre visible (tri MA85 appliqué à chaque niveau)
// ─────────────────────────────────────────────────────────────────────────────

data class BrowserRowData(
    val entry: FileRepository.Entry,
    val depth: Int
)

@Composable
private fun rememberBrowserRows(viewModel: MainViewModel, query: String, sortMode: SortMode): List<BrowserRowData> {
    // Lire les StateMap pendant la composition : remember(map) ne voit pas leurs mutations internes.
    val children = viewModel.browserChildren
    val expanded = viewModel.expandedDirs
    val out = mutableListOf<BrowserRowData>()

    fun walk(path: String, depth: Int) {
        val list = children[path] ?: return
        for (entry in sortEntries(list, sortMode)) {
            if (query.isNotBlank() && !entry.name.contains(query, ignoreCase = true)) continue
            out.add(BrowserRowData(entry, depth))
            if (entry.isDir && expanded.containsKey(entry.path)) walk(entry.path, depth + 1)
        }
    }
    walk(".", 0)
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Ligne de l'arbre : sélection multiple (MA85), glisser latéral pour
// Copier le chemin / Renommer / Supprimer (MA85).
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserRow(
    row: BrowserRowData,
    viewModel: MainViewModel,
    selected: Boolean,
    selectionMode: Boolean,
    drag: FileDragState,
    dropBounds: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Rect>,
    onOpen: (FileRepository.Entry) -> Unit,
    onLongClick: (FileRepository.Entry) -> Unit,
    onAction: (String, FileRepository.Entry) -> Unit
) {
    val c = Zcolors
    val e = row.entry
    val isModified = viewModel.modifiedPaths.containsKey(e.path)
    val expanded = viewModel.expandedDirs.containsKey(e.path)
    // feat-072 : dossier survolé pendant un glisser
    val estCible = drag.actif && e.isDir && e.path != drag.draggingPath &&
        !e.path.startsWith(drag.draggingPath.orEmpty() + "/")
    val survole = estCible && drag.hoverDir == e.path
    var rowCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // feat-072 : enregistre la zone de dépôt des dossiers visibles.
    if (e.isDir && !drag.actif) {
        androidx.compose.runtime.DisposableEffect(e.path, expanded) {
            onDispose { dropBounds.remove(e.path) }
        }
    }

    // MA85 : le glisser latéral révèle Copier le chemin / Renommer / Supprimer.
    // Désactivé pendant la sélection multiple.
    SwipeActionsRow(
        enabled = !selectionMode && !drag.actif,
        onCopyPath = { onAction("copypath", e) },
        onRename = { onAction("rename", e) },
        onDelete = { onAction("delete", e) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .onGloballyPositioned { coords ->
                    rowCoords = coords
                    if (e.isDir) dropBounds[e.path] = coords.boundsInWindow()
                }
                .background(
                    when {
                        survole -> c.greenBg
                        selected -> c.cyanBg
                        isModified -> c.violetBg.copy(alpha = 0.35f)
                        else -> c.surface
                    }
                )
                .pointerInput(e.path, selectionMode) {
                    // feat-072 : appui long puis glisser → déplacer vers le dossier survolé.
                    if (selectionMode) return@pointerInput
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            if (dropBounds.isNotEmpty()) {
                                drag.draggingPath = e.path
                                drag.position = (rowCoords?.positionInWindow() ?: Offset.Zero) + start
                                drag.hoverDir = null
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.position += amount
                            val cible = dropBounds.entries
                                .filter { it.key != drag.draggingPath && !drag.draggingPath.orEmpty().startsWith(it.key + "/") }
                                .firstOrNull { it.value.contains(drag.position) }?.key
                            drag.hoverDir = cible
                        },
                        onDragEnd = {
                            val source = drag.draggingPath
                            val cible = drag.hoverDir
                            if (source != null && cible != null && cible != source) {
                                viewModel.moveSelected(listOf(source), cible)
                            }
                            drag.reset()
                        },
                        onDragCancel = { drag.reset() }
                    )
                }
                .combinedClickable(
                    onClick = { onOpen(e) },
                    onLongClick = { onLongClick(e) }
                )
                .padding(start = (12 + row.depth * 22).dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isModified) {
                // Barre d'accent dégradée sur la gauche
                Box(
                    Modifier
                        .size(width = 3.dp, height = 34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.gradLine)
                )
                Spacer(Modifier.width(10.dp))
            }

            // MA85 : pastille de sélection multiple.
            if (selectionMode) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) c.cyan else Color.Transparent)
                        .border(1.5.dp, if (selected) c.cyan else c.text3.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Default.Check, null, tint = c.bg, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
            }

            FileIconBadge(entry = e)

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.name,
                        style = TextStyle(
                            fontFamily = if (e.isDir) Inter else JetBrainsMono,
                            fontWeight = if (e.isDir) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        ),
                        color = c.text
                    )
                    if (isModified) {
                        Spacer(Modifier.width(8.dp))
                        StatusBadge("Agent", c.violet)
                    }
                }
                if (!e.isDir) {
                    Text(
                        formatSize(e.size),
                        style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                        color = c.text3
                    )
                }
            }

            if (e.isDir) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Replier" else "Déplier",
                    tint = c.text3,
                    modifier = Modifier.size(18.dp)
                )
            }
            RowMenu(e, viewModel) { action -> onAction(action, e) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MA85 — Glisser latéral : la ligne coulisse vers la gauche et révèle
// trois actions (Copier le chemin, Renommer, Supprimer).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwipeActionsRow(
    enabled: Boolean,
    onCopyPath: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionsWidthPx = with(density) { 204.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val settle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Referme la ligne quand le mode sélection s'active.
    LaunchedEffect(enabled) {
        if (!enabled && offset.value != 0f) {
            offset.animateTo(0f, settle)
        }
    }

    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        // Actions derrière la ligne, alignées à droite.
        // MA85 · correctif mode verre : avec un preset translucide (surface à
        // 5,5 % d'opacité), la ligne ne masquait plus les pavés colorés, visibles
        // en permanence sous chaque élément. Les actions ne sont donc peintes
        // qu'à mesure que le glissement les révèle (alpha = progression du
        // swipe) : invisibles au repos quel que soit le matériau de surface.
        Row(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = (-offset.value / actionsWidthPx).coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeAction("Copier", Icons.Default.ContentCopy, Color(0xFF2563EB)) { onCopyPath() }
            SwipeAction("Renommer", Icons.Default.Edit, Color(0xFFD97706)) { onRename() }
            SwipeAction("Suppr.", Icons.Default.Delete, Color(0xFFDC2626)) { onDelete() }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(actionsWidthPx) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    val next = (offset.value + amount).coerceIn(-actionsWidthPx, 0f)
                                    scope.launch { offset.snapTo(next) }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        offset.animateTo(
                                            if (offset.value < -actionsWidthPx / 2) -actionsWidthPx else 0f,
                                            settle
                                        )
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { offset.animateTo(0f, settle) }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(68.dp)
            .background(color)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
            color = Color.White,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MA85 — Cellule de la vue grille
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(
    entry: FileRepository.Entry,
    isModified: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val c = Zcolors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) c.cyanBg else c.surface)
            .border(1.dp, if (isModified) c.violet else c.border, MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            FileIconBadge(entry = entry)
            if (selectionMode) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(if (selected) c.cyan else c.bg2)
                        .border(1.dp, if (selected) c.cyan else c.border2, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Default.Check, null, tint = c.bg, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            entry.name,
            style = TextStyle(
                fontFamily = if (entry.isDir) Inter else JetBrainsMono,
                fontWeight = if (entry.isDir) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.5.sp
            ),
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (entry.isDir) "Dossier" else formatSize(entry.size),
            style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
            color = c.text3,
            maxLines = 1
        )
        if (!entry.isDir && formatDate(entry.lastModified).isNotEmpty()) {
            Text(
                formatDate(entry.lastModified),
                style = TextStyle(fontFamily = Inter, fontSize = 10.sp),
                color = c.text3,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icônes colorées par type de fichier (MA85)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FileIconBadge(entry: FileRepository.Entry) {
    val c = Zcolors
    val size = 36.dp
    when {
        entry.isDir -> Box(
            Modifier.size(size).clip(MaterialTheme.shapes.small).background(c.amberBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Folder, null, tint = c.amber, modifier = Modifier.size(19.dp))
        }
        else -> {
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            data class TypeSpec(val letter: String?, val icon: ImageVector?, val fg: Color, val bg: Color)

            // MA85 : une couleur par type — Kotlin, HTML, CSS, JS, JSON, Markdown, images…
            val spec = when (ext) {
                "kt", "kts", "java" -> TypeSpec("K", null, c.violet, c.violetBg)
                "html", "htm", "xml", "svg" -> TypeSpec("H", null, c.red, c.redBg)
                "css", "scss" -> TypeSpec("C", null, c.cyan, c.cyanBg)
                "js", "jsx", "ts", "tsx", "mjs" -> TypeSpec("J", null, c.amber, c.amberBg)
                "json" -> TypeSpec("{", null, c.green, c.greenBg)
                "md", "txt" -> TypeSpec("M", null, c.green, c.greenBg)
                "png", "jpg", "jpeg", "webp", "gif" -> TypeSpec(null, Icons.Default.Image, c.green, c.greenBg)
                "gradle", "properties", "toml", "pro", "yaml", "yml" -> TypeSpec("G", null, c.cyan, c.cyanBg)
                else -> TypeSpec(null, Icons.Default.InsertDriveFile, c.text2, c.surface2)
            }
            Box(
                Modifier.size(size).clip(MaterialTheme.shapes.small).background(spec.bg),
                contentAlignment = Alignment.Center
            ) {
                if (spec.letter != null) {
                    Text(
                        spec.letter,
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = spec.fg
                    )
                } else {
                    Icon(spec.icon!!, null, tint = spec.fg, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun RowMenu(
    e: FileRepository.Entry,
    viewModel: MainViewModel,
    onAction: (String) -> Unit
) {
    val c = Zcolors
    var open by remember { mutableStateOf(false) }
    val estFavori = e.path in viewModel.browserFavorites
    Box {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, "Actions", tint = c.text3, modifier = Modifier.size(18.dp))
        }
        // MA78 : menu unifié en verre ; MA85 : « Copier le chemin » ajouté.
        ZMenu(
            expanded = open,
            onDismiss = { open = false }
        ) {
            if (e.isDir) {
                ZMenuItem(
                    label = "Nouveau fichier ici",
                    icon = Icons.Default.InsertDriveFile,
                    onClick = { open = false; onAction("newfile") }
                )
                ZMenuItem(
                    label = "Nouveau dossier ici",
                    icon = Icons.Default.CreateNewFolder,
                    onClick = { open = false; onAction("newfolder") }
                )
            }
            ZMenuItem(
                label = "Copier le chemin",
                icon = Icons.Default.ContentCopy,
                onClick = { open = false; onAction("copypath") }
            )
            // feat-076 : épingler le fichier dans les favoris
            ZMenuItem(
                label = if (estFavori) "Retirer des favoris" else "Épingler dans les favoris",
                icon = if (estFavori) Icons.Default.StarBorder else Icons.Default.Star,
                onClick = { open = false; viewModel.toggleFavorite(e.path) }
            )
            // feat-073 : zipper / dézipper
            ZMenuItem(
                label = "Zipper",
                icon = Icons.Default.Archive,
                onClick = { open = false; viewModel.zipSelected(listOf(e.path), e.name) }
            )
            if (e.name.endsWith(".zip", ignoreCase = true)) {
                ZMenuItem(
                    label = "Extraire ici",
                    icon = Icons.Default.Unarchive,
                    onClick = { open = false; viewModel.unzipBrowserFile(e.path, ".") }
                )
            }
            ZMenuItem(
                label = "Renommer",
                icon = Icons.Default.Edit,
                onClick = { open = false; onAction("rename") }
            )
            ZMenuSeparator()
            ZMenuItem(
                label = "Supprimer (corbeille)",
                icon = Icons.Default.Delete,
                destructive = true,
                onClick = { open = false; onAction("delete") }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// feat-078 — Modèles de nouveaux fichiers (HTML, Python, Kotlin, README).
// ─────────────────────────────────────────────────────────────────────────────

/** Contenus de départ pour « Nouveau fichier depuis un modèle » (feat-078). */
object TemplatesFichiers {
    val HTML = """<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Page</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <h1>Bonjour</h1>
  <script src="script.js"></script>
</body>
</html>
"""

    val PYTHON = """#!/usr/bin/env python3
\"\"\"Script principal.\"\"\"


def main() -> None:
    print("Bonjour")


if __name__ == "__main__":
    main()
"""

    val KOTLIN = """fun main() {
    println("Bonjour")
}
"""

    val README = """# Projet

Description courte du projet.

## Démarrage

1. Étape une
2. Étape deux

## Structure

- `src/` — code source
"""
}

/** Entrée de menu qui demande le chemin du fichier à créer depuis un modèle (feat-078). */
@Composable
private fun FichierModele(
    label: String,
    nomDefaut: String,
    contenu: String,
    onCreer: (String, String) -> Unit
) {
    val c = Zcolors
    var demande by remember { mutableStateOf(false) }
    ZMenuItem(
        label = label,
        icon = Icons.Default.InsertDriveFile,
        onClick = { demande = true }
    )
    if (demande) {
        var nom by remember { mutableStateOf(nomDefaut) }
        GlassAlertDialog(
            onDismissRequest = { demande = false },
            title = { Text("Nouveau fichier depuis modèle", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreer(nom.trim(), contenu)
                    demande = false
                }) { Text("Créer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { demande = false }) { Text("Annuler", color = c.text2) }
            }
        )
    }
}

/** Puce « favori » ou « récent » de l'explorateur (feat-076) : ouvre au tap, retire au × si épinglé. */
@Composable
private fun ChipFavoriRecent(
    path: String,
    couleur: androidx.compose.ui.graphics.Color,
    icone: ImageVector,
    onOpen: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val c = Zcolors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .border(1.dp, c.border2, RoundedCornerShape(50))
            .clickable { onOpen() }
            .padding(start = 8.dp, end = if (onRemove != null) 4.dp else 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Icon(icone, null, tint = couleur, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            path.substringAfterLast('/'),
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
            color = c.text,
            maxLines = 1
        )
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Close, "Retirer " + path + " des favoris",
                tint = c.text3,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() }
                    .padding(4.dp)
            )
        }
    }
}
