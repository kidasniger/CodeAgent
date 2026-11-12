package com.zagent.app.ui.components.common

// corr-011 — Famille extraite de ZComponents.kt (55 Ko) : GradientButton, SecondaryButton, CircleIconButton, SectionLabel, StatusBadge, IconBadge, ModelChip, MonoChip, TypingPill.
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
import androidx.compose.ui.res.stringResource
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
import com.zagent.app.R
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
                role = Role.Button, // MàJ 96
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
                role = Role.Button, // MàJ 96
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
    size: Dp = 48.dp,
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
                role = Role.Button, // MàJ 96
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(20.dp))
    }
}

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
                role = Role.Button, // MàJ 96
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
                    stringResource(R.string.action_stop),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.red
                )
            }
        }
    }
}