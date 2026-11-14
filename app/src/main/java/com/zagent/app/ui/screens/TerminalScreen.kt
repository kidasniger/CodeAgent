package com.zagent.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.JetBrainsMono
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.res.ResourcesCompat
import com.termux.view.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.zagent.app.R
import com.zagent.app.terminal.TerminalController
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.glassSurface

@Composable
fun TerminalScreen(
    controller: TerminalController,
    showExtraKeys: Boolean,
    onBack: () -> Unit,
    onOpenPath: (String, Int?) -> Unit = { _, _ -> } // feat-056
) {
    val c = Zcolors
    val state = controller.state
    val termBg = c.bg.toArgb()
    val termFg = c.text.toArgb()
    var showAgent by rememberSaveable { mutableStateOf(false) }
    var showHisto by rememberSaveable { mutableStateOf(false) } // feat-047
    var showPaquets by rememberSaveable { mutableStateOf(false) } // feat-052
    var showBashrc by rememberSaveable { mutableStateOf(false) } // feat-054
    var ongletARenommer by rememberSaveable { mutableStateOf(-1) } // feat-048
    // Session 17 / vague E — snippets (ter-056), profil env (ter-055),
    // taille de police (ter-061), mode paysage (ter-060).
    var showSnips by rememberSaveable { mutableStateOf(false) }
    var showEnvProfil by rememberSaveable { mutableStateOf(false) }
    var showPolice by rememberSaveable { mutableStateOf(false) }
    var paysage by rememberSaveable { mutableStateOf(false) }

    // ter-060 — mode écran large : l'activité passe en paysage tant que
    // l'option est active (et restitue à la sortie de l'écran).
    val activite = LocalContext.current as? android.app.Activity
    DisposableEffect(paysage) {
        activite?.requestedOrientation = if (paysage)
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            activite?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // MA84 : fond, texte et palette ANSI alignés sur le thème clair/sombre.
    LaunchedEffect(termBg, termFg, c.isDark, controller.session) {
        controller.applyColors(termBg, termFg, c.isDark)
    }

    LaunchedEffect(state) {
        if (state is TerminalController.State.Ready && controller.session == null) {
            controller.openSession()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(24.dp), LocalContext.current)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_retour), tint = c.text)
            }
            Text(
                stringResource(R.string.ui_terminal),
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            if (state is TerminalController.State.Ready) {
                if (!showAgent) {
                    HeaderText("A−") { controller.changeFontSize(-1) }
                    HeaderText("A+") { controller.changeFontSize(1) }
                    // feat-051 — palette ANSI courante, tap = palette suivante.
                    HeaderText(controller.themeAnsi) {
                        val i = controller.themesAnsi.indexOf(controller.themeAnsi)
                        controller.reglerThemeAnsi(controller.themesAnsi[(i + 1) % controller.themesAnsi.size])
                    }
                }
                IconButton(onClick = { controller.openSession() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.terminal_relancer_onglet), tint = c.text2)
                }
            }
        }

        when (state) {
            is TerminalController.State.Idle -> Centered {
                Text(
                    stringResource(R.string.terminal_telecharge_note),
                    color = c.text2,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Button(onClick = { controller.install() }) { Text(stringResource(R.string.terminal_installer)) }
            }

            is TerminalController.State.Working -> Centered {
                Text(state.label, color = c.text)
                Spacer(Modifier.height(12.dp))
                val p = state.progress
                if (p != null) {
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            is TerminalController.State.Failed -> Centered {
                Text(
                    "Échec : ${state.message}",
                    color = c.red,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                // err-074 — « Réparer » : repose les droits et relance la configuration sans retélécharger.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { controller.install() }) { Text(stringResource(R.string.action_reessayer)) }
                    Button(onClick = { controller.reparer() }) { Text(stringResource(R.string.action_reparer)) }
                }
            }

            is TerminalController.State.Ready -> {
                TabsRow(
                    controller, showAgent,
                    onAgent = { showAgent = it; if (it) { showPaquets = false; showBashrc = false } },
                    onRename = { ongletARenommer = it }, // feat-048
                    showPaquets = showPaquets,
                    showBashrc = showBashrc,
                    onPaquets = { showPaquets = !showPaquets; if (showPaquets) { showAgent = false; showBashrc = false; showHisto = false } },
                    onBashrc = { showBashrc = !showBashrc; if (showBashrc) { showAgent = false; showPaquets = false; showHisto = false } }
                )
                when {
                    showAgent -> AgentLogPane(controller, Modifier.weight(1f).fillMaxWidth(), onOpenPath) // feat-056/057
                    showPaquets -> PaquetsPane(controller, Modifier.weight(1f).fillMaxWidth()) // feat-052
                    showBashrc -> BashrcPane(controller, Modifier.weight(1f).fillMaxWidth()) // feat-054
                    else -> {
                        ShellPane(controller, termBg, Modifier.weight(1f).fillMaxWidth())
                        if (showHisto) {
                            HistoriquePane(controller) { showHisto = false } // feat-047
                        }
                        if (showExtraKeys) {
                            ExtraKeys(
                                controller,
                                onHisto = { showHisto = !showHisto; if (showHisto) { showPaquets = false; showBashrc = false } },
                                histoActif = showHisto,
                                onSnips = { showSnips = true },
                                onEnv = { showEnvProfil = true },
                                onPolice = { showPolice = true },
                                onPaysage = { paysage = !paysage },
                                paysageActif = paysage
                            )
                        }
                    }
                }
                // feat-048 — dialogue de renommage d'onglet.
                if (ongletARenommer >= 0) {
                    val tab = controller.tabs.getOrNull(ongletARenommer)
                    if (tab != null) {
                        var nom by remember(ongletARenommer) { mutableStateOf(tab.nom.orEmpty()) }
                        AlertDialog(
                            onDismissRequest = { ongletARenommer = -1 },
                            title = { Text(stringResource(R.string.terminal_renommer_onglet), color = c.text) },
                            text = {
                                TextField(
                                    value = nom,
                                    onValueChange = { nom = it },
                                    placeholder = { Text(stringResource(R.string.terminal_nom_shell, tab.number)) },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    controller.renommerOnglet(ongletARenommer, nom)
                                    ongletARenommer = -1
                                }) { Text("OK", color = c.cyan) }
                            },
                            dismissButton = {
                                TextButton(onClick = { ongletARenommer = -1 }) { Text(stringResource(R.string.action_annuler), color = c.text2) }
                            },
                            containerColor = c.surface2
                        )
                    } else ongletARenommer = -1
                }
                // Session 17 / vague E — dialogues Snips / Env / Police.
                if (showSnips) {
                    SnipsDialog(
                        controller,
                        commandeCourante = controller.historiqueProjet().lastOrNull().orEmpty(),
                        onInserer = { controller.send(it) },
                        onFermer = { showSnips = false }
                    )
                }
                if (showEnvProfil) {
                    EnvProfilDialog(controller) { showEnvProfil = false }
                }
                if (showPolice) {
                    PoliceTerminalDialog(controller) { showPolice = false }
                }
            }
        }
    }
}

