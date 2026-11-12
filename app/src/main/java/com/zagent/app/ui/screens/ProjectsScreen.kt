package com.zagent.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.components.common.SecondaryButton
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA57 — Écran Projets plein écran (remplace ProjectPickerDialog) : projets
// récents avec dernière activité, favoris épinglés en haut, « Nouveau projet »
// (ouvre la galerie de templates MA68) et « Ouvrir un dossier » (import SAF).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProjectsScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    var newName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.importFolder(it) } }

    val projects = remember { viewModel.listProjects() }
    val favorites = remember { viewModel.settingsManager.favoriteProjects }
    val current = viewModel.workingFolderUriString

    OverlayScreen(
        title = stringResource(R.string.ui_projets),
        subtitle = current?.substringAfterLast('/') ?: "Aucun projet actif",
        onBack = onClose
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton(
                text = "Nouveau projet",
                onClick = {
                    onClose()
                    viewModel.isTemplatesOpen = true
                },
                modifier = Modifier.weight(1f),
                height = 48.dp,
                leading = { Icon(Icons.Default.Add, null, tint = c.onGrad, modifier = Modifier.size(17.dp)) }
            )
            SecondaryButton(
                text = "Ouvrir un dossier",
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier.weight(1f),
                height = 48.dp
            )
        }

        Spacer(Modifier.height(16.dp))

        if (projects.isEmpty()) {
            OverlayEmptyState(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.ui_aucun_projet),
                text = "Crée un projet depuis un template ou importe un dossier de ton téléphone. L'agent n'accède qu'au projet actif."
            )
        } else {
            val favs = projects.filter { it in favorites }
            val others = projects.filter { it !in favorites }

            if (favs.isNotEmpty()) {
                OverlaySection("Favoris")
                GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    favs.forEach { name -> ProjectRow(viewModel, name, current, onClose, isFavorite = true) }
                }
                Spacer(Modifier.height(14.dp))
            }

            OverlaySection("Tous les projets")
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                if (others.isEmpty() && favs.isNotEmpty()) {
                    Text(
                        stringResource(R.string.ui_tous_epingles),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                        color = c.text3,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                others.forEach { name -> ProjectRow(viewModel, name, current, onClose, isFavorite = false) }
            }
        }

        Spacer(Modifier.height(18.dp))
        OverlaySection("Créer un projet vide")
        GlassCard {
            androidx.compose.material3.OutlinedTextField(
                value = newName,
                onValueChange = { newName = it; createError = null },
                placeholder = { Text("nom-du-projet", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp), color = c.text3) },
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.5.sp, color = c.text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            if (createError != null) {
                Text(
                    createError.orEmpty(),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.red
                )
                Spacer(Modifier.height(8.dp))
            }
            GradientButton(
                text = "Créer et ouvrir",
                enabled = newName.isNotBlank(),
                onClick = {
                    val err = viewModel.createProject(newName)
                    if (err == null) onClose() else createError = err
                },
                height = 46.dp
            )
        }
    }
}

@Composable
private fun ProjectRow(
    viewModel: MainViewModel,
    name: String,
    current: String?,
    onClose: () -> Unit,
    isFavorite: Boolean
) {
    val c = Zcolors
    val dir = remember(name) { File(viewModel.projectsRootPublic(), name) }
    val df = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()) }
    val isActive = current != null && current.endsWith("/" + name)
    val fileCount = remember(name) { countFiles(dir) }
    // ux-084 — espace disque utilisé par le projet (exploration bornée).
    val taille = remember(name) { formatTaille(dirSize(dir)) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) c.cyanBg else androidx.compose.ui.graphics.Color.Transparent)
            .clickable {
                viewModel.selectProject(name)
                onClose()
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isActive) c.cyanBg else c.violetBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Widgets, null,
                tint = if (isActive) c.cyan else c.violet,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                fileCount.toString() + " fichiers · " + taille + " · modifié le " + df.format(Date(dir.lastModified())),
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                color = c.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable { viewModel.toggleProjectFavorite(name) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                if (isFavorite) "Retirer des favoris" else "Épingler aux favoris",
                tint = if (isFavorite) c.amber else c.text3,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun countFiles(dir: File): Int {
    if (!dir.isDirectory) return 0
    var n = 0
    val stack = ArrayDeque(listOf(dir))
    var guard = 0
    while (stack.isNotEmpty() && guard < 800) {
        guard++
        val d = stack.removeFirst()
        val children = d.listFiles() ?: continue
        for (f in children) {
            if (f.isDirectory) {
                if (f.name != "node_modules" && f.name != ".git") stack.add(f)
            } else n++
        }
    }
    return n
}

/** ux-084 — Taille cumulée d'un dossier de projet (exploration bornée à 2000 entrées). */
private fun dirSize(dir: File): Long {
    if (!dir.isDirectory) return 0L
    var total = 0L
    var guard = 0
    val stack = ArrayDeque(listOf(dir))
    while (stack.isNotEmpty() && guard < 2000) {
        guard++
        val d = stack.removeFirst()
        val children = d.listFiles() ?: continue
        for (f in children) {
            if (f.isDirectory) {
                if (f.name != "node_modules" && f.name != ".git") stack.add(f)
            } else total += f.length()
        }
    }
    return total
}

/** ux-084 — Formatage court d'une taille en octets (Ko / Mo / Go). */
private fun formatTaille(octets: Long): String {
    if (octets <= 0L) return "0 o"
    val ko = octets / 1024.0
    val mo = ko / 1024.0
    val go = mo / 1024.0
    return when {
        go >= 1.0 -> String.format(java.util.Locale.FRANCE, "%.1f Go", go)
        mo >= 1.0 -> String.format(java.util.Locale.FRANCE, "%.1f Mo", mo)
        ko >= 1.0 -> String.format(java.util.Locale.FRANCE, "%.0f Ko", ko)
        else -> octets.toString() + " o"
    }
}
