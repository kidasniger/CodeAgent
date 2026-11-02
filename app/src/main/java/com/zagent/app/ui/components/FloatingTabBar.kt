package com.zagent.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.theme.LocalGlass
import com.zagent.app.ui.theme.LocalHazeState
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.theme.glassSurface
import com.zagent.app.ui.viewmodel.AppScreen

@Composable
fun FloatingTabBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalGlass.current
    val haze = LocalHazeState.current
    val items = listOf(
        Triple("Chat", Icons.Default.ChatBubbleOutline, AppScreen.CHAT),
        Triple("Fichiers", Icons.Default.FolderOpen, AppScreen.FILE_BROWSER),
        Triple("Éditeur", Icons.Default.Code, AppScreen.EDITOR),
        Triple("Terminal", Icons.Default.Terminal, AppScreen.TERMINAL),
        Triple("Réglages", Icons.Default.Settings, AppScreen.SETTINGS)
    )
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
            .glassSurface(haze, glass, Zcolors.isDark, RoundedCornerShape(50.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, icon, screen) ->
            val selected = currentScreen == screen
            Column(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (selected) Zcolors.primary.copy(alpha = glass.activeTab.coerceIn(0f, 1f)) else Color.Transparent)
                    .clickable { onNavigate(screen) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(icon, contentDescription = label, tint = if (selected) Zcolors.primary else Zcolors.text2, modifier = Modifier.size(19.dp))
                Text(label, color = if (selected) Zcolors.text else Zcolors.text2, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
