package com.zagent.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

/** Choix d'un projet existant (~/projects/...), création, import (zip ou dossier) et export en zip. */
@Composable
fun ProjectPickerDialog(viewModel: MainViewModel) {
    val c = Zcolors
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // La liste est relue après chaque import (le statut change à la fin du transfert).
    val projects = remember(viewModel.transferStatus, viewModel.transferBusy) { viewModel.listProjects() }
    val current = viewModel.workingFolderUriString
    var exportTarget by remember { mutableStateOf<String?>(null) }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importZip(uri)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.importFolder(uri)
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val target = exportTarget
        if (uri != null && target != null) viewModel.exportProject(target, uri)
        exportTarget = null
    }

    AlertDialog(
        onDismissRequest = { viewModel.isProjectPickerOpen = false },
        containerColor = c.bg2,
        title = { Text("Projets", fontFamily = SpaceGrotesk, color = c.text) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (projects.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        projects.forEach { p ->
                            val active = current != null && current.endsWith("/projects/$p")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectProject(p)
                                        viewModel.isProjectPickerOpen = false
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Folder, null,
                                    tint = if (active) c.green else c.amber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    p,
                                    color = if (active) c.green else c.text,
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                )
                                Text(
                                    "Exporter",
                                    color = if (viewModel.transferBusy) c.text3 else c.cyan,
                                    style = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier
                                        .clickable(enabled = !viewModel.transferBusy) {
                                            exportTarget = p
                                            exportPicker.launch("$p.zip")
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        "Aucun projet pour l'instant. Crée le premier ci-dessous ou importe-en un.",
                        color = c.text2,
                        style = TextStyle(fontSize = 13.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    placeholder = { Text("nom-du-projet", fontFamily = JetBrainsMono, color = c.text3) },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = c.amber, style = TextStyle(fontSize = 12.sp))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = !viewModel.transferBusy,
                        onClick = {
                            zipPicker.launch(
                                arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                            )
                        }
                    ) { Text("Importer un zip", color = if (viewModel.transferBusy) c.text3 else c.cyan) }
                    TextButton(
                        enabled = !viewModel.transferBusy,
                        onClick = { folderPicker.launch(null) }
                    ) { Text("Importer un dossier", color = if (viewModel.transferBusy) c.text3 else c.cyan) }
                }
                viewModel.transferStatus?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        color = if (it.startsWith("Erreur")) c.amber else c.text2,
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val err = viewModel.createProject(name)
                    if (err == null) viewModel.isProjectPickerOpen = false else error = err
                }
            }) {
                Text("Créer", color = c.cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.isProjectPickerOpen = false }) {
                Text("Fermer", color = c.text2)
            }
        }
    )
}
