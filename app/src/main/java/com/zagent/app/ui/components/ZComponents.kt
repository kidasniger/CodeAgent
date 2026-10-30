package com.zagent.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Logo Z (dessiné en Canvas : Z dégradé + étincelle + curseur terminal)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ZLogo(modifier: Modifier = Modifier, progress: Float = 1f, sparkAlpha: Float = 1f) {
    val c = Zcolors
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val strokeW = s * 0.155f
        val brush = Brush.linearGradient(
            colors = listOf(c.cyan, c.violet),
            start = Offset(s * 0.2f, s * 0.2f),
            end = Offset(s * 0.8f, s * 0.85f)
        )
        val cap = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Segments du Z : barre haute, diagonale, barre basse
        val segments = listOf(
            listOf(Offset(s * 0.26f, s * 0.24f), Offset(s * 0.74f, s * 0.24f)),
            listOf(Offset(s * 0.74f, s * 0.24f), Offset(s * 0.26f, s * 0.76f)),
            listOf(Offset(s * 0.26f, s * 0.76f), Offset(s * 0.60f, s * 0.76f))
        )
        val total = segments.size
        segments.forEachIndexed { i, seg ->
            val a = seg[0]
            val b = seg[1]
            val localStart = (progress * total - i).coerceIn(0f, 1f)
            if (localStart > 0f) {
                val end = Offset(
                    a.x + (b.x - a.x) * localStart,
                    a.y + (b.y - a.y) * localStart
                )
                val p = Path().apply { moveTo(a.x, a.y); lineTo(end.x, end.y) }
                drawPath(p, brush, style = cap)
            }
        }

        // Curseur terminal (petit bloc cyan en bas à droite)
        if (progress > 0.92f) {
            drawRoundRect(
                color = c.cyan,
                topLeft = Offset(s * 0.68f, s * 0.76f - strokeW * 0.5f),
                size = Size(strokeW * 0.55f, strokeW),
                cornerRadius = CornerRadius(strokeW * 0.27f)
            )
        }

        // Étincelle 4 branches (violet) en haut à droite
        if (sparkAlpha > 0f) {
            val cx = s * 0.82f
            val cy = s * 0.16f
            val r1 = s * 0.085f
            val r2 = s * 0.032f
            val spark = Path().apply {
                var angle = -Math.PI / 2.0
                for (k in 0 until 8) {
                    val rad = (if (k % 2 == 0) r1 else r2).toDouble()
                    val x = cx + rad * cos(angle)
                    val y = cy + rad * sin(angle)
                    if (k == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
                    angle += Math.PI / 4.0
                }
                close()
            }
            drawPath(spark, c.violet.copy(alpha = sparkAlpha))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fond ambiant : blobs dégradés + grille technique discrète
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val c = Zcolors
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Blobs radiaux
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.cyan.copy(alpha = if (c.isDark) 0.10f else 0.14f), Color.Transparent),
                center = Offset(w * 0.12f, h * 0.05f),
                radius = w * 0.9f
            ),
            center = Offset(w * 0.12f, h * 0.05f),
            radius = w * 0.9f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.violet.copy(alpha = if (c.isDark) 0.13f else 0.10f), Color.Transparent),
                center = Offset(w * 0.95f, h * 0.92f),
                radius = w * 1.0f
            ),
            center = Offset(w * 0.95f, h * 0.92f),
            radius = w * 1.0f
        )

        // Grille 8 dp très discrète
        val step = 40.dp.toPx()
        var x = 0f
        while (x < w) {
            drawLine(c.gridline, Offset(x, 0f), Offset(x, h), 1f)
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(c.gridline, Offset(0f, y), Offset(w, y), 1f)
            y += step
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cartes glass
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Zcolors.surface,
    borderColor: Color = Zcolors.border,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Boutons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    leading: (@Composable () -> Unit)? = null
) {
    val c = Zcolors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .then(
                if (enabled) Modifier.background(c.gradBtn)
                else Modifier.background(c.surface2).border(1.dp, c.border2, shape)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leading != null) leading()
            Text(
                text,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = if (enabled) c.onGrad else c.text2
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val c = Zcolors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.border2, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leading != null) leading()
            Text(
                text,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = c.text
            )
        }
    }
}

/** Bouton rond translucide (barre du haut). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconTint: Color = Zcolors.text,
    tintBackground: Color = Zcolors.surface2
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tintBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Libellés, badges & pastilles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, letterSpacing = 1.6.sp
        ),
        color = Zcolors.text2,
        modifier = modifier
    )
}

/** Pastille de statut : « Valide », « Agent », « ok »… */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val c = Zcolors
    val bg = when (color) {
        c.green -> c.greenBg
        c.red -> c.redBg
        c.amber -> c.amberBg
        c.cyan -> c.cyanBg
        c.violet -> c.violetBg
        else -> c.surface2
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = color
        )
    }
}

/** Badge carré arrondi contenant une icône, à teinte sémantique. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val c = Zcolors
    val bg = when (tint) {
        c.green -> c.greenBg
        c.red -> c.redBg
        c.amber -> c.amberBg
        c.cyan -> c.cyanBg
        c.violet -> c.violetBg
        else -> c.surface2
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Puce modèle en mono : glm-4.7-flash / glm-4.5-flash. */
@Composable
fun ModelChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Zcolors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    if (c.isDark) Color(0xFF2B2142) else Color(0xFFF0EBFF)
                } else c.bg2
            )
            .border(1.dp, if (selected) c.violet else c.border2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = if (selected) c.violet else c.text2,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Chip mono translucide (ex. nom de modèle dans l'app bar). */
@Composable
fun MonoChip(text: String, modifier: Modifier = Modifier, tint: Color = Zcolors.cyan) {
    val c = Zcolors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.cyanBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animations & états
// ─────────────────────────────────────────────────────────────────────────────

/** « Réflexion de l'agent ··· » — pastille animée au-dessus de la saisie. */
@Composable
fun TypingPill(label: String, modifier: Modifier = Modifier) {
    val c = Zcolors
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface2)
            .border(1.dp, c.border, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Brush.linearGradient(listOf(c.cyan, c.violet)), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = c.text
        )
        Spacer(Modifier.width(6.dp))
        Row {
            repeat(3) { i ->
                val raw = ((phase - i) % 3f)
                val d = (if (raw < 0f) raw + 3f else raw) / 3f
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(4.dp)
                        .background(c.text2.copy(alpha = 0.25f + 0.75f * (1f - d)), CircleShape)
                )
            }
        }
    }
}

/** Grand état central (vide, hors ligne…). */
@Composable
fun EmptyState(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    iconOverride: (@Composable () -> Unit)? = null
) {
    val c = Zcolors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    when (tint) {
                        c.green -> c.greenBg; c.red -> c.redBg; c.amber -> c.amberBg
                        c.cyan -> c.cyanBg; c.violet -> c.violetBg; else -> c.surface2
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconOverride != null) iconOverride()
            else Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = c.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = c.text2,
            textAlign = TextAlign.Center
        )
        if (primaryText != null && onPrimary != null) {
            Spacer(Modifier.height(24.dp))
            GradientButton(primaryText, onPrimary, modifier = Modifier.fillMaxWidth())
        }
        if (secondaryText != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSecondary)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(secondaryText, color = c.text2, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Barre de progression dégradée (téléchargement de mise à jour). */
@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier) {
    val c = Zcolors
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(c.surface3)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(c.gradBtn)
        )
    }
}

/** Ligne de séparation douce entre deux rangées d'une carte. */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Zcolors.border)
    )
}
