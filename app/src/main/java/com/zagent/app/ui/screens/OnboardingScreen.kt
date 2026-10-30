package com.zagent.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.ModelChip
import com.zagent.app.ui.components.ZLogo
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors

/**
 * Onboarding 4 écrans : agent / dossier sécurisé / validation des écritures / clé API.
 */
@Composable
fun OnboardingScreen(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onFolderPicked: (android.net.Uri) -> Unit,
    onApiKeySaved: (String) -> Unit,
    initialApiKey: String,
    onFinished: () -> Unit
) {
    val c = Zcolors
    var page by remember { mutableStateOf(0) }
    var apiKeyDraft by remember { mutableStateOf(initialApiKey) }
    var showKey by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onFolderPicked(uri)
    }

    fun finish() = onFinished()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Barre du haut : mini logo + Passer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZLogo(modifier = Modifier.size(26.dp))
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { finish() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "Passer",
                    color = c.text2,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                )
            }
        }

        Crossfade(targetState = page, label = "onb") { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.6f))
                when (p) {
                    0 -> IlluAgent()
                    1 -> IlluFolder()
                    2 -> IlluValidation()
                    else -> IlluKey(
                        apiKey = apiKeyDraft,
                        onApiKeyChange = { apiKeyDraft = it },
                        showKey = showKey,
                        onToggleShow = { showKey = !showKey },
                        currentModel = currentModel,
                        onModelSelected = onModelSelected
                    )
                }
                Spacer(Modifier.weight(0.6f))

                // Titre + corps
                val (title, body) = when (p) {
                    0 -> "Un agent qui code\npour toi" to
                            "ZAgent lit, crée et modifie les fichiers de ton projet directement depuis ton téléphone. Décris une tâche, il s'occupe du reste."
                    1 -> "Choisis ton dossier\nde projet" to
                            "L'agent n'accède qu'au dossier que tu sélectionnes. Aucun autre fichier de ton téléphone n'est lu ni modifié."
                    2 -> "Tu valides chaque\nécriture" to
                            "Chaque création ou modification de fichier passe par une confirmation avec aperçu du diff. Le mode Auto t'évite les allers-retours."
                    else -> "Branche ta clé\nAPI Z.ai" to
                            "Colle ta clé Z.ai, choisis ton modèle et lance ton premier agent en quelques secondes."
                }
                Text(
                    title,
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp),
                    color = c.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    body,
                    style = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 22.sp),
                    color = c.text2,
                    textAlign = TextAlign.Center
                )

                // Progression
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i ->
                        val active = i == page
                        Box(
                            Modifier
                                .size(width = if (active) 26.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .then(if (active) Modifier.background(c.gradBtn) else Modifier.background(c.surface3))
                        )
                    }
                }

                // Bouton principal
                Spacer(Modifier.height(18.dp))
                GradientButton(
                    text = if (p < 3) "Continuer" else "Commencer",
                    onClick = {
                        if (p < 3) page++
                        else {
                            if (apiKeyDraft.isNotBlank()) onApiKeySaved(apiKeyDraft)
                            finish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    leading = {
                        Icon(
                            if (p < 3) Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            tint = c.onGrad,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration 1 : téléphone éditeur + puces d'outils
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IlluAgent() {
    val c = Zcolors
    Box(Modifier.height(330.dp), contentAlignment = Alignment.Center) {
        // Halo dégradé
        Box(
            Modifier
                .size(250.dp)
                .background(
                    Brush.radialGradient(listOf(c.violetBg, Color.Transparent)),
                    CircleShape
                )
        )
        // Carte téléphone
        GlassCard(
            modifier = Modifier
                .width(190.dp)
                .align(Alignment.Center),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) {
                    Box(Modifier.size(6.dp).background(c.surface3, CircleShape))
                }
            }
            Spacer(Modifier.height(14.dp))
            CodeBar(52.dp, c.violet)
            CodeBar(74.dp, c.cyan)
            CodeBar(40.dp, c.surface3)
            CodeBar(64.dp, c.green)
            CodeBar(30.dp, c.surface3)
            CodeBar(56.dp, c.violet)
            CodeBar(44.dp, c.surface3)
        }
        // Puce outil en haut
        OnbToolChip(
            name = "read_file",
            arg = "MainActivity.kt",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
        // Pastille génération en bas
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.bg2)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.violetBg),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = c.violet, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text("Génération du code…", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp), color = c.text)
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = c.cyan)
        }
    }
}

