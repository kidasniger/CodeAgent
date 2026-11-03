package com.zagent.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import com.zagent.app.ui.editor.ConsolePanel
import com.zagent.app.ui.editor.PreviewPane
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.ZMenu
import com.zagent.app.ui.components.ZMenuItem
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.editor.EditorTab
import com.zagent.app.ui.editor.Kind
import com.zagent.app.ui.editor.Lang
import com.zagent.app.ui.editor.SyntaxHighlighter
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.zagent.app.ui.editor.EditorController

/**
 * Barre d'onglets de fichiers sous l'en-tête (MA79) : nom, point d'accent si modifié,
 * × pour fermer, appui long pour « Fermer » / « Fermer les autres ».
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EditorTabBar(
    editor: EditorController,
    onRequestClose: (Int) -> Unit
) {
    val c = Zcolors
    val listState = rememberScrollState()
    var menuIndex by remember { mutableStateOf<Int?>(null) }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(listState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        editor.tabs.forEachIndexed { index, t ->
            val selected = index == editor.activeIndex
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(if (selected) c.bg else c.surface2)
                        .combinedClickable(
                            onClick = { editor.select(index) },
                            onLongClick = { menuIndex = index }
                        )
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t.name,
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selected) c.text else c.text2,
                        maxLines = 1
                    )
                    if (t.dirty) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(c.cyan))
                    }
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { if (t.dirty) onRequestClose(index) else editor.close(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Fermer l'onglet", tint = c.text3, modifier = Modifier.size(14.dp))
                    }
                }
                // MA78 : menu unifié en verre (remplace GlassDropdownMenu + DropdownMenuItem).
                ZMenu(
                    expanded = menuIndex == index,
                    onDismiss = { menuIndex = null }
                ) {
                    ZMenuItem(
                        label = "Fermer",
                        icon = Icons.Default.Close,
                        onClick = {
                            menuIndex = null
                            if (t.dirty) onRequestClose(index) else editor.close(index)
                        }
                    )
                    // Plusieurs onglets seulement : « Fermer les autres » n'a pas de sens sinon.
                    if (editor.tabs.size > 1) {
                        ZMenuItem(
                            label = "Fermer les autres",
                            icon = Icons.Default.DeleteOutline,
                            onClick = {
                                menuIndex = null
                                val kept = editor.closeOthers(t)
                                if (kept > 0) {
                                    editor.info = if (kept == 1) "1 onglet modifié conservé" else "$kept onglets modifiés conservés"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
