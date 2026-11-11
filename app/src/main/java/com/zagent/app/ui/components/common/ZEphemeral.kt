package com.zagent.app.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.Zcolors
import kotlinx.coroutines.delay

/**
 * ui-091 — Messages éphémères stylés avec le thème (remplace les Toasts
 * système gris). Une seule message à la fois, auto-masqué après 3 s,
 * tap pour fermer. Le rendu utilise Zcolors : lisible en clair ET en sombre.
 *
 * Usage : `ZEphemeral.show("Chemin copié")` puis placer `ZEphemeralHost()`
 * une fois dans l'arbre racine (MainActivity › AppRoot).
 */
object ZEphemeral {

    enum class Type { NEUTRE, SUCCES, ERREUR }

    /** Message affiché (null = rien). Paire (texte, type). */
    val message = mutableStateOf<Pair<String, Type>?>(null)

    internal val emisA = mutableStateOf(0L)

    fun show(texte: String, type: Type = Type.NEUTRE) {
        message.value = texte.take(140) to type
        emisA.value = System.currentTimeMillis()
    }

    /** Ferme le message courant (tap de l'utilisateur). */
    fun fermer() {
        message.value = null
    }
}

/** Hôte d'affichage des messages éphémères : à placer UNE fois au sommet de l'UI. */
@Composable
fun ZEphemeralHost(modifier: Modifier = Modifier) {
    val c = Zcolors
    val courant = ZEphemeral.message.value

    // Auto-masquage après 3 secondes (uniquement si le message n'a pas changé).
    LaunchedEffect(courant, ZEphemeral.emisA.value) {
        if (courant != null) {
            delay(3000)
            if (ZEphemeral.message.value == courant) ZEphemeral.fermer()
        }
    }

    AnimatedVisibility(
        visible = courant != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val (texte, type) = courant ?: return@AnimatedVisibility
        val (bg, bord, icone, iconeTint) = when (type) {
            ZEphemeral.Type.SUCCES -> Quad(c.greenBg, c.green, Icons.Default.CheckCircle, c.green)
            ZEphemeral.Type.ERREUR -> Quad(c.redBg, c.red, Icons.Default.ErrorOutline, c.red)
            ZEphemeral.Type.NEUTRE -> Quad(c.surface2, c.border, Icons.Default.Info, c.cyan)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(bg)
                .clickable { ZEphemeral.fermer() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icone, null, tint = iconeTint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                texte,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = if (type == ZEphemeral.Type.NEUTRE) c.text else iconeTint
            )
        }
    }
}

/** Petite structure interne (bg, bord, icône, teinte) du message éphémère. */
private data class Quad(
    val bg: Color,
    val bord: Color,
    val icone: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color
)
