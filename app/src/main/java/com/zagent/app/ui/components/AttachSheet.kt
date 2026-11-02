package com.zagent.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image as PhotoIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MA49 — Feuille « + » : grandes tuiles d'ajouts + vignettes des pièces jointes
// ─────────────────────────────────────────────────────────────────────────────

/** Feuille en verre avec 4 grandes tuiles : Fichier, Image, Dossier du projet, Coller (MA49). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachSheet(
    hasProject: Boolean,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onAttachProject: () -> Unit,
    onPaste: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = Zcolors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(28.dp), LocalContext.current)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Ajouter au message",
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                color = c.text
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Le contenu est joint au contexte de l'agent avec ton message.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                color = c.text2
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachTile(
                    icon = Icons.Default.AttachFile,
                    tint = c.cyan,
                    title = "Fichier",
                    description = "Joindre un fichier du téléphone",
                    modifier = Modifier.weight(1f),
                    onClick = onPickFile
                )
                AttachTile(
                    icon = PhotoIcon,
                    tint = c.violet,
                    title = "Image",
                    description = "Photo ou capture d'écran",
                    modifier = Modifier.weight(1f),
                    onClick = onPickImage
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachTile(
                    icon = Icons.Default.FolderOpen,
                    tint = c.amber,
                    title = "Dossier du projet",
                    description = "Arborescence complète du projet",
                    enabled = hasProject,
                    modifier = Modifier.weight(1f),
                    onClick = onAttachProject
                )
                AttachTile(
                    icon = Icons.Default.ContentPaste,
                    tint = c.green,
                    title = "Coller",
                    description = "Insérer le presse-papiers",
                    modifier = Modifier.weight(1f),
                    onClick = onPaste
                )
            }
        }
    }
}

/** Grande tuile de la feuille d'ajouts : icône teintée, titre et description courte. */
@Composable
private fun AttachTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = Zcolors
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface2)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            description,
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 14.sp),
            color = c.text3,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Vignettes retirables des pièces jointes, affichées au-dessus du champ (MA49). */
@Composable
fun AttachmentThumbsRow(
    attachments: List<MainViewModel.ChatAttachment>,
    onRemove: (Long) -> Unit
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { a ->
            Box {
                if (a.isImage && a.uri != null) {
                    AsyncImage(
                        model = a.uri,
                        contentDescription = a.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, c.border2, RoundedCornerShape(12.dp))
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surface3)
                            .border(1.dp, c.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            null,
                            tint = c.text2,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            a.name,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                            color = c.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                }
                // Bouton de retrait, en haut à droite de la vignette
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(c.scrim)
                        .clickable { onRemove(a.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Retirer " + a.name,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
