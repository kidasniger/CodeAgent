package com.zagent.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zagent.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var apiKey by remember { mutableStateOf(viewModel.apiKeyInput) }
    var model by remember { mutableStateOf(viewModel.modelInput) }
    var baseUrl by remember { mutableStateOf(viewModel.baseUrlInput) }
    var autoMode by remember { mutableStateOf(viewModel.autoModeInput) }
    var showPassword by remember { mutableStateOf(viewModel.showKeyVisible) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setWorkingFolder(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages ZAgent") },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveSettings(apiKey, model, baseUrl, autoMode)
                    }) {
                        Text("Enregistrer")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Clé API Z.ai / OpenAI") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Afficher/Cacher la clé"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { apiKey = "" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Changer la clé")
                }
            }

            Text("Modèle", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = model == "glm-4.5-flash",
                    onClick = { model = "glm-4.5-flash" },
                    label = { Text("glm-4.5-flash (Défaut)") }
                )
                FilterChip(
                    selected = model == "glm-4.7-flash",
                    onClick = { model = "glm-4.7-flash" },
                    label = { Text("glm-4.7-flash") }
                )
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("URL de base (Base URL)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode auto", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Écrit les fichiers sans demander de confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoMode,
                    onCheckedChange = { autoMode = it }
                )
            }

            Divider()

            Text("Dossier de travail", style = MaterialTheme.typography.titleMedium)
            val folderUriStr = viewModel.workingFolderUriString
            Text(
                text = if (folderUriStr.isNullOrEmpty()) "Aucun dossier sélectionné" else "Dossier: $folderUriStr",
                style = MaterialTheme.typography.bodyMedium,
                color = if (folderUriStr.isNullOrEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Choisir le dossier de travail")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveSettings(apiKey, model, baseUrl, autoMode)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Appliquer et Fermer")
            }
        }
    }
}
