package com.zagent.app.ui.components.common

// corr-011 — Famille extraite de ZComponents.kt (55 Ko) : ZMenu, ZMenuItem, ZMenuSeparator.
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
