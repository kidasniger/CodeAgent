package com.zagent.app.ui.screens

import androidx.compose.foundation.clickable
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

/** Choix d'un projet existant (~/projects/...) ou création d'un nouveau projet. */
@Composable
fun ProjectPickerDialog(viewModel: MainViewModel) {
    val c = Zcolors
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val projects = remember { viewModel.listProjects() }
    val current = viewModel.workingFolderUriString

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
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        "Aucun projet pour l'instant. Crée le premier ci-dessous.",
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
