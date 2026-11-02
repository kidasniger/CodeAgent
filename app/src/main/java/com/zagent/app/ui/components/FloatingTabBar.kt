package com.zagent.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
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
 *   Changer de projet, Nouveau terminal…) construit avec ZMenu (MA78) ;
 * - Badge numérique sur l'onglet Chat s'il y a une confirmation en attente
 *   ([chatBadge]).
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
    val c = Zcolors

    // MA73 : glissement de la barre sous le bord bas quand elle est masquée.
    val hiddenOffsetY = with(density) { 140.dp.roundToPx() }
    val offsetY by animateIntAsState(
        targetValue = if (visible) 0 else hiddenOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBarOffset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "tabBarAlpha"
    )

    var menuFor by remember { mutableStateOf<AppScreen?>(null) }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .offset { IntOffset(0, offsetY) }
                .graphicsLayer { this.alpha = alpha }
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .glassSurface(haze, glass, c.isDark, RoundedCornerShape(50.dp), context),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItemData.entries.forEach { item ->
                val selected = currentScreen == item.screen
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .then(
                                if (selected) Modifier.glassSurface(
                                    haze,
                                    glass,
                                    c.isDark,
                                    CircleShape,
                                    context
                                ) else Modifier
                            )
                            .background(
                                if (selected) c.cyan.copy(alpha = glass.activeTab.coerceIn(0f, 1f)) else Color.Transparent,
                                CircleShape
                            )
                            .combinedClickable(
                                onClick = { onNavigate(item.screen) },
                                onLongClick = {
                                    if (item.hasQuickMenu) menuFor = item.screen
                                }
                            )
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (selected) c.cyan else c.text2,
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
                            item.label,
                            color = if (selected) c.text else c.text2,
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

/** Onglet de la barre : libellé, icône, écran cible, menu rapide disponible ou non. */
private enum class TabItemData(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: AppScreen,
    val hasQuickMenu: Boolean
) {
    CHAT("Chat", Icons.Default.ChatBubbleOutline, AppScreen.CHAT, true),
    FILES("Fichiers", Icons.Default.FolderOpen, AppScreen.FILE_BROWSER, true),
    EDITOR("Éditeur", Icons.Default.Code, AppScreen.EDITOR, false),
    TERMINAL("Terminal", Icons.Default.Terminal, AppScreen.TERMINAL, true),
    SETTINGS("Réglages", Icons.Default.Settings, AppScreen.SETTINGS, false)
}
