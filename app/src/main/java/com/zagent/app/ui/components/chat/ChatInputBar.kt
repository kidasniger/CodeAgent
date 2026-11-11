package com.zagent.app.ui.components.chat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.data.ProjectSearch
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Barre de saisie flottante — LOT 4 (MA48 à MA55)
// MA48 champ auto-extensible · MA49 feuille « + » · MA50 envoi ⇄ stop animé
// MA51 chip modèle · MA52 commandes slash · MA53 mentions @fichier
// MA54 dictée vocale · MA55 brouillon + estimation de tokens
// ─────────────────────────────────────────────────────────────────────────────

/** Suggestion de commande slash (icône gérée côté UI, logique dans MainViewModel). */
private data class SlashUi(val command: String, val icon: ImageVector, val label: String)

private val slashUiCommands = listOf(
    SlashUi("/nouveau", Icons.Default.Add, "Démarrer une nouvelle conversation"),
    SlashUi("/modèle", Icons.Default.Tune, "Choisir le modèle de la conversation"),
    SlashUi("/model", Icons.Default.Tune, "Choisir le modèle (alias de /modèle)"), // feat-011
    SlashUi("/diff", Icons.Default.CompareArrows, "Voir les changements Git du projet"),
    SlashUi("/tester", Icons.Default.Science, "Lancer les tests du projet"),
    SlashUi("/compact", Icons.Default.Compress, "Résumer l'historique ancien pour alléger le contexte"), // feat-009
    SlashUi("/undo", Icons.Default.Undo, "Annuler le dernier échange"), // feat-011
    SlashUi("/plan", Icons.Default.Checklist, "Mode Plan : validation d'un plan avant écriture"), // feat-013
    SlashUi("/effacer", Icons.Default.DeleteOutline, "Effacer les messages du fil"),
    SlashUi("/clear", Icons.Default.DeleteOutline, "Effacer les messages (alias de /effacer)"), // feat-011
    SlashUi("/aide", Icons.Default.HelpOutline, "Voir les commandes disponibles")
)

/**
 * Surligne les mentions « @chemin » qui correspondent à de vrais fichiers du projet :
 * fond teinté + couleur d'accent, l'apparence d'un chip dans le texte (MA53).
 */
