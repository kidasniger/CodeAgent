package com.zagent.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Globe
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.IconBadge
import com.zagent.app.ui.components.ModelChip
import com.zagent.app.ui.components.SectionLabel
import com.zagent.app.ui.components.StatusBadge
import com.zagent.app.ui.components.CardDivider
import com.zagent.app.ui.components.ZLogo
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Réglages : clé & modèle · agent · mises à jour · apparence
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val c = Zcolors

    var apiKey by rememberSaveable { mutableStateOf(viewModel.apiKeyInput) }
    var baseUrl by rememberSaveable { mutableStateOf(viewModel.baseUrlInput) }
    var autoMode by rememberSaveable { mutableStateOf(viewModel.autoModeInput) }
    var model by rememberSaveable { mutableStateOf(viewModel.modelInput) }

    // Sauvegarde automatique à chaque modification
    LaunchedEffect(apiKey, model, baseUrl, autoMode) {
        viewModel.saveSettings(apiKey, model, baseUrl, autoMode)
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.setWorkingFolder(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Barre du haut
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onDismiss)
            Spacer(Modifier.width(12.dp))
            Text("Réglages", style = MaterialTheme.typography.headlineSmall, color = c.text)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── CLÉ & MODÈLE ────────────────────────────────────────────────
            SectionLabel("Clé & modèle")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                var showKey by rememberSaveable { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Key, c.cyan)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Clé API Z.ai", style = MaterialTheme.typography.titleMedium, color = c.text)
                                Spacer(Modifier.width(8.dp))
                                StatusBadge(if (apiKey.isBlank()) "Absente" else "Valide", if (apiKey.isBlank()) c.amber else c.green)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                maskedKey(apiKey),
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                                color = c.text2
                            )
                        }
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Afficher / masquer",
                            tint = c.text2,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showKey = !showKey }
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    if (showKey) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("sk-za…", fontFamily = JetBrainsMono, color = c.text3) },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = c.surface2,
                                unfocusedContainerColor = c.surface2,
                                focusedIndicatorColor = c.cyan,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = c.cyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))

                // Modèle
                ExpandableRow(
                    icon = { IconBadge(Icons.Default.AutoAwesome, c.violet) },
                    title = "Modèle",
                    subtitle = "$model · flash",
                    subtitleMono = true
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModelChip("glm-4.7-flash", model == "glm-4.7-flash") { model = "glm-4.7-flash" }
                        ModelChip("glm-4.5-flash", model == "glm-4.5-flash") { model = "glm-4.5-flash" }
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))

                // URL de base
                var editUrl by rememberSaveable { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editUrl = !editUrl }
                    ) {
                        IconBadge(Icons.Default.Globe, c.green)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("URL de base", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                baseUrl,
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                                color = c.text2,
                                maxLines = 1
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null, tint = c.text3,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsRotate(if (editUrl) 90f else 0f)
                        )
                    }
                    if (editUrl) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.text),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = c.surface2,
                                unfocusedContainerColor = c.surface2,
                                focusedIndicatorColor = c.cyan,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = c.cyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── AGENT ───────────────────────────────────────────────────────
            SectionLabel("Agent")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Tune, c.violet)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mode Auto", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                "Écrire les fichiers sans confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text2
                            )
                        }
                        Switch(
                            checked = autoMode,
                            onCheckedChange = { autoMode = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedTrackColor = c.green,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = c.surface3,
                                uncheckedThumbColor = c.bg2,
                                uncheckedBorderColor = c.border2
                            )
                        )
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { folderLauncher.launch(null) }
                    ) {
                        IconBadge(Icons.Default.Folder, c.amber)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Dossier de travail", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                shortUri(viewModel.workingFolderUriString) ?: "Aucun dossier sélectionné",
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                                color = if (viewModel.workingFolderUriString.isNullOrEmpty()) c.amber else c.text2,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.text3, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── MISES À JOUR ────────────────────────────────────────────────
            SectionLabel("Mises à jour")
            Spacer(Modifier.height(8.dp))
            com.zagent.app.update.UpdateSettingsCard()

            Spacer(Modifier.height(20.dp))

            // ── APPARENCE ───────────────────────────────────────────────────
            SectionLabel("Apparence")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.violetBg),
                        contentAlignment = Alignment.Center
                    ) {
                        // Demi-lune : icône thème
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(c.violet)
                        )
                        Box(
                            Modifier
                                .size(18.dp)
                                .padding(end = 9.dp)
                                .clip(CircleShape)
                                .background(c.violetBg)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Thème", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    ThemeSegmented(
                        dark = viewModel.themeIsDark,
                        onDark = { viewModel.setTheme(it) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "ZAgent · fabriqué pour Z.ai",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.text3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Aides
// ─────────────────────────────────────────────────────────────────────────────

private fun Modifier.graphicsRotate(degrees: Float): Modifier =
    this.then(Modifier.rotate(degrees))

@Composable
private fun ExpandableRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    subtitleMono: Boolean = false,
    content: @Composable () -> Unit
) {
    val c = Zcolors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(
                    subtitle,
                    style = TextStyle(
                        fontFamily = if (subtitleMono) JetBrainsMono else Inter,
                        fontSize = 11.5.sp
                    ),
                    color = c.text2,
                    maxLines = 1
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = c.text3,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsRotate(if (open) 90f else 0f)
            )
        }
        if (open) {
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeSegmented(dark: Boolean, onDark: (Boolean) -> Unit) {
    val c = Zcolors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface3)
            .border(1.dp, c.border, RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        SegOption("Sombre", dark, { onDark(true) }, c)
        SegOption("Clair", !dark, { onDark(false) }, c)
    }
}

@Composable
private fun SegOption(label: String, selected: Boolean, onClick: () -> Unit, c: com.zagent.app.ui.theme.ZColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.bg2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = if (selected) c.text else c.text2
        )
    }
}

/** Masque la clé : sk-za••••4f2a */
private fun maskedKey(key: String): String {
    if (key.isBlank()) return "Aucune clé configurée"
    return if (key.length > 9) "${key.take(5)}••••${key.takeLast(4)}" else "••••"
}

/** Affiche la fin de l'URI d'un dossier de façon lisible. */
private fun shortUri(uri: String?): String? {
    if (uri.isNullOrEmpty()) return null
    val decoded = Uri.decode(uri)
    val primary = decoded.substringAfter("primary:", "")
    return if (primary.isNotEmpty()) "/storage/emulated/0/$primary".removeSuffix(":") else decoded.takeLast(40)
}
