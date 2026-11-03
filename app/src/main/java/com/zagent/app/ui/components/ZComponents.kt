package com.zagent.app.ui.components

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

// ─────────────────────────────────────────────────────────────────────────
// MA86 — Fil d'Ariane cliquable
// ─────────────────────────────────────────────────────────────────────────

/**
 * Fil d'Ariane en police mono 12 sp (MA86) : chaque segment est cliquable,
 * séparé par « › ». Le dernier segment (position actuelle) est en texte fort,
 * les précédents en accent. Défilement horizontal automatique vers la fin
 * quand le chemin s'allonge.
 */
@Composable
fun ZBreadcrumb(
    segments: List<String>,
    modifier: Modifier = Modifier,
    onSegmentClick: (Int) -> Unit = { }
) {
    val c = Zcolors
    val scroll = rememberScrollState()
    LaunchedEffect(segments) {
        // Toujours afficher la fin du chemin (segment courant).
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { i, segment ->
            if (i > 0) {
                Text(
                    "›",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = c.text3,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
            }
            Text(
                segment,
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = if (i == segments.lastIndex) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (i == segments.lastIndex) c.text else c.cyan,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = i < segments.lastIndex) { onSegmentClick(i) }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// MA87 — États vides illustrés
// ─────────────────────────────────────────────────────────────────────────

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
 * Curseur commun des réglages visuels avec valeur formatée et réinitialisation contextuelle.
 */
@Composable
fun ZSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    defaultValue: Float? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Zcolors.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(format(value), color = Zcolors.text2)
                if (defaultValue != null && onReset != null && kotlin.math.abs(value - defaultValue) > 0.001f) {
                    TextButton(onClick = onReset) { Text("Réinitialiser") }
                }
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Zcolors.cyan,
                inactiveTrackColor = Zcolors.border2
            )
        )
    }
}

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
    val glass = LocalGlass.current
    val transition = rememberInfiniteTransition(label = "fond-ambiant")
    // MàJ 95 : blobs figés (état statique) quand « Réduire les animations » est actif.
    val driftAnim = if (LocalReduceMotion.current) null
    else transition.animateFloat(
        -0.12f, 0.12f,
        infiniteRepeatable(tween(26000), RepeatMode.Reverse),
        label = "deplacement"
    )
    val drift = driftAnim?.value ?: 0f
    val intensity = if (glass.enabled) 0.12f else 0.025f
    Canvas(modifier = modifier.drawWithCache {
        val w = size.width
        val h = size.height
        onDrawBehind {
            drawRect(c.bg)
            if (glass.enabled) {
                val p1 = Offset(w * (0.18f + drift), h * 0.12f)
                val p2 = Offset(w * 0.86f, h * (0.78f - drift))
                drawCircle(brush = Brush.radialGradient(listOf(c.cyan.copy(alpha = intensity), Color.Transparent), p1, w * 0.72f), radius = w * 0.72f, center = p1)
                drawCircle(brush = Brush.radialGradient(listOf(c.violet.copy(alpha = intensity * 0.85f), Color.Transparent), p2, w * 0.62f), radius = w * 0.62f, center = p2)
                val p3 = Offset(w * 0.48f, h * 0.48f)
                drawCircle(brush = Brush.radialGradient(listOf(c.amber.copy(alpha = intensity * 0.42f), Color.Transparent), p3, w * 0.38f), radius = w * 0.38f, center = p3)
            }
        }
    }, onDraw = {})
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
    val shape = Zshapes.pill
    // MàJ 92 — micro-interaction d'appui : ressort + assombrissement arrondi,
    // ripple par défaut remplacé par un highlight cohérent avec la forme.
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .pressScale(interaction, shape)
            .clip(shape)
            .then(
                if (enabled) Modifier.background(c.gradBtn)
                else Modifier.background(c.surface2).border(1.dp, c.border2, shape)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
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
    val shape = Zshapes.pill
    // MàJ 92 — micro-interaction d'appui (voir pressScale).
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .pressScale(interaction, shape)
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.border2, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
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
    size: Dp = 40.dp,
    iconTint: Color = Zcolors.text,
    tintBackground: Color = Zcolors.surface2
) {
    val context = LocalContext.current
    // MàJ 92 — micro-interaction d'appui (voir pressScale).
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, CircleShape, echelle = 0.94f)
            .glassSurface(LocalHazeState.current, LocalGlass.current, Zcolors.isDark, CircleShape, context)
            .background(tintBackground.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
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
            .clip(Zshapes.pill)
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
            .clip(MaterialTheme.shapes.small)
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
    // MàJ 92 — micro-interaction d'appui (voir pressScale).
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, MaterialTheme.shapes.small, echelle = 0.97f)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) {
                    if (c.isDark) Color(0xFF2B2142) else Color(0xFFF0EBFF)
                } else c.bg2
            )
            .border(1.dp, if (selected) c.violet else c.border2, MaterialTheme.shapes.small)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
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
            .clip(Zshapes.pill)
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

