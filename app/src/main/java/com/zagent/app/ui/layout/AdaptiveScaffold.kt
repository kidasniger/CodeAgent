package com.zagent.app.ui.layout

import android.content.res.Configuration
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.ConversationMeta
import com.zagent.app.ui.screens.ChatScreen
import com.zagent.app.ui.screens.EditorScreen
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.AppScreen
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mise en page adaptative (MA75 + MA76).
 *
 * MA75 — Tablettes et pliables (WindowSizeClass) : en largeur Medium/Expanded le
 * chat est affiché avec un panneau Historique à gauche ; en Expanded l'éditeur
 * partage l'écran avec la conversation. En Compact, comportement du téléphone.
 *
 * MA76 — Paysage téléphone : l'éditeur et la conversation sont côte à côte
 * (55 / 45 par défaut) avec une poignée de redimensionnement et un bouton pour
 * passer chaque panneau en plein écran.
 */

// ─────────────────────────────────────────────────────────────────────────────
// MA75 — Panneau chat adaptatif (Historique + Chat en Medium/Expanded)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdaptiveChatPane(
    viewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass
) {
    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        ChatScreen(
            viewModel = viewModel,
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )
    } else {
        Row(Modifier.fillMaxSize()) {
            HistoryPane(
                viewModel = viewModel,
                modifier = Modifier
                    .weight(0.34f)
                    .fillMaxHeight()
            )
            Box(
                Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
            ) {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                )
            }
        }
    }
}