class MentionHighlight(
    private val files: Set<String>,
    private val chipBg: Color,
    private val chipFg: Color
) : VisualTransformation {
    private val regex = Regex("@[A-Za-z0-9._/-]+")

    override fun filter(text: AnnotatedString): TransformedText {
        if (!text.text.contains('@')) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text.text)
        regex.findAll(text.text).forEach { m ->
            if (files.contains(m.value.substring(1))) {
                builder.addStyle(
                    SpanStyle(color = chipFg, background = chipBg, fontWeight = FontWeight.Medium),
                    m.range.first,
                    m.range.last + 1
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/** Barre de saisie flottante du chat, version LOT 4. */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    onStop: () -> Unit,
    viewModel: MainViewModel
) {
    val c = Zcolors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var attachSheetOpen by remember { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    // Dernières valeurs pour les rappels asynchrones (résultats de la dictée)
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // ── MA49 : sélecteurs de fichiers ────────────────────────────────────────
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachmentUri(it) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachmentUri(it) }
    }

    // ── MA54 : dictée vocale (SpeechRecognizer) ────────────────────────────
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    // feat-022 — mode continu (appui long sur le micro) : la dictée se relance
    // toute seule jusqu'à un nouvel appui long ou l'envoi.
    var dicteeContinue by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
    }

    fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            com.zagent.app.ui.components.common.ZEphemeral.show("La dictée vocale n'est pas disponible sur cet appareil.")
            return
        }
        // feat-022 — la base du champ est capturée au départ : les résultats
        // partiels réécrivent la fin du champ sans dupliquer la transcription.
        val baseDictee = currentValue
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "Rien à transcrire : réessaie."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "Autorise le micro pour dicter."
                    else -> "Dictée interrompue, réessaie."
                }
                com.zagent.app.ui.components.common.ZEphemeral.show(msg)
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (text.isNotEmpty()) {
                    val nouveau = if (baseDictee.isBlank()) text else baseDictee + " " + text
                    currentOnValueChange(nouveau)
                    // feat-022 — mode continu : relance automatique de l'écoute.
                    if (dicteeContinue) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (dicteeContinue) startRecognition()
                        }, 350)
                    }
                } else if (dicteeContinue) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (dicteeContinue) startRecognition()
                    }, 350)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // feat-022 — transcription en direct pendant la dictée.
                val partiel = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (partiel.isNotEmpty()) {
                    currentOnValueChange(if (baseDictee.isBlank()) partiel else baseDictee + " " + partiel)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // feat-022 — la langue suit l'interface (fr-FR par défaut, en-US si UI en anglais).
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (viewModel.langueUi == "en") "en-US" else "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // feat-022 — ponctuation automatique quand le moteur la gère (API 33+).
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, "android.speech.extras.EXTRA_ENABLE_FORMATTING_AUTOMATIC_PUNCTUATION")
            }
        }
        recognizer = sr
        sr.startListening(intent)
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition()
        else com.zagent.app.ui.components.common.ZEphemeral.show("Autorise le micro dans les réglages Android pour dicter.")
    }

    // ── MA52 / MA53 : détection des commandes slash et des mentions @ ────────
    val slashQuery: String? = if (
        value.startsWith("/") && value.length <= 40 &&
        !value.substringAfter('/').contains(' ') && !value.contains('\n')
    ) value else null

    val atIndex = value.lastIndexOf('@')
    val mention: Pair<Int, String>? = if (atIndex >= 0) {
        val q = value.substring(atIndex + 1)
        if (!q.contains(' ') && !q.contains('\n') && q.length <= 120) atIndex to q else null
    } else null

    val mentionVisual = remember(viewModel.projectFiles, c.cyan, c.cyanBg) {
        MentionHighlight(viewModel.projectFiles.toSet(), c.cyanBg, c.cyan)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp)
    ) {
        // ── feat-010 : compteur de tokens du contexte + saisie, au-dessus du champ ──
        val ctxTokens = viewModel.contextTokenEstimate
        if (ctxTokens > 0 || value.length > 500) {
            Text(
                if (value.length > 500)
                    "≈ " + ctxTokens + " tokens (contexte) · ≈ " + (value.length / 4) + " (saisie)"
                else
                    "≈ " + ctxTokens + " tokens (contexte)",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                color = c.text3,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 4.dp)
            )
        }

        // ── MA52 : liste des commandes slash au-dessus du champ ─────────────
        if (slashQuery != null) {
            SlashSuggestionList(
                query = slashQuery,
                onPick = { cmd ->
                    val action = viewModel.executeSlashCommand(cmd)
                    onValueChange("")
                    if (action == MainViewModel.SlashAction.OPEN_MODEL_PICKER) modelMenuOpen = true
                }
            )
        }

        // ── MA53 : autocomplétion @fichier au-dessus du champ ───────────────
        if (slashQuery == null && mention != null) {
            MentionSuggestionList(
                files = viewModel.projectFiles,
                query = mention.second,
                onPick = { path ->
                    onValueChange(value.take(mention.first) + "@" + path + " ")
                }
            )
        }

        // ── Conteneur en verre, rayon 28 dp (MA48) ──────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.10f else 1f))
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), context)
                .border(1.dp, c.border2.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // ── MA49 : vignettes des pièces jointes, au-dessus du champ ─────
            if (viewModel.attachments.isNotEmpty()) {
                AttachmentThumbsRow(
                    attachments = viewModel.attachments.toList(),
                    onRemove = { viewModel.removeAttachment(it) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.Bottom) {
                // ── MA49 : bouton « + » qui ouvre la feuille d'ajouts ───────
                Box(
                    Modifier
                        .size(48.dp) // MàJ 96 : zone tactile
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { attachSheetOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_attachment),
                        tint = c.text2,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))

                // ── MA48 : champ auto-extensible, 1 à 8 lignes ──────────────
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.input_placeholder),
                            style = TextStyle(fontFamily = Inter, fontSize = 15.sp),
                            color = c.text3
                        )
                    },
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = c.text),
                    minLines = 1,
                    maxLines = 8,
                    visualTransformation = mentionVisual,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (enabled && value.isNotBlank()) {
                                onSend()
                                focusRequester.requestFocus()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = c.cyan
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .focusRequester(focusRequester)
                )
                Spacer(Modifier.width(6.dp))

                // ── MA54 : micro visible quand le champ est vide ────────────
                if (value.isBlank() && !isLoading) {
                    MicButton(
                        isListening = isListening,
                        continu = dicteeContinue, // feat-022
                        onClick = {
                            if (isListening) {
                                recognizer?.stopListening()
                                isListening = false
                            } else {
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        // feat-022 — appui long : bascule le mode dictée continue.
                        onLongClick = {
                            dicteeContinue = !dicteeContinue
                            com.zagent.app.ui.components.common.ZEphemeral.show(
                                if (dicteeContinue) "Dictée continue activée — la reconnaissance se relance toute seule."
                                else "Dictée continue désactivée."
                            )
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                }

                // ── MA50 : envoi ⇄ stop animé ───────────────────────────────
                SendStopButton(
                    canSend = value.isNotBlank(),
                    enabled = enabled,
                    isLoading = isLoading,
                    onSend = {
                        onSend()
                        focusRequester.requestFocus()
                    },
                    onStop = onStop
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── MA51 : chip modèle en bas à gauche du champ ─────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModelChipMenu(
                    globalModel = viewModel.modelInput,
                    currentChatModel = viewModel.chatModel,
                    expanded = modelMenuOpen,
                    onExpandedChange = { modelMenuOpen = it },
                    onSelect = { viewModel.selectChatModel(it) },
                    favoris = viewModel.modelesFavoris
                )
            }
        }
    }

    // ── MA49 : feuille d'ajouts en verre ─────────────────────────────────────
    if (attachSheetOpen) {
        AttachSheet(
            hasProject = !viewModel.workingFolderUriString.isNullOrBlank(),
            onPickFile = {
                attachSheetOpen = false
                filePicker.launch("*/*")
            },
            onPickImage = {
                attachSheetOpen = false
                imagePicker.launch("image/*")
            },
            onAttachProject = {
                attachSheetOpen = false
                viewModel.addProjectTreeAttachment()
            },
            onPaste = {
                attachSheetOpen = false
                val pasted = clipboard.getText()?.text.orEmpty()
                if (pasted.isNotBlank()) {
                    onValueChange(if (value.isBlank()) pasted else value + "\n" + pasted)
                }
            },
            onDismiss = { attachSheetOpen = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants internes de la barre de saisie
// ─────────────────────────────────────────────────────────────────────────────

/** Bouton micro : cercle discret, onde animée pendant l'écoute (MA54). */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun MicButton(isListening: Boolean, continu: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val c = Zcolors
    Box(
        Modifier
            .size(48.dp) // MàJ 96 : zone tactile
            .clip(CircleShape)
            .background(
                when {
                    isListening -> c.cyanBg
                    continu -> c.amberBg // feat-022 — continu = pastille ambrée
                    else -> c.surface2
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) VoiceWave()
        else Icon(Icons.Default.Mic, stringResource(R.string.cd_dictate), tint = c.text2, modifier = Modifier.size(18.dp))
    }
}

/** Onde de 4 barres animées, affichée pendant la dictée (MA54). */
@Composable
private fun VoiceWave() {
    val c = Zcolors
    val transition = rememberInfiniteTransition(label = "voix")
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { i ->
            val h by transition.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(420 + i * 70), RepeatMode.Reverse),
                label = "barre" + i
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(14.dp * h)
                    .background(c.cyan, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Bouton d'envoi : cercle plein accent avec flèche ↑ ; pendant la génération il se
 * transforme en carré (Stop) avec pulsation douce ; grisé quand le champ est vide (MA50).
 */
@Composable
private fun SendStopButton(
    canSend: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val c = Zcolors
    val corner by animateDpAsState(if (isLoading) 11.dp else 21.dp, label = "envoiForme")
    val transition = rememberInfiniteTransition(label = "envoiPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "envoiEchelle"
    )
    val active = if (isLoading) true else (canSend && enabled)
    Box(
        Modifier
            .size(48.dp) // MàJ 96 : zone tactile
            .scale(if (isLoading) pulse else 1f)
            .clip(RoundedCornerShape(corner))
            .then(if (active) Modifier.background(c.gradBtn) else Modifier.background(c.surface3))
            .clickable(enabled = active, role = Role.Button) { if (isLoading) onStop() else onSend() },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isLoading, label = "envoiIcone") { generating ->
            if (generating) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = stringResource(R.string.cd_stop_agent),
                    tint = c.onGrad,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.cd_send),
                    tint = if (canSend && enabled) c.onGrad else c.text3,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Liste des commandes slash filtrées en direct, posée au-dessus du champ (MA52). */
@Composable
private fun SlashSuggestionList(query: String, onPick: (String) -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val matches = remember(query) {
        val q = query.lowercase()
        slashUiCommands.filter {
            it.command.startsWith(q) || it.command.removePrefix("/").startsWith(q.removePrefix("/"))
        }
    }
    if (matches.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(18.dp), context)
            .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(18.dp))
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .heightIn(max = 244.dp)
            .verticalScroll(rememberScrollState())
    ) {
        matches.forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(s.command) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(c.cyanBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(s.icon, null, tint = c.cyan, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        s.command,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                        color = c.text
                    )
                    Text(
                        s.label,
                        style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                        color = c.text2
                    )
                }
            }
        }
    }
}

/** Liste des fichiers proposés après un « @ », filtrage flou via ProjectSearch (MA53). */
@Composable
private fun MentionSuggestionList(files: List<String>, query: String, onPick: (String) -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val matches = remember(files, query) { ProjectSearch.fuzzyFileMatches(files, query, 8) }
    if (matches.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(18.dp), context)
            .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(18.dp))
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .heightIn(max = 244.dp)
            .verticalScroll(rememberScrollState())
    ) {
        matches.forEach { path ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(path) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.violetBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = c.violet, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    path,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
