package com.zagent.app.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.LocalReduceMotion
import com.zagent.app.ui.theme.ZMouvement
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.AppScreen

/**
 * Barre d'onglets flottante (MA73).
 *
 * - Se masque en glissant vers le bas quand on défile vers le bas, et remonte
 *   au défilement vers le haut (l'état [visible] est piloté par le détecteur de
 *   défilement de MainActivity) ;
 * - Appui long sur un onglet : menu rapide en verre (Nouvelle conversation,
 *   Changer de projet, Nouveau terminal…) construit avec ZMenu (MA78), avec un
 *   retour haptique LongPress (MàJ 91) ;
 * - Badge numérique sur l'onglet Chat s'il y a une confirmation en attente
 *   ([chatBadge]) ;
 * - MàJ 94 : la pastille de verre de l'onglet actif GLIT désormais d'un onglet
 *   à l'autre (animateDpAsState + ressort amorti) au lieu d'apparaître /
 *   disparaître, et s'étire légèrement pendant le mouvement ;
 * - MàJ 95 : repositionnement instantané (sans étirement) quand « Réduire les
 *   animations » est actif.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingTabBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    chatBadge: Int = 0,
    onNewChat: () -> Unit = {},
    onChangeProject: () -> Unit = {},
    onNewTerminal: () -> Unit = {}
) {
    val context = LocalContext.current
    val glass = LocalGlass.current
    val haze = LocalHazeState.current
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val haptique = rememberZHaptics()
    val c = Zcolors

    // MA73 : glissement de la barre sous le bord bas quand elle est masquée.
    val hiddenOffsetY = with(density) { 140.dp.roundToPx() }
    val offsetY by animateIntAsState(
        targetValue = if (visible) 0 else hiddenOffsetY,
        animationSpec = if (reduceMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBarOffset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) ZMouvement.FONDU_REDUIT_MS else 200),
        label = "tabBarAlpha"
    )

    var menuFor by remember { mutableStateOf<AppScreen?>(null) }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        // ── MàJ 94 : géométrie de la pastille glissante ──────────────────────
        val nbOnglets = TabItemData.entries.size
        // Le conteneur en verre est inset de 12.dp de chaque côté (padding ci-dessous).
        val largeurContenu = maxWidth - 24.dp
        val largeurSlot = largeurContenu / nbOnglets
        val indexActif = TabItemData.entries
            .indexOfFirst { it.screen == currentScreen }
            .coerceAtLeast(0)
        val cibleX = largeurSlot * indexActif
        val largeurBase = largeurSlot - 8.dp

        val decalage by animateDpAsState(
            targetValue = cibleX,
            animationSpec = if (reduceMotion) snap() else spring(
                dampingRatio = 0.78f,
                stiffness = 380f
            ),
            label = "pastilleOnglet"
        )
        val enMouvement by remember(cibleX) {
            derivedStateOf { (decalage - cibleX).value > 0.4f }
        }
        val etirement by animateFloatAsState(
            targetValue = if (enMouvement && !reduceMotion) 1.14f else 1f,
            animationSpec = if (reduceMotion) snap() else spring(
                dampingRatio = 0.7f,
                stiffness = 500f
            ),
            label = "etirementPastille"
        )

        Box(
            Modifier
                .offset { IntOffset(0, offsetY) }
                .graphicsLayer { this.alpha = alpha }
                .fillMaxWidth()
                // maj-031 — préparation targetSdk 35 : edge-to-edge y sera
                // obligatoire. La barre flottante doit se poser AU-DESSUS de la
                // barre de navigation système (geste ou 3 boutons), exactement
                // comme ChatInputBar le fait déjà avec navigationBarsPadding().
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Conteneur en verre (même rendu qu'auparavant).
            // Correctif : height(IntrinsicSize.Min) borne la hauteur du conteneur
            // à celle de la rangée d'onglets. Sans cette borne, la pastille en
            // fillMaxHeight héritait du maxHeight de l'écran entier et s'étalait
            // en une bande verticale sur toute la hauteur de l'écran.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .glassSurface(haze, glass, c.isDark, RoundedCornerShape(50.dp), context)
            ) {
                // Pastille de verre de l'onglet actif — glisse d'un onglet à
                // l'autre (MàJ 94) et s'étire légèrement pendant le mouvement.
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(
                            x = decalage + 4.dp - (largeurBase * (etirement - 1f)) / 2f
                        )
                        .width(largeurBase * etirement)
                        .fillMaxHeight()
                        .padding(vertical = 5.dp)
                        .glassSurface(haze, glass, c.isDark, CircleShape, context)
                        .background(
                            c.cyan.copy(alpha = glass.activeTab.coerceIn(0f, 1f)),
                            CircleShape
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItemData.entries.forEach { item ->
                        val actif = currentScreen == item.screen
                        Box(Modifier.weight(1f)) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    // MàJ 96 — rôle Tab + état « sélectionné » exposé
                                    // aux lecteurs d'écran ; ordre de focus = ordre visuel.
                                    .semantics { selected = actif }
                                    .combinedClickable(
                                        onClick = { onNavigate(item.screen) },
                                        onLongClick = {
                                            // MàJ 91 — LongPress pour les menus.
                                            haptique.menu()
                                            if (item.hasQuickMenu) menuFor = item.screen
                                        },
                                        role = Role.Tab
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box {
                                    Icon(
                                        item.icon,
                                        contentDescription = stringResource(item.labelRes),
                                        tint = if (actif) c.cyan else c.text2,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    // MA73 : badge numérique sur Chat (confirmations en attente).
                                    if (item.screen == AppScreen.CHAT && chatBadge > 0) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 7.dp, y = (-3).dp)
                                                .clip(CircleShape)
                                                .background(c.red)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                if (chatBadge > 9) "9+" else chatBadge.toString(),
                                                style = TextStyle(fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 8.sp),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    stringResource(item.labelRes),
                                    color = if (actif) c.text else c.text2,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // MA73 : menu rapide de l'appui long, ancré sur l'onglet.
                            if (menuFor == item.screen && item.hasQuickMenu) {
                                ZMenu(expanded = true, onDismiss = { menuFor = null }) {
                                    when (item.screen) {
                                        AppScreen.CHAT -> ZMenuItem(
                                            label = "Nouvelle conversation",
                                            icon = Icons.Default.Add,
                                            onClick = { menuFor = null; onNewChat() }
                                        )
                                        AppScreen.FILE_BROWSER -> ZMenuItem(
                                            label = "Changer de projet",
                                            icon = Icons.Default.FolderOpen,
                                            onClick = { menuFor = null; onChangeProject() }
                                        )
                                        AppScreen.TERMINAL -> ZMenuItem(
                                            label = "Nouveau terminal",
                                            icon = Icons.Default.Add,
                                            onClick = { menuFor = null; onNewTerminal() }
                                        )
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Onglet de la barre : libellé (ressource, MàJ 100), icône, écran cible, menu rapide disponible ou non. */
private enum class TabItemData(
    @StringRes val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: AppScreen,
    val hasQuickMenu: Boolean
) {
    CHAT(R.string.tab_chat, Icons.Default.ChatBubbleOutline, AppScreen.CHAT, true),
    FILES(R.string.tab_files, Icons.Default.FolderOpen, AppScreen.FILE_BROWSER, true),
    EDITOR(R.string.tab_editor, Icons.Default.Code, AppScreen.EDITOR, false),
    TERMINAL(R.string.tab_terminal, Icons.Default.Terminal, AppScreen.TERMINAL, true),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings, AppScreen.SETTINGS, false)
}
