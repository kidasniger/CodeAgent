package com.zagent.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
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
import com.zagent.app.ui.components.GlassAlertDialog
import com.zagent.app.ui.components.CircleIconButton
import com.zagent.app.data.HapticLevel
import com.zagent.app.ui.theme.StylePresetIds
import com.zagent.app.ui.theme.GlassMaterial
import com.zagent.app.ui.theme.StylePresets
import com.zagent.app.ui.theme.LocalZColors
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

@Composable
fun AppearanceSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    var destination by rememberSaveable { mutableStateOf<String?>(null) }

    // Les sous-écrans (Thème, Verre, Police, Densité) se ferment avant Apparence.
    BackHandler(enabled = destination != null) {
        destination = null
    }
    var showResetAppearance by rememberSaveable { mutableStateOf(false) }
    var selectedStylePreset by rememberSaveable {
        mutableStateOf(viewModel.settingsManager.stylePresetId)
    }
    // L'accent est réglé séparément pour le clair et le sombre (mise à jour 7).
    val activeIsDark = LocalZColors.current.isDark
    var accentEditorDark by rememberSaveable { mutableStateOf(activeIsDark) }
    var accentLightHex by rememberSaveable {
        mutableStateOf(viewModel.settingsManager.accentColorHexFor(false))
    }
    var accentDarkHex by rememberSaveable {
        mutableStateOf(viewModel.settingsManager.accentColorHexFor(true))
    }
    val accentHex = if (accentEditorDark) accentDarkHex else accentLightHex
    LaunchedEffect(destination) {
        if (destination == null) {
            selectedStylePreset = viewModel.settingsManager.stylePresetId
            accentLightHex = viewModel.settingsManager.accentColorHexFor(false)
            accentDarkHex = viewModel.settingsManager.accentColorHexFor(true)
        }
    }
    fun hueOf(hex: String): Float = hex.takeIf { it.isNotBlank() }?.let {
        runCatching {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(it), hsv)
            hsv[0]
        }.getOrDefault(0f)
    } ?: 0f
    var accentHue by rememberSaveable { mutableStateOf(hueOf(accentHex)) }
    val accentSwatches = listOf(
        "Bleu-gris" to "#64748B",
        "Rouge" to "#EF4444",
        "Orange" to "#F97316",
        "Jaune" to "#EAB308",
        "Vert" to "#22C55E",
        "Cyan" to "#06B6D4",
        "Bleu" to "#3B82F6",
        "Violet" to "#8B5CF6",
        "Rose" to "#EC4899"
    )
    fun selectAccent(hex: String) {
        viewModel.settingsManager.setAccentColorHexFor(accentEditorDark, hex)
        if (accentEditorDark) accentDarkHex = hex else accentLightHex = hex
        viewModel.settingsManager.markStylePresetCustom()
        accentHue = hueOf(hex)
        selectedStylePreset = viewModel.settingsManager.stylePresetId
    }
    fun syncHueFromAccent() {
        accentHue = hueOf(accentHex)
    }
    val styleOptions = listOf(
        StylePresetIds.MINIMAL to "Minimal",
        StylePresetIds.GLASS to "Verre",
        StylePresetIds.CONTRAST to "Contraste"
    )

    when (destination) {
        "theme" -> ThemeSettingsScreen(viewModel, onBack = { destination = null })
        "glass" -> GlassSettingsScreen(viewModel = viewModel, onBack = { destination = null })
        "font" -> FontSettingsScreen(viewModel, onBack = { destination = null })
        "density" -> DensitySettingsScreen(viewModel, onBack = { destination = null })
        else -> SettingsPageScaffold(title = "Apparence", onBack = onBack) {
            if (showResetAppearance) {
                GlassAlertDialog(
                    onDismissRequest = { showResetAppearance = false },
                    title = { Text("Tout réinitialiser ?") },
                    text = { Text("Tous les réglages d’apparence seront restaurés : thème, presets, accents, verre clair et sombre, OLED, couleurs système, rayons et horaires. Les clés API et les projets ne seront pas modifiés.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.settingsManager.resetAppearance()
                            viewModel.updateThemeMode(com.zagent.app.ui.theme.ThemeMode.SYSTEM)
                            viewModel.updateOledBlack(false)
                            viewModel.updateDynamicSystemColors(false)
                            viewModel.updateCornerRadius(16)
                            viewModel.updateFontFamily("INTER")
                            viewModel.updateFontScale(1f)
                            viewModel.updateDensityScale(1f)
                            viewModel.updateScheduledDarkTime("19:00")
                            viewModel.updateScheduledLightTime("07:00")
                            selectedStylePreset = StylePresetIds.GLASS
                            viewModel.settingsManager.styleBasePresetId = StylePresetIds.GLASS
                            accentLightHex = ""
                            accentDarkHex = ""
                            accentHue = 0f
                            showResetAppearance = false
                        }) { Text("Réinitialiser") }
                    },
                    dismissButton = { TextButton(onClick = { showResetAppearance = false }) { Text("Annuler") } }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.TextButton(onClick = { showResetAppearance = true }) {
                    Text("Tout réinitialiser")
                }
            }
            SettingsSectionLabel("STYLE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                styleOptions.forEach { (id, label) ->
                    FilterChip(
                        selected = selectedStylePreset == id,
                        onClick = {
                            selectedStylePreset = id
                            val preset = StylePresets.fromId(id)
                            val settings = viewModel.settingsManager
                            settings.stylePresetId = id
                            settings.styleBasePresetId = id
                            listOf(false, true).forEach { mode ->
                                settings.setAccentColorHexFor(mode, preset.colour)
                            }
                            viewModel.updateCornerRadius(preset.radius.toInt().coerceIn(8, 28))
                            listOf(false, true).forEach { dark ->
                                settings.setGlassMaterialFor(dark, if (preset.glassMaterial == "NONE") "Solid" else "Liquid")
                                settings.setGlassColorFor(dark, preset.colour)
                                settings.setGlassBlurFor(dark, preset.frost)
                                settings.setGlassTintFor(dark, preset.film)
                                settings.setGlassFilmFor(dark, preset.film)
                                settings.setGlassActiveTabFor(dark, preset.activeTab)
                                val modernGlass = settings.loadGlassSettings(dark).copy(
                                    enabled = id != StylePresetIds.MINIMAL && id != StylePresetIds.CONTRAST,
                                    material = if (preset.glassMaterial.contains("QUALITY")) GlassMaterial.QUALITY else GlassMaterial.BALANCED
                                )
                                settings.saveGlassSettings(dark, modernGlass)
                            }
                            val activeDark = when (viewModel.themeMode) {
                                com.zagent.app.ui.theme.ThemeMode.DARK -> true
                                com.zagent.app.ui.theme.ThemeMode.LIGHT -> false
                                else -> systemDark
                            }
                            viewModel.reloadGlassSettings(activeDark)
                            accentLightHex = preset.colour
                            accentDarkHex = preset.colour
                        },
                        label = { Text(label) }
                    )
                }
                if (selectedStylePreset == StylePresetIds.CUSTOM) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Perso") }
                    )
                }
            }

            SettingsSectionLabel("TEINTE")
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("MODIFIER POUR", color = Zcolors.text3, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !accentEditorDark,
                            onClick = { accentEditorDark = false; syncHueFromAccent() },
                            label = { Text("Clair") }
                        )
                        FilterChip(
                            selected = accentEditorDark,
                            onClick = { accentEditorDark = true; syncHueFromAccent() },
                            label = { Text("Sombre") }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Couleur d'accent", color = Zcolors.text, style = MaterialTheme.typography.titleMedium)
                        FilterChip(
                            selected = accentHex.isBlank(),
                            onClick = { selectAccent("") },
                            label = { Text("Aucune") }
                        )
                    }
                    Text(
                        if (accentHex.isBlank()) "Palette d'origine" else accentHex.uppercase(),
                        color = Zcolors.text2,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("ARC-EN-CIEL", color = Zcolors.text3, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Box(Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape)) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.Red, Color(0xFFFF8A00), Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color(0xFF8B00FF), Color.Red)
                                ),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                        }
                        Slider(
                            value = accentHue,
                            onValueChange = { hue ->
                                accentHue = hue
                                val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.82f, 0.95f))
                                selectAccent(String.format("#%06X", 0xFFFFFF and argb))
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("COULEURS RAPIDES", color = Zcolors.text3, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accentSwatches.forEach { (label, hex) ->
                            val swatch = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(29.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (accentHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                        color = if (accentHex.equals(hex, ignoreCase = true)) Zcolors.text else Zcolors.border2,
                                        shape = CircleShape
                                    )
                                    .clickable(onClickLabel = label) { selectAccent(hex) }
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        accentSwatches.forEach { (label, _) ->
                            Text(
                                text = label.take(1),
                                color = Zcolors.text3,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(29.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

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

            // MA72 : coupe les transitions de navigation (fondu + glissement).
            SettingsSectionLabel("ANIMATIONS")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateReduceAnimations(!viewModel.reduceAnimations) }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Zcolors.cyanBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Animation, contentDescription = null, tint = Zcolors.cyan)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Réduire les animations", style = MaterialTheme.typography.titleMedium, color = Zcolors.text)
                        Spacer(Modifier.height(2.dp))
                        Text("Transitions de navigation instantanées", style = MaterialTheme.typography.bodySmall, color = Zcolors.text2)
                    }
                    Switch(
                        checked = viewModel.reduceAnimations,
                        onCheckedChange = { viewModel.updateReduceAnimations(it) }
                    )
                }
            }

            // MàJ 91 — retour haptique ciblé (menus, curseurs, envoi, fin de tâche).
            SettingsSectionLabel("RETOUR HAPTIQUE")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Zcolors.violetBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = Zcolors.violet)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Retour haptique", style = MaterialTheme.typography.titleMedium, color = Zcolors.text)
                        Spacer(Modifier.height(2.dp))
                        Text("Menus, curseurs, envoi et fin de tâche", style = MaterialTheme.typography.bodySmall, color = Zcolors.text2)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HapticLevel.entries.forEach { niveau ->
                        FilterChip(
                            selected = viewModel.hapticLevel == niveau,
                            onClick = { viewModel.updateHapticLevel(niveau) },
                            label = { Text(niveau.libelle) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // MA81 : l'éditeur garde un fond de code sombre, même en interface claire.
            SettingsSectionLabel("ÉDITEUR")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateCodeAlwaysDark(!viewModel.codeAlwaysDark) }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Zcolors.violetBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Zcolors.violet)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Code toujours sombre", style = MaterialTheme.typography.titleMedium, color = Zcolors.text)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "L'éditeur garde son fond nuit et sa palette adaptée ; désactivé, le code suit le thème clair.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Zcolors.text2
                        )
                    }
                    Switch(
                        checked = viewModel.codeAlwaysDark,
                        onCheckedChange = { viewModel.updateCodeAlwaysDark(it) }
                    )
                }
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
            .statusBarsPadding()
            .navigationBarsPadding()
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
            .clip(MaterialTheme.shapes.large)
            .background(c.surface)
            .border(1.dp, c.border, MaterialTheme.shapes.large)
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
                .clip(MaterialTheme.shapes.small)
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
