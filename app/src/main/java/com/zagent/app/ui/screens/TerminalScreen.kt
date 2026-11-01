package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.core.content.res.ResourcesCompat
import com.termux.view.TerminalView
import com.zagent.app.R
import com.zagent.app.terminal.TerminalController
import com.zagent.app.ui.theme.Zcolors

@Composable
fun TerminalScreen(controller: TerminalController, onBack: () -> Unit) {
    val c = Zcolors
    val state = controller.state
    val termBg = c.bg.toArgb()
    val termFg = c.text.toArgb()
    var showAgent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(termBg, termFg, controller.session) {
        controller.applyColors(termBg, termFg)
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
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = c.text)
            }
            Text(
                "Terminal",
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            if (state is TerminalController.State.Ready) {
                if (!showAgent) {
                    HeaderText("A−") { controller.changeFontSize(-1) }
                    HeaderText("A+") { controller.changeFontSize(1) }
                }
                IconButton(onClick = { controller.openSession() }) {
                    Icon(Icons.Default.Refresh, "Relancer l'onglet", tint = c.text2)
                }
            }
        }

        when (state) {
            is TerminalController.State.Idle -> Centered {
                Text(
                    "Le terminal a besoin de télécharger son système (une seule fois).",
                    color = c.text2,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Button(onClick = { controller.install() }) { Text("Installer le terminal") }
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
                Button(onClick = { controller.install() }) { Text("Réessayer") }
            }

            is TerminalController.State.Ready -> {
                TabsRow(controller, showAgent, onAgent = { showAgent = it })
                if (showAgent) {
                    AgentLogPane(controller, Modifier.weight(1f).fillMaxWidth())
                } else {
                    ShellPane(controller, termBg, Modifier.weight(1f).fillMaxWidth())
                    ExtraKeys(controller)
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun TabsRow(controller: TerminalController, showAgent: Boolean, onAgent: (Boolean) -> Unit) {
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.cyan else c.bg2)
                    .clickable {
                        controller.activeTab = i
                        onAgent(false)
                    }
                    .padding(start = 14.dp, end = if (controller.tabs.size > 1) 6.dp else 14.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shell " + tab.number,
                    color = if (on) c.bg else c.text2,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
                if (controller.tabs.size > 1) {
                    Text(
                        "×",
                        color = if (on) c.bg else c.text3,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { controller.closeTab(i) }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
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
                .clip(RoundedCornerShape(10.dp))
                .background(if (showAgent) c.cyan else c.bg2)
                .clickable { onAgent(true) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                "Agent",
                color = if (showAgent) c.bg else c.text2,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/** Onglet « Agent » : commandes lancées par l'IA et leur sortie, en lecture seule. */
@Composable
private fun AgentLogPane(controller: TerminalController, modifier: Modifier) {
    val c = Zcolors
    val fullLog = controller.agentLog
    val log = androidx.compose.runtime.remember(fullLog) {
        com.zagent.app.ui.components.tailForDisplay(fullLog, 16000)
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
                "Lecture seule : actions et commandes de l'agent",
                color = c.text3,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier.weight(1f)
            )
            if (log.isNotEmpty()) {
                Text(
                    "Vider",
                    color = c.amber,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { controller.clearAgentLog() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            com.zagent.app.ui.components.AnsiText(
                text = log.ifEmpty { "Aucune commande lancée par l'agent pour le moment." },
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp),
                baseColor = if (log.isEmpty()) c.text3 else c.text,
                linkColor = c.cyan
            )
        }
    }
}

@Composable
private fun ShellPane(controller: TerminalController, termBg: Int, modifier: Modifier) {
    val c = Zcolors
    val session = controller.session
    val density = LocalDensity.current
    Box(modifier) {
        if (session != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        setTerminalViewClient(controller.terminalViewClient())
                        setTextSize(with(density) { controller.fontSize.dp.roundToPx() })
                        tag = controller.fontSize
                        ResourcesCompat.getFont(ctx, R.font.jetbrainsmono_regular)
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
                Text("Session terminée", color = c.text2)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { controller.openSession() }) { Text("Relancer") }
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
            .clip(RoundedCornerShape(8.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = c.text)
    }
}

@Composable
private fun ExtraKeys(controller: TerminalController) {
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
        Key("-", onClick = { controller.send("-") })
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
        Key("Projet", onClick = { controller.cdToProject() })
        Key("^C", onClick = { controller.send("\u0003") })
    }
}
