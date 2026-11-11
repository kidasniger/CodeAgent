package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.update.ReleaseSummary
import com.zagent.app.update.UpdateManager
import com.zagent.app.update.UpdateState
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.components.common.ZLogo
import com.zagent.app.ui.components.chat.MarkdownText
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MA66 — Écran À propos et Mises à jour : logo, version, vérification des mises
// à jour avec progression (UpdateManager), journal des nouveautés en Markdown,
// licences open source, lien de feedback.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val updateState by UpdateManager.state.collectAsStateWithLifecycle() // MàJ 101 : ne collecte que si l'écran est visible

    // maj-073 — dernières versions publiées, chargées une fois à l'ouverture
    // (appel réseau bloquant relégué sur IO ; liste vide = section masquée).
    var releases by remember { mutableStateOf<List<ReleaseSummary>?>(null) }
    LaunchedEffect(Unit) {
        releases = withContext(Dispatchers.IO) { UpdateManager.fetchRecentReleases(5) }
    }

    OverlayScreen(title = "À propos", subtitle = "ZAgent — agent de code sur téléphone", onBack = onBack) {
        // ── Carte identité ───────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZLogo(modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "ZAgent",
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp),
                color = c.text
            )
            Spacer(Modifier.height(2.dp))
            // MA95 : versionName + versionCode depuis BuildConfig.
            MonoLine(
                "Version " + UpdateManager.currentVersionName() + " · build " +
                    com.zagent.app.BuildConfig.VERSION_CODE,
                c.text2
            )
        }

        // ── Licences open source (MA96) ─────────────────────────────────────
        OverlaySection("Licences open source")
        GlassCard {
            val licences = listOf(
                "Jetpack Compose & AndroidX" to "Apache 2.0",
                "OkHttp / Okio (Square)" to "Apache 2.0",
                "Coil (coil3)" to "Apache 2.0",
                "Haze (Chris Banes)" to "Apache 2.0",
                "ZXing" to "Apache 2.0",
                "kotlinx.coroutines & serialization" to "Apache 2.0",
                "Termux (terminal-view, terminal-emulator)" to "GPLv3"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                licences.forEach { (lib, lic) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            lib,
                            style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                            color = c.text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            lic,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                            color = c.text2
                        )
                    }
                }
            }
        }

        OverlaySection("Mises à jour")
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (val s = updateState) {
                    is UpdateState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = c.cyan, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        StatusText("Vérification en cours…", c.text2)
                    }
                    is UpdateState.Downloading -> {
                        Column(Modifier.weight(1f)) {
                            StatusText("Téléchargement : " + s.percent + " %", c.cyan)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { s.percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = c.cyan,
                                trackColor = c.surface3
                            )
                        }
                    }
                    is UpdateState.Available -> {
                        Column(Modifier.weight(1f)) {
                            StatusText("Version " + s.info.tag + " disponible", c.amber)
                            if (s.info.notes.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    s.info.notes.take(300),
                                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                                    color = c.text2,
                                    maxLines = 5
                                )
                            }
                        }
                    }
                    is UpdateState.UpToDate -> {
                        StatusText("Déjà à jour (" + s.currentVersion + ")", c.green)
                    }
                    is UpdateState.Error -> {
                        StatusText("Erreur : " + s.message.take(80), c.red)
                    }
                    else -> StatusText("Dernière vérification sur demande", c.text2)
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Default.Refresh, "Vérifier les mises à jour",
                    tint = c.cyan,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { UpdateManager.check(auto = false) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Journal des nouveautés ───────────────────────────────────────────
        OverlaySection("Nouveautés de cette version")
        GlassCard {
            MarkdownText(
                changelogMarkdown,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── maj-073 : dernières versions publiées sur GitHub ─────────────────
        if (!releases.isNullOrEmpty()) {
            OverlaySection("Dernières versions publiées")
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    releases.orEmpty().forEach { r ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    r.tag,
                                    style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                                    color = c.cyan
                                )
                                if (r.prerelease) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "bêta",
                                        style = TextStyle(fontFamily = Inter, fontSize = 10.sp),
                                        color = c.amber,
                                        modifier = Modifier
                                            .background(c.amber.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    r.date,
                                    style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                                    color = c.text3
                                )
                            }
                            if (r.notes.isNotBlank() && r.notes != r.tag) {
                                MarkdownText(
                                    r.notes,
                                    baseColor = c.text2,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Licences ─────────────────────────────────────────────────────────
        OverlaySection("Licences open source")
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            listOf(
                "Jetpack Compose & AndroidX" to "Apache License 2.0",
                "Kotlin & Coroutines" to "Apache License 2.0",
                "Haze (dev.chrisbanes.haze)" to "Apache License 2.0",
                "Coil" to "Apache License 2.0",
                "OkHttp" to "Apache License 2.0",
                "kotlinx.serialization" to "Apache License 2.0",
                "ZXing" to "Apache License 2.0",
                "Termux terminal-emulator / terminal-view" to "GPLv3"
            ).forEach { (name, lic) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(c.cyan, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        name,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                        color = c.text,
                        modifier = Modifier.weight(1f)
                    )
                    MonoLine(lic, c.text3)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Feedback ─────────────────────────────────────────────────────────
        OverlaySection("Feedback")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
                .border(1.dp, c.border2, RoundedCornerShape(50))
                .clickable {
                    runCatching {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:feedback@zagent.app")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback ZAgent " + UpdateManager.currentVersionName())
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Envoyer un feedback"))
                    }
                }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BugReport, null, tint = c.cyan, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Signaler un problème ou proposer une idée",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = c.text
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        color = color
    )
}

/** Journal des nouveautés de la version courante (MA66), rendu en Markdown. */
private val changelogMarkdown = """
### Derniers apports

- **Tiroir Historique** : conversations groupées par jour, épinglables, supprimables
- **Écran Projets** plein écran avec favoris et templates
- **Recherche globale** : conversations, fichiers et code avec extrait surligné
- **Modèles** : catalogue, vitesse et modèle personnalisé avec URL dédiée
- **Usage & coûts** : graphique 7/30 jours, répartition, export CSV
- **Git** : indexation, commit, historique et branches en plein écran
- **Aperçu plein écran** : appareils, rotation, QR code et console JS
- **Instructions, Permissions, Activité** : l'agent sous ton contrôle
""".trim()
