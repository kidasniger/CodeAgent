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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.ModelCatalog
import com.zagent.app.data.ModelInfo
import com.zagent.app.ui.components.Lot5Screen
import com.zagent.app.ui.components.Lot5Section
import com.zagent.app.ui.components.SpeedDots
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA59 — Écran Modèles : carte par modèle (nom, description, fenêtre de contexte,
// prix configurés KEY_PRICE_IN/OUT, vitesse ●●○, badge « Recommandé »), sélection
// au tap et « Modèle personnalisé » avec champ base URL.
// Le catalogue vit dans com.zagent.app.data.ModelCatalog (GLM z.ai + NVIDIA NIM,
// chaque modèle portant l'URL de son fournisseur).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ModelsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    var customModel by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf(viewModel.baseUrlInput) }

    Lot5Screen(title = "Modèles", subtitle = "Actif : " + viewModel.effectiveModel, onBack = onBack) {
        val priceText = if (viewModel.priceInInput <= 0f && viewModel.priceOutInput <= 0f)
            "Prix non configurés"
        else
            "Entrée " + formatPrice(viewModel.priceInInput) + " · Sortie " + formatPrice(viewModel.priceOutInput) + " / M tokens"

        Lot5Section("Catalogue — discussion & code")
        ModelCatalog.all.filter { !it.specialized }.forEach { m ->
            ModelCard(
                model = m,
                selected = viewModel.effectiveModel == m.id,
                priceText = priceText,
                onSelect = {
                    viewModel.selectGlobalModel(m.id)
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Lot5Section("Spécialisés (pas pour discuter)")
        Text(
            "Modération, extraction de documents, traduction, calibration — fournis par NVIDIA NIM, hors usage agent.",
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp),
            color = c.text2
        )
        Spacer(Modifier.height(8.dp))
        ModelCatalog.all.filter { it.specialized }.forEach { m ->
            ModelCard(
                model = m,
                selected = viewModel.effectiveModel == m.id,
                priceText = priceText,
                onSelect = {
                    viewModel.selectGlobalModel(m.id)
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Lot5Section("Modèle personnalisé")
        androidx.compose.material3.OutlinedTextField(
            value = customModel,
            onValueChange = { customModel = it },
            placeholder = { Text("identifiant du modèle (ex : mon-org/mon-modele)", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp), color = c.text3) },
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            placeholder = { Text("URL de base de l'API (chat/completions)", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp), color = c.text3) },
            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp, color = c.text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Le modèle personnalisé est enregistré comme réglage global. Les prix par million de tokens restent ceux de Réglages.",
            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp),
            color = c.text2
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.surface2)
                    .border(1.dp, c.border2, RoundedCornerShape(50))
                    .clickable {
                        if (customModel.isNotBlank()) {
                            viewModel.selectGlobalModel(customModel.trim())
                            viewModel.saveBaseUrl(customUrl.trim())
                            customModel = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Appliquer le modèle",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                    color = if (customModel.isNotBlank()) c.text else c.text3
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Lot5Section("Notes")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, tint = c.amber, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Astuce : le chip modèle du champ de saisie change le modèle de la conversation en cours seulement.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                color = c.text2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModelCard(model: ModelInfo, selected: Boolean, priceText: String, onSelect: () -> Unit) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.cyanBg else c.surface)
            .border(
                1.dp,
                if (selected) c.cyan else c.border,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) c.cyanBg else c.violetBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Tune, null,
                tint = if (selected) c.cyan else c.violet,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    model.label,
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = c.text,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                if (model.recommended) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.amberBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Recommandé",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                            color = c.amber
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                if (selected) {
                    Icon(Icons.Default.Check, "Sélectionné", tint = c.cyan, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                model.description,
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp),
                color = c.text2
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(model.id)
                Spacer(Modifier.width(10.dp))
                MonoLabel(model.contextWindow)
                Spacer(Modifier.width(10.dp))
                SpeedDots(model.speed)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                priceText,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                color = c.text3
            )
        }
    }
}

@Composable
private fun MonoLabel(text: String) {
    val c = Zcolors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
            color = c.text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatPrice(v: Float): String =
    if (v <= 0f) "—" else "$" + String.format(Locale.US, "%.2f", v)