/** Étape courante : trois points animés + texte + bouton Stop intégré (MA44). */
@Composable
fun TypingPill(label: String, modifier: Modifier = Modifier, onStop: (() -> Unit)? = null) {
    val c = Zcolors
    val reduceAnim = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "dots")
    // MàJ 95 : points figés quand « Réduire les animations » est actif.
    val phase = if (reduceAnim) 0f
    else transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    ).value
    Row(
        modifier = modifier
            .clip(Zshapes.pill)
            .background(c.surface2)
            .border(1.dp, c.border, Zshapes.pill)
            .padding(start = 14.dp, end = if (onStop != null) 6.dp else 16.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = c.text
        )
        if (onStop != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(c.red.copy(alpha = 0.12f))
                    .clickable(onClick = onStop)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "Stop",
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.red
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
                    .clip(Zshapes.pill)
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

// ─────────────────────────────────────────────────────────────────────────────
// MA78 — Menu « ⋯ » contextuel unifié en verre
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Menu déroulant unifié (MA78) : conteneur en verre arrondi partagé par le chat,
 * l'éditeur, les fichiers et la barre d'onglets. À placer dans un Box parent —
 * il s'ancre sur ce parent (comme DropdownMenu). Les éléments destructifs
 * s'affichent en rouge via ZMenuItem(destructive = true), les raccourcis
 * clavier/touches apparaissent à droite en police mono.
 */
@Composable
fun ZMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        content = content
    )
}

/** Élément du menu unifié : icône + libellé, raccourci facultatif à droite, style destructif en rouge. */
@Composable
fun ZMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shortcut: String? = null,
    destructive: Boolean = false
) {
    val c = Zcolors
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = TextStyle(
                    fontFamily = Inter,
                    fontSize = 13.5.sp,
                    fontWeight = if (destructive) FontWeight.Medium else FontWeight.Normal
                ),
                color = if (destructive) c.red else c.text
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) c.red else c.text2,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (shortcut != null) {
            {
                Text(
                    shortcut,
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                    color = c.text3
                )
            }
        } else null,
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = if (destructive) c.red else c.text,
            leadingIconColor = if (destructive) c.red else c.text2,
            trailingIconColor = c.text3
        ),
        modifier = modifier
    )
}