@Composable
private fun HeaderText(label: String, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        color = c.text2,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabsRow(
    controller: TerminalController,
    showAgent: Boolean,
    onAgent: (Boolean) -> Unit,
    onRename: (Int) -> Unit,
    showPaquets: Boolean = false,
    showBashrc: Boolean = false,
    onPaquets: () -> Unit = {},
    onBashrc: () -> Unit = {}
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        controller.tabs.forEachIndexed { i, tab ->
            val on = !showAgent && controller.activeTab == i
            Row(
                Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (on) c.cyan else c.bg2)
                    // feat-048 — appui long : renommer l'onglet.
                    .combinedClickable(
                        onClick = {
                            controller.activeTab = i
                            onAgent(false)
                        },
                        onLongClick = { onRename(i) }
                    )
                    .padding(start = 14.dp, end = if (controller.tabs.size > 1) 6.dp else 14.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tab.nom ?: ("Shell " + tab.number),
                    color = if (on) c.bg else c.text2,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                if (controller.tabs.size > 1) {
                    Text(
                        "×",
                        color = if (on) c.bg else c.text3,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable { controller.closeTab(i) }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
        Box(
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(c.bg2)
                .clickable {
                    controller.addTab()
                    onAgent(false)
                }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("+", color = c.text, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
        Box(
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (showAgent) c.cyan else c.bg2)
                .clickable { onAgent(true) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                stringResource(R.string.ui_agent),
                color = if (showAgent) c.bg else c.text2,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        // feat-052 — gestion des paquets.
        Box(
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (showPaquets) c.cyan else c.bg2)
                .clickable { onPaquets() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                stringResource(R.string.ui_paquets),
                color = if (showPaquets) c.bg else c.text2,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        // feat-054 — .bashrc et alias.
        Box(
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (showBashrc) c.cyan else c.bg2)
                .clickable { onBashrc() }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                stringResource(R.string.ui_bashrc),
                color = if (showBashrc) c.bg else c.text2,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/** Onglet « Agent » : commandes lancées par l'IA et leur sortie, en lecture seule. */
@Composable
private fun AgentLogPane(
    controller: TerminalController,
    modifier: Modifier,
    onOpenPath: (String, Int?) -> Unit = { _, _ -> }
) {
    val c = Zcolors
    val fullLog = controller.agentLog
    // feat-057 — recherche dans la sortie de l'agent.
    var recherche by rememberSaveable { mutableStateOf("") }
    val log = androidx.compose.runtime.remember(fullLog, recherche) {
        val base = com.zagent.app.ui.components.common.tailForDisplay(fullLog, 16000)
        if (recherche.isBlank()) base
        else base.lineSequence().filter { it.contains(recherche, ignoreCase = true) }.joinToString("\n")
    }
    val scroll = rememberScrollState()
    LaunchedEffect(log) { scroll.scrollTo(scroll.maxValue) }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.securite_lecture_seule),
                color = c.text3,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f)
            )
            if (log.isNotEmpty()) {
                Text(
                    stringResource(R.string.action_vider),
                    color = c.amber,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { controller.clearAgentLog() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        // feat-057 — champ de recherche filtrant les lignes de sortie.
        TextField(
            value = recherche,
            onValueChange = { recherche = it },
            placeholder = { Text(stringResource(R.string.terminal_recherche_sortie), style = TextStyle(fontSize = 12.5.sp)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = c.text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                focusedIndicatorColor = c.cyan,
                unfocusedIndicatorColor = c.border2,
                cursorColor = c.cyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            com.zagent.app.ui.components.common.AnsiText(
                text = log.ifEmpty {
                    if (recherche.isBlank()) "Aucune commande lancée par l'agent pour le moment."
                    else "Aucune ligne ne correspond à « $recherche »."
                },
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp),
                baseColor = if (log.isEmpty()) c.text3 else c.text,
                linkColor = c.cyan,
                dark = c.isDark,
                // feat-056 — tap sur « chemin:ligne » ouvre l'éditeur à la ligne.
                onLinkClick = { lien ->
                    if (lien.startsWith("file:")) {
                        val corps = lien.removePrefix("file:")
                        val chemin = corps.substringBeforeLast(':')
                        val ligne = corps.substringAfterLast(':').toIntOrNull()
                        if (chemin.isNotEmpty()) onOpenPath(chemin, ligne)
                    }
                }
            )
        }
    }
}

@Composable
private fun ShellPane(controller: TerminalController, termBg: Int, modifier: Modifier) {
    val c = Zcolors
    val session = controller.session
    val density = LocalDensity.current
    // feat-050 — accumulation du pincement : un cran A± par palier de zoom.
    val zoomAccum = remember { mutableStateOf(1f) }
    Box(
        modifier
            // feat-050 — pincer pour changer la taille du texte.
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomAccum.value *= zoom
                    if (zoomAccum.value > 1.22f) {
                        controller.changeFontSize(1)
                        zoomAccum.value = 1f
                    } else if (zoomAccum.value < 0.82f) {
                        controller.changeFontSize(-1)
                        zoomAccum.value = 1f
                    }
                }
            }
    ) {
        if (session != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        setTerminalViewClient(controller.terminalViewClient())
                        setTextSize(with(density) { controller.fontSize.dp.roundToPx() })
                        tag = controller.fontSize
                        ResourcesCompat.getFont(ctx, R.font.jetbrainsmono_variable)
                            ?.let { setTypeface(it) }
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setBackgroundColor(termBg)
                        controller.view = this
                        attachSession(session)
                        requestFocus()
                    }
                },
                update = { v ->
                    v.setBackgroundColor(termBg)
                    controller.view = v
                    if (v.tag != controller.fontSize) {
                        v.tag = controller.fontSize
                        v.setTextSize(with(density) { controller.fontSize.dp.roundToPx() })
                    }
                    if (v.currentSession !== session) {
                        v.attachSession(session)
                        v.onScreenUpdated()
                    }
                },
                onRelease = { v -> if (controller.view === v) controller.view = null }
            )
        }
        if (controller.sessionEnded) {
            Centered {
                Text(stringResource(R.string.terminal_session_terminee), color = c.text2)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { controller.openSession() }) { Text(stringResource(R.string.action_relancer)) }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun RowScope.Key(label: String? = null, icon: ImageVector? = null, active: Boolean = false, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (active) c.cyan else c.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) c.bg else c.text)
        } else if (label != null) {
            Text(label, color = if (active) c.bg else c.text)
        }
    }
}

@Composable
private fun RowScope.IconKey(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(c.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = c.text)
    }
}

@Composable
private fun ExtraKeys(
    controller: TerminalController,
    onHisto: () -> Unit = {},
    histoActif: Boolean = false,
    onSnips: () -> Unit = {},
    onEnv: () -> Unit = {},
    onPolice: () -> Unit = {},
    onPaysage: () -> Unit = {},
    paysageActif: Boolean = false
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bg2)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Key("Esc", onClick = { controller.send("\u001b") })
        Key("Tab", onClick = { controller.send("\t") })
        Key("Ctrl", active = controller.ctrlActive, onClick = { controller.ctrlActive = !controller.ctrlActive })
        Key("/", onClick = { controller.send("/") })
        Key("~", onClick = { controller.send("~") })
        Key("|", onClick = { controller.send("|") })
        IconKey(Icons.AutoMirrored.Filled.ArrowBack, "Gauche", onClick = { controller.send("\u001b[D") })
        IconKey(Icons.Default.ArrowUpward, "Haut", onClick = { controller.send("\u001b[A") })
        IconKey(Icons.Default.ArrowDownward, "Bas", onClick = { controller.send("\u001b[B") })
        IconKey(Icons.AutoMirrored.Filled.ArrowForward, "Droite", onClick = { controller.send("\u001b[C") })
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bg2)
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Key("Coller", onClick = { controller.paste() })
        Key("Copier tout", onClick = { controller.copyAll() })
        // feat-049 — partage de la sortie du terminal.
        Key("Partager", onClick = { controller.partagerSortie() })
        Key("Projet", onClick = { controller.cdToProject() })
        // MA84 : efface l'écran de l'onglet actif.
        Key("Effacer", onClick = { controller.clearActiveScreen() })
        Key("^C", onClick = { controller.send("\u0003") })
        // feat-047 — historique des commandes.
        Key("Histo", active = histoActif, onClick = { onHisto() })
        // Session 17 / vague E — outils projet du terminal.
        Key("Snips", onClick = onSnips)
        Key("Env", onClick = onEnv)
        Key("Aa", onClick = onPolice)
        Key("Paysage", active = paysageActif, onClick = onPaysage)
    }
}

