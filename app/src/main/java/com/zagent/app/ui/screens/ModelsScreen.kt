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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.zagent.app.data.Capacite
import com.zagent.app.data.Fournisseur
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
// MàJ 101 — les modèles sont regroupés par fournisseur (z.ai, NVIDIA NIM,
// passerelles personnalisées), avec l'état de la clé de chaque API.
// MàJ 102 — chaque carte porte ses badges de capacité (Texte, Code, Vision,
// Raisonnement…) et une étoile pour épingler le modèle en favori.
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

        val fournisseurs = viewModel.fournisseurs
        fournisseurs.forEach { f ->
            val modelesFournisseur = ModelCatalog.all.filter { it.baseUrl == f.baseUrl }
            if (modelesFournisseur.isEmpty()) return@forEach
            val conversationnels = modelesFournisseur.filter { !it.specialized }
            val specialises = modelesFournisseur.filter { it.specialized }

            Lot5Section(f.nom)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusCleFournisseur(f)
            }
            Spacer(Modifier.height(8.dp))
            conversationnels.forEach { m ->
                ModelCard(
                    model = m,
                    selected = viewModel.effectiveModel == m.id,
                    favori = m.id in viewModel.modelesFavoris,
                    priceText = priceText,
                    onSelect = { viewModel.selectGlobalModel(m.id) },
                    onFavori = { viewModel.basculeFavori(m.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
            if (specialises.isNotEmpty()) {
                Text(
                    "Spécialisés " + f.nom + " — modération, extraction, traduction, calibration : hors usage agent.",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(8.dp))
                specialises.forEach { m ->
                    ModelCard(
                        model = m,
                        selected = viewModel.effectiveModel == m.id,
                        favori = m.id in viewModel.modelesFavoris,
                        priceText = priceText,
                        onSelect = { viewModel.selectGlobalModel(m.id) },
                        onFavori = { viewModel.basculeFavori(m.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
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
            "Astuce : enregistre plutôt la passerelle dans Réglages › Fournisseurs d'IA avec sa propre clé — ses modèles arriveront ici automatiquement.",
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
                "Astuce : le chip modèle du champ de saisie change le modèle de la conversation en cours seulement. Les étoiles épinglent tes modèles en tête du sélecteur.",
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                color = c.text2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Badge d'état de la clé d'un fournisseur (MàJ 101). */
@Composable
private fun StatusCleFournisseur(f: Fournisseur) {
    val c = Zcolors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (f.cle.isBlank()) c.amberBg else c.greenBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            if (f.cle.isBlank()) "Clé absente — Réglages › Fournisseurs d'IA" else "Clé enregistrée",
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp),
            color = if (f.cle.isBlank()) c.amber else c.green
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    selected: Boolean,
    favori: Boolean,
    priceText: String,
    onSelect: () -> Unit,
    onFavori: () -> Unit
) {
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
                // MàJ 102 — étoile favori : épingle le modèle en tête des sélecteurs.
                Icon(
                    if (favori) Icons.Default.Star else Icons.Default.StarBorder,
                    if (favori) "Retirer des favoris" else "Ajouter aux favoris",
                    tint = if (favori) c.amber else c.text3,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onFavori)
                        .padding(4.dp)
                        .size(16.dp)
                )
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
            Spacer(Modifier.height(6.dp))
            // MàJ 102 — badges de capacité (Texte, Code, Vision, Raisonnement…).
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                model.capacites.sortedBy { it.ordinal }.forEach { cap ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(c.violetBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            cap.libelle,
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 10.sp),
                            color = c.violet
                        )
                    }
                }
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
