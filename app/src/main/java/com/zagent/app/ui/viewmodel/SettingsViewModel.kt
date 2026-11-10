package com.zagent.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.zagent.app.data.HapticLevel
import com.zagent.app.data.SettingsManager
import com.zagent.app.ui.theme.GlassSettings
import com.zagent.app.ui.theme.ThemeMode

/**
 * corr-004 — Les états de réglages quittent MainViewModel.
 *
 * Chaque propriété est un miroir de [SettingsManager] : l'UI lit ici, les
 * boutons d'écriture passent par les fonctions `update*` qui répercutent la
 * valeur dans le stockage persistant. MainViewModel expose les mêmes
 * propriétés par délégation, donc aucun écran n'est modifié.
 *
 * Ne sont pas extraits ici : les fournisseurs IA et favoris de modèles
 * (état métier du chat), `modelInput`/`baseUrlInput`/`autoModeInput`
 * (configuration du moteur) et `workingFolderUriString` (projet ouvert).
 */
class SettingsViewModel(
    private val settings: SettingsManager
) : ViewModel() {

    /** Affichage en clair du champ de clé API (corr-022 : champ mot de passe). */
    var showKeyVisible by mutableStateOf(false)

    // ── Apparence & flux de démarrage ─────────────────────────────────────
    var themeMode by mutableStateOf(settings.themeMode)
        private set
    var oledBlack by mutableStateOf(settings.oledBlack)
        private set
    var dynamicSystemColors by mutableStateOf(settings.dynamicSystemColors)
        private set
    var cornerRadius by mutableStateOf(settings.cornerRadius)
        private set
    var scheduledDarkTime by mutableStateOf(settings.scheduledDarkTime)
        private set
    var scheduledLightTime by mutableStateOf(settings.scheduledLightTime)
        private set
    var fontFamilyId by mutableStateOf(settings.fontFamilyId)
        private set
    var fontScale by mutableStateOf(settings.fontScale)
        private set
    var densityScale by mutableStateOf(settings.densityScale)
        private set

    fun updateFontFamily(value: String) { fontFamilyId = value; settings.fontFamilyId = value }
    // MàJ 97 — plage d'échelle de texte 85 % – 130 %.
    fun updateFontScale(value: Float) { fontScale = value.coerceIn(0.85f, 1.30f); settings.fontScale = fontScale }
    fun updateDensityScale(value: Float) { densityScale = value.coerceIn(0.88f, 1.12f); settings.densityScale = densityScale }

    fun updateOledBlack(value: Boolean) { oledBlack = value; settings.oledBlack = value }
    fun updateDynamicSystemColors(value: Boolean) { dynamicSystemColors = value; settings.dynamicSystemColors = value }
    fun updateCornerRadius(value: Int) { cornerRadius = value; settings.cornerRadius = value }
    fun updateScheduledDarkTime(value: String) { scheduledDarkTime = value; settings.scheduledDarkTime = value }
    fun updateScheduledLightTime(value: String) { scheduledLightTime = value; settings.scheduledLightTime = value }

    var glassSettings by mutableStateOf(settings.loadGlassSettings(false))

    /** MA72 : coupe les transitions de navigation (fondu/glissement) quand il est actif. */
    var reduceAnimations by mutableStateOf(settings.reduceAnimations)
        private set

    fun updateReduceAnimations(value: Boolean) {
        reduceAnimations = value
        settings.reduceAnimations = value
    }

    /** MàJ 91 — niveau de retour haptique (Désactivé / Léger / Normal). */
    var hapticLevel by mutableStateOf(settings.hapticLevel)
        private set

    fun updateHapticLevel(value: HapticLevel) {
        hapticLevel = value
        settings.hapticLevel = value
    }

    /**
     * MàJ 100 — langue de l'interface ("" = système, "fr", "en"). L'application
     * effective est faite par MainActivity via AppCompatDelegate.
     */
    var langueUi by mutableStateOf(settings.langueUi)
        private set

    fun updateLangue(value: String) {
        langueUi = value
        settings.langueUi = value
    }

    /** MA81 : « Code toujours sombre » — fond sombre de l'éditeur même en UI claire. */
    var codeAlwaysDark by mutableStateOf(settings.codeAlwaysDark)
        private set

    fun updateCodeAlwaysDark(value: Boolean) {
        codeAlwaysDark = value
        settings.codeAlwaysDark = value
    }

    /** MA83 : taille de police de l'éditeur (10 à 22 sp), mémorisée. */
    var editorFontSize by mutableStateOf(settings.editorFontSize)
        private set

    fun updateEditorFontSize(value: Int) {
        val target = value.coerceIn(10, 22)
        if (target == editorFontSize) return
        editorFontSize = target
        settings.editorFontSize = target
    }

    fun updateGlassSettings(dark: Boolean, value: GlassSettings) {
        glassSettings = value
        settings.saveGlassSettings(dark, value)
    }

    fun reloadGlassSettings(dark: Boolean) { glassSettings = settings.loadGlassSettings(dark) }

    /** Usage tokens/coût sous les messages (MA45). */
    var usageUnderMessages by mutableStateOf(settings.usageUnderMessages)

    fun toggleUsageUnderMessages() {
        usageUnderMessages = !usageUnderMessages
        settings.usageUnderMessages = usageUnderMessages
    }

    var onboardingDone by mutableStateOf(settings.onboardingDone)

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        settings.themeMode = mode
    }

    fun completeOnboarding() {
        onboardingDone = true
        settings.onboardingDone = true
    }

    // ── Limite d'étapes ────────────────────────────────────────────────────
    var maxStepsInput by mutableStateOf(settings.maxSteps)

    fun setMaxSteps(value: Int) {
        val n = value.coerceIn(5, 100)
        maxStepsInput = n
        settings.maxSteps = n
    }

    // ── Prix par million de tokens (coût estimé de la conversation) ────────
    var priceInInput by mutableStateOf(settings.priceInPerM)
    var priceOutInput by mutableStateOf(settings.priceOutPerM)

    fun setPrices(priceIn: Float, priceOut: Float) {
        priceInInput = priceIn.coerceAtLeast(0f)
        priceOutInput = priceOut.coerceAtLeast(0f)
        settings.priceInPerM = priceInInput
        settings.priceOutPerM = priceOutInput
    }
}
