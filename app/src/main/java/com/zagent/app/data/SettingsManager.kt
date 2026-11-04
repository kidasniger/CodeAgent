package com.zagent.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zagent.app.ui.theme.ThemeMode
import com.zagent.app.ui.theme.StylePresetIds
import com.zagent.app.ui.theme.ZAccentOverrideDark
import com.zagent.app.ui.theme.ZAccentOverrideLight
import com.zagent.app.ui.theme.GlassMaterial
import com.zagent.app.ui.theme.GlassSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * MàJ 91 — niveau de retour haptique choisi dans Réglages › Apparence.
 * DÉSACTIVÉ coupe tout retour, LÉGER envoie des vibrations courtes et faibles,
 * NORMAL le niveau standard.
 */
enum class HapticLevel(val libelle: String) {
    DESACTIVE("Désactivé"),
    LEGER("Léger"),
    NORMAL("Normal")
}

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
        migrateAccentKey()
        refreshAccentOverrides()
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

    /**
     * Migration de l'ancienne clé d'accent globale « accent_color » vers deux clés
     * distinctes clair/sombre ; la couleur existante est copiée dans les deux modes.
     * Ne s'exécute qu'une seule fois (l'ancienne clé disparaît après migration).
     */
    private fun migrateAccentKey() {
        if (prefs.contains(KEY_ACCENT_COLOR)) {
            val hex = prefs.getString(KEY_ACCENT_COLOR, "") ?: ""
            prefs.edit()
                .putString(KEY_ACCENT_LIGHT, hex)
                .putString(KEY_ACCENT_DARK, hex)
                .remove(KEY_ACCENT_COLOR)
                .apply()
        }
    }

    private fun parseAccent(hex: String): Color? = runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrNull()

    /** Rend les accents persistants visibles immédiatement par `Zcolors` au démarrage. */
    private fun refreshAccentOverrides() {
        ZAccentOverrideLight.value = accentColorHexFor(false).takeIf { it.isNotBlank() }?.let { parseAccent(it) }
        ZAccentOverrideDark.value = accentColorHexFor(true).takeIf { it.isNotBlank() }?.let { parseAccent(it) }
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

    /** Couleur d'accent personnalisée pour le mode clair ou sombre ; chaîne vide = palette native (« Aucune »). */
    fun accentColorHexFor(dark: Boolean): String =
        prefs.getString(if (dark) KEY_ACCENT_DARK else KEY_ACCENT_LIGHT, "") ?: ""

    /** Enregistre l'accent du mode choisi et met à jour l'override observable par `Zcolors`. */
    fun setAccentColorHexFor(dark: Boolean, value: String) {
        val normalized = value.trim()
        prefs.edit().putString(if (dark) KEY_ACCENT_DARK else KEY_ACCENT_LIGHT, normalized).apply()
        val parsed = normalized.takeIf { it.isNotBlank() }?.let { parseAccent(it) }
        if (dark) ZAccentOverrideDark.value = parsed else ZAccentOverrideLight.value = parsed
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


    var glassEnabled: Boolean
        get() = prefs.getBoolean(KEY_GLASS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GLASS_ENABLED, value).apply()

    var glassAlwaysRespectSetting: Boolean
        get() = prefs.getBoolean(KEY_GLASS_RESPECT, false)
        set(value) = prefs.edit().putBoolean(KEY_GLASS_RESPECT, value).apply()

    fun loadGlassSettings(dark: Boolean): GlassSettings {
        val materialName = prefs.getString(glassKey(KEY_GLASS_MATERIAL_V2, dark), "BALANCED") ?: "BALANCED"
        val hex = prefs.getString(glassKey(KEY_GLASS_COLOUR_V2, dark), "") ?: ""
        val colour = if (hex.isBlank()) Color.Unspecified else runCatching {
            Color(android.graphics.Color.parseColor(hex))
        }.getOrDefault(Color.Unspecified)
        return GlassSettings(
            material = GlassMaterial.entries.firstOrNull { it.name == materialName } ?: GlassMaterial.BALANCED,
            colour = colour,
            frost = prefs.getFloat(glassKey(KEY_GLASS_FROST_DP, dark), 6f).coerceIn(0f, 24f).dp,
            tint = prefs.getFloat(glassKey(KEY_GLASS_COLOUR_INTENSITY, dark), 0.70f).coerceIn(0f, 1f),
            film = prefs.getFloat(glassKey(KEY_GLASS_FILM_V2, dark), if (dark) 0.35f else 0.60f).coerceIn(0f, 1f),
            activeTab = prefs.getFloat(glassKey(KEY_GLASS_ACTIVE_INTENSITY, dark), 0.26f).coerceIn(0f, 1f),
            enabled = glassEnabled,
            alwaysRespectSetting = glassAlwaysRespectSetting
        )
    }

    fun saveGlassSettings(dark: Boolean, value: GlassSettings) {
        val hex = if (value.colour == Color.Unspecified) "" else String.format(
            "#%06X", 0xFFFFFF and android.graphics.Color.rgb(
                (value.colour.red * 255).toInt(), (value.colour.green * 255).toInt(), (value.colour.blue * 255).toInt()
            )
        )
        prefs.edit()
            .putString(glassKey(KEY_GLASS_MATERIAL_V2, dark), value.material.name)
            .putString(glassKey(KEY_GLASS_COLOUR_V2, dark), hex)
            .putFloat(glassKey(KEY_GLASS_FROST_DP, dark), value.frost.value.coerceIn(0f, 24f))
            .putFloat(glassKey(KEY_GLASS_COLOUR_INTENSITY, dark), value.tint.coerceIn(0f, 1f))
            .putFloat(glassKey(KEY_GLASS_FILM_V2, dark), value.film.coerceIn(0f, 1f))
            .putFloat(glassKey(KEY_GLASS_ACTIVE_INTENSITY, dark), value.activeTab.coerceIn(0f, 1f))
            .putBoolean(KEY_GLASS_ENABLED, value.enabled)
            .putBoolean(KEY_GLASS_RESPECT, value.alwaysRespectSetting)
            .apply()
    }

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

    /** Préférences typographiques globales. */
    var fontFamilyId: String
        get() = prefs.getString(KEY_FONT_FAMILY, "INTER") ?: "INTER"
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()

    var fontScale: Float
        // MàJ 97 — plage étendue à 130 % (au lieu de 125 %).
        get() = prefs.getFloat(KEY_FONT_SCALE, 1f).coerceIn(0.85f, 1.30f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SCALE, value.coerceIn(0.85f, 1.30f)).apply()

    /** Échelle de densité appliquée aux dimensions dp de l'interface. */
    var densityScale: Float
        get() = prefs.getFloat(KEY_DENSITY_SCALE, 1f).coerceIn(0.88f, 1.12f)
        set(value) = prefs.edit().putFloat(KEY_DENSITY_SCALE, value.coerceIn(0.88f, 1.12f)).apply()

    /**
     * MàJ 100 — langue de l'interface choisie dans Réglages › Apparence :
     * "" = langue du système, "fr" ou "en". Appliquée via AppCompatDelegate
     * (langue par application), persistée ici pour survivre aux redémarrages.
     */
    var langueUi: String
        get() = prefs.getString(KEY_LANGUE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LANGUE, value).apply()

    /** Affiche la ligne tokens/coût sous chaque message d'agent (MA45). */
    var usageUnderMessages: Boolean
        get() = prefs.getBoolean(KEY_USAGE_UNDER, false)
        set(value) = prefs.edit().putBoolean(KEY_USAGE_UNDER, value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    /** Dernier identifiant de conversation : sert de clé aux brouillons (MA55). */
    var lastConversationId: String
        get() = prefs.getString(KEY_LAST_CONVERSATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CONVERSATION, value).apply()

    /** Brouillon non envoyé d'une conversation, restauré à la réouverture (MA55). */
    fun draftFor(conversationId: String): String =
        prefs.getString(KEY_DRAFT_PREFIX + conversationId, "") ?: ""

    fun saveDraft(conversationId: String, text: String) {
        prefs.edit().putString(KEY_DRAFT_PREFIX + conversationId, text).apply()
    }

    fun clearDraft(conversationId: String) {
        prefs.edit().remove(KEY_DRAFT_PREFIX + conversationId).apply()
    }

    /** Réinitialise uniquement les préférences d'apparence, sans toucher aux clés API ni aux projets. */
    fun resetAppearance() {
        prefs.edit()
            .putString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            .putString(KEY_STYLE_PRESET, StylePresetIds.GLASS)
            .putString(KEY_STYLE_BASE_PRESET, StylePresetIds.GLASS)
            .putString(KEY_ACCENT_LIGHT, "")
            .putString(KEY_ACCENT_DARK, "")
            .putBoolean(KEY_OLED_BLACK, false)
            .putBoolean(KEY_DYNAMIC_COLORS, false)
            .putInt(KEY_CORNER_RADIUS, 16)
            .putString(KEY_FONT_FAMILY, "INTER")
            .putFloat(KEY_FONT_SCALE, 1f)
            .putFloat(KEY_DENSITY_SCALE, 1f)
            .putString(KEY_SCHEDULED_DARK, "19:00")
            .putString(KEY_SCHEDULED_LIGHT, "07:00")
            .putString(KEY_GLASS_MATERIAL, "Liquid")
            .putString(KEY_GLASS_COLOR, "#22D3EE")
            .putFloat(KEY_GLASS_BLUR, 0.55f)
            .putFloat(KEY_GLASS_TINT, 0.35f)
            .putFloat(KEY_GLASS_FILM, 0.22f)
            .putString(KEY_GLASS_ACTIVE_TAB, "Chat")
            .putBoolean(KEY_GLASS_ENABLED, true)
            .putBoolean(KEY_GLASS_RESPECT, false)
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
            .putString(glassKey(KEY_GLASS_MATERIAL_V2, false), "BALANCED")
            .putString(glassKey(KEY_GLASS_COLOUR_V2, false), "")
            .putFloat(glassKey(KEY_GLASS_FROST_DP, false), 6f)
            .putFloat(glassKey(KEY_GLASS_COLOUR_INTENSITY, false), 0.70f)
            .putFloat(glassKey(KEY_GLASS_FILM_V2, false), 0.60f)
            .putFloat(glassKey(KEY_GLASS_ACTIVE_INTENSITY, false), 0.26f)
            .putString(glassKey(KEY_GLASS_MATERIAL_V2, true), "BALANCED")
            .putString(glassKey(KEY_GLASS_COLOUR_V2, true), "")
            .putFloat(glassKey(KEY_GLASS_FROST_DP, true), 6f)
            .putFloat(glassKey(KEY_GLASS_COLOUR_INTENSITY, true), 0.70f)
            .putFloat(glassKey(KEY_GLASS_FILM_V2, true), 0.35f)
            .putFloat(glassKey(KEY_GLASS_ACTIVE_INTENSITY, true), 0.26f)
            // MàJ 91 : retour haptique normal par défaut.
            .putString(KEY_HAPTIC_LEVEL, HapticLevel.NORMAL.name)
            // LOT 7 : rendus et tailles par défaut de l'éditeur (MA81/MA83).
            .putBoolean(KEY_CODE_ALWAYS_DARK, true)
            .putInt(KEY_EDITOR_FONT_SIZE, 14)
            .apply()
        ZAccentOverrideLight.value = null
        ZAccentOverrideDark.value = null
    }

    // ── LOT 6 ────────────────────────────────────────────────────────────────

    /**
     * MA72 : quand cette option est active, les transitions de navigation
     * (fondu + glissement horizontal, glissement du bas des feuilles) sont
     * désactivées et les écrans changent instantanément.
     */
    var reduceAnimations: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_ANIMATIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_REDUCE_ANIMATIONS, value).apply()

    /** MàJ 91 — niveau de retour haptique (Désactivé / Léger / Normal). */
    var hapticLevel: HapticLevel
        get() = HapticLevel.entries.firstOrNull {
            it.name == prefs.getString(KEY_HAPTIC_LEVEL, null)
        } ?: HapticLevel.NORMAL
        set(value) = prefs.edit().putString(KEY_HAPTIC_LEVEL, value.name).apply()

    /**
     * MA79 : onglets de l'éditeur ouverts dans un projet (index actif puis chemins relatifs,
     * un par ligne), pour les rouvrir au redémarrage.
     */
    fun loadEditorSession(project: String): Pair<List<String>, Int> {
        val raw = prefs.getString(KEY_EDITOR_SESSION + project.hashCode(), null)
            ?: return Pair(emptyList(), 0)
        val lines = raw.split('\n')
        val idx = lines.firstOrNull()?.toIntOrNull() ?: 0
        return Pair(lines.drop(1).filter { it.isNotBlank() }, idx)
    }

    fun saveEditorSession(project: String, paths: List<String>, active: Int) {
        val key = KEY_EDITOR_SESSION + project.hashCode()
        if (paths.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, (listOf(active.toString()) + paths).joinToString("\n")).apply()
        }
    }

    /** Fichiers que l'utilisateur autorise toujours (écritures automatiques). */
    var alwaysAllowedPaths: Set<String>
        get() = prefs.getStringSet(KEY_ALWAYS_ALLOW, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ALWAYS_ALLOW, value).apply()

    // ── LOT 7 ──────────────────────────────────────────────────────────────

    /**
     * MA81 : garde le fond de code sombre et la palette « nuit » de l'éditeur
     * même quand l'interface est en mode clair. Activé par défaut : l'éditeur
     * garde son rendu habituel ; désactivé, le code suit le thème (fond clair,
     * coloration adaptée).
     */
    var codeAlwaysDark: Boolean
        get() = prefs.getBoolean(KEY_CODE_ALWAYS_DARK, true)
        set(value) = prefs.edit().putBoolean(KEY_CODE_ALWAYS_DARK, value).apply()

    /**
     * MA83 : taille de police de l'éditeur en sp (10 à 22), réglable au
     * pincement sur l'éditeur ou depuis Réglages › Apparence › Police.
     */
    var editorFontSize: Int
        get() = prefs.getInt(KEY_EDITOR_FONT_SIZE, 14).coerceIn(10, 22)
        set(value) = prefs.edit().putInt(KEY_EDITOR_FONT_SIZE, value.coerceIn(10, 22)).apply()

    // ── LOT 5 ────────────────────────────────────────────────────────────────

    /** Projets épinglés en favoris, affichés en haut de l'écran Projets (MA57). */
    var favoriteProjects: Set<String>
        get() = prefs.getStringSet(KEY_FAV_PROJECTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAV_PROJECTS, value).apply()

    /** Interrupteurs d'autorisation par type d'action de l'agent (MA64). */
    var allowRead: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_READ, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_READ, value).apply()

    var allowWrite: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_WRITE, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_WRITE, value).apply()

    var allowDelete: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_DELETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_DELETE, value).apply()

    var allowExecute: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_EXECUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_EXECUTE, value).apply()

    private val prefJson = Json { ignoreUnknownKeys = true }

    /** Historique des dernières recherches globales (MA58), du plus récent au plus ancien. */
    val searchHistory: List<String>
        get() = runCatching {
            val raw = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
            if (raw.isBlank()) emptyList()
            else prefJson.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrNull().orEmpty()

    /** Ajoute une requête à l'historique (dédupliquée, plafonnée à 10 entrées). */
    fun addSearchQuery(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val next = (listOf(q) + searchHistory.filter { !it.equals(q, ignoreCase = true) }).take(10)
        runCatching {
            prefs.edit().putString(KEY_SEARCH_HISTORY, prefJson.encodeToString(ListSerializer(String.serializer()), next)).apply()
        }
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    private val instrSerializer = MapSerializer(String.serializer(), String.serializer())

    /** Instructions personnalisées par projet (nom du dossier → texte), MA63. */
    private fun loadProjectInstructions(): Map<String, String> = runCatching {
        val raw = prefs.getString(KEY_PROJECT_INSTR, "") ?: ""
        if (raw.isBlank()) emptyMap()
        else prefJson.decodeFromString(instrSerializer, raw)
    }.getOrNull().orEmpty()

    fun projectInstructionsFor(project: String): String =
        loadProjectInstructions()[project].orEmpty()

    fun setProjectInstructions(project: String, value: String) {
        val map = loadProjectInstructions().toMutableMap()
        if (value.isBlank()) map.remove(project) else map[project] = value
        runCatching {
            prefs.edit().putString(KEY_PROJECT_INSTR, prefJson.encodeToString(instrSerializer, map)).apply()
        }
    }

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
        private const val KEY_ACCENT_LIGHT = "accent_color_light"
        private const val KEY_ACCENT_DARK = "accent_color_dark"
        private const val KEY_GLASS_MATERIAL = "glass_material"
        private const val KEY_GLASS_COLOR = "glass_color"
        private const val KEY_GLASS_BLUR = "glass_blur"
        private const val KEY_GLASS_TINT = "glass_tint"
        private const val KEY_GLASS_FILM = "glass_film"
        private const val KEY_GLASS_ACTIVE_TAB = "glass_active_tab"
        private const val KEY_GLASS_ENABLED = "glass_enabled"
        private const val KEY_GLASS_RESPECT = "glass_always_respect"
        private const val KEY_GLASS_MATERIAL_V2 = "glass_material_v2"
        private const val KEY_GLASS_COLOUR_V2 = "glass_colour_v2"
        private const val KEY_GLASS_FROST_DP = "glass_frost_dp"
        private const val KEY_GLASS_COLOUR_INTENSITY = "glass_colour_intensity"
        private const val KEY_GLASS_FILM_V2 = "glass_film_v2"
        private const val KEY_GLASS_ACTIVE_INTENSITY = "glass_active_intensity"
        private const val KEY_OLED_BLACK = "oled_black"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_DENSITY_SCALE = "density_scale"
        private const val KEY_LANGUE = "langue_ui"
        private const val KEY_USAGE_UNDER = "usage_under_messages"
        private const val KEY_DYNAMIC_COLORS = "dynamic_system_colors"
        private const val KEY_SCHEDULED_DARK = "scheduled_dark_time"
        private const val KEY_SCHEDULED_LIGHT = "scheduled_light_time"
        private const val KEY_ALWAYS_ALLOW = "always_allow_paths"
        private const val KEY_LAST_CONVERSATION = "last_conversation_id"
        private const val KEY_DRAFT_PREFIX = "draft_"
        private const val KEY_FAV_PROJECTS = "favorite_projects"
        private const val KEY_ALLOW_READ = "allow_read"
        private const val KEY_ALLOW_WRITE = "allow_write"
        private const val KEY_ALLOW_DELETE = "allow_delete"
        private const val KEY_ALLOW_EXECUTE = "allow_execute"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_PROJECT_INSTR = "project_instructions"
        private const val KEY_REDUCE_ANIMATIONS = "reduce_animations"
        private const val KEY_HAPTIC_LEVEL = "haptic_level"
        private const val KEY_EDITOR_SESSION = "editor_session_"
        private const val KEY_CODE_ALWAYS_DARK = "code_always_dark"
        private const val KEY_EDITOR_FONT_SIZE = "editor_font_size"
    }
}
