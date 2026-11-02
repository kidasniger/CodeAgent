package com.zagent.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

enum class GlassMaterial { FASTEST, BALANCED, QUALITY }

data class GlassSettings(
    val material: GlassMaterial = GlassMaterial.BALANCED,
    val colour: Color = Color.Unspecified,
    val frost: Dp = 6.dp,
    val tint: Float = 0.70f,
    val film: Float = 0.60f,
    val activeTab: Float = 0.26f,
    val enabled: Boolean = true,
    val alwaysRespectSetting: Boolean = false
)

val LocalGlass = staticCompositionLocalOf { GlassSettings() }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

fun effectiveGlassMaterial(context: Context, settings: GlassSettings): GlassMaterial {
    if (!settings.enabled) return GlassMaterial.FASTEST
    if (settings.alwaysRespectSetting) return settings.material
    val activity = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return if (Build.VERSION.SDK_INT < 31 || activity?.isLowRamDevice == true || power?.isPowerSaveMode == true) {
        GlassMaterial.FASTEST
    } else settings.material
}

fun GlassMaterial.label(): String = when (this) {
    GlassMaterial.FASTEST -> "Rapide"
    GlassMaterial.BALANCED -> "Équilibré"
    GlassMaterial.QUALITY -> "Qualité"
}

fun Modifier.glassSurface(
    state: HazeState?,
    settings: GlassSettings,
    dark: Boolean,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    context: Context? = null
): Modifier {
    val material = when {
        !settings.enabled -> GlassMaterial.FASTEST
        context != null -> effectiveGlassMaterial(context, settings)
        Build.VERSION.SDK_INT < 31 -> GlassMaterial.FASTEST
        else -> settings.material
    }
    val neutral = if (dark) Color(0xFF292826) else Color(0xFFFFFEFC)
    val tintColor = if (settings.colour == Color.Unspecified) Color.Transparent else settings.colour.copy(alpha = settings.tint.coerceIn(0f, 1f))
    val style = HazeStyle(
        backgroundColor = neutral.copy(alpha = (0.10f + settings.film.coerceIn(0f, 1f) * 0.72f).coerceIn(0.10f, 0.88f)),
        tints = if (tintColor.alpha > 0f) listOf(HazeTint(tintColor)) else emptyList(),
        blurRadius = when (material) {
            GlassMaterial.FASTEST -> 0.dp
            GlassMaterial.BALANCED -> (settings.frost / 2f).coerceAtMost(8.dp)
            GlassMaterial.QUALITY -> settings.frost.coerceIn(0.dp, 24.dp)
        },
        noiseFactor = if (material == GlassMaterial.QUALITY) 0.04f else 0f
    )
    var result = this.clip(shape)
    result = if (material != GlassMaterial.FASTEST && Build.VERSION.SDK_INT >= 31 && state != null) {
        result.hazeChild(state = state, style = style)
    } else {
        result.background(
            (if (settings.colour == Color.Unspecified) neutral else settings.colour)
                .copy(alpha = (0.16f + settings.film.coerceIn(0f, 1f) * 0.70f).coerceIn(0.16f, 0.92f))
        )
    }
    if (material == GlassMaterial.QUALITY) {
        result = result.border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, Color.White.copy(alpha = 0.20f))),
            shape
        ).drawBehind {
            drawLine(Color.White.copy(alpha = 0.25f), Offset(12.dp.toPx(), 1.dp.toPx()), Offset(size.width - 12.dp.toPx(), 1.dp.toPx()), 0.7.dp.toPx())
        }
    }
    return result
}