@Composable
private fun CodeBar(width: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        Modifier
            .padding(vertical = 5.dp)
            .size(width = width, height = 9.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun OnbToolChip(name: String, arg: String, modifier: Modifier = Modifier) {
    val c = Zcolors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.bg2)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.violetBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Build, null, tint = c.violet, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(name, style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = c.cyan)
        Spacer(Modifier.width(6.dp))
        Text("·", color = c.text3, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(arg, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.text2)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Check, null, tint = c.green, modifier = Modifier.size(14.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration 2 : dossier sécurisé
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IlluFolder() {
    val c = Zcolors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Fil d'ariane mono
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DevProjects/", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp), color = c.text2)
            Spacer(Modifier.width(6.dp))
            Text("zagent-demo", style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = c.cyan)
        }
        Spacer(Modifier.height(18.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(210.dp)
                    .background(Brush.radialGradient(listOf(c.cyanBg, Color.Transparent)), CircleShape)
            )
            GlassCard(
                modifier = Modifier.width(240.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.amberBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, null, tint = c.amber, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("zagent-demo", style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), color = c.text)
                        Spacer(Modifier.height(3.dp))
                        Text("14 fichiers", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.text2)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Badge accès restreint
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.greenBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, null, tint = c.green, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Accès restreint au dossier choisi", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = c.green)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration 3 : confirmation d'écriture avec mini-diff
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IlluValidation() {
    val c = Zcolors
    GlassCard(
        modifier = Modifier.width(290.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Text("Autoriser l'écriture ?", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), color = c.text)
        Spacer(Modifier.height(10.dp))

        // Mini-diff
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.codeBg)
                .padding(vertical = 6.dp)
        ) {
            DiffRow("12", "dependencies {", null, Color(0xFF8B98AD))
            DiffRow("13", "  implementation(\"coil\")", "+", Color(0xFF7EE8A2))
            DiffRow("14", "}", null, Color(0xFF8B98AD))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(c.surface2)
                    .border(1.dp, c.border2, RoundedCornerShape(50))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Refuser", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = c.text2)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(c.gradBtn)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Autoriser", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = c.onGrad)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Mode Auto : écritures sans confirmation",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
            color = c.text3
        )
    }
}

@Composable
private fun DiffRow(no: String, text: String, sign: String?, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (sign != null && sign == "+") Color(0x1434D399) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(no, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp), color = Color(0xFF43506B))
        Spacer(Modifier.width(8.dp))
        if (sign != null) {
            Text(sign, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = color)
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Illustration 4 : clé API masquée + choix du modèle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IlluKey(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showKey: Boolean,
    onToggleShow: () -> Unit,
    currentModel: String,
    onModelSelected: (String) -> Unit
) {
    val c = Zcolors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(
            modifier = Modifier.width(300.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
        ) {
            TextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                placeholder = {
                    Text(
                        "sk-za•••• •••• 4f2a",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp),
                        color = c.text3
                    )
                },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = c.text),
                trailingIcon = {
                    Icon(
                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Afficher / masquer",
                        tint = c.text2,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleShow)
                            .padding(6.dp)
                            .size(20.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModelChip(
                "glm-4.7-flash",
                currentModel == "glm-4.7-flash",
                onClick = { onModelSelected("glm-4.7-flash") },
                modifier = Modifier.weight(1f)
            )
            ModelChip(
                "glm-4.5-flash",
                currentModel == "glm-4.5-flash",
                onClick = { onModelSelected("glm-4.5-flash") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.greenBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, null, tint = c.green, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Clé stockée localement, chiffrée", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = c.green)
        }
    }
}
