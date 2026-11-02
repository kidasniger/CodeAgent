package com.zagent.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.StylePresetIds
import com.zagent.app.ui.theme.ZAccentOverride

class SettingsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "zagent_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        migrateThemeKey()
        accentColorHex.takeIf { it.isNotBlank() }?.let { hex ->
            runCatching { ZAccentOverride.value = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
        }
    }

    /**
     * Migration de l'ancienne clé booléenne « theme_dark » vers le mode à 3 états :
     * true → DARK, false → LIGHT, puis suppression de l'ancienne clé.
     * Ne s'exécute qu'une seule fois (l'ancienne clé disparaît après migration).
     */
    private fun migrateThemeKey() {
        if (prefs.contains(LEGACY_KEY_THEME_DARK)) {
            val wasDark = prefs.getBoolean(LEGACY_KEY_THEME_DARK, true)
            prefs.edit()
                .putString(KEY_THEME_MODE, if (wasDark) ThemeMode.DARK.name else ThemeMode.LIGHT.name)
                .remove(LEGACY_KEY_THEME_DARK)
                .apply()
        }
    }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, "glm-4.5-flash") ?: "glm-4.5-flash"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "https://api.z.ai/api/paas/v4/chat/completions") ?: "https://api.z.ai/api/paas/v4/chat/completions"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var autoMode: Boolean
        get() = prefs.getBoolean(KEY_AUTO_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_MODE, value).apply()

    /** Mode intelligent : nouveaux fichiers et edit_file sans confirmation, écrasement et suppression confirmés. */
    var smartMode: Boolean
        get() = prefs.getBoolean(KEY_SMART_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_MODE, value).apply()

    var workingFolderUri: String?
        get() = prefs.getString(KEY_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_URI, value).apply()

    /** Mode de thème : Système / Clair / Sombre (voir [ThemeMode]). */
    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME_MODE, null) } ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    /** Preset de base conservé lorsque l'utilisateur passe en mode personnalisé. */
    var styleBasePresetId: String
        get() = prefs.getString(KEY_STYLE_BASE_PRESET, null) ?: stylePresetId.takeIf { it != StylePresetIds.CUSTOM } ?: StylePresetIds.GLASS
        set(value) = prefs.edit().putString(KEY_STYLE_BASE_PRESET, value).apply()

    /** Identifiant du preset actif ; CUSTOM indique une modification manuelle. */
    var stylePresetId: String
        get() = prefs.getString(KEY_STYLE_PRESET, StylePresetIds.GLASS) ?: StylePresetIds.GLASS
        set(value) = prefs.edit().putString(KEY_STYLE_PRESET, value).apply()

    /** À appeler lorsqu'un curseur ou réglage visuel est modifié manuellement. */
    fun markStylePresetCustom() {
        stylePresetId = StylePresetIds.CUSTOM
    }

    /** Couleur d'accent personnalisée ; chaîne vide = palette native (« Aucune »). */
    var accentColorHex: String
        get() = prefs.getString(KEY_ACCENT_COLOR, "") ?: ""
        set(value) {
            val normalized = value.trim()
            prefs.edit().putString(KEY_ACCENT_COLOR, normalized).apply()
            ZAccentOverride.value = if (normalized.isBlank()) null else runCatching {
                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(normalized))
            }.getOrNull()
        }

    /** Réglages Verre isolés pour les thèmes clair et sombre. Les anciennes clés globales servent de valeur de migration. */
    fun glassMaterialFor(dark: Boolean): String = prefs.getString(glassKey(KEY_GLASS_MATERIAL, dark), glassMaterial) ?: glassMaterial
    fun setGlassMaterialFor(dark: Boolean, value: String) = prefs.edit().putString(glassKey(KEY_GLASS_MATERIAL, dark), value).apply()
    fun glassColorFor(dark: Boolean): String = prefs.getString(glassKey(KEY_GLASS_COLOR, dark), glassColor) ?: glassColor
    fun setGlassColorFor(dark: Boolean, value: String) = prefs.edit().putString(glassKey(KEY_GLASS_COLOR, dark), value).apply()
    fun glassBlurFor(dark: Boolean): Float = prefs.getFloat(glassKey(KEY_GLASS_BLUR, dark), glassBlur).coerceIn(0f, 1f)
    fun setGlassBlurFor(dark: Boolean, value: Float) = prefs.edit().putFloat(glassKey(KEY_GLASS_BLUR, dark), value.coerceIn(0f, 1f)).apply()
    fun glassTintFor(dark: Boolean): Float = prefs.getFloat(glassKey(KEY_GLASS_TINT, dark), glassTint).coerceIn(0f, 1f)
    fun setGlassTintFor(dark: Boolean, value: Float) = prefs.edit().putFloat(glassKey(KEY_GLASS_TINT, dark), value.coerceIn(0f, 1f)).apply()
    fun glassFilmFor(dark: Boolean): Float = prefs.getFloat(glassKey(KEY_GLASS_FILM, dark), glassFilm).coerceIn(0f, 1f)
    fun setGlassFilmFor(dark: Boolean, value: Float) = prefs.edit().putFloat(glassKey(KEY_GLASS_FILM, dark), value.coerceIn(0f, 1f)).apply()
    fun glassActiveTabFor(dark: Boolean): String = prefs.getString(glassKey(KEY_GLASS_ACTIVE_TAB, dark), glassActiveTab) ?: glassActiveTab
    fun setGlassActiveTabFor(dark: Boolean, value: String) = prefs.edit().putString(glassKey(KEY_GLASS_ACTIVE_TAB, dark), value).apply()

    private fun glassKey(base: String, dark: Boolean) = "${base}_${if (dark) "dark" else "light"}"

    /** Noir OLED et rayon global des composants. */
    var oledBlack: Boolean
        get() = prefs.getBoolean(KEY_OLED_BLACK, false)
        set(value) = prefs.edit().putBoolean(KEY_OLED_BLACK, value).apply()

    var cornerRadius: Int
        get() = prefs.getInt(KEY_CORNER_RADIUS, 16).coerceIn(8, 28)
        set(value) = prefs.edit().putInt(KEY_CORNER_RADIUS, value.coerceIn(8, 28)).apply()

    var dynamicSystemColors: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, value).apply()

    var scheduledDarkTime: String
        get() = prefs.getString(KEY_SCHEDULED_DARK, "19:00") ?: "19:00"
        set(value) = prefs.edit().putString(KEY_SCHEDULED_DARK, value).apply()

    var scheduledLightTime: String
        get() = prefs.getString(KEY_SCHEDULED_LIGHT, "07:00") ?: "07:00"
        set(value) = prefs.edit().putString(KEY_SCHEDULED_LIGHT, value).apply()

    /** Configuration persistante de l'aperçu et des surfaces Verre. */
    var glassMaterial: String
        get() = prefs.getString(KEY_GLASS_MATERIAL, "Liquid") ?: "Liquid"
        set(value) = prefs.edit().putString(KEY_GLASS_MATERIAL, value).apply()

    var glassColor: String
        get() = prefs.getString(KEY_GLASS_COLOR, "#22D3EE") ?: "#22D3EE"
        set(value) = prefs.edit().putString(KEY_GLASS_COLOR, value).apply()

    var glassBlur: Float
        get() = prefs.getFloat(KEY_GLASS_BLUR, 0.55f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat(KEY_GLASS_BLUR, value.coerceIn(0f, 1f)).apply()

    var glassTint: Float
        get() = prefs.getFloat(KEY_GLASS_TINT, 0.35f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat(KEY_GLASS_TINT, value.coerceIn(0f, 1f)).apply()

    var glassFilm: Float
        get() = prefs.getFloat(KEY_GLASS_FILM, 0.22f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat(KEY_GLASS_FILM, value.coerceIn(0f, 1f)).apply()

    var glassActiveTab: String
        get() = prefs.getString(KEY_GLASS_ACTIVE_TAB, "Chat") ?: "Chat"
        set(value) = prefs.edit().putString(KEY_GLASS_ACTIVE_TAB, value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    /** Réinitialise uniquement les préférences d'apparence, sans toucher aux clés API ni aux projets. */
    fun resetAppearance() {
        prefs.edit()
            .putString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            .putString(KEY_STYLE_PRESET, StylePresetIds.GLASS)
            .putString(KEY_STYLE_BASE_PRESET, StylePresetIds.GLASS)
            .putString(KEY_ACCENT_COLOR, "")
            .putBoolean(KEY_OLED_BLACK, false)
            .putBoolean(KEY_DYNAMIC_COLORS, false)
            .putInt(KEY_CORNER_RADIUS, 16)
            .putString(KEY_SCHEDULED_DARK, "19:00")
            .putString(KEY_SCHEDULED_LIGHT, "07:00")
            .putString(KEY_GLASS_MATERIAL, "Liquid")
            .putString(KEY_GLASS_COLOR, "#22D3EE")
            .putFloat(KEY_GLASS_BLUR, 0.55f)
            .putFloat(KEY_GLASS_TINT, 0.35f)
            .putFloat(KEY_GLASS_FILM, 0.22f)
            .putString(KEY_GLASS_ACTIVE_TAB, "Chat")
            .putString(glassKey(KEY_GLASS_MATERIAL, false), "Liquid")
            .putString(glassKey(KEY_GLASS_COLOR, false), "#22D3EE")
            .putFloat(glassKey(KEY_GLASS_BLUR, false), 0.55f)
            .putFloat(glassKey(KEY_GLASS_TINT, false), 0.35f)
            .putFloat(glassKey(KEY_GLASS_FILM, false), 0.22f)
            .putString(glassKey(KEY_GLASS_ACTIVE_TAB, false), "Chat")
            .putString(glassKey(KEY_GLASS_MATERIAL, true), "Liquid")
            .putString(glassKey(KEY_GLASS_COLOR, true), "#22D3EE")
            .putFloat(glassKey(KEY_GLASS_BLUR, true), 0.55f)
            .putFloat(glassKey(KEY_GLASS_TINT, true), 0.35f)
            .putFloat(glassKey(KEY_GLASS_FILM, true), 0.22f)
            .putString(glassKey(KEY_GLASS_ACTIVE_TAB, true), "Chat")
            .apply()
        ZAccentOverride.value = null
    }

    /** Fichiers que l'utilisateur autorise toujours (écritures automatiques). */
    var alwaysAllowedPaths: Set<String>
        get() = prefs.getStringSet(KEY_ALWAYS_ALLOW, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ALWAYS_ALLOW, value).apply()

    /** Instructions personnalisées ajoutées au prompt de l'agent à chaque demande. */
    var customInstructions: String
        get() = prefs.getString(KEY_CUSTOM_INSTR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_INSTR, value).apply()

    /** Nombre maximal d'étapes de l'agent par demande (5 à 100). */
    var maxSteps: Int
        get() = prefs.getInt(KEY_MAX_STEPS, 20).coerceIn(5, 100)
        set(value) = prefs.edit().putInt(KEY_MAX_STEPS, value.coerceIn(5, 100)).apply()

    /** Prix par million de tokens en entrée (0 = inconnu), pour le calcul du coût. */
    var priceInPerM: Float
        get() = prefs.getFloat(KEY_PRICE_IN, 0f)
        set(value) = prefs.edit().putFloat(KEY_PRICE_IN, value.coerceAtLeast(0f)).apply()

    /** Prix par million de tokens en sortie (0 = inconnu). */
    var priceOutPerM: Float
        get() = prefs.getFloat(KEY_PRICE_OUT, 0f)
        set(value) = prefs.edit().putFloat(KEY_PRICE_OUT, value.coerceAtLeast(0f)).apply()

    companion object {
        private const val KEY_MAX_STEPS = "max_steps"
        private const val KEY_PRICE_IN = "price_in_per_m"
        private const val KEY_PRICE_OUT = "price_out_per_m"
        private const val KEY_CUSTOM_INSTR = "custom_instructions"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTO_MODE = "auto_mode"
        private const val KEY_SMART_MODE = "smart_mode"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val LEGACY_KEY_THEME_DARK = "theme_dark"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_STYLE_PRESET = "style_preset"
        private const val KEY_STYLE_BASE_PRESET = "style_base_preset"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_GLASS_MATERIAL = "glass_material"
        private const val KEY_GLASS_COLOR = "glass_color"
        private const val KEY_GLASS_BLUR = "glass_blur"
        private const val KEY_GLASS_TINT = "glass_tint"
        private const val KEY_GLASS_FILM = "glass_film"
        private const val KEY_GLASS_ACTIVE_TAB = "glass_active_tab"
        private const val KEY_OLED_BLACK = "oled_black"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_DYNAMIC_COLORS = "dynamic_system_colors"
        private const val KEY_SCHEDULED_DARK = "scheduled_dark_time"
        private const val KEY_SCHEDULED_LIGHT = "scheduled_light_time"
        private const val KEY_ALWAYS_ALLOW = "always_allow_paths"
    }
}