/** Panneau latéral Historique (MA75) : liste statique des conversations. */
@Composable
fun HistoryPane(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    LaunchedEffect(Unit) { viewModel.refreshConversations() }
    val conversations = remember(viewModel.conversationList) {
        viewModel.conversationList.sortedWith(
            compareByDescending<ConversationMeta> { it.pinned }.thenByDescending { it.updatedAt }
        )
    }
    val dayFormat = remember { SimpleDateFormat("d MMM", Locale.FRENCH) }

    Column(
        modifier
            .background(c.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Historique",
                    style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = c.text
                )
                Text(
                    if (conversations.size <= 1) "${conversations.size} conversation" else "${conversations.size} conversations",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = c.text3
                )
            }
            // Nouvelle conversation, même action que le bouton « + » du chat.
            Box(
                Modifier
                    .size(36.dp)
                    .background(c.surface2, CircleShape)
                    .clickable { viewModel.clearConversation() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nouvelle conversation",
                    tint = c.cyan,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        HorizontalDivider(color = c.surface2)

        if (conversations.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Tes conversations apparaîtront ici, groupées par jour.",
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.text3,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(conversations, key = { it.id }) { meta ->
                    HistoryPaneRow(
                        meta = meta,
                        selected = meta.id == viewModel.conversationId,
                        dayLabel = dayFormat.format(Date(meta.updatedAt)),
                        onClick = { viewModel.switchToConversation(meta.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryPaneRow(
    meta: ConversationMeta,
    selected: Boolean,
    dayLabel: String,
    onClick: () -> Unit
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) c.cyanBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (meta.pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Épinglée",
                        tint = c.amber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    meta.title.ifBlank { "Sans titre" },
                    style = TextStyle(fontFamily = Inter, fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "$dayLabel · ${meta.messageCount} message${if (meta.messageCount > 1) "s" else ""}",
                style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MA75 / MA76 — Panneau éditeur adaptatif (Éditeur + Chat)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Choix de la mise en page de l'éditeur :
 * - Expanded (tablette/pliable) ou paysage téléphone (Compact) : deux panneaux ;
 * - sinon, éditeur seul comme avant.
 */
@Composable
fun AdaptiveEditorPane(
    viewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass,
    landscape: Boolean
) {
    val split = widthSizeClass == WindowWidthSizeClass.Expanded ||
        (landscape && widthSizeClass == WindowWidthSizeClass.Compact)
    if (!split) {
        EditorScreen(viewModel = viewModel, onBack = { viewModel.navigateBack() })
    } else {
        SplitEditorChat(
            viewModel = viewModel,
            tablet = widthSizeClass != WindowWidthSizeClass.Compact
        )
    }
}

/**
 * MA76 — Éditeur et conversation côte à côte : fichier à gauche, conversation à
 * droite, poignée de redimensionnement au centre et boutons pour passer chaque
 * panneau en plein écran. La proportion est conservée pendant la session.
 */
@Composable
fun SplitEditorChat(
    viewModel: MainViewModel,
    tablet: Boolean
) {
    val c = Zcolors
    val context = LocalContext.current
    val glass = LocalGlass.current
    val haze = LocalHazeState.current

    // Part de l'éditeur (0..1) ; 55 % en paysage téléphone, 60 % sur tablette.
    var fraction by rememberSaveable { mutableStateOf(if (tablet) 0.60f else 0.55f) }
    // -1 = deux panneaux ; 0 = éditeur plein écran ; 1 = conversation plein écran.
    var focusPane by rememberSaveable { mutableStateOf(-1) }
    var rowWidthPx by remember { mutableStateOf(1f) }

    // ui-071 — posture du pliable : « table à dessin » (pliable à moitié
    // ouvert) empile les deux panneaux le long de la charnière ; « livre »
    // (à plat, grand écran) garde le côte à côte existant.
    var tableDessin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(context) {
        WindowInfoTracker.getOrCreate(context).windowLayoutInfo(context).collect { info ->
            tableDessin = info.displayFeatures.any { it is FoldingFeature && it.state == FoldingFeature.State.HALF_OPENED }
        }
    }
    val panneauEditeur: @Composable () -> Unit = {
        EditorScreen(viewModel = viewModel, onBack = { viewModel.navigateBack() })
    }
    val panneauChat: @Composable () -> Unit = {
        ChatScreen(viewModel = viewModel, onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) })
    }

    Box(Modifier.fillMaxSize()) {
        if (tableDessin && focusPane == -1) {
            // ui-071 — mode table à dessin : empilement le long de la charnière.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(0.55f)) { panneauEditeur() }
                HorizontalDivider(color = c.border, thickness = 1.dp)
                Box(Modifier.fillMaxWidth().weight(0.45f)) { panneauChat() }
            }
        } else Row(
            Modifier
                .fillMaxSize()
                .onSizeChanged { rowWidthPx = it.width.coerceAtLeast(1).toFloat() }
        ) {
            Box(
                Modifier.weight(
                    when (focusPane) {
                        1 -> 0f
                        0 -> 1f
                        else -> fraction
                    }
                )
            ) {
                panneauEditeur()
            }

            // ── Poignée centrale : boutons plein écran + glissement ──────────
            Column(
                Modifier
                    .width(26.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (focusPane == -1) {
                    HandleButton(
                        icon = Icons.Default.Fullscreen,
                        label = "Éditeur en plein écran",
                        onClick = { focusPane = 0 }
                    )
                    Spacer(Modifier.height(10.dp))
                    // ui-073 — poignée élargie et contrastée ; à la fin du
                    // glissement, aimantation à l'équilibre 50/50 quand on
                    // lâche à moins de 6 % de la position centrale.
                    Box(
                        Modifier
                            .width(32.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (fraction > 0.44f && fraction < 0.56f) c.cyan.copy(alpha = 0.28f) else c.surface3)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    fraction = (fraction + delta / rowWidthPx).coerceIn(0.28f, 0.72f)
                                },
                                onDragStopped = {
                                    if (abs(fraction - 0.5f) < 0.06f) {
                                        fraction = 0.5f   // aimantation 50/50
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .width(5.dp)
                                .height(44.dp)
                                .background(
                                    if (fraction > 0.44f && fraction < 0.56f) c.cyan
                                    else c.text2,
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    HandleButton(
                        icon = Icons.Default.Fullscreen,
                        label = "Conversation en plein écran",
                        onClick = { focusPane = 1 }
                    )
                } else {
                    // Revenir aux deux panneaux : la poignée devient un bouton de restauration.
                    HandleButton(
                        icon = Icons.Default.FullscreenExit,
                        label = "Réafficher les deux panneaux",
                        onClick = { focusPane = -1 }
                    )
                }
            }

            Box(
                Modifier.weight(
                    when (focusPane) {
                        0 -> 0f
                        1 -> 1f
                        else -> 1f - fraction
                    }
                )
            ) {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                )
            }
        }

        // Aide visuelle quand un panneau est en plein écran.
        if (focusPane >= 0) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 96.dp)
                    .glassSurface(haze, glass, c.isDark, RoundedCornerShape(50.dp), context)
                    .background(c.surface2.copy(alpha = if (glass.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                    .clickable { focusPane = -1 }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FullscreenExit,
                    contentDescription = null,
                    tint = c.text2,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Quitter le plein écran",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.sp),
                    color = c.text2
                )
            }
        }
    }
}

@Composable
private fun HandleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val c = Zcolors
    Box(
        Modifier
            .size(28.dp)
            .background(c.surface2, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = c.text2, modifier = Modifier.size(15.dp))
    }
}
