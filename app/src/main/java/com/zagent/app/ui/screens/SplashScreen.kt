package com.zagent.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.BuildConfig
import com.zagent.app.ui.components.AmbientBackground
import com.zagent.app.ui.components.ZLogo
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.delay

private const val SPLASH_MS = 2_400L

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val c = Zcolors

    // Progression du tracé du Z, puis apparition du nom et du slogan
    var drawProgress by remember { mutableFloatStateOf(0f) }
    var titleAlpha by remember { mutableFloatStateOf(0f) }
    var sparkAlpha by remember { mutableFloatStateOf(0f) }
    val animatedDraw by animateFloatAsState(
        targetValue = drawProgress,
        animationSpec = tween(900),
        label = "zdraw"
    )
    val animatedTitle by animateFloatAsState(
        targetValue = titleAlpha,
        animationSpec = tween(500),
        label = "title"
    )
    val animatedSpark by animateFloatAsState(
        targetValue = sparkAlpha,
        animationSpec = tween(400),
        label = "spark"
    )

    LaunchedEffect(Unit) {
        drawProgress = 1f
        delay(500)
        sparkAlpha = 1f
        titleAlpha = 1f
        delay(SPLASH_MS - 500)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        AmbientBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ZLogo(
                modifier = Modifier.size(112.dp),
                progress = animatedDraw,
                sparkAlpha = animatedSpark
            )
            Spacer(Modifier.height(28.dp))

            // « Z » en dégradé + « Agent » en texte plein
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Z",
                    style = TextStyle(
                        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        brush = c.grad
                    ),
                    color = c.text
                )
                Text(
                    "Agent",
                    style = TextStyle(
                        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = c.text
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ton agent de code de poche",
                style = MaterialTheme.typography.bodyLarge,
                color = c.text2
            )
        }

        // Points de chargement + version
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val on = when (i) {
                        0 -> animatedTitle > 0.1f
                        1 -> animatedTitle > 0.4f
                        else -> animatedTitle > 0.7f
                    }
                    Box(
                        Modifier
                            .size(if (on) 8.dp else 7.dp)
                            .background(
                                if (on) c.text2.copy(alpha = 0.9f) else c.text3.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                }
            }
            Text(
                "v${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                color = c.text3
            )
        }
    }
}