/**
 * feat-047 — Panneau de l'historique des commandes exécutées (persistant),
 * avec recherche ; tap = envoi de la commande à l'onglet actif.
 */
@Composable
private fun HistoriquePane(controller: TerminalController, onFermer: () -> Unit) {
    val c = Zcolors
    var q by rememberSaveable { mutableStateOf("") }
    val liste = remember(controller.historiqueCommandes, q) {
        controller.historiqueCommandes
            .filter { q.isBlank() || it.contains(q, ignoreCase = true) }
            .takeLast(30)
            .reversed()
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bg2)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Historique des commandes (" + controller.historiqueCommandes.size + ")",
                color = c.text2,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.action_vider),
                color = c.amber,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { controller.viderHistorique() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                stringResource(R.string.action_fermer),
                color = c.cyan,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onFermer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text(stringResource(R.string.terminal_recherche_commande), style = TextStyle(fontSize = 12.5.sp)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = c.text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                focusedIndicatorColor = c.cyan,
                unfocusedIndicatorColor = c.border2,
                cursorColor = c.cyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            Modifier
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (liste.isEmpty()) {
                Text(
                    if (controller.historiqueCommandes.isEmpty()) "Aucune commande exécutée pour l'instant."
                    else "Aucune commande ne correspond.",
                    color = c.text3,
                    style = TextStyle(fontSize = 12.sp),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            liste.forEach { cmd ->
                Text(
                    cmd,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp),
                    color = c.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable {
                            controller.send(cmd + "\n")
                            onFermer()
                        }
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                )
            }
        }
    }
}
/**
 * feat-052 — Gestion des paquets du terminal : liste installée, recherche,
 * installation et suppression via pkg (exécution dans le shell embarqué).
 */
