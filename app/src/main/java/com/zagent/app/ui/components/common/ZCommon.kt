package com.zagent.app.ui.components.common

// corr-011 — Famille extraite de ZComponents.kt (55 Ko) : ZBreadcrumb, ZSliderRow, ZLogo, AmbientBackground, pressScale, shimmer, ZHaptics, rememberZHaptics.
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
