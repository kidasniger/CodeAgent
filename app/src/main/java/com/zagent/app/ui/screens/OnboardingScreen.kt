package com.zagent.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.Color
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
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.Zcolors

// ─────────────────────────────────────────────────────────────────────────────
// MA69 — Onboarding repensé en 3 pages :
//   1. Bienvenue + choix du thème (clair / sombre / système) avec aperçu live
//   2. Clé API + modèle
//   3. Dossier de projet + essai de l'effet verre (interrupteur)
// Indicateur de pages minimaliste, bouton « Passer », transitions horizontales.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onFolderPicked: (android.net.Uri) -> Unit,
    onApiKeySaved: (String) -> Unit,
    initialApiKey: String,
    onFinished: () -> Unit,
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeSelected: (ThemeMode) -> Unit = {},
    onGlassTrial: (Boolean) -> Unit = {}
) {
    val c = Zcolors
    var page by remember { mutableStateOf(0) }
    var apiKeyDraft by remember { mutableStateOf(initialApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var themeChoice by remember { mutableStateOf(initialThemeMode) }
    var glassTrial by remember { mutableStateOf(false) }

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
        // Barre du haut : logo + Passer
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

        // Pages avec transition horizontale
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } togetherWith slideOutHorizontally { -it })
                } else {
                    (slideInHorizontally { -it } togetherWith slideOutHorizontally { it })
                }
            },
            label = "onb"
        ) { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                when (p) {
                    0 -> {
                        Text(
                            "Bienvenue sur\nZAgent",
                            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp),
                            color = c.text,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Un agent de code qui lit, crée et modifie les fichiers de ton projet, directement depuis ton téléphone. Décris une tâche, il s'occupe du reste.",
                            style = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 22.sp),
                            color = c.text2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            "Choisis ton thème",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = c.text3
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThemeOption("Système", themeChoice == ThemeMode.SYSTEM) {
                                themeChoice = ThemeMode.SYSTEM; onThemeSelected(ThemeMode.SYSTEM)
                            }
                            ThemeOption("Clair", themeChoice == ThemeMode.LIGHT) {
                                themeChoice = ThemeMode.LIGHT; onThemeSelected(ThemeMode.LIGHT)
                            }
                            ThemeOption("Sombre", themeChoice == ThemeMode.DARK) {
                                themeChoice = ThemeMode.DARK; onThemeSelected(ThemeMode.DARK)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        ThemePreview()
                    }

                    1 -> {
                        Text(
                            "Branche ta clé\nAPI Z.ai",
                            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp),
                            color = c.text,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Colle ta clé Z.ai et choisis ton modèle : tu pourras tout changer plus tard dans Réglages.",
                            style = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 22.sp),
                            color = c.text2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(22.dp))
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, null, tint = c.cyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Clé API",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = c.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Afficher / masquer",
                                    tint = c.text2,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { showKey = !showKey }
                                        .padding(6.dp)
                                        .size(18.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            TextField(
                                value = apiKeyDraft,
                                onValueChange = { apiKeyDraft = it },
                                placeholder = { Text("sk-za…", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp), color = c.text3) },
                                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.5.sp, color = c.text),
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = c.cyan,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = c.cyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Modèle de départ",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = c.text3
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModelChip("glm-4.5-flash", currentModel == "glm-4.5-flash") {
                                onModelSelected("glm-4.5-flash")
                            }
                            ModelChip("glm-4.7-flash", currentModel == "glm-4.7-flash") {
                                onModelSelected("glm-4.7-flash")
                            }
                        }
                    }

                    else -> {
                        Text(
                            "Un dossier\npar projet",
                            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp),
                            color = c.text,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "L'agent n'accède qu'au projet actif. Importe un dossier de ton téléphone pour commencer, ou crée un projet plus tard depuis l'écran Projets.",
                            style = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 22.sp),
                            color = c.text2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.surface2)
                                .border(1.dp, c.border2, RoundedCornerShape(16.dp))
                                .clickable { folderLauncher.launch(null) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, null, tint = c.cyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Choisir un dossier du téléphone",
                                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = c.text
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.surface)
                                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                                .clickable {
                                    glassTrial = !glassTrial
                                    onGlassTrial(glassTrial)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Palette, null, tint = c.violet, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Essayer l'effet verre",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                    color = c.text
                                )
                                Text(
                                    "Verre dépoli flottant sur les barres et feuilles. Réglable dans Apparence.",
                                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                                    color = c.text2
                                )
                            }
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (glassTrial) c.cyan else c.surface3)
                                    .border(1.dp, if (glassTrial) c.cyan else c.border2, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (glassTrial) {
                                    Icon(Icons.Default.Check, null, tint = c.onGrad, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }

                // Indicateur de pages minimaliste
                Spacer(Modifier.height(26.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        val active = i == page
                        Box(
                            Modifier
                                .size(width = if (active) 26.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .then(if (active) Modifier.background(c.gradBtn) else Modifier.background(c.surface3))
                        )
                    }
                }

                // Boutons principaux
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = if (page < 2) "Continuer" else "Commencer",
                    onClick = {
                        if (page < 2) {
                            page++
                        } else {
                            if (apiKeyDraft.isNotBlank()) onApiKeySaved(apiKeyDraft)
                            finish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    leading = {
                        Icon(
                            if (page < 2) Icons.Default.ArrowForward else Icons.Default.Check,
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

/** Option de thème : chip égale avec l'état sélectionné marqué. */
@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.cyanBg else c.surface2)
            .border(1.dp, if (selected) c.cyan else c.border2, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Default.Check, null, tint = c.cyan, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            style = TextStyle(
                fontFamily = Inter,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = if (selected) c.cyan else c.text2
        )
    }
}

/** Aperçu live : une mini-carte de chat dessinée avec la palette active (MA69). */
@Composable
private fun ThemePreview() {
    val c = Zcolors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.bg2)
            .border(1.dp, c.border, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(c.gradBtn)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.surface3)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(14.dp))
                .background(c.cyanBg)
                .padding(12.dp)
        ) {
            Text(
                "Je modifie le fichier demandé…",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                color = c.text
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.gradBtn)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "Envoyer",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.onGrad
                )
            }
        }
    }
}
