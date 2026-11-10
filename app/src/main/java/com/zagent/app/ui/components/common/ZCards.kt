package com.zagent.app.ui.components.common

// corr-011 — Famille extraite de ZComponents.kt (55 Ko) : ZEmptyKind, ZEmptyState, EmptyState, GlassCard, GradientProgress, CardDivider, GlassPreviewCard, RectangleSquelette, SkeletonListe, SkeletonListeHistorique, SkeletonListeFichiers, SkeletonListeProjets, SkeletonCartesModeles.
// Même package, API inchangée : aucun écran n'est modifié.

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.glassSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.blur
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
import com.zagent.app.ui.theme.Zshapes
import com.zagent.app.data.HapticLevel
import com.zagent.app.ui.theme.LocalHapticNiveau
import com.zagent.app.ui.theme.LocalReduceMotion
import com.zagent.app.ui.theme.ZMouvement
import kotlin.math.cos
import kotlin.math.sin

/** Variantes d'illustration d'état vide (MA87). */
enum class ZEmptyKind { FOLDER, RESULTS, OFFLINE, CONVERSATION }

/**
 * État vide illustré (MA87) : dessin au trait (trait continu, accent unique)
 * pour un dossier vide, un résultat de recherche infructueux, un problème de
 * connexion ou un historique vide, avec titre et description.
 */
