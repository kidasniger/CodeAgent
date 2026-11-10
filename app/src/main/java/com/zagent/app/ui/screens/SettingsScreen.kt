package com.zagent.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
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
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.screens.settings.AppearanceSettingsScreen
import com.zagent.app.ui.screens.ModelsScreen
import com.zagent.app.ui.screens.UsageScreen
import com.zagent.app.ui.screens.InstructionsScreen
import com.zagent.app.ui.screens.PermissionsScreen
import com.zagent.app.ui.screens.ActivityScreen
import com.zagent.app.ui.screens.AboutScreen
import com.zagent.app.ui.screens.HelpScreen
import com.zagent.app.ui.components.Lot5Row
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Réglages : clé & modèle · agent · mises à jour · apparence
// ─────────────────────────────────────────────────────────────────────────────

/**
 * @param openAppearance MA71 : ouvre directement la sous-vue Apparence
 * (utilisé par la route settings/appearance et le deep link zagent://settings/appearance).
 */
@Composable
fun SettingsScreen(viewModel: MainViewModel, onDismiss: () -> Unit, openAppearance: Boolean = false) {
    val c = Zcolors
    var showAppearanceSettings by rememberSaveable { mutableStateOf(openAppearance) }

    // Le retour système dépile d'abord Apparence, sans changer l'onglet principal.
    BackHandler(enabled = showAppearanceSettings) {
        showAppearanceSettings = false
    }

    if (showAppearanceSettings) {
        AppearanceSettingsScreen(
            viewModel = viewModel,
            onBack = { showAppearanceSettings = false }
        )
        return
    }

    // ── Sous-écrans du LOT 5 (MA56 → MA70) ───────────────────────────────
    // MA39 : un raccourci « Modèles » peut pré-demander l'ouverture de cet écran.
    var showModels by rememberSaveable { mutableStateOf(viewModel.pendingOpenModels) }
    LaunchedEffect(Unit) { viewModel.pendingOpenModels = false }
    var showUsage by rememberSaveable { mutableStateOf(false) }
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    var showPermissions by rememberSaveable { mutableStateOf(false) }
    var showActivity by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val anyLot5 = showModels || showUsage || showInstructions || showPermissions ||
        showActivity || showAbout || showHelp
    fun closeLot5() {
        showModels = false
        showUsage = false
        showInstructions = false
        showPermissions = false
        showActivity = false
        showAbout = false
        showHelp = false
    }
    BackHandler(enabled = anyLot5) { closeLot5() }
    if (showModels) { ModelsScreen(viewModel) { closeLot5() }; return }
    if (showUsage) { UsageScreen(viewModel) { closeLot5() }; return }
    if (showInstructions) { InstructionsScreen(viewModel) { closeLot5() }; return }
    if (showPermissions) { PermissionsScreen(viewModel) { closeLot5() }; return }
    if (showActivity) { ActivityScreen(viewModel) { closeLot5() }; return }
    if (showAbout) { AboutScreen(viewModel) { closeLot5() }; return }
    if (showHelp) { HelpScreen(viewModel) { closeLot5() }; return }

    var autoMode by rememberSaveable { mutableStateOf(viewModel.autoModeInput) }
    var model by rememberSaveable { mutableStateOf(viewModel.modelInput) }

    // MàJ 101 — les clés vivent dans les cartes Fournisseurs (une par API,
    // enregistrées dès la frappe avec un court délai). Ne restent ici que le
    // modèle global et le mode Auto : EncryptedSharedPreferences chiffre à
    // chaque écriture, un enregistrement par frappe provoquait des à-coups.
    LaunchedEffect(model, autoMode) {
        kotlinx.coroutines.delay(400)
        viewModel.saveSettings(viewModel.apiKeyInput, model, viewModel.baseUrlInput, autoMode)
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
            // ── Espace agent (LOT 5) ──────────────────────────────────────
            SectionLabel("Espace agent")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Lot5Row(
                        Icons.Default.Tune, "Modèles",
                        "Catalogue, vitesse, modèle personnalisé",
                        tint = c.violet,
                        onClick = { showModels = true }
                    )
                    Lot5Row(
                        Icons.Default.SsidChart, "Usage & coûts",
                        "Tokens sur 7 / 30 jours, coût, export CSV",
                        tint = c.cyan,
                        onClick = { showUsage = true }
                    )
                    Lot5Row(
                        Icons.Default.Edit, "Instructions personnalisées",
                        "Style de l'agent, global et par projet",
                        tint = c.amber,
                        onClick = { showInstructions = true }
                    )
                    Lot5Row(
                        Icons.Default.Security, "Permissions & sécurité",
                        "Autorisations par action et historique",
                        tint = c.red,
                        onClick = { showPermissions = true }
                    )
                    Lot5Row(
                        Icons.Default.History, "Journal d'activité",
                        "Tout ce que l'agent a fait, annulable",
                        tint = c.green,
                        onClick = { showActivity = true }
                    )
                    Lot5Row(
                        Icons.Default.HelpOutline, "Raccourcis & gestes",
                        "Gestes, commandes slash, clavier",
                        tint = c.cyan,
                        onClick = { showHelp = true }
                    )
                    Lot5Row(
                        Icons.Default.Info, "À propos & mises à jour",
                        "Version, nouveautés, licences",
                        tint = c.violet,
                        onClick = { showAbout = true }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            // ── FOURNISSEURS D'IA (MàJ 101) : une carte par API, une clé par API ──
            SectionLabel("Fournisseurs d'IA")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                viewModel.fournisseurs.forEachIndexed { index, f ->
                    if (index > 0) CardDivider(Modifier.padding(horizontal = 16.dp))
                    CarteFournisseur(
                        f = f,
                        resultatTest = viewModel.resultatsTestConnexion[f.id],
                        testEnCours = f.id in viewModel.testsConnexionEnCours,
                        onCle = { viewModel.majFournisseur(f.id, cle = it) },
                        onUrl = { viewModel.majFournisseur(f.id, url = it) },
                        onTest = { viewModel.testerFournisseur(f.id) },
                        onSupprimer = { viewModel.supprimerFournisseur(f.id) }
                    )
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))
                AjoutFournisseurSection(
                    onAjouter = { nom, url, cle -> viewModel.ajouterFournisseur(nom, url, cle) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── MODÈLE PAR DÉFAUT ───────────────────────────────────────────
            SectionLabel("Modèle par défaut")
            Spacer(Modifier.height(8.dp))
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                ExpandableRow(
                    icon = { IconBadge(Icons.Default.AutoAwesome, c.violet) },
                    title = "Modèle",
                    subtitle = model,
                    subtitleMono = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelChip(
                            "glm-4.7-flash",
                            model == "glm-4.7-flash",
                            onClick = { model = "glm-4.7-flash" },
                            modifier = Modifier.weight(1f)
                        )
                        ModelChip(
                            "glm-4.5-flash",
                            model == "glm-4.5-flash",
                            onClick = { model = "glm-4.5-flash" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "NVIDIA NIM et autres fournisseurs : Réglages › Modèles. La clé de chaque API part avec ses modèles (MàJ 101).",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                        color = c.text2
                    )
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
                    var smart by remember { mutableStateOf(viewModel.smartModeInput) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Tune, c.cyan)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mode intelligent", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                "Nouveaux fichiers et petites modifications sans confirmation ; écrasement et suppression confirmés",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text2
                            )
                        }
                        Switch(
                            checked = smart,
                            onCheckedChange = { smart = it; viewModel.setSmartMode(it) },
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
                            .clickable { viewModel.isProjectPickerOpen = true }
                    ) {
                        IconBadge(Icons.Default.Folder, c.amber)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Projet actif", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                shortUri(viewModel.workingFolderUriString) ?: "Aucun projet sélectionné",
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                                color = if (viewModel.workingFolderUriString.isNullOrEmpty()) c.amber else c.text2,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.text3, modifier = Modifier.size(20.dp))
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    var editInstr by remember { mutableStateOf(false) }
                    var instr by remember { mutableStateOf(viewModel.customInstructionsInput) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editInstr = !editInstr }
                    ) {
                        IconBadge(Icons.Default.AutoAwesome, c.violet)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Instructions personnalisées", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                if (instr.isBlank()) "Règles ajoutées à chaque demande (en plus de AGENT.md du projet)"
                                else instr.trim().lines().first().take(60),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text2,
                                maxLines = 1
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null, tint = c.text3,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsRotate(if (editInstr) 90f else 0f)
                        )
                    }
                    if (editInstr) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = instr,
                            onValueChange = {
                                instr = it
                                viewModel.setCustomInstructions(it)
                            },
                            placeholder = {
                                Text(
                                    "Ex : réponds en français, utilise TypeScript, pas de commentaires inutiles…",
                                    color = c.text3,
                                    style = TextStyle(fontSize = 12.sp)
                                )
                            },
                            textStyle = TextStyle(fontSize = 13.sp, color = c.text),
                            minLines = 3,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = c.surface2,
                                unfocusedContainerColor = c.surface2,
                                focusedBorderColor = c.cyan,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = c.cyan
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    var steps by remember { mutableStateOf(viewModel.maxStepsInput) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Tune, c.cyan)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Limite d'étapes", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                "Étapes maximum par demande, puis bouton Continuer",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text2
                            )
                        }
                        Text(
                            "−",
                            color = c.text,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(c.surface2)
                                .clickable {
                                    steps = (steps - 5).coerceAtLeast(5)
                                    viewModel.setMaxSteps(steps)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        Text(
                            steps.toString(),
                            color = c.text,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Text(
                            "+",
                            color = c.text,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(c.surface2)
                                .clickable {
                                    steps = (steps + 5).coerceAtMost(100)
                                    viewModel.setMaxSteps(steps)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
                CardDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    var editPrices by remember { mutableStateOf(false) }
                    var priceIn by remember {
                        mutableStateOf(if (viewModel.priceInInput > 0f) viewModel.priceInInput.toString() else "")
                    }
                    var priceOut by remember {
                        mutableStateOf(if (viewModel.priceOutInput > 0f) viewModel.priceOutInput.toString() else "")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editPrices = !editPrices }
                    ) {
                        IconBadge(Icons.Default.Tune, c.green)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Prix des tokens", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                if (viewModel.priceInInput > 0f || viewModel.priceOutInput > 0f)
                                    "Entrée " + viewModel.priceInInput + " / sortie " + viewModel.priceOutInput + " par million (USD)"
                                else "Renseigne les prix pour afficher le coût de la conversation",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text2,
                                maxLines = 1
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null, tint = c.text3,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsRotate(if (editPrices) 90f else 0f)
                        )
                    }
                    if (editPrices) {
                        Spacer(Modifier.height(10.dp))
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = c.surface2,
                            unfocusedContainerColor = c.surface2,
                            focusedBorderColor = c.cyan,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = c.cyan
                        )
                        OutlinedTextField(
                            value = priceIn,
                            onValueChange = {
                                priceIn = it
                                viewModel.setPrices(
                                    it.replace(',', '.').toFloatOrNull() ?: 0f,
                                    priceOut.replace(',', '.').toFloatOrNull() ?: 0f
                                )
                            },
                            label = { Text("Entrée, USD par million de tokens", color = c.text3, style = TextStyle(fontSize = 12.sp)) },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = fieldColors,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = priceOut,
                            onValueChange = {
                                priceOut = it
                                viewModel.setPrices(
                                    priceIn.replace(',', '.').toFloatOrNull() ?: 0f,
                                    it.replace(',', '.').toFloatOrNull() ?: 0f
                                )
                            },
                            label = { Text("Sortie, USD par million de tokens", color = c.text3, style = TextStyle(fontSize = 12.sp)) },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = fieldColors,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )
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
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppearanceSettings = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Icons.Default.Tune, c.violet)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Apparence", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(
                            "Thème, verre, police et densité",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.text2
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Ouvrir l’apparence",
                        tint = c.text3,
                        modifier = Modifier.size(22.dp)
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

/** Contrôle segmenté à 3 options : Système / Clair / Sombre. */
@Composable
private fun ThemeSegmented(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val c = Zcolors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface3)
            .border(1.dp, c.border, RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        SegOption("Système", mode == ThemeMode.SYSTEM, { onSelect(ThemeMode.SYSTEM) }, c)
        SegOption("Clair", mode == ThemeMode.LIGHT, { onSelect(ThemeMode.LIGHT) }, c)
        SegOption("Sombre", mode == ThemeMode.DARK, { onSelect(ThemeMode.DARK) }, c)
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

/** MàJ 101 — carte d'un fournisseur : nom, badge de clé, champs (clé, URL), test de connexion. */
@Composable
private fun CarteFournisseur(
    f: com.zagent.app.data.Fournisseur,
    resultatTest: String?,
    testEnCours: Boolean,
    onCle: (String) -> Unit,
    onUrl: (String) -> Unit,
    onTest: () -> Unit,
    onSupprimer: () -> Unit
) {
    val c = Zcolors
    var ouvert by rememberSaveable { mutableStateOf(false) }
    var cleVisible by rememberSaveable { mutableStateOf(false) }
    var cleLocale by rememberSaveable(f.id) { mutableStateOf(f.cle) }
    var urlLocale by rememberSaveable(f.id) { mutableStateOf(f.baseUrl) }

    // Enregistrement différé (400 ms) : EncryptedSharedPreferences chiffre à
    // chaque écriture, un enregistrement par frappe provoquait des à-coups.
    LaunchedEffect(cleLocale) {
        kotlinx.coroutines.delay(400)
        if (cleLocale != f.cle) onCle(cleLocale)
    }
    LaunchedEffect(urlLocale) {
        kotlinx.coroutines.delay(400)
        if (urlLocale != f.baseUrl) onUrl(urlLocale)
    }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { ouvert = !ouvert }
        ) {
            IconBadge(
                Icons.Default.Key,
                if (f.id == com.zagent.app.data.Fournisseurs.ID_ZAI) c.cyan else c.green
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.nom, style = MaterialTheme.typography.titleMedium, color = c.text)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(
                        if (f.cle.isBlank()) "Clé absente" else "Clé enregistrée",
                        if (f.cle.isBlank()) c.amber else c.green
                    )
                    if (!f.actif) {
                        Spacer(Modifier.width(6.dp))
                        StatusBadge("Inactif", c.text2)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    maskedKey(f.cle),
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                    color = c.text2
                )
            }
            Icon(
                if (ouvert) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Ouvrir / fermer",
                tint = c.text2,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { ouvert = !ouvert }
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
        if (ouvert) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = cleLocale,
                onValueChange = { cleLocale = it },
                placeholder = {
                    Text(
                        if (f.id == com.zagent.app.data.Fournisseurs.ID_NVIDIA) "nvapi-…" else "sk-…",
                        fontFamily = JetBrainsMono, color = c.text3
                    )
                },
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
                singleLine = true,
                visualTransformation = if (cleVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        if (cleVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Afficher / masquer la clé",
                        tint = c.text2,
                        modifier = Modifier.clickable { cleVisible = !cleVisible }
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface2,
                    unfocusedContainerColor = c.surface2,
                    focusedBorderColor = c.cyan,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = c.cyan
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (f.personnalise) {
                OutlinedTextField(
                    value = urlLocale,
                    onValueChange = { urlLocale = it },
                    placeholder = {
                        Text("https://…/v1/chat/completions", fontFamily = JetBrainsMono, color = c.text3)
                    },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.text),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = c.surface2,
                        unfocusedContainerColor = c.surface2,
                        focusedBorderColor = c.cyan,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = c.cyan
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .border(1.dp, c.border2, RoundedCornerShape(50))
                        .clickable(enabled = !testEnCours) { onTest() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (testEnCours) "Test…" else "Tester la connexion",
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                        color = c.cyan
                    )
                }
                if (f.personnalise) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onSupprimer)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Supprimer",
                            style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                            color = c.red
                        )
                    }
                }
                resultatTest?.let { res ->
                    Text(
                        res,
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 15.sp),
                        color = if (res.startsWith("Connecté")) c.green else c.amber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** MàJ 101 — ajout d'un fournisseur : URL + clé, avec détection automatique (z.ai / NVIDIA). */
@Composable
private fun AjoutFournisseurSection(onAjouter: (String, String, String) -> Unit) {
    val c = Zcolors
    var ouvert by rememberSaveable { mutableStateOf(false) }
    var nom by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var cle by rememberSaveable { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { ouvert = !ouvert }
        ) {
            IconBadge(Icons.Default.Add, c.violet)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Ajouter un fournisseur", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(
                    "OpenRouter, Groq, Ollama local, LM Studio… tout endpoint au format OpenAI",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = c.text2
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null, tint = c.text3,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsRotate(if (ouvert) 90f else 0f)
            )
        }
        if (ouvert) {
            Spacer(Modifier.height(10.dp))
            ChampSimple(nom, { nom = it }, "Nom (optionnel — sinon détecté)", mono = false)
            Spacer(Modifier.height(8.dp))
            ChampSimple(url, { url = it }, "URL de l'API (…/v1/chat/completions)", mono = true)
            Spacer(Modifier.height(8.dp))
            ChampSimple(cle, { cle = it }, "Clé API", mono = true)
            if (cle.isNotBlank() || url.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                val libelle = when (com.zagent.app.data.Fournisseurs.detecter(cle.ifBlank { url })) {
                    com.zagent.app.data.Fournisseurs.ID_NVIDIA -> "Détecté : NVIDIA NIM (clé nvapi-…)"
                    com.zagent.app.data.Fournisseurs.ID_ZAI -> "Détecté : z.ai (GLM)"
                    else -> "Passerelle personnalisée (format OpenAI)"
                }
                Text(
                    libelle,
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.cyan
                )
            }
            Spacer(Modifier.height(10.dp))
            val valide = url.isNotBlank() && cle.isNotBlank()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (valide) c.cyanBg else c.surface2)
                    .border(1.dp, if (valide) c.cyan else c.border2, RoundedCornerShape(50))
                    .clickable(enabled = valide) {
                        onAjouter(nom, url, cle)
                        nom = ""; url = ""; cle = ""
                        ouvert = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Enregistrer ce fournisseur",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                    color = if (valide) c.cyan else c.text3
                )
            }
        }
    }
}

/** Champ de saisie une ligne, style Réglages. */
@Composable
private fun ChampSimple(valeur: String, onChange: (String) -> Unit, placeholder: String, mono: Boolean) {
    val c = Zcolors
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        placeholder = {
            Text(
                placeholder,
                style = TextStyle(fontFamily = if (mono) JetBrainsMono else Inter, fontSize = 12.sp),
                color = c.text3
            )
        },
        textStyle = TextStyle(fontFamily = if (mono) JetBrainsMono else Inter, fontSize = 13.sp, color = c.text),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surface2,
            unfocusedContainerColor = c.surface2,
            focusedBorderColor = c.cyan,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = c.cyan
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Masque la clé : sk-za••••4f2a */
private fun maskedKey(key: String): String {
    if (key.isBlank()) return "Aucune clé configurée"
    return if (key.length > 9) "${key.take(5)}••••${key.takeLast(4)}" else "••••"
}

/** Affiche la fin de l'URI d'un dossier de façon lisible. */
private fun shortUri(uri: String?): String? {
    if (uri.isNullOrEmpty()) return null
    val rel = uri.substringAfter("/files/home", "")
    return if (rel.isNotEmpty()) "~$rel" else uri.takeLast(40)
}
