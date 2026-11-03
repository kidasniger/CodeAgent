package com.zagent.app.ui.editor

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.makeQrBitmap
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString

// ─────────────────────────────────────────────────────────────────────────────
// MA62 — Aperçu plein écran : WebView plein cadre avec barre fine (recharger,
// URL, appareils téléphone / tablette / bureau, rotation, ouvrir dans le
// navigateur, QR code de l'adresse locale) et console JS repliable en bas.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PreviewScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var reloadTick by remember { mutableStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    var showConsole by remember { mutableStateOf(false) }
    var landscape by remember { mutableStateOf(false) }
    // 0 = téléphone, 1 = tablette, 2 = bureau
    var device by remember { mutableStateOf(0) }
    val deviceWidths = listOf(390, 834, 1280)

    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
    ) {
        // ── Barre fine du haut ───────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BarIcon(Icons.Default.Close, "Fermer l'aperçu") { onClose() }
            BarIcon(Icons.Default.Refresh, "Recharger") { reloadTick++ }
            Column(Modifier.weight(1f)) {
                // MA88 : voyant du serveur local devant l'adresse.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (currentUrl.startsWith("http")) c.green else c.red)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        viewModel.previewScreenPath.ifBlank { "aperçu" },
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text,
                        maxLines = 1
                    )
                }
                Text(
                    currentUrl.ifBlank { "…" },
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 9.5.sp),
                    color = c.text3,
                    maxLines = 1
                )
            }
            BarIcon(Icons.Default.ScreenRotation, "Pivoter") {
                landscape = !landscape
                (context as? Activity)?.requestedOrientation =
                    if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            BarIcon(Icons.Default.OpenInNew, "Ouvrir dans le navigateur") {
                if (currentUrl.startsWith("http")) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(currentUrl))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
            }
            // MA88 : copier l'adresse locale.
            BarIcon(Icons.Default.ContentCopy, "Copier l'URL") {
                if (currentUrl.startsWith("http")) {
                    clipboard.setText(AnnotatedString(currentUrl))
                    Toast.makeText(context, "URL copiée", Toast.LENGTH_SHORT).show()
                }
            }
            BarIcon(Icons.Default.QrCode2, "QR code de l'adresse") {
                if (currentUrl.startsWith("http")) showQr = true
            }
        }
        // Sélecteur d'appareil : trois chips fines sous la barre
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Téléphone", "Tablette", "Bureau").forEachIndexed { i, label ->
                val active = device == i
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) c.cyanBg else c.surface2)
                        .border(
                            1.dp,
                            if (active) c.cyan else c.border,
                            RoundedCornerShape(50)
                        )
                        .clickable { device = i }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        label,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (active) c.cyan else c.text2
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BarIcon(Icons.Default.Terminal, "Console JS") { showConsole = !showConsole }
        }

        // ── WebView, cadré selon l'appareil choisi ───────────────────────────
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxWidthDp = maxWidth
            val target = deviceWidths[device]
            val frame: Modifier = if (target >= maxWidthDp.value.toInt()) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxHeight()
                    .width(target.dp)
                    .align(Alignment.TopCenter)
                    .border(1.dp, c.border2, RoundedCornerShape(0.dp))
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                PreviewPane(
                    path = viewModel.previewScreenPath,
                    reloadKey = viewModel.previewTick + reloadTick + 100000,
                    readResource = viewModel.editor::readResource,
                    console = viewModel.console,
                    onUrl = { currentUrl = it },
                    modifier = frame,
                    quickBar = false // MA88 : la barre dédiée de cet écran remplace la pilule.
                )
            }
        }

        // ── Console JS repliable ─────────────────────────────────────────────
        if (showConsole) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(c.bg2)
                    .border(1.dp, c.border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Console" + (if (viewModel.console.errorCount > 0) " · " + viewModel.console.errorCount + " erreur(s)" else ""),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                        color = if (viewModel.console.errorCount > 0) c.red else c.text2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Replier",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = c.cyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showConsole = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                ConsolePanel(viewModel.console, Modifier.fillMaxSize())
            }
        }
    }

    if (showQr) {
        val bmp = remember(currentUrl) { makeQrBitmap(currentUrl) }
        GlassAlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("Scanner depuis un autre appareil", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.text) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR code de l'adresse locale", modifier = Modifier.size(220.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentUrl,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = c.text2
                    )
                }
            },
            confirmButton = {
                Text(
                    "Fermer",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = c.cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showQr = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        )
    }
}

@Composable
private fun BarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = c.text2, modifier = Modifier.size(19.dp))
    }
}
