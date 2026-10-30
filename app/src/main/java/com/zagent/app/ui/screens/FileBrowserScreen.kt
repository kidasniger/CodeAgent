package com.zagent.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.FileRepository
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.StatusBadge
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Explorateur : arbre du projet, recherche, badges « Agent »
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FileBrowserScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val c = Zcolors
    var query by rememberSaveable { mutableStateOf("") }
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshBrowser() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Barre supérieure
        Column(Modifier.padding(horizontal = 16.dp)) {
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
                if (viewModel.browserLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = c.cyan)
                    Spacer(Modifier.width(10.dp))
                }
                CircleIconButton(Icons.Default.Refresh, "Rafraîchir", onClick = { viewModel.refreshBrowser() })
                Spacer(Modifier.width(10.dp))
                CircleIconButton(Icons.Default.CreateNewFolder, "Nouveau dossier", onClick = { showNewFolderDialog = true })
            }
            // Fil d'ariane sécurisé
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

        Spacer(Modifier.height(10.dp))

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

        // Arbre
        val rows = rememberBrowserRows(viewModel, query)
        if (!viewModel.browserChildren.containsKey(".") && viewModel.browserLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.cyan)
            }
        } else {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = { "${it.entry.path}:${it.depth}" }) { row ->
                        BrowserRow(row, viewModel)
                    }
                    if (rows.isEmpty()) {
                        item {
                            Text(
                                if (query.isBlank()) "Dossier vide." else "Aucun fichier trouvé.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.text2,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }
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
                        "✦ ${viewModel.modifiedPaths.size} fichier${if (viewModel.modifiedPaths.size > 1) "s" else ""} modifié${if (viewModel.modifiedPaths.size > 1) "s" else ""} par l'agent",
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

    if (showNewFolderDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = c.bg2,
            title = { Text("Nouveau dossier", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("src/utils", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createBrowserFolder(name.trim())
                        }
                        showNewFolderDialog = false
                    }
                ) { Text("Créer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Annuler", color = c.text2) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Construction de l'arbre visible
// ─────────────────────────────────────────────────────────────────────────────

data class BrowserRow(
    val entry: FileRepository.Entry,
    val depth: Int
)

@Composable
private fun rememberBrowserRows(viewModel: MainViewModel, query: String): List<BrowserRow> {
    val children = viewModel.browserChildren
    val expanded = viewModel.expandedDirs

    return androidx.compose.runtime.remember(children, expanded, query) {
        val out = mutableListOf<BrowserRow>()

        fun walk(path: String, depth: Int) {
            val list = children[path] ?: return
            for (e in list) {
                if (query.isNotBlank() && !e.name.contains(query, ignoreCase = true)) continue
                out.add(BrowserRow(e, depth))
                if (e.isDir && expanded.contains(e.path)) walk(e.path, depth + 1)
            }
        }
        walk(".", 0)
        out
    }
}

@Composable
private fun BrowserRow(row: BrowserRow, viewModel: MainViewModel) {
    val c = Zcolors
    val e = row.entry
    val isModified = viewModel.modifiedPaths.containsKey(e.path)
    val expanded = viewModel.expandedDirs.containsKey(e.path)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (isModified) {
                    Modifier
                        .background(c.violetBg.copy(alpha = 0.35f))
                } else Modifier
            )
            .clickable {
                if (e.isDir) viewModel.toggleDir(e.path)
            }
            .padding(start = (12 + row.depth * 22).dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
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
    }
}

@Composable
private fun FileIconBadge(entry: FileRepository.Entry) {
    val c = Zcolors
    val size = 36.dp
    when {
        entry.isDir -> Box(
            Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(c.amberBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Folder, null, tint = c.amber, modifier = Modifier.size(19.dp))
        }
        else -> {
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            data class TypeSpec(val letter: String?, val icon: ImageVector?, val fg: Color, val bg: Color)

            val spec = when (ext) {
                "kt", "kts", "java" -> TypeSpec(ext.take(1).uppercase(), null, c.violet, c.violetBg)
                "gradle", "properties", "toml" -> TypeSpec("G", null, c.cyan, c.cyanBg)
                "md", "txt" -> TypeSpec("M", null, c.cyan, c.cyanBg)
                "png", "jpg", "jpeg", "webp", "gif" -> TypeSpec(null, Icons.Default.Image, c.green, c.greenBg)
                "json", "xml", "yaml", "yml", "pro" -> TypeSpec(null, Icons.Default.InsertDriveFile, c.text2, c.surface2)
                else -> TypeSpec(null, Icons.Default.InsertDriveFile, c.text2, c.surface2)
            }
            Box(
                Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(spec.bg),
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

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 o"
    val ko = bytes / 1024.0
    return if (ko < 1000) {
        String.format(Locale.FRANCE, "%.1f Ko", ko)
    } else {
        String.format(Locale.FRANCE, "%.1f Mo", ko / 1024.0)
    }
}
