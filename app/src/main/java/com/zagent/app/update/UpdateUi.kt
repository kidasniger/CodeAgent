package com.zagent.app.update

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zagent.app.ui.components.CardDivider
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.GradientProgress
import com.zagent.app.ui.components.IconBadge
import com.zagent.app.ui.components.SecondaryButton
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private fun mb(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f Mo", bytes / 1_048_576.0)

private fun findActivity(context: Context): ComponentActivity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * À placer une seule fois, tout en haut du contenu de l'Activity.
 * Vérifie automatiquement au démarrage et affiche les fenêtres de mise à jour.
 */
@Composable
fun UpdateHost() {
    val context = LocalContext.current
    remember { UpdateManager.init(context) }
    val state by UpdateManager.state.collectAsState()
    val dismissed by UpdateManager.dismissedTag.collectAsState()

    LaunchedEffect(Unit) {
        delay(1500)
        UpdateManager.check(auto = true)
    }

    DisposableEffect(context) {
        val activity = findActivity(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) UpdateManager.onResume()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    when (val s = state) {
        is UpdateState.Available -> if (s.info.tag != dismissed) AvailableDialog(s)
        is UpdateState.Downloading -> if (s.info.tag != dismissed) DownloadingDialog(s)
        is UpdateState.Ready -> if (s.info.tag != dismissed) ReadyDialog(s)
        is UpdateState.Error -> if (s.info != null && s.info.tag != dismissed) ErrorDialog(s, s.info)
        else -> Unit
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogues de mise à jour (design ZAgent)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpdateDialogShell(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val c = Zcolors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = c.bg2,
        shape = RoundedCornerShape(24.dp),
        title = {},
        text = { content() },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun AvailableDialog(s: UpdateState.Available) {
    val c = Zcolors
    UpdateDialogShell(onDismissRequest = { UpdateManager.dismiss(s.info.tag) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Download, c.cyan, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Mise à jour ${s.info.tag} disponible",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                        color = c.text
                    )
                    Text(
                        "${mb(s.info.apk.size)} · installation vérifiée par signature",
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = c.text2
                    )
                }
            }
            if (s.info.apk.sha256 == null) {
                Text(
                    "Empreinte SHA-256 non publiée : intégrité vérifiée par la taille et la signature Android.",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = c.text3
                )
            }
            CardDivider()
            Text(
                s.info.notes,
                style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 20.sp),
                color = c.text2,
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Plus tard",
                    onClick = { UpdateManager.dismiss(s.info.tag) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                GradientButton(
                    "Télécharger",
                    onClick = { UpdateManager.startDownload() },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
            }
        }
    }
}

@Composable
private fun DownloadingDialog(s: UpdateState.Downloading) {
    val c = Zcolors
    UpdateDialogShell(onDismissRequest = { UpdateManager.dismiss(s.info.tag) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicatorProgress(s.percent)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Téléchargement… ${s.percent} %",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                        color = c.text
                    )
                    Text(
                        if (s.total > 0) "${mb(s.downloaded)} / ${mb(s.total)}" else mb(s.downloaded),
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text2
                    )
                }
            }
            GradientProgress(s.percent / 100f)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Annuler",
                    onClick = { UpdateManager.cancelDownload() },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                SecondaryButton(
                    "Masquer",
                    onClick = { UpdateManager.dismiss(s.info.tag) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
            }
        }
    }
}

@Composable
private fun CircularProgressIndicatorProgress(percent: Int) {
    val c = Zcolors
    androidx.compose.material3.CircularProgressIndicator(
        progress = { percent / 100f },
        modifier = Modifier.size(44.dp),
        strokeWidth = 4.dp,
        color = c.cyan,
        trackColor = c.surface3
    )
}

@Composable
private fun ReadyDialog(s: UpdateState.Ready) {
    val c = Zcolors
    UpdateDialogShell(onDismissRequest = { UpdateManager.dismiss(s.info.tag) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Verified, c.green, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Mise à jour prête",
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                        color = c.text
                    )
                    Text(
                        "v${s.info.tag.removePrefix("v")} téléchargée et vérifiée",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text2
                    )
                }
            }
            Text(
                "Android va te demander de confirmer l'installation.",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp),
                color = c.text2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Plus tard",
                    onClick = { UpdateManager.dismiss(s.info.tag) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                GradientButton(
                    "Installer",
                    onClick = { UpdateManager.install() },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
            }
        }
    }
}