@Composable
fun ZEmptyState(
    kind: ZEmptyKind,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = Zcolors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()
            val gray = c.text3
            val graySoft = c.text3.copy(alpha = 0.45f)
            when (kind) {
                ZEmptyKind.FOLDER -> {
                    // Languette du dossier
                    drawRoundRect(
                        color = graySoft,
                        topLeft = Offset(w * 0.22f, h * 0.26f),
                        size = Size(w * 0.18f, h * 0.12f),
                        cornerRadius = CornerRadius(stroke * 2),
                        style = Stroke(stroke)
                    )
                    // Corps du dossier
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.22f, h * 0.38f),
                        size = Size(w * 0.56f, h * 0.36f),
                        cornerRadius = CornerRadius(stroke * 2),
                        style = Stroke(stroke)
                    )
                    // Accent : point au centre du dossier vide
                    drawCircle(color = c.cyan, radius = stroke * 2.2f, center = Offset(w * 0.5f, h * 0.56f))
                }
                ZEmptyKind.RESULTS -> {
                    // Loupe (contour gris)
                    val r = h * 0.26f
                    val cx = w * 0.44f
                    val cy = h * 0.42f
                    drawCircle(color = gray, radius = r, center = Offset(cx, cy), style = Stroke(stroke))
                    // Accent : manche de la loupe
                    drawLine(
                        color = c.cyan,
                        start = Offset(cx + r * 0.72f, cy + r * 0.72f),
                        end = Offset(cx + r * 1.65f, cy + r * 1.65f),
                        strokeWidth = stroke * 1.25f,
                        cap = StrokeCap.Round
                    )
                    // Point d'interrogation suggéré par deux traits
                    drawLine(color = graySoft, start = Offset(cx - r * 0.28f, cy - r * 0.30f), end = Offset(cx + r * 0.22f, cy - r * 0.30f), strokeWidth = stroke)
                    drawLine(color = graySoft, start = Offset(cx - r * 0.05f, cy + r * 0.16f), end = Offset(cx - r * 0.05f, cy + r * 0.30f), strokeWidth = stroke)
                }
                ZEmptyKind.OFFLINE -> {
                    // Nuage : pilule + deux bosses
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.24f, h * 0.42f),
                        size = Size(w * 0.52f, h * 0.22f),
                        cornerRadius = CornerRadius(h * 0.11f),
                        style = Stroke(stroke)
                    )
                    drawCircle(color = gray, radius = h * 0.13f, center = Offset(w * 0.38f, h * 0.42f), style = Stroke(stroke))
                    drawCircle(color = gray, radius = h * 0.10f, center = Offset(w * 0.56f, h * 0.42f), style = Stroke(stroke))
                    // Accent : barre diagonale (connexion coupée)
                    drawLine(
                        color = c.red,
                        start = Offset(w * 0.28f, h * 0.78f),
                        end = Offset(w * 0.72f, h * 0.22f),
                        strokeWidth = stroke * 1.25f,
                        cap = StrokeCap.Round
                    )
                }
                ZEmptyKind.CONVERSATION -> {
                    // Bulle de dialogue
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.26f, h * 0.24f),
                        size = Size(w * 0.48f, h * 0.36f),
                        cornerRadius = CornerRadius(stroke * 3),
                        style = Stroke(stroke)
                    )
                    // Queue de la bulle
                    val tail = Path().apply {
                        moveTo(w * 0.38f, h * 0.60f)
                        lineTo(w * 0.34f, h * 0.76f)
                        lineTo(w * 0.48f, h * 0.60f)
                        close()
                    }
                    drawPath(tail, color = gray, style = Stroke(stroke, join = StrokeJoin.Round))
                    // Trois points, celui du centre en accent
                    drawCircle(color = graySoft, radius = stroke * 1.3f, center = Offset(w * 0.40f, h * 0.42f))
                    drawCircle(color = c.cyan, radius = stroke * 1.6f, center = Offset(w * 0.50f, h * 0.42f))
                    drawCircle(color = graySoft, radius = stroke * 1.3f, center = Offset(w * 0.60f, h * 0.42f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = c.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = c.text2,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(Zshapes.pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(actionText, color = c.cyan, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * État vide illustré (MA87) : dessin au trait (trait continu, accent unique)
 * pour un dossier vide, un résultat de recherche infructueux, un problème de
 * connexion ou un historique vide, avec titre et description.
 */
@Composable
fun ZEmptyState(
    kind: ZEmptyKind,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = Zcolors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()
            val gray = c.text3
            val graySoft = c.text3.copy(alpha = 0.45f)
            when (kind) {
                ZEmptyKind.FOLDER -> {
                    // Languette du dossier
                    drawRoundRect(
                        color = graySoft,
                        topLeft = Offset(w * 0.22f, h * 0.26f),
                        size = Size(w * 0.18f, h * 0.12f),
                        cornerRadius = CornerRadius(stroke * 2),
                        style = Stroke(stroke)
                    )
                    // Corps du dossier
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.22f, h * 0.38f),
                        size = Size(w * 0.56f, h * 0.36f),
                        cornerRadius = CornerRadius(stroke * 2),
                        style = Stroke(stroke)
                    )
                    // Accent : point au centre du dossier vide
                    drawCircle(color = c.cyan, radius = stroke * 2.2f, center = Offset(w * 0.5f, h * 0.56f))
                }
                ZEmptyKind.RESULTS -> {
                    // Loupe (contour gris)
                    val r = h * 0.26f
                    val cx = w * 0.44f
                    val cy = h * 0.42f
                    drawCircle(color = gray, radius = r, center = Offset(cx, cy), style = Stroke(stroke))
                    // Accent : manche de la loupe
                    drawLine(
                        color = c.cyan,
                        start = Offset(cx + r * 0.72f, cy + r * 0.72f),
                        end = Offset(cx + r * 1.65f, cy + r * 1.65f),
                        strokeWidth = stroke * 1.25f,
                        cap = StrokeCap.Round
                    )
                    // Point d'interrogation suggéré par deux traits
                    drawLine(color = graySoft, start = Offset(cx - r * 0.28f, cy - r * 0.30f), end = Offset(cx + r * 0.22f, cy - r * 0.30f), strokeWidth = stroke)
                    drawLine(color = graySoft, start = Offset(cx - r * 0.05f, cy + r * 0.16f), end = Offset(cx - r * 0.05f, cy + r * 0.30f), strokeWidth = stroke)
                }
                ZEmptyKind.OFFLINE -> {
                    // Nuage : pilule + deux bosses
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.24f, h * 0.42f),
                        size = Size(w * 0.52f, h * 0.22f),
                        cornerRadius = CornerRadius(h * 0.11f),
                        style = Stroke(stroke)
                    )
                    drawCircle(color = gray, radius = h * 0.13f, center = Offset(w * 0.38f, h * 0.42f), style = Stroke(stroke))
                    drawCircle(color = gray, radius = h * 0.10f, center = Offset(w * 0.56f, h * 0.42f), style = Stroke(stroke))
                    // Accent : barre diagonale (connexion coupée)
                    drawLine(
                        color = c.red,
                        start = Offset(w * 0.28f, h * 0.78f),
                        end = Offset(w * 0.72f, h * 0.22f),
                        strokeWidth = stroke * 1.25f,
                        cap = StrokeCap.Round
                    )
                }
                ZEmptyKind.CONVERSATION -> {
                    // Bulle de dialogue
                    drawRoundRect(
                        color = gray,
                        topLeft = Offset(w * 0.26f, h * 0.24f),
                        size = Size(w * 0.48f, h * 0.36f),
                        cornerRadius = CornerRadius(stroke * 3),
                        style = Stroke(stroke)
                    )
                    // Queue de la bulle
                    val tail = Path().apply {
                        moveTo(w * 0.38f, h * 0.60f)
                        lineTo(w * 0.34f, h * 0.76f)
                        lineTo(w * 0.48f, h * 0.60f)
                        close()
                    }
                    drawPath(tail, color = gray, style = Stroke(stroke, join = StrokeJoin.Round))
                    // Trois points, celui du centre en accent
                    drawCircle(color = graySoft, radius = stroke * 1.3f, center = Offset(w * 0.40f, h * 0.42f))
                    drawCircle(color = c.cyan, radius = stroke * 1.6f, center = Offset(w * 0.50f, h * 0.42f))
                    drawCircle(color = graySoft, radius = stroke * 1.3f, center = Offset(w * 0.60f, h * 0.42f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = c.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = c.text2,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(Zshapes.pill)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(actionText, color = c.cyan, style = MaterialTheme.typography.labelLarge)
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
                    .clip(Zshapes.pill)
                    .clickable(onClick = onSecondary)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(secondaryText, color = c.text2, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = Zshapes.card,
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

/** Barre de progression dégradée (téléchargement de mise à jour). */
@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier) {
    val c = Zcolors
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(Zshapes.pill)
            .background(c.surface3)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(Zshapes.pill)
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

/**
 * Aperçu isolé des surfaces Liquid Glass.
 *
 * Les paramètres sont volontairement fournis par l'écran parent : leurs changements
 * ne forcent pas la recomposition des autres sections de la page.
 */
@Composable
fun GlassPreviewCard(
    modifier: Modifier = Modifier,
    material: String = "Liquid",
    glassColor: Color = Zcolors.cyan,
    blur: Float = 0.55f,
    tint: Float = 0.35f,
    film: Float = 0.22f,
    activeTab: String = "Chat"
) {
    val c = Zcolors
    val blurLevel = blur.coerceIn(0f, 1f)
    val tintLevel = tint.coerceIn(0f, 1f)
    val filmLevel = film.coerceIn(0f, 1f)
    val surfaceAlpha = when (material.lowercase()) {
        "solid" -> 0.90f
        "frosted" -> (0.38f + blurLevel * 0.18f + filmLevel * 0.18f).coerceIn(0.38f, 0.74f)
        else -> (0.12f + blurLevel * 0.20f + filmLevel * 0.28f).coerceIn(0.12f, 0.60f)
    }
    val surfaceColor = glassColor.copy(alpha = surfaceAlpha)
    val outlineColor = glassColor.copy(
        alpha = if (material.equals("Solid", ignoreCase = true)) 0.95f
        else (0.20f + tintLevel * 0.48f).coerceIn(0.20f, 0.68f)
    )
    val tabs = listOf("Chat", "Fichiers", "Éditeur", "Terminal", "Réglages")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(Zshapes.sheet)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        glassColor.copy(alpha = 0.98f),
                        c.violet.copy(alpha = 0.72f),
                        glassColor.copy(alpha = 0.82f)
                    )
                )
            )
    ) {
        Canvas(Modifier.matchParentSize().blur((blurLevel * 10f).dp)) {
            val stripeGap = 18.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.07f + filmLevel * 0.16f),
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = 7.dp.toPx()
                )
                x += stripeGap
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.10f + blurLevel * 0.10f),
                radius = size.height * 0.58f,
                center = Offset(size.width * 0.82f, size.height * 0.16f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Zshapes.pill)
                    .background(surfaceColor)
                    .border(1.dp, outlineColor, Zshapes.pill)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Écrire un message…",
                    color = Color.White.copy(alpha = 0.88f),
                    fontFamily = Inter,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    material,
                    color = Color.White.copy(alpha = 0.78f),
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Zshapes.pill)
                    .background(surfaceColor.copy(alpha = (surfaceAlpha + 0.12f).coerceAtMost(0.72f)))
                    .border(1.dp, outlineColor.copy(alpha = 0.78f), Zshapes.pill)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(Zshapes.pill)
                            .background(
                                if (selected) Color.White.copy(alpha = 0.20f + tintLevel * 0.20f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (tab) {
                                "Fichiers" -> "Fich."
                                "Réglages" -> "Régl."
                                else -> tab
                            },
                            color = Color.White.copy(alpha = if (selected) 1f else 0.72f),
                            fontFamily = Inter,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 7.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/** Rectangle arrondi de base pour tous les squelettes (MàJ 90). */
@Composable
fun RectangleSquelette(
    modifier: Modifier = Modifier,
    forme: Shape = RoundedCornerShape(12.dp),
    couleur: Color? = null
) {
    val c = Zcolors
    Box(
        modifier
            .clip(forme)
            .background(couleur ?: c.surface2)
            .shimmer(forme)
    )
}

/**
 * Squelette générique de liste (MàJ 90) : [lignes] rangées « icône + deux lignes
 * de texte ». Remplace les spinners pour l'historique, les projets et les fichiers.
 */
@Composable
fun SkeletonListe(
    modifier: Modifier = Modifier,
    lignes: Int = 6,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    Column(
        modifier.fillMaxWidth().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(lignes) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RectangleSquelette(modifier = Modifier.size(38.dp), forme = CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RectangleSquelette(modifier = Modifier.fillMaxWidth(0.55f).height(13.dp))
                    RectangleSquelette(
                        modifier = Modifier.fillMaxWidth(0.85f).height(11.dp),
                        forme = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

/** Squelette de l'historique de conversations (MàJ 90). */
@Composable
fun SkeletonListeHistorique(modifier: Modifier = Modifier, lignes: Int = 6) {
    SkeletonListe(modifier = modifier, lignes = lignes)
}

/** Squelette de la liste de fichiers (MàJ 90). */
@Composable
fun SkeletonListeFichiers(modifier: Modifier = Modifier, lignes: Int = 7) {
    SkeletonListe(modifier = modifier, lignes = lignes)
}

/** Squelette de la liste de projets (MàJ 90) : grandes cartes empilées. */
@Composable
fun SkeletonListeProjets(modifier: Modifier = Modifier, lignes: Int = 4) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(lignes) {
            RectangleSquelette(
                modifier = Modifier.fillMaxWidth().height(76.dp),
                forme = RoundedCornerShape(20.dp)
            )
        }
    }
}

/** Squelette des cartes de modèles (MàJ 90) : pilules en défilement horizontal. */
@Composable
fun SkeletonCartesModeles(modifier: Modifier = Modifier, cartes: Int = 3) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(cartes) {
            RectangleSquelette(
                modifier = Modifier.size(width = 132.dp, height = 46.dp),
                forme = RoundedCornerShape(999.dp)
            )
        }
    }
}