@Composable
private fun PaquetsPane(controller: TerminalController, modifier: Modifier) {
    val c = Zcolors
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var paquets by remember { mutableStateOf<List<String>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var q by rememberSaveable { mutableStateOf("") }
    var nouveau by rememberSaveable { mutableStateOf("") }
    var operation by remember { mutableStateOf<String?>(null) }

    fun charger() {
        chargement = true
        scope.launch(Dispatchers.IO) {
            val out = controller.runCommand("pkg list-installed 2>/dev/null | cut -d/ -f1", null, 60) { }
            val liste = out.lines().map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("Erreur") && !it.contains(" ") }
                .sorted()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                paquets = liste
                chargement = false
            }
        }
    }
    fun agir(commande: String) {
        operation = commande
        scope.launch(Dispatchers.IO) {
            controller.runCommand(commande, null, 300) { }
            kotlinx.coroutines.withContext(Dispatchers.Main) { operation = null }
            charger()
        }
    }
    LaunchedEffect(Unit) { charger() }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (chargement) "Chargement des paquets installés…"
                else "Paquets installés (" + paquets.size + ")",
                color = c.text2,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = nouveau,
                onValueChange = { nouveau = it },
                placeholder = { Text(stringResource(R.string.terminal_paquet_hint), style = TextStyle(fontSize = 12.5.sp)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = c.text),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.surface2,
                    unfocusedContainerColor = c.surface2,
                    focusedIndicatorColor = c.cyan,
                    unfocusedIndicatorColor = c.border2,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.action_installer),
                color = if (nouveau.isBlank() || operation != null) c.text3 else c.green,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = nouveau.isNotBlank() && operation == null) {
                        val nom = nouveau.trim()
                        if (Regex("[a-zA-Z0-9._+-]+").matches(nom)) {
                            agir("pkg install -y " + nom)
                            nouveau = ""
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        TextField(
            value = q,
            onValueChange = { q = it },
            placeholder = { Text(stringResource(R.string.terminal_recherche_paquet), style = TextStyle(fontSize = 12.5.sp)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = c.text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                focusedIndicatorColor = c.cyan,
                unfocusedIndicatorColor = c.border2,
                cursorColor = c.cyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (operation != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "En cours : " + operation + " — la sortie s'affiche dans l'onglet Agent.",
                color = c.amber,
                style = TextStyle(fontSize = 12.sp)
            )
        }
        Spacer(Modifier.height(4.dp))
        if (!chargement) {
            val filtres = paquets.filter { q.isBlank() || it.contains(q, ignoreCase = true) }
            if (filtres.isEmpty()) {
                Text(
                    if (paquets.isEmpty()) "Aucun paquet installé (ou terminal non installé)."
                    else "Aucun paquet ne correspond.",
                    color = c.text3,
                    style = TextStyle(fontSize = 12.sp),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            filtres.forEach { nom ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        nom,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                        color = c.text,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.action_supprimer),
                        color = c.red,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = operation == null) { agir("pkg uninstall -y " + nom) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * feat-054 — ~/.bashrc éditable avec modèles d'alias : le fichier est sourcé
 * par chaque nouveau shell interactif du terminal.
 */
@Composable
private fun BashrcPane(controller: TerminalController, modifier: Modifier) {
    val c = Zcolors
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var texte by rememberSaveable { mutableStateOf<String?>(null) } // null = chargement
    var message by rememberSaveable { mutableStateOf("") }

    // feat-054 — modèles d'alias prêts à insérer.
    val modeles = listOf(
        "alias ll='ls -la'",
        "alias gs='git status -sb'",
        "alias gd='git diff'",
        "alias ..='cd ..'",
        "alias cls='clear'",
        "alias py='python'"
    )

    LaunchedEffect(Unit) {
        if (texte == null) {
            val contenu = kotlinx.coroutines.withContext(Dispatchers.IO) { controller.lireBashrc() }
            texte = contenu
        }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.terminal_bashrc_note),
                color = c.text2,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            modeles.forEach { m ->
                Text(
                    m,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                    color = c.cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .clickable {
                            texte = (texte.orEmpty().trimEnd() + "\n" + m).trimStart()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TextField(
            value = texte.orEmpty(),
            onValueChange = { texte = it; message = "" },
            placeholder = { Text(stringResource(R.string.terminal_alias_hint), style = TextStyle(fontSize = 12.5.sp)) },
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp, color = c.text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                focusedIndicatorColor = c.cyan,
                unfocusedIndicatorColor = c.border2,
                cursorColor = c.cyan
            ),
            minLines = 5,
            maxLines = 14,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.action_enregistrer),
                color = c.green,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        val contenu = texte.orEmpty()
                        val ok = if (contenu.isBlank()) controller.ecrireBashrc("") else controller.ecrireBashrc(contenu + if (contenu.endsWith("\n")) "" else "\n")
                        message = if (ok) "Enregistré — actif dans les nouveaux shells." else "Échec d'enregistrement."
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(12.dp))
            if (message.isNotEmpty()) {
                Text(message, color = if (message.startsWith("Enreg")) c.green else c.red, style = TextStyle(fontSize = 12.sp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session 17 / vague E — dialogues Snips (ter-056), profil env (ter-055) et
// taille de police (ter-061) du terminal.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SnipsDialog(
    controller: TerminalController,
    commandeCourante: String,
    onInserer: (String) -> Unit,
    onFermer: () -> Unit
) {
    val c = Zcolors
    var snippets by remember { mutableStateOf(controller.snippetsProjet()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.ter_snips), fontFamily = com.zagent.app.ui.theme.SpaceGrotesk, color = c.text) },
        text = {
            Column {
                Text(
                    stringResource(R.string.ter_snips_hint),
                    style = TextStyle(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(8.dp))
                if (snippets.isEmpty()) {
                    Text("(aucun snippet pour ce projet)", style = TextStyle(fontSize = 12.sp), color = c.text3)
                }
                snippets.take(12).forEach { s ->
                    Text(
                        s,
                        style = TextStyle(fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 12.sp),
                        color = c.cyan,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onInserer(s); onFermer() }
                            .padding(vertical = 8.dp)
                    )
                }
                if (commandeCourante.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.ter_snips_ajouter),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = c.amber,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                controller.ajouterSnippet(commandeCourante)
                                snippets = controller.snippetsProjet()
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.action_fermer), color = c.cyan) }
        }
    )
}

@Composable
internal fun EnvProfilDialog(
    controller: TerminalController,
    onFermer: () -> Unit
) {
    val c = Zcolors
    var valeur by remember { mutableStateOf(controller.profilEnvProjet()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.ter_env_profil), fontFamily = com.zagent.app.ui.theme.SpaceGrotesk, color = c.text) },
        text = {
            androidx.compose.material3.TextField(
                value = valeur,
                onValueChange = { valeur = it },
                placeholder = { Text("API_URL=http://10.0.2.2:8080\nNO_COLOR=1", fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 11.5.sp, color = c.text3) },
                textStyle = TextStyle(fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 12.5.sp, color = c.text),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = c.surface2,
                    unfocusedContainerColor = c.surface2,
                    focusedIndicatorColor = c.cyan,
                    unfocusedIndicatorColor = c.border2,
                    cursorColor = c.cyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { controller.ecrireProfilEnvProjet(valeur); onFermer() }) {
                Text(stringResource(R.string.action_enregistrer), color = c.cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.action_annuler), color = c.text2) }
        }
    )
}

@Composable
internal fun PoliceTerminalDialog(
    controller: TerminalController,
    onFermer: () -> Unit
) {
    val c = Zcolors
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.ter_police), fontFamily = com.zagent.app.ui.theme.SpaceGrotesk, color = c.text) },
        text = {
            Column {
                Text(
                    controller.taillePoliceTerminal.toString() + " sp",
                    style = TextStyle(fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 14.sp),
                    color = c.text,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.Slider(
                    value = controller.taillePoliceTerminal.toFloat(),
                    onValueChange = { controller.reglerTaillePolice(it.toInt()) },
                    valueRange = 9f..22f,
                    steps = 12
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.action_fermer), color = c.cyan) }
        }
    )
}