@Composable
private fun ErrorDialog(s: UpdateState.Error, info: ReleaseInfo) {
    val c = Zcolors
    UpdateDialogShell(onDismissRequest = { UpdateManager.dismiss(info.tag) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.ErrorOutline, c.red, size = 46.dp, iconSize = 22.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Mise à jour impossible",
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
                    color = c.text
                )
            }
            Text(
                s.message,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp),
                color = c.text2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "Fermer",
                    onClick = { UpdateManager.dismiss(info.tag) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                GradientButton(
                    "Réessayer",
                    onClick = { UpdateManager.startDownload() },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Carte des réglages : version + vérification manuelle
// ─────────────────────────────────────────────────────────────────────────────

/** Carte à placer dans l'écran des réglages : version installée + vérification manuelle. */
@Composable
fun UpdateSettingsCard(modifier: Modifier = Modifier) {
    val c = Zcolors
    val context = LocalContext.current
    remember { UpdateManager.init(context) }
    val state by UpdateManager.state.collectAsState()

    GlassCard(contentPadding = PaddingValues(0.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.Download, c.cyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Version", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text(
                        "${UpdateManager.currentVersionName()} · build ${com.zagent.app.BuildConfig.VERSION_CODE}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = c.text2
                    )
                }
            }
            val status = when (val s = state) {
                UpdateState.Idle -> null
                UpdateState.Checking -> "Vérification en cours…"
                is UpdateState.UpToDate -> "L'application est à jour."
                is UpdateState.Available -> "Nouvelle version ${s.info.tag} disponible (${mb(s.info.apk.size)})."
                is UpdateState.Downloading -> "Téléchargement : ${s.percent} %"
                is UpdateState.Ready -> "Mise à jour téléchargée et vérifiée."
                is UpdateState.NeedsPermission -> "Autorise l'installation dans les paramètres, puis reviens dans l'application."
                is UpdateState.Error -> s.message
            }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp), color = c.text2)
            }
            when (val s = state) {
                is UpdateState.Downloading -> {
                    Spacer(Modifier.height(10.dp))
                    GradientProgress(s.percent / 100f)
                }
                UpdateState.Checking -> {
                    Spacer(Modifier.height(10.dp))
                    GradientProgress(0.35f)
                }
                is UpdateState.Available -> {
                    Spacer(Modifier.height(12.dp))
                    GradientButton("Télécharger la mise à jour", onClick = { UpdateManager.startDownload() }, height = 44.dp)
                }
                is UpdateState.Ready, is UpdateState.NeedsPermission -> {
                    Spacer(Modifier.height(12.dp))
                    GradientButton("Installer maintenant", onClick = { UpdateManager.install() }, height = 44.dp)
                }
                is UpdateState.Error -> {
                    if (s.info != null) {
                        Spacer(Modifier.height(12.dp))
                        GradientButton("Réessayer le téléchargement", onClick = { UpdateManager.startDownload() }, height = 44.dp)
                    } else {
                        Spacer(Modifier.height(12.dp))
                        CheckUpdatesButton { UpdateManager.check(auto = false) }
                    }
                }
                else -> {
                    Spacer(Modifier.height(12.dp))
                    CheckUpdatesButton { UpdateManager.check(auto = false) }
                }
            }
        }
    }
}

@Composable
private fun CheckUpdatesButton(onClick: () -> Unit) {
    val c = Zcolors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .border(1.dp, c.border2, RoundedCornerShape(50))
            .clickableRow(onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Refresh, null, tint = c.text, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Vérifier les mises à jour",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            color = c.text
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
