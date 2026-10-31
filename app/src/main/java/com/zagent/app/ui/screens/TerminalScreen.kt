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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    LaunchedEffect(state) {
        if (state is TerminalController.State.Ready && controller.session == null) {
            controller.startSession()
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
                IconButton(onClick = { controller.startSession() }) {
                    Icon(Icons.Default.Refresh, "Nouvelle session", tint = c.text2)
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
                val session = controller.session
                val density = LocalDensity.current
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (session != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                TerminalView(ctx, null).apply {
                                    setTerminalViewClient(controller.terminalViewClient())
                                    setTextSize(with(density) { 14.dp.roundToPx() })
                                    ResourcesCompat.getFont(ctx, R.font.jetbrainsmono_regular)
                                        ?.let { setTypeface(it) }
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    controller.view = this
                                    attachSession(session)
                                    requestFocus()
                                }
                            },
                            update = { v ->
                                controller.view = v
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
                            Button(onClick = { controller.startSession() }) { Text("Relancer") }
                        }
                    }
                }
                ExtraKeys(controller)
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
private fun RowScope.Key(label: String, active: Boolean = false, onClick: () -> Unit) {
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
        Text(label, color = if (active) c.bg else c.text)
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
        Key("\u2190", onClick = { controller.send("\u001b[D") })
        Key("\u2191", onClick = { controller.send("\u001b[A") })
        Key("\u2193", onClick = { controller.send("\u001b[B") })
        Key("\u2192", onClick = { controller.send("\u001b[C") })
    }
}
