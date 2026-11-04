package com.zagent.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.zagent.app.ui.components.ZMenuSeparator
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

// Petits composants de l'éditeur : icônes, champ de recherche, barre plein écran, console

@Composable
internal fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: Color? = null, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint ?: c.text2, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text),
        cursorBrush = SolidColor(c.cyan),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(c.surface2)
                    .border(1.dp, c.border, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = TextStyle(fontFamily = Inter, fontSize = 13.sp), color = c.text3)
                }
                inner()
            }
        }
    )
}
@Composable
internal fun FullscreenBar(
    title: String,
    errors: Int,
    menuOpen: Boolean,
    onMenu: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onConsole: () -> Unit,
    onExit: () -> Unit
) {
    val c = Zcolors
    Column(Modifier.fillMaxWidth().background(c.bg2.copy(alpha = 0.95f))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                color = c.text,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            SmallIcon(Icons.Default.Refresh, "Recharger") { onRefresh() }
            ConsoleIcon(errors) { onConsole() }
            Box {
                SmallIcon(Icons.Default.MoreVert, "Menu") { onMenu(true) }
                // MA78 : menu unifié en verre (remplace GlassDropdownMenu + DropdownMenuItem).
                ZMenu(expanded = menuOpen, onDismiss = { onMenu(false) }) {
                    ZMenuItem(label = "Recharger", icon = Icons.Default.Refresh, onClick = { onMenu(false); onRefresh() })
                    ZMenuItem(label = "Console", icon = Icons.Default.Terminal, onClick = { onMenu(false); onConsole() })
                    ZMenuSeparator()
                    ZMenuItem(label = "Quitter le plein écran", icon = Icons.Default.FullscreenExit, onClick = { onMenu(false); onExit() })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}

/** Bouton console avec badge rouge du nombre d'erreurs. */
@Composable
internal fun ConsoleIcon(errors: Int, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Terminal, "Console", tint = c.text2, modifier = Modifier.size(20.dp))
        if (errors > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 1.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.red)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    if (errors > 99) "99+" else errors.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * MA82 — Pastille d'option de la recherche : « Aa » (respecter la casse) et
 * « ab| » (mot entier). Active = fond et texte accent.
 */
@Composable
internal fun OptionChip(label: String, description: String, active: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    Box(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (active) c.cyanBg else c.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            color = if (active) c.cyan else c.text2
        )
    }
}
