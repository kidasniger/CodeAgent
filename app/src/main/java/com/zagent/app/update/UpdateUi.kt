package com.zagent.app.update

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
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

@Composable
private fun AvailableDialog(s: UpdateState.Available) {
    AlertDialog(
        onDismissRequest = { UpdateManager.dismiss(s.info.tag) },
        title = { Text("Mise à jour disponible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${s.info.tag.removePrefix("v")} (installée : ${s.currentVersion})")
                Text("Taille : ${mb(s.info.apk.size)}")
                if (s.info.apk.sha256 == null) {
                    Text(
                        "Empreinte SHA-256 non publiée : intégrité vérifiée par la taille et la signature Android.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    s.info.notes,
                    modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = { TextButton(onClick = { UpdateManager.startDownload() }) { Text("Télécharger") } },
        dismissButton = { TextButton(onClick = { UpdateManager.dismiss(s.info.tag) }) { Text("Plus tard") } }
    )
}

@Composable
private fun DownloadingDialog(s: UpdateState.Downloading) {
    AlertDialog(
        onDismissRequest = { UpdateManager.dismiss(s.info.tag) },
        title = { Text("Téléchargement en cours") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text(
                    if (s.total > 0) "${s.percent} % · ${mb(s.downloaded)} / ${mb(s.total)}" else mb(s.downloaded)
                )
            }
        },
        confirmButton = { TextButton(onClick = { UpdateManager.dismiss(s.info.tag) }) { Text("Masquer") } },
        dismissButton = { TextButton(onClick = { UpdateManager.cancelDownload() }) { Text("Annuler") } }
    )
}

@Composable
private fun ReadyDialog(s: UpdateState.Ready) {
    AlertDialog(
        onDismissRequest = { UpdateManager.dismiss(s.info.tag) },
        title = { Text("Mise à jour prête") },
        text = { Text("La version ${s.info.tag.removePrefix("v")} est téléchargée et vérifiée. Android va te demander de confirmer l'installation.") },
        confirmButton = { TextButton(onClick = { UpdateManager.install() }) { Text("Installer") } },
        dismissButton = { TextButton(onClick = { UpdateManager.dismiss(s.info.tag) }) { Text("Plus tard") } }
    )
}

@Composable
private fun ErrorDialog(s: UpdateState.Error, info: ReleaseInfo) {
    AlertDialog(
        onDismissRequest = { UpdateManager.dismiss(info.tag) },
        title = { Text("Mise à jour impossible") },
        text = { Text(s.message) },
        confirmButton = { TextButton(onClick = { UpdateManager.startDownload() }) { Text("Réessayer") } },
        dismissButton = { TextButton(onClick = { UpdateManager.dismiss(info.tag) }) { Text("Fermer") } }
    )
}

/** Carte à placer dans l'écran des réglages : version installée + vérification manuelle. */
@Composable
fun UpdateSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    remember { UpdateManager.init(context) }
    val state by UpdateManager.state.collectAsState()

    Card(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mises à jour", style = MaterialTheme.typography.titleMedium)
            Text("Version installée : ${UpdateManager.currentVersionName()}", style = MaterialTheme.typography.bodyMedium)

            val status = when (val s = state) {
                UpdateState.Idle -> "Aucune vérification effectuée."
                UpdateState.Checking -> "Vérification en cours…"
                is UpdateState.UpToDate -> "L'application est à jour."
                is UpdateState.Available -> "Nouvelle version ${s.info.tag.removePrefix("v")} disponible (${mb(s.info.apk.size)})."
                is UpdateState.Downloading -> "Téléchargement : ${s.percent} %"
                is UpdateState.Ready -> "Mise à jour téléchargée et vérifiée."
                is UpdateState.NeedsPermission -> "Autorise l'installation dans les paramètres, puis reviens dans l'application."
                is UpdateState.Error -> s.message
            }
            Text(status, style = MaterialTheme.typography.bodyMedium)

            when (val s = state) {
                is UpdateState.Available -> Button(onClick = { UpdateManager.startDownload() }) { Text("Télécharger") }
                is UpdateState.Ready -> Button(onClick = { UpdateManager.install() }) { Text("Installer") }
                is UpdateState.NeedsPermission -> Button(onClick = { UpdateManager.install() }) { Text("Installer") }
                is UpdateState.Downloading -> LinearProgressIndicator(
                    progress = { s.percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                UpdateState.Checking -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UpdateState.Error -> {
                    if (s.info != null) {
                        Button(onClick = { UpdateManager.startDownload() }) { Text("Réessayer") }
                    } else {
                        OutlinedButton(onClick = { UpdateManager.check(auto = false) }) { Text("Vérifier les mises à jour") }
                    }
                }
                else -> OutlinedButton(onClick = { UpdateManager.check(auto = false) }) { Text("Vérifier les mises à jour") }
            }
        }
    }
}