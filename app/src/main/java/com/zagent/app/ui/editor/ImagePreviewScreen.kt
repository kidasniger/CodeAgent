package com.zagent.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zagent.app.R
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.io.File

/**
 * feat-070 — Visionneuse d'images intégrée (png, jpg, webp, gif, bmp) : ouverte
 * depuis l'explorateur au lieu de renvoyer systématiquement vers une application
 * externe. Pincement pour zoomer, double usage du bouton externe pour partager.
 * Le SVG n'est pas décodé par BitmapFactory : il continue via « Ouvrir avec ».
 */
@Composable
fun ImagePreviewScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    val path = viewModel.imagePreviewPath
    val fichier = remember(path) {
        // Le chemin relatif du projet est résolu par le FileRepository (fileUri),
        // mais AsyncImage accepte directement un File si le projet est un dossier réel.
        viewModel.imageFileFor(path)
    }
    var echelle by remember(path) { mutableFloatStateOf(1f) }
    var offsetCam by remember(path) { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        if (fichier != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(fichier).build(),
                contentDescription = path,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = echelle.coerceIn(0.5f, 6f)
                        scaleY = echelle.coerceIn(0.5f, 6f)
                        translationY = offsetCam
                    }
                    .pointerInput(path) {
                        detectTransformGestures { _, _, zoom, _ ->
                            echelle = (echelle * zoom).coerceIn(0.5f, 6f)
                        }
                    }
            )
        } else {
            Text(
                "Image introuvable : " + path,
                color = Color.White,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Close, stringResource(R.string.action_fermer),
                tint = Color.White,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onClose() }
                    .padding(7.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                path,
                color = Color.White.copy(alpha = 0.85f),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.OpenInNew, stringResource(R.string.content_ouvrir_autre_app),
                tint = Color.White,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.openBrowserFile(path) }
                    .padding(7.dp)
            )
        }

        // Double tap = réinitialiser le zoom
        Text(
            if (echelle > 1.05f) "Zoom " + (echelle * 100).toInt() + " %" else "",
            color = Color.White.copy(alpha = 0.6f),
            style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )
    }
}