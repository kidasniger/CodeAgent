package com.zagent.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zagent.app.data.SettingsManager
import com.zagent.app.ui.components.GlassPreviewCard
import com.zagent.app.ui.theme.StylePresetIds
import com.zagent.app.ui.theme.Zcolors

private val glassColorOptions = listOf(
    "#22D3EE" to "Cyan",
    "#8B5CF6" to "Violet",
    "#D97757" to "Corail",
    "#34D399" to "Menthe",
    "#F5F5F5" to "Blanc",
    "#F59E0B" to "Ambre"
)

private val glassMaterials = listOf("Liquid", "Frosted", "Solid")
private val glassTabs = listOf("Chat", "Fichiers", "Éditeur", "Terminal", "Réglages")

@Composable
fun GlassSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { SettingsManager(context) }

    var material by remember { mutableStateOf(settings.glassMaterial) }
    var colorHex by remember { mutableStateOf(settings.glassColor) }
    var blur by remember { mutableStateOf(settings.glassBlur) }
    var tint by remember { mutableStateOf(settings.glassTint) }
    var film by remember { mutableStateOf(settings.glassFilm) }
    var activeTab by remember { mutableStateOf(settings.glassActiveTab) }
    var isCustom by remember { mutableStateOf(settings.stylePresetId == StylePresetIds.CUSTOM) }

    fun markCustom() {
        settings.markStylePresetCustom()
        isCustom = true
    }

    val previewColor = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrDefault(Color.Cyan)
    }

    SettingsPageScaffold(title = "Verre", onBack = onBack) {
        GlassPreviewCard(
            modifier = Modifier.fillMaxWidth(),
            material = material,
            glassColor = previewColor,
            blur = blur,
            tint = tint,
            film = film,
            activeTab = activeTab
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isCustom) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Perso") }
                )
            }
        }

        SettingsSectionLabel("MATÉRIAU")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                glassMaterials.forEach { option ->
                    FilterChip(
                        selected = material == option,
                        onClick = {
                            material = option
                            settings.glassMaterial = option
                            markCustom()
                        },
                        label = { Text(option) }
                    )
                }
            }
        }

        SettingsSectionLabel("COULEUR")
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Couleur du verre", color = Zcolors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    glassColorOptions.forEach { (hex, label) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(color, CircleShape)
                                    .then(
                                        if (colorHex == hex) Modifier.border(
                                            width = 3.dp,
                                            color = Zcolors.text,
                                            shape = CircleShape
                                        ) else Modifier.border(1.dp, Zcolors.border2, CircleShape)
                                    )
                                    .clickable {
                                        colorHex = hex
                                        settings.glassColor = hex
                                        markCustom()
                                    }
                            )
                            Text(label, color = Zcolors.text2)
                        }
                    }
                }
            }
        }

        SettingsSectionLabel("EFFETS")
        SettingsCard {
            GlassSlider(
                title = "Flou",
                value = blur,
                onValueChange = {
                    blur = it
                    settings.glassBlur = it
                    markCustom()
                }
            )
            SettingsInsetDivider()
            GlassSlider(
                title = "Teinte",
                value = tint,
                onValueChange = {
                    tint = it
                    settings.glassTint = it
                    markCustom()
                }
            )
            SettingsInsetDivider()
            GlassSlider(
                title = "Film",
                value = film,
                onValueChange = {
                    film = it
                    settings.glassFilm = it
                    markCustom()
                }
            )
        }

        SettingsSectionLabel("ONGLET ACTIF")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                glassTabs.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = {
                            activeTab = tab
                            settings.glassActiveTab = tab
                            markCustom()
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Zcolors.text)
            Text("${(value * 100).toInt()} %", color = Zcolors.text2)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
