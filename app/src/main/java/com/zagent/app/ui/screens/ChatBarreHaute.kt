package com.zagent.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.IconBadge
import com.zagent.app.ui.components.common.MonoChip
import com.zagent.app.ui.components.chat.UsageLine
import com.zagent.app.ui.components.common.ZLogo
import com.zagent.app.ui.components.common.ZMenu
import com.zagent.app.ui.components.common.ZMenuItem
import com.zagent.app.ui.components.common.ZMenuSeparator
import com.zagent.app.ui.components.common.rememberZHaptics
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 99 — découpe de ChatScreen : barre supérieure, toasts et carte
// d'erreur. Anciennement fonctions privées de ChatScreen.kt, mêmes signatures
// (visibilité internal, même paquet) : aucun changement d'appel nécessaire.
// MàJ 96 — zone tactile 48 dp et rôle Button sur les icônes d'action.
// MàJ 100 — libellés extraits vers strings.xml (values-en fourni).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun AmbientTopBar(
    viewModel: MainViewModel,
    scrolled: Boolean,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    /** feat-001 — ouvre la barre de recherche dans la conversation courante. */
    onSearchInConversation: () -> Unit = {},
    /** feat-002 — ouvre le panneau des messages épinglés. */
    onOpenPinned: () -> Unit = {},
    /** feat-002 — nombre de messages épinglés (affiché dans le menu). */
    nbEpingles: Int = 0
) {
    val c = Zcolors
    val haptique = rememberZHaptics() // MàJ 91
    // MA78 : menu « ⋯ » unifié regroupant les actions secondaires.
    var moreMenuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (scrolled) Modifier.glassSurface(
                    LocalHazeState.current,
                    LocalGlass.current,
                    c.isDark,
                    RoundedCornerShape(24.dp),
                    LocalContext.current
                ) else Modifier
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZLogo(modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ZAgent",
                    style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                    color = c.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (dot, label) = when {
                        viewModel.isLoading -> c.cyan to stringResource(R.string.status_agent_active)
                        viewModel.hasBlockingError -> c.red to stringResource(R.string.status_error)
                        else -> c.green to stringResource(R.string.status_online)
                    }
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(dot, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Medium), color = dot)
                    Spacer(Modifier.width(8.dp))
                    // sec-022 — empreinte visuelle du fournisseur actif : la pastille
                    // devant le modèle porte la teinte stable de sa passerelle.
                    val idFournisseurActif = viewModel.fournisseurActifId()
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                androidx.compose.ui.graphics.Color(
                                    com.zagent.app.data.Fournisseurs.teinteStable(idFournisseurActif)
                                ),
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(4.dp))
                    MonoChip(
                        viewModel.effectiveModel,
                        modifier = Modifier
                            .widthIn(max = 160.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.isModelDialogOpen = true }
                    )
                }
                UsageLine(viewModel.totalTokens, viewModel.tokensEstimated, viewModel.estimatedCost)
                // git-079 — badge de statut CI quand un push est en vol / suivi.
                viewModel.statutActionsCi?.let { ci ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ci,
                            style = TextStyle(fontFamily = com.zagent.app.ui.theme.JetBrainsMono, fontSize = 10.sp),
                            color = c.amber
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Groupe d'actions (48 dp par icône, MàJ 96) dans une pilule de verre.
            Row(
                Modifier
                    .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                    .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarPillIcon(Icons.Default.Menu, stringResource(R.string.cd_open_history), onClick = { viewModel.isHistoryOpen = true })
                TopBarPillIcon(Icons.Default.Search, stringResource(R.string.cd_open_search), onClick = { viewModel.openSearchScreen() })
                TopBarPillIcon(Icons.Default.Share, stringResource(R.string.cd_share_conversation), onClick = { viewModel.isShareSheetOpen = true })
                // MA78 : un seul menu déroulant en verre pour les actions secondaires.
                Box {
                    TopBarPillIcon(Icons.Default.MoreHoriz, stringResource(R.string.cd_more_actions), onClick = { haptique.menu(); moreMenuOpen = true })
                    ZMenu(expanded = moreMenuOpen, onDismiss = { moreMenuOpen = false }) {
                        ZMenuItem(
                            label = stringResource(R.string.menu_file_browser),
                            icon = Icons.Default.FolderOpen,
                            onClick = {
                                moreMenuOpen = false
                                viewModel.navigateTo(com.zagent.app.ui.viewmodel.AppScreen.FILE_BROWSER)
                            }
                        )
                        ZMenuItem(
                            label = stringResource(R.string.menu_git),
                            icon = Icons.Default.AccountTree,
                            onClick = {
                                moreMenuOpen = false
                                viewModel.isGitScreenOpen = true
                            }
                        )
                        ZMenuItem(
                            label = stringResource(R.string.menu_models),
                            icon = Icons.Default.Bolt,
                            onClick = {
                                moreMenuOpen = false
                                viewModel.isModelDialogOpen = true
                            }
                        )
                        ZMenuItem(
                            label = stringResource(R.string.menu_prompt_templates),
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                moreMenuOpen = false
                                viewModel.isTemplatesOpen = true
                            }
                        )
                        // feat-001 — recherche dans la conversation courante
                        ZMenuItem(
                            label = "Rechercher dans la conversation",
                            icon = Icons.Default.Search,
                            onClick = {
                                moreMenuOpen = false
                                onSearchInConversation()
                            }
                        )
                        // feat-002 — panneau des messages épinglés
                        ZMenuItem(
                            label = if (nbEpingles > 0) "Messages épinglés (" + nbEpingles + ")" else "Messages épinglés",
                            icon = Icons.Default.PushPin,
                            onClick = {
                                moreMenuOpen = false
                                onOpenPinned()
                            }
                        )
                        // feat-014 — écriture autorisée ou non, par conversation.
                        ZMenuItem(
                            label = if (viewModel.ecritureAutoriseeConversation)
                                "Interdire les écritures dans cette conversation"
                            else "Autoriser les écritures dans cette conversation",
                            icon = Icons.Default.Lock,
                            onClick = {
                                moreMenuOpen = false
                                viewModel.reglerEcritureConversation(!viewModel.ecritureAutoriseeConversation)
                            }
                        )
                        ZMenuSeparator()
                        ZMenuItem(
                            label = stringResource(R.string.menu_settings),
                            icon = Icons.Default.Settings,
                            onClick = {
                                moreMenuOpen = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Icône ronde du groupe d'actions de la barre du haut — zone tactile 48 dp (MàJ 96). */
@Composable
internal fun TopBarPillIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Zcolors.text, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toast discret en pilule de verre (MA43)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun GlassToastPill(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Zcolors
    val blocking = viewModel.hasBlockingError || viewModel.stepLimitReached
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(viewModel.statusMessage, blocking) {
        visible = true
        if (!blocking && viewModel.statusMessage.isNotBlank()) {
            delay(4000)
            visible = false
        }
    }
    val data: ToastData? = when {
        viewModel.isKeyInvalid -> ToastData(Icons.Default.Key, c.red, stringResource(R.string.toast_key_missing), stringResource(R.string.menu_settings) to onOpenSettings)
        viewModel.isNetworkError -> ToastData(Icons.Default.WifiOff, c.amber, stringResource(R.string.toast_offline), stringResource(R.string.action_retry) to { viewModel.retryLast() })
        viewModel.showFallbackModelButton || viewModel.isCode1305Error ->
            ToastData(Icons.Default.Bolt, c.amber, stringResource(R.string.toast_model_overloaded), stringResource(R.string.toast_other_model) to { viewModel.switchToOtherModel() })
        viewModel.stepLimitReached -> ToastData(Icons.Default.Bolt, c.amber, stringResource(R.string.toast_step_limit), stringResource(R.string.toast_continue) to { viewModel.continueAgent() })
        !viewModel.isLoading && viewModel.statusMessage.isNotBlank() ->
            ToastData(Icons.Default.ErrorOutline, c.amber, viewModel.statusMessage.take(60), stringResource(R.string.action_retry) to { viewModel.retryLast() })
        else -> null
    }
    AnimatedVisibility(visible = visible && data != null) {
        data?.let { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .glassSurface(LocalHazeState.current, LocalGlass.current, c.isDark, RoundedCornerShape(50.dp), LocalContext.current)
                    .background(c.surface2.copy(alpha = if (LocalGlass.current.enabled) 0.12f else 1f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(t.icon, null, tint = t.tint, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    t.text,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                t.action?.let { (label, onClick) ->
                    Text(
                        label,
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.cyan,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onClick, role = Role.Button)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/** Notification courte affichée dans la pilule de verre (MA43). */
internal data class ToastData(val icon: ImageVector, val tint: Color, val text: String, val action: Pair<String, () -> Unit>?)

/** Relances automatiques en cas de perte de réseau (logique conservée de l'ancienne bannière). */
@Composable
internal fun AutoRetryEffect(viewModel: MainViewModel) {
    var countdown by remember { mutableStateOf(30) }
    LaunchedEffect(Unit) {
        while (viewModel.autoRetryCount < 3) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            viewModel.retryLast(auto = true)
            countdown = 30
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Erreur humaine dans le fil, avec actions (MA47)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun InlineErrorCard(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Zcolors
    val title = when {
        viewModel.isKeyInvalid -> stringResource(R.string.error_title_key_refused)
        viewModel.isNetworkError -> stringResource(R.string.error_title_network)
        viewModel.showFallbackModelButton || viewModel.isCode1305Error -> stringResource(R.string.error_title_model_overloaded)
        viewModel.stepLimitReached -> stringResource(R.string.error_title_step_limit)
        else -> stringResource(R.string.error_title_generic)
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        backgroundColor = c.redBg,
        borderColor = c.red.copy(alpha = 0.30f),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(Icons.Default.ErrorOutline, c.red)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                    color = c.text
                )
                if (viewModel.statusMessage.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        viewModel.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text2,
                        // ux-093 — le lecteur d'écran annonce dynamiquement les
                        // changements de statut de l'agent (erreurs, progression).
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton(
                        stringResource(R.string.action_retry),
                        onClick = { viewModel.retryLast() },
                        modifier = Modifier.weight(1f, fill = false),
                        height = 38.dp
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenSettings, role = Role.Button)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            stringResource(R.string.action_open_settings),
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = c.text2
                        )
                    }
                }
            }
        }
    }
}
