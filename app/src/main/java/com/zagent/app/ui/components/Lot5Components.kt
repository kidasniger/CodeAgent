package com.zagent.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors

// ─────────────────────────────────────────────────────────────────────────────
// Kit d'interface partagé du LOT 5 (MA56 → MA70) : squelette d'écran plein cadre,
// onglets en chips, état vide, ligne réglage, champ de recherche, vitesse modèle,
// rendu QR (ZXing). Style Minimal/Claude : calme, chaud, peu de bordures.
// ─────────────────────────────────────────────────────────────────────────────

/** Squelette des nouveaux écrans plein cadre : barre fine retour + titre, contenu scrollable. */
@Composable
fun Lot5Screen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Zcolors
    Column(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = c.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            actions()
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            content = content
        )
    }
}

/** Réceptacle des actions de la barre de titre : le vrai RowScope de foundation. */
// (plus besoin d'un wrapper dédié)

/** Onglets du LOT 5 : chips égales, style sobre, sans bordure marquée (MA58/MA61). */
@Composable
fun Lot5Tabs(tabs: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = Zcolors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) c.bg2 else Color.Transparent)
                    .border(
                        1.dp,
                        if (active) c.border2 else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontFamily = Inter,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.5.sp
                    ),
                    color = if (active) c.text else c.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** État vide illustré du LOT 5 (MA57 et autres écrans). */
@Composable
fun Lot5EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    val c = Zcolors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(c.cyanBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.cyan, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = c.text
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp),
            color = c.text2
        )
    }
}

/** Ligne réglage générique : icône teintée, titre, sous-titre, action finale. */
@Composable
fun Lot5Row(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Zcolors.cyan,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (tint == c.cyan) c.cyanBg else tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.5.sp),
                color = c.text
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Champ de recherche arrondi, discret (MA56/MA58). */
@Composable
fun Lot5SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = TextStyle(fontFamily = Inter, fontSize = 14.sp), color = c.text3) },
        textStyle = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, color = c.text),
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.bg2,
            unfocusedContainerColor = c.bg2,
            focusedBorderColor = c.cyan,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = c.cyan
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Vitesse d'un modèle : trois points, pleins jusqu'à [level] (MA59). */
@Composable
fun SpeedDots(level: Int, modifier: Modifier = Modifier) {
    val c = Zcolors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(6.dp)
                    .background(if (i < level) c.cyan else c.surface3, CircleShape)
            )
        }
    }
}

/** Génère un bitmap QR (noir sur transparent) pour [content] ; null en cas d'échec (MA62). */
fun makeQrBitmap(content: String, sizePx: Int = 512): Bitmap? = runCatching {
    val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
    val matrix = com.google.zxing.qrcode.QRCodeWriter()
        .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bmp
}.getOrNull()

/** Carte à deux couleurs du graphique d'usage (MA60) : barres cyan, total en violet, tooltip au tap. */
@Composable
fun UsageBarChart(
    bars: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    tapped: (Int) -> Unit = {},
    selectedIndex: Int = -1
) {
    val c = Zcolors
    val maxV = (bars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(0.0001f)
    Canvas(
        modifier.pointerInput(bars) {
            detectTapGestures { offset ->
                if (bars.isEmpty()) return@detectTapGestures
                val n = bars.size
                val gap = size.width * 0.02f
                val barW = (size.width - gap * (n - 1)) / n
                val i = (offset.x / (barW + gap)).toInt().coerceIn(0, n - 1)
                tapped(i)
            }
        }
    ) {
        if (bars.isEmpty()) return@Canvas
        val n = bars.size
        val gap = size.width * 0.02f
        val barW = (size.width - gap * (n - 1)) / n
        bars.forEachIndexed { i, (_, v) ->
            val h = (v / maxV) * size.height * 0.92f
            val x = i * (barW + gap)
            drawRoundRect(
                color = if (i == selectedIndex) c.violet else c.cyan,
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.25f, barW * 0.25f)
            )
        }
    }
}

/** Titre de section discret réutilisé par les écrans LOT 5. */
@Composable
fun Lot5Section(text: String, modifier: Modifier = Modifier) {
    SectionLabel(text, modifier)
    Spacer(Modifier.height(8.dp))
}

/** Ligne mono discrète pour afficher du code / des chemins (MA58, MA61). */
@Composable
fun MonoLine(text: String, color: Color, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        text,
        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp, lineHeight = 16.sp),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
