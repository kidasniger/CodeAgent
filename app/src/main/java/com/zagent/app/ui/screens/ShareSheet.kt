package com.zagent.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState as m3SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassModalBottomSheet
import com.zagent.app.ui.components.Lot5Row
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api

// ─────────────────────────────────────────────────────────────────────────────
// MA70 — Feuille d'export / partage de conversation : Copier en Markdown,
// Enregistrer en .md, Exporter en PDF, Partager via Intent + options
// « Inclure les appels d'outils » et « Masquer les chemins locaux ».
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var includeTools by remember { mutableStateOf(false) }
    var hidePaths by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }

    val sheetState = m3SheetState(skipPartiallyExpanded = true)

    val saveMd = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportConversationMarkdown(uri, includeTools, hidePaths) { ok ->
                notice = if (ok) "Conversation enregistrée en .md" else "Échec de l'enregistrement"
            }
        }
    }

    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportConversationPdf(uri, includeTools, hidePaths) { ok ->
                notice = if (ok) "PDF exporté" else "Échec de l'export PDF"
            }
        }
    }

    GlassModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Partager la conversation",
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
                color = c.text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Le contenu est converti en Markdown, prêt à relire ou archiver.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                color = c.text2
            )
            Spacer(Modifier.height(12.dp))

            // ── Options ──────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    ToggleMini("Inclure les appels d'outils", includeTools) { includeTools = it }
                    ToggleMini("Masquer les chemins locaux", hidePaths) { hidePaths = it }
                }
            }
            Spacer(Modifier.height(12.dp))

            Lot5Row(
                icon = Icons.Default.ContentCopy,
                title = "Copier en Markdown",
                subtitle = "Le texte complet dans le presse-papiers",
                onClick = {
                    clipboard.setText(AnnotatedString(viewModel.buildConversationMarkdown(includeTools, hidePaths)))
                    notice = "Markdown copié"
                }
            )
            Lot5Row(
                icon = Icons.Default.Save,
                title = "Enregistrer en .md",
                subtitle = "Choisir l'emplacement puis sauvegarder",
                onClick = { saveMd.launch("conversation-zagent.md") }
            )
            Lot5Row(
                icon = Icons.Default.Description,
                title = "Exporter en PDF",
                subtitle = "Document paginé, lisible partout",
                tint = c.red,
                onClick = { savePdf.launch("conversation-zagent.pdf") }
            )
            Lot5Row(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Partager via…",
                subtitle = "Intent Android (mail, notes, messageries)",
                tint = c.green,
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Conversation ZAgent")
                        putExtra(Intent.EXTRA_TEXT, viewModel.buildConversationMarkdown(includeTools, hidePaths))
                    }
                    context.startActivity(Intent.createChooser(send, "Partager la conversation"))
                }
            )

            if (notice != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    notice.orEmpty(),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                    color = c.green
                )
            }
        }
    }
}

@Composable
private fun ToggleMini(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
            color = c.text,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) c.cyan else c.surface3)
                .border(1.dp, if (checked) c.cyan else c.border2, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text("✓", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = c.onGrad)
            }
        }
    }
}
