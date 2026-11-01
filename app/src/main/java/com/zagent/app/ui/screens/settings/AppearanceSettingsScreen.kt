package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun AppearanceSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf<String?>(null) }

    when (destination) {
        "theme" -> ThemeSettingsScreen(viewModel, onBack = { destination = null })
        "glass" -> GlassSettingsScreen(onBack = { destination = null })
        "font" -> FontSettingsScreen(onBack = { destination = null })
        "density" -> DensitySettingsScreen(onBack = { destination = null })
        else -> SettingsPageScaffold(title = "Apparence", onBack = onBack) {
            SettingsSectionLabel("PERSONNALISATION")
            SettingsCard {
                AppearanceNavigationRow(
                    icon = Icons.Default.Contrast,
                    title = "Thème",
                    subtitle = "Système, clair ou sombre",
                    onClick = { destination = "theme" }
                )
                SettingsInsetDivider()
                AppearanceNavigationRow(
                    icon = Icons.Default.Layers,
                    title = "Verre",
                    subtitle = "Matériau, couleur et effets",
                    onClick = { destination = "glass" }
                )
                SettingsInsetDivider()
                AppearanceNavigationRow(
                    icon = Icons.Default.FormatSize,
                    title = "Police",
                    subtitle = "Famille et taille du texte",
                    onClick = { destination = "font" }
                )
                SettingsInsetDivider()
                AppearanceNavigationRow(
                    icon = Icons.Default.DensityMedium,
                    title = "Densité",
                    subtitle = "Espacement et compacité",
                    onClick = { destination = "density" }
                )
            }
        }
    }
}

@Composable
fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val c = Zcolors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                onClick = onBack
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = c.text
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = Zcolors.text3,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.8.sp,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    val c = Zcolors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
fun SettingsInsetDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
        color = Zcolors.border,
        thickness = 1.dp
    )
}

@Composable
fun AppearanceNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val c = Zcolors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.violetBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = c.violet)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.text2)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = c.text3,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SettingsInfoRow(title: String, description: String) {
    val c = Zcolors
    Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = c.text2)
    }
}