/** Séparateur fin du menu unifié (MA78). */
@Composable
fun ZMenuSeparator() {
    val c = Zcolors
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        thickness = 1.dp,
        color = c.text3.copy(alpha = 0.14f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// MàJ 92 — Micro-interaction d'appui sur tous les boutons
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Réduit l'élément à [echelle] avec une animation ressort à l'appui et dessine
 * un léger assombrissement arrondi cohérent avec [forme]. À combiner avec un
 * `clickable(interactionSource = …, indication = null, …)` pour remplacer le
 * ripple par défaut par ce highlight.
 * MàJ 95 : l'échelle est neutralisée quand « Réduire les animations » est actif ;
 * seul un voile bref reste appliqué.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    forme: Shape,
    echelle: Float = 0.96f
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val appuye by interactionSource.collectIsPressedAsState()

    val echelleAnim by animateFloatAsState(
        targetValue = if (appuye && !reduceMotion) echelle else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "appuiEchelle"
    )
    val voile by animateFloatAsState(
        targetValue = if (appuye) 0.10f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) ZMouvement.FONDU_REDUIT_MS else 110),
        label = "appuiVoile"
    )

    Modifier
        .graphicsLayer {
            scaleX = echelleAnim
            scaleY = echelleAnim
        }
        .drawWithContent {
            drawContent()
            if (voile > 0.001f) {
                // Voile arrondi : on découpe selon la forme du bouton (Rectangle,
                // Rounded via roundRect, ou Generic) puis on assombrit.
                val contour = forme.createOutline(size, layoutDirection, this)
                val chemin = when (contour) {
                    is Outline.Rectangle -> Path().apply { addRect(contour.rect) }
                    is Outline.Rounded -> Path().apply { addRoundRect(contour.roundRect) }
                    is Outline.Generic -> contour.path
                }
                clipPath(chemin) {
                    drawRect(Color.Black.copy(alpha = voile))
                }
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// MàJ 90 — Squelettes de chargement (shimmer)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bande claire diagonale qui traverse l'élément (MàJ 90). À poser après
 * `clip(forme)` et `background(...)` — voir [RectangleSquelette] pour l'usage type.
 * MàJ 95 : état statique (pas de bande) quand « Réduire les animations » est actif.
 */
fun Modifier.shimmer(forme: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    if (LocalReduceMotion.current) {
        Modifier
    } else {
        val transition = rememberInfiniteTransition(label = "squelette")
        val progression = transition.animateFloat(
            initialValue = -0.6f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZMouvement.SHIMMER_MS, easing = LinearEasing)
            ),
            label = "bandeShimmer"
        ).value
        Modifier.drawWithContent {
            drawContent()
            val x = progression * size.width
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    start = Offset(x - size.width * 0.35f, 0f),
                    end = Offset(x + size.width * 0.35f, size.height)
                )
            )
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

// ─────────────────────────────────────────────────────────────────────────────
// MàJ 91 — Retour haptique ciblé
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Retour haptique centralisé (MàJ 91) : le niveau vient du réglage
 * « Retour haptique » (Désactivé / Léger / Normal) via [LocalHapticNiveau].
 * - [menu] : LongPress à l'ouverture des menus contextuels ;
 * - [curseur] : TextHandleMove pendant le glissement sur les curseurs ;
 * - [envoi] : légère vibration à l'envoi d'un message ;
 * - [tacheTerminee] : double tick léger à la fin d'une tâche.
 */
class ZHaptics constructor(
    private val feedback: HapticFeedback,
    private val view: View,
    private val niveau: HapticLevel
) {
    fun menu() {
        if (niveau == HapticLevel.DESACTIVE) return
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun curseur() {
        if (niveau == HapticLevel.DESACTIVE) return
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun envoi() {
        if (niveau == HapticLevel.DESACTIVE) return
        vibrer(
            dureeMs = if (niveau == HapticLevel.LEGER) 18L else 35L,
            amplitude = if (niveau == HapticLevel.LEGER) 60 else 140
        )
    }

    fun tacheTerminee() {
        if (niveau == HapticLevel.DESACTIVE) return
        vibrer(20L, if (niveau == HapticLevel.LEGER) 70 else 120)
        view.postDelayed(
            { vibrer(35L, if (niveau == HapticLevel.LEGER) 80 else 170) },
            90L
        )
    }

    private fun vibrer(dureeMs: Long, amplitude: Int) {
        try {
            val vibreur: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val gestionnaire = view.context.getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE
                ) as VibratorManager
                gestionnaire.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibreur.hasVibrator()) return
            vibreur.vibrate(
                VibrationEffect.createOneShot(dureeMs, amplitude.coerceIn(1, 255))
            )
        } catch (_: Exception) {
            // Appareil sans vibreur : on ignore silencieusement.
        }
    }
}

/** Mémorise le retour haptique pour la composition courante (MàJ 91). */
@Composable
fun rememberZHaptics(): ZHaptics {
    val feedback = LocalHapticFeedback.current
    val view = LocalView.current
    val niveau = LocalHapticNiveau.current
    return remember(feedback, view, niveau) { ZHaptics(feedback, view, niveau) }
}
