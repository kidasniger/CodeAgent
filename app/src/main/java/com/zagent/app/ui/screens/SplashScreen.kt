package com.zagent.app.ui.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zagent.app.ui.theme.ZMouvement

/**
 * MàJ 93 — L'ancien écran splash Compose maintenu 2,4 s (logo animé + points)
 * a été supprimé : le splash est désormais fourni par le système via
 * androidx.core:core-splashscreen (voir res/values/themes.xml, la dépendance
 * core-splashscreen et MainActivity.installSplashScreen()). Le fond du splash
 * suit le thème, donc aucun flash blanc en mode sombre.
 *
 * Il ne reste ici que le fondu de 400 ms qui révèle l'application après la
 * fermeture du splash système — réduit à un fondu court si « Réduire les
 * animations » est actif (MàJ 95).
 */
@Composable
fun EntreeApplication(
    reduceMotion: Boolean,
    contenu: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val duree = if (reduceMotion) ZMouvement.FONDU_REDUIT_MS else ZMouvement.FONDU_APP_MS

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = duree, easing = LinearOutSlowInEasing),
        label = "alphaEntreeApplication"
    )
    val echelle by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0.985f,
        animationSpec = tween(durationMillis = duree, easing = LinearOutSlowInEasing),
        label = "echelleEntreeApplication"
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = echelle
                scaleY = echelle
            }
    ) {
        contenu()
    }
}
