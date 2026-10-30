package com.zagent.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val pendingWrite = viewModel.pendingWrite
    if (pendingWrite != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resolvePendingWrite(false) },
            title = { Text("Confirmation requise") },
            text = { Text("Écrire ${pendingWrite.path} ?\n\nContenu:\n${pendingWrite.content.take(300)}...") },
            confirmButton = {
                TextButton(onClick = { viewModel.resolvePendingWrite(true) }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolvePendingWrite(false) }) {
                    Text("Refuser")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ZAgent", style = MaterialTheme.typography.titleLarge)
                        val folderStr = viewModel.workingFolderUriString
                        val folderName = if (folderStr.isNullOrEmpty()) "Aucun dossier" else folderStr.substringAfterLast("%2F").substringAfterLast("/")
                        Text("Dossier: $folderName | Modèle: ${viewModel.modelInput}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Nouvelle conversation")
                    }
                    IconButton(onClick = { viewModel.isSettingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Réglages")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (viewModel.statusMessage.isNotEmpty() || viewModel.isLoading) {
                Surface(
                    color = if (viewModel.isCode1305Error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                            Text(
                                text = viewModel.statusMessage.ifEmpty { "Chargement..." },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (viewModel.showFallbackModelButton) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.switchToOtherModel() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Essayer l'autre modèle (${if (viewModel.modelInput == "glm-4.5-flash") "glm-4.7-flash" else "glm-4.5-flash"})")
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    when (msg.role) {
                        "user" -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Text(
                                        text = msg.content ?: "",
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        "assistant" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (!msg.content.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                        modifier = Modifier.widthIn(max = 300.dp)
                                    ) {
                                        Text(
                                            text = msg.content,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                if (msg.toolCalls != null) {
                                    for (tc in msg.toolCalls) {
                                        Text(
                                            text = "🔧 ${tc.name}(...)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "tool" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "${msg.toolLog ?: "🔧 outil"} -> ${(msg.content ?: "").take(100)}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Écrire un message à l'agent...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(text)
                            }
                        },
                        enabled = !viewModel.isLoading && inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Envoyer")
                    }
                }
            }
        }
    }
}
