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

/**
 * corr-018 — Persistance chiffrée des réglages (EncryptedSharedPreferences).
 *
 * Point d'entrée unique du stockage : clés API par fournisseur, modèle, dossier
 * de travail, apparence, polices, verre, haptique, langue, onboarding,
 * brouillons par conversation (MA55), sessions d'éditeur, favoris, instructions
 * de projet. Les ViewModel en gardent des miroirs d'état (SettingsViewModel).
 */
class SettingsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * err-084 — Si le fichier de préférences chiffrées est corrompu (clé
     * Keystore invalidée par une restauration, stockage altéré), la création
     * d'EncryptedSharedPreferences lève et l'application crasherait au
     * démarrage en boucle. On supprime alors le fichier illisible et on
     * repart sur des préférences vides : l'utilisateur reconfigure, mais
     * l'application démarre.
     */
    /** err-085 — vrai si le coffre chiffré a été réinitialisé (restauration, Keystore invalidé) : l'utilisateur doit ressaisir ses clés. */
    var clesReinitialisees: Boolean = false
        private set

    private val prefs = try {
        creerPrefs(context, masterKey)
    } catch (e: Exception) {
        context.deleteSharedPreferences("zagent_secure_prefs")
        creerPrefs(context, masterKey).also { clesReinitialisees = true }
    }

    private fun creerPrefs(context: Context, masterKey: MasterKey) =
        EncryptedSharedPreferences.create(
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

    // ── MàJ 101 : fournisseurs multiples (une clé API par fournisseur) ───────

    /**
     * Fournisseurs enregistrés (intégrés + personnalisés), avec leur clé dédiée.
     * Au premier accès après MàJ 101, l'ancienne clé unique [KEY_API_KEY] migre
     * automatiquement vers le fournisseur z.ai : l'utilisateur n'a rien à refaire.
     */
    var fournisseurs: List<Fournisseur>
        get() {
            val brut = prefs.getString(KEY_FOURNISSEURS, null)
            if (brut == null) {
                val ancienne = prefs.getString(KEY_API_KEY, "") ?: ""
                val liste = Fournisseurs.integres().map {
                    if (it.id == Fournisseurs.ID_ZAI) it.copy(cle = ancienne) else it
                }
                prefs.edit().putString(KEY_FOURNISSEURS, Fournisseurs.encode(liste)).apply()
                return liste
            }
            return Fournisseurs.decode(brut)
        }
        set(value) = prefs.edit().putString(KEY_FOURNISSEURS, Fournisseurs.encode(value)).apply()

    /** MàJ 103 — modèles épinglés en favoris (identifiants du catalogue), affichés en tête du sélecteur. */
    var modelesFavoris: Set<String>
        get() = prefs.getStringSet(KEY_MODELES_FAVORIS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_MODELES_FAVORIS, value).apply()

    /**
     * Session 12 — modèles détectés par clé : pour chaque fournisseur (id), la
     * liste des modèles renvoyée par son endpoint /models. Persistée en JSON.
     */
    var modelesDetectes: Map<String, List<String>>
        get() = runCatching {
            val brut = prefs.getString(KEY_MODELES_DETECTES, "") ?: ""
            if (brut.isBlank()) emptyMap()
            else prefJson.decodeFromString(modelesDetectesSerializer, brut)
        }.getOrDefault(emptyMap())
        set(value) = prefs.edit().putString(
            KEY_MODELES_DETECTES,
            prefJson.encodeToString(modelesDetectesSerializer, value)
        ).apply()

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
        get() = prefs.getFloat(KEY_FONT_SCALE, 1f).coerceIn(Constants.FONT_SCALE_MIN, Constants.FONT_SCALE_MAX)
        set(value) = prefs.edit().putFloat(KEY_FONT_SCALE, value.coerceIn(Constants.FONT_SCALE_MIN, Constants.FONT_SCALE_MAX)).apply()

    /** Échelle de densité appliquée aux dimensions dp de l'interface (dens-004 : 80 % à 112 %). */
    var densityScale: Float
        get() = prefs.getFloat(KEY_DENSITY_SCALE, 1f).coerceIn(Constants.DENSITE_MIN, Constants.DENSITE_MAX)
        set(value) = prefs.edit().putFloat(KEY_DENSITY_SCALE, value.coerceIn(Constants.DENSITE_MIN, Constants.DENSITE_MAX)).apply()

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
        get() = prefs.getInt(KEY_EDITOR_FONT_SIZE, 14).coerceIn(Constants.POLICE_EDITOR_MIN, Constants.POLICE_EDITOR_MAX)
        set(value) = prefs.edit().putInt(KEY_EDITOR_FONT_SIZE, value.coerceIn(Constants.POLICE_EDITOR_MIN, Constants.POLICE_EDITOR_MAX)).apply()

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

    /** corr-029 — Mode lecture seule : tous les outils d'écriture de l'agent sont refusés. */
    var lectureSeule: Boolean
        get() = prefs.getBoolean("lecture_seule", false)
        set(value) = prefs.edit().putBoolean("lecture_seule", value).apply()

    /**
     * feat-032 — Recherche web de l'agent : désactivée par défaut (aucune
     * requête réseau sortante hors API modèle) ; activable dans les réglages.
     * Le fournisseur intégré est DuckDuckGo (HTML léger), sans clé requise.
     */
    var rechercheWebActive: Boolean
        get() = prefs.getBoolean("recherche_web_active", false)
        set(value) = prefs.edit().putBoolean("recherche_web_active", value).apply()

    /**
     * feat-045 — Permission par outil : "" (comportement par défaut),
     * "autoriser" (passe les portes de confirmation), "demander" (défaut)
     * ou "refuser" (l'outil est interdit). Persisté en JSON via org.json.
     */
    fun permissionOutil(nomOutil: String): String =
        permissionsOutils[nomOutil].orEmpty()

    // ux-020 — permissions par outil MEMORISEES PAR PROJET : « Toujours
    // autoriser cet outil dans ce projet ». Stockage imbriqué JSON :
    // { nomProjet: { nomOutil: "autoriser" | "refuser" } }.
    fun permissionOutilProjet(nomProjet: String, nomOutil: String): String =
        permissionsOutilsParProjet[nomProjet]?.get(nomOutil).orEmpty()

    fun reglerPermissionOutilProjet(nomProjet: String, nomOutil: String, valeur: String) {
        val actuel = permissionsOutilsParProjet.toMutableMap()
        val pourProjet = (actuel[nomProjet] ?: emptyMap()).toMutableMap()
        if (valeur.isBlank()) pourProjet.remove(nomOutil) else pourProjet[nomOutil] = valeur
        if (pourProjet.isEmpty()) actuel.remove(nomProjet) else actuel[nomProjet] = pourProjet
        permissionsOutilsParProjet = actuel
    }

    var permissionsOutilsParProjet: Map<String, Map<String, String>>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString("permissions_outils_projet", "{}") ?: "{}")
            val map = LinkedHashMap<String, Map<String, String>>()
            o.keys().forEach { projet ->
                val po = o.optJSONObject(projet) ?: return@forEach
                val inner = LinkedHashMap<String, String>()
                po.keys().forEach { outil -> inner[outil] = po.optString(outil) }
                map[projet] = inner
            }
            map
        }.getOrDefault(emptyMap())
        set(value) = prefs.edit().putString("permissions_outils_projet", org.json.JSONObject(value.mapValues { row -> org.json.JSONObject(row.value) }).toString()).apply()

    fun reglerPermissionOutil(nomOutil: String, valeur: String) {
        val actuel = permissionsOutils.toMutableMap()
        if (valeur.isBlank()) actuel.remove(nomOutil) else actuel[nomOutil] = valeur
        permissionsOutils = actuel
    }

    var permissionsOutils: Map<String, String>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString("permissions_outils", "{}") ?: "{}")
            val map = LinkedHashMap<String, String>()
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                map[k] = o.optString(k)
            }
            map
        }.getOrDefault(emptyMap())
        set(value) {
            val o = org.json.JSONObject()
            for ((k, v) in value) o.put(k, v)
            prefs.edit().putString("permissions_outils", o.toString()).apply()
        }

    /**
     * feat-014 — Outils d'écriture autorisés ou non PAR CONVERSATION.
     * Absent = autorisé (les portes de confirmation habituelles s'appliquent).
     */
    var ecritureParConversation: Map<String, Boolean>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString("ecriture_conversations", "{}") ?: "{}")
            val map = LinkedHashMap<String, Boolean>()
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                map[k] = o.optBoolean(k, true)
            }
            map
        }.getOrDefault(emptyMap())
        set(value) {
            val o = org.json.JSONObject()
            for ((k, v) in value) o.put(k, v)
            prefs.edit().putString("ecriture_conversations", o.toString()).apply()
        }

    /** feat-017 — Évaluations des réponses (pouces), clé « conversationId:time » → ±1. */
    var evaluations: Map<String, Int>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString("evaluations_messages", "{}") ?: "{}")
            val map = LinkedHashMap<String, Int>()
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                map[k] = o.optInt(k, 0)
            }
            map
        }.getOrDefault(emptyMap())
        set(value) {
            val o = org.json.JSONObject()
            for ((k, v) in value) o.put(k, v)
            prefs.edit().putString("evaluations_messages", o.toString()).apply()
        }

    /**
     * feat-025 — Bascule automatique vers un modèle de secours en cas de
     * surcharge (429/1305) ou de clé refusée. Activée par défaut (MàJ 103) ;
     * désactivable pour rester strictement sur le modèle choisi.
     */
    var repliAutoActif: Boolean
        get() = prefs.getBoolean("repli_auto_actif", true)
        set(value) = prefs.edit().putBoolean("repli_auto_actif", value).apply()

    private val prefJson = Json { coerceInputValues = true; ignoreUnknownKeys = true }

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
    /** Session 12 — sérialiseur Map<id fournisseur, liste de modèles détectés>. */
    private val modelesDetectesSerializer =
        MapSerializer(String.serializer(), ListSerializer(String.serializer()))

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
        get() = prefs.getInt(KEY_MAX_STEPS, 20).coerceIn(Constants.ETAPES_MIN, Constants.ETAPES_MAX)
        set(value) = prefs.edit().putInt(KEY_MAX_STEPS, value.coerceIn(5, 100)).apply()

    /** Prix par million de tokens en entrée (0 = inconnu), pour le calcul du coût. */
    var priceInPerM: Float
        get() = prefs.getFloat(KEY_PRICE_IN, 0f)
        set(value) = prefs.edit().putFloat(KEY_PRICE_IN, value.coerceAtLeast(0f)).apply()

    /** Prix par million de tokens en sortie (0 = inconnu). */
    var priceOutPerM: Float
        get() = prefs.getFloat(KEY_PRICE_OUT, 0f)
        set(value) = prefs.edit().putFloat(KEY_PRICE_OUT, value.coerceAtLeast(0f)).apply()

    /**
     * feat-082 — Jeton GitHub (fine-grained PAT) stocké chiffré, utilisé par
     * le clonage, push/pull sur dépôt privé et la création de PR (GitHubApi).
     * Jamais loggé (ZLog.masquer couvre github_pat_… / pat_…).
     */
    var jetonGitHub: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    /** feat-076 — Fichiers épinglés (favoris) de l'explorateur. */
    var favorisFichiers: Set<String>
        get() = prefs.getStringSet(KEY_FAVORIS_FICHIERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORIS_FICHIERS, value).apply()

    /** feat-076 — Derniers fichiers ouverts (max 8, LRU dans le ViewModel). */
    var recentsFichiers: List<String>
        get() = prefs.getString(KEY_RECENTS_FICHIERS, null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_RECENTS_FICHIERS, value.joinToString("\n")).apply()

    /**
     * feat-098 — Budget quotidien de tokens (0 = désactivé). Une fois dépassé,
     * l'application alerte avant chaque envoi (l'envoi reste possible : simple alerte).
     */
    var budgetTokensJour: Int
        get() = prefs.getInt(KEY_BUDGET_TOKENS, 0).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_BUDGET_TOKENS, value).apply()

    // ── err-032 : proxy HTTP (réseaux d'entreprise / VPN) ───────────────────

    /** Hôte du proxy HTTP (" " ou vide = pas de proxy). */
    var proxyHost: String
        get() = prefs.getString(KEY_PROXY_HOST, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_HOST, value.trim()).apply()

    /** Port du proxy HTTP (0 = pas de proxy). */
    var proxyPort: Int
        get() = prefs.getInt(KEY_PROXY_PORT, 0).coerceIn(0, 65535)
        set(value) = prefs.edit().putInt(KEY_PROXY_PORT, value).apply()

    /** Proxy configuré et valide, ou null (err-032). */
    fun proxyActif(): java.net.Proxy? {
        val h = proxyHost
        val p = proxyPort
        if (h.isBlank() || p <= 0) return null
        return java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(h, p))
    }

    // ── Session 17 / vague B — sécurité & secrets (sec-013..024) ────────────

    // sec-013 — coffre d'alias secrets : l'IA ne voit jamais la valeur, elle
    // liste les alias et insère {{secret:alias}} ; la substitution se fait à
    // l'exécution (run_command), hors du contexte envoyé au modèle.
    private val KEY_SECRETS = "secrets_alias"

    var secretsAlias: Map<String, String>
        get() {
            val brut = prefs.getString(KEY_SECRETS, "{}") ?: "{}"
            return runCatching {
                val o = org.json.JSONObject(brut)
                val noms = o.names() ?: return@runCatching emptyMap<String, String>()
                (0 until noms.length()).associate { noms.getString(it) to o.getString(noms.getString(it)) }
            }.getOrDefault(emptyMap())
        }
        private set(value) {
            val o = org.json.JSONObject()
            value.forEach { (k, v) -> o.put(k, v) }
            prefs.edit().putString(KEY_SECRETS, o.toString()).apply()
        }

    /** sec-013 — enregistre (ou remplace) un secret sous alias. */
    fun definirSecret(alias: String, valeur: String) {
        val a = alias.trim().take(40)
        if (a.isEmpty() || valeur.isEmpty()) return
        secretsAlias = secretsAlias + (a to valeur)
        journalerAccesSecret("definition", a)
    }

    /** sec-013 — supprime un secret d'alias. */
    fun supprimerSecret(alias: String) {
        secretsAlias = secretsAlias - alias
        journalerAccesSecret("suppression", alias)
    }

    /** sec-013 — valeur réelle d'un alias (jamais envoyée au modèle). */
    fun secretValeur(alias: String): String? {
        val v = secretsAlias[alias]
        if (v != null) journalerAccesSecret("lecture", alias)
        return v
    }

    /** sec-013 — substitue les {{secret:alias}} d'une commande à l'exécution. */
    fun substituerSecrets(commande: String): String {
        var sortie = commande
        Regex("\\{\\{secret:([^}]{1,40})\\}\\}").findAll(commande).forEach { m ->
            val alias = m.groupValues[1]
            val v = secretValeur(alias)
            if (v != null) sortie = sortie.replace(m.value, v)
        }
        return sortie
    }

    // sec-015 — expiration de session : re-demande du code PIN avant d'utiliser
    // les clés après 24 h (optionnel).
    private val KEY_PIN = "code_pin"
    private val KEY_PIN_ACTIF = "pin_actif"
    private val KEY_PIN_CLES = "pin_exige_cles"
    private val KEY_PIN_HORODATAGE = "pin_cles_debloquees_le"

    /** sec-015 — code PIN (stocké chiffré par EncryptedSharedPreferences). */
    var codePin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN, value.trim().take(12)).apply()

    /** sec-015 — un code PIN est défini. */
    val pinActif: Boolean get() = prefs.getString(KEY_PIN, "")?.isNotBlank() == true

    /** sec-015 — exige une reverification du PIN avant l'usage des clés après 24 h. */
    var exigePinCles: Boolean
        get() = prefs.getBoolean(KEY_PIN_CLES, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_CLES, value).apply()

    /** sec-015 — dernier déblocage validé (0 = jamais). */
    var horodatageClesDebloquees: Long
        get() = prefs.getLong(KEY_PIN_HORODATAGE, 0L)
        set(value) = prefs.edit().putLong(KEY_PIN_HORODATAGE, value).apply()

    /** sec-015 — les clés exigent-elles une reverification maintenant ? */
    fun clesExigentReverification(): Boolean =
        exigePinCles && pinActif &&
            (horodatageClesDebloquees == 0L ||
                System.currentTimeMillis() - horodatageClesDebloquees > 24L * 60 * 60 * 1000)

    /** sec-015 — valide le PIN (retourne true et marque le déblocage si correct). */
    fun verifierPin(saisie: String): Boolean {
        val ok = pinActif && saisie.trim() == codePin
        if (ok) {
            horodatageClesDebloquees = System.currentTimeMillis()
            journalerAccesSecret("deblocage_cles", "pin")
        }
        return ok
    }

    // sec-016 — journal d'accès aux secrets (qui/quoi/quand, JAMAIS de valeur).
    private val KEY_JOURNAL_SECRETS = "journal_secrets"

    fun journalerAccesSecret(action: String, cible: String) {
        runCatching {
            val l = prefs.getString(KEY_JOURNAL_SECRETS, "") ?: ""
            val entree = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                .format(java.util.Date()) + " — " + action + " · " + cible
            val lignes = l.split("\n").filter { it.isNotBlank() }.toMutableList()
            lignes.add(entree)
            // 200 dernières entrées suffisent ; les plus anciennes partent.
            while (lignes.size > 200) lignes.removeAt(0)
            prefs.edit().putString(KEY_JOURNAL_SECRETS, lignes.joinToString("\n")).apply()
        }
    }

    /** sec-016 — journal consultable (texte horodaté, aucune valeur secrète). */
    fun lireJournalSecrets(): List<String> =
        (prefs.getString(KEY_JOURNAL_SECRETS, "") ?: "").split("\n").filter { it.isNotBlank() }.reversed()

    // sec-017 — jeton GitHub révoqué détecté (HTTP 401) → guidage rotation.
    private val KEY_JETON_REVOQUE = "jeton_revoque_le"

    var jetonRevoqueDetecteLe: Long
        get() = prefs.getLong(KEY_JETON_REVOQUE, 0L)
        set(value) = prefs.edit().putLong(KEY_JETON_REVOQUE, value).apply()

    /** sec-017 — enregistre une détection de 401 (révocation probable). */
    fun signalerJetonRevoque() {
        jetonRevoqueDetecteLe = System.currentTimeMillis()
        journalerAccesSecret("jeton_revoque_401", "github")
    }

    // sec-019 — verrou biométrique global optionnel (Android Keystore).
    private val KEY_VERROU_BIO = "verrou_biometrie"

    var verrouBiometrie: Boolean
        get() = prefs.getBoolean(KEY_VERROU_BIO, false)
        set(value) {
            prefs.edit().putBoolean(KEY_VERROU_BIO, value).apply()
            if (value) journalerAccesSecret("verrou_bio_active", "app") else journalerAccesSecret("verrou_bio_desactive", "app")
        }

    // sec-021 — liste blanche de domaines pour fetch_url (vide = tout autorisé).
    private val KEY_DOMAINES_AUTORISES = "domaines_autorises"

    // ── Session 17 / vague C — IA & outils (ia-028..040) ────────────────────

    // ia-037 — mode « relire avant d'envoyer » : l'IA propose, l'utilisateur valide.
    private val KEY_RELIRE = "relire_avant_envoyer"

    var relireAvantEnvoyer: Boolean
        get() = prefs.getBoolean(KEY_RELIRE, false)
        set(value) = prefs.edit().putBoolean(KEY_RELIRE, value).apply()

    // ia-039 — plafond de taille d'un résultat d'outil (au-delà : [tronqué]).
    private val KEY_LIMITE_OUTIL = "limite_resultat_outil"

    var limiteResultatOutil: Int
        get() = prefs.getInt(KEY_LIMITE_OUTIL, 20000).coerceIn(2000, 100_000)
        set(value) = prefs.edit().putInt(KEY_LIMITE_OUTIL, value.coerceIn(2000, 100_000)).apply()

    // ia-038 — verrou de tour : persisté au début d'un tour, effacé à la fin ;
    // s'il survit au processus (crash), le redémarrage libère les outils bloqués.
    private val KEY_TOUR_EN_COURS = "tour_en_cours"

    var tourEnCours: Boolean
        get() = prefs.getBoolean(KEY_TOUR_EN_COURS, false)
        set(value) = prefs.edit().putBoolean(KEY_TOUR_EN_COURS, value).apply()

    // ia-035 — confiance des confirmations (SESSION seule, jamais persistée) :
    // un même outil avec les mêmes arguments confirmé récemment est auto-approuvé.
    private val confianceSession = HashSet<String>()

    fun outilFiable(empreinte: String): Boolean = confianceSession.contains(empreinte)

    fun marquerOutilFiable(empreinte: String) {
        if (confianceSession.size < 50) confianceSession.add(empreinte)
    }

    // ── Session 17 / vague D — modèles & coûts (mod-041..052) ───────────────

    private val KEY_DETECTION_LE = "detection_modeles_le"
    private val KEY_FENETRES = "fenetres_contexte"
    private val KEY_PRIX_DETE = "prix_modeles_detectes"
    private val KEY_QUOTA = "quota_par_fournisseur"

    /** mod-042 — dernier test /models réussi par fournisseur (ms, 0 = jamais). */
    var detectionModelesLe: Map<String, Long>
        get() = lireMapLong(KEY_DETECTION_LE)
        set(value) = ecrireMapLong(KEY_DETECTION_LE, value)

    /** mod-044 — fenêtre de contexte détectée par modèle (id → tokens, 0 = inconnue). */
    var fenetresContexte: Map<String, Long>
        get() = lireMapLong(KEY_FENETRES)
        set(value) = ecrireMapLong(KEY_FENETRES, value)

    private fun lireMapLong(cle: String): Map<String, Long> = runCatching {
        val o = org.json.JSONObject(prefs.getString(cle, "{}") ?: "{}")
        val noms = o.names() ?: return@runCatching emptyMap<String, Long>()
        (0 until noms.length()).associate { noms.getString(it) to o.getLong(noms.getString(it)) }
    }.getOrDefault(emptyMap())

    private fun ecrireMapLong(cle: String, valeur: Map<String, Long>) {
        val o = org.json.JSONObject()
        valeur.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(cle, o.toString()).apply()
    }

    /** mod-043 — prix saisis pour un modèle détecté (id → "prixIn|prixOut" en €/M). */
    var prixModelesDetectes: Map<String, Pair<Float, Float>>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString(KEY_PRIX_DETE, "{}") ?: "{}")
            val noms = o.names() ?: return@runCatching emptyMap<String, Pair<Float, Float>>()
            (0 until noms.length()).associate {
                val parties = o.getString(noms.getString(it)).split("|")
                noms.getString(it) to Pair(
                    parties.getOrNull(0)?.toFloatOrNull() ?: 0f,
                    parties.getOrNull(1)?.toFloatOrNull() ?: 0f
                )
            }
        }.getOrDefault(emptyMap())
        private set(value) {
            val o = org.json.JSONObject()
            value.forEach { (k, v) -> o.put(k, "${v.first}|${v.second}") }
            prefs.edit().putString(KEY_PRIX_DETE, o.toString()).apply()
        }

    /** mod-043 — fixe le prix d'un modèle détecté (valeurs ≤ 0 = retire). */
    fun definirPrixModeleDetecte(modelId: String, prixIn: Float, prixOut: Float) {
        val m = if (prixIn <= 0f && prixOut <= 0f) prixModelesDetectes - modelId
        else prixModelesDetectes + (modelId to Pair(prixIn, prixOut))
        prixModelesDetectes = m
    }

    /** mod-043 — prix effectif d'un modèle : saisie détectée d'abord, sinon réglage global. */
    fun prixEffectif(modelId: String, prixInGlobal: Float, prixOutGlobal: Float): Pair<Float, Float> =
        prixModelesDetectes[modelId] ?: Pair(prixInGlobal, prixOutGlobal)

    /** mod-045 — ids connus comme gratuits (badge « gratuit » des cartes détectées). */
    val idsGratuitsConnus: Set<String> = setOf("glm-4.5-flash")

    /** mod-050 — quota mensuel en € par fournisseur (0 = pas de quota). */
    var quotaParFournisseur: Map<String, Float>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString(KEY_QUOTA, "{}") ?: "{}")
            val noms = o.names() ?: return@runCatching emptyMap<String, Float>()
            (0 until noms.length()).associate { noms.getString(it) to (o.getDouble(noms.getString(it)).toFloat()) }
        }.getOrDefault(emptyMap())
        set(value) {
            val o = org.json.JSONObject()
            value.forEach { (k, v) -> o.put(k, v.toDouble()) }
            prefs.edit().putString(KEY_QUOTA, o.toString()).apply()
        }

    var domainesAutorises: List<String>
        get() = (prefs.getString(KEY_DOMAINES_AUTORISES, "") ?: "")
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_DOMAINES_AUTORISES, value.joinToString(",")).apply()

    /** sec-021 — l'hôte d'une URL est-il autorisé par la liste blanche ? */
    fun domaineAutorise(url: String): Boolean {
        val liste = domainesAutorises
        if (liste.isEmpty()) return true
        val hote = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: return false
        return liste.any { hote == it || hote.endsWith(".$it") }
    }

    // sec-024 — export/import réglages avec empreinte SHA-256 de vérification.
    // L'empreinte porte sur la chaîne brute du corps (jamais re-sérialisée),
    // donc la vérification est déterministe d'un appareil à l'autre.
    fun exporterReglagesVerifie(): String {
        val corps = exporterReglages()
        val empreinte = java.security.MessageDigest.getInstance("SHA-256")
            .digest(corps.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        // « corps » reste une CHAÎNE brute (jamais re-sérialisée) : l'empreinte
        // calculée à l'import est donc identique à celle de l'export.
        return org.json.JSONObject()
            .put("type", "zagent-reglages")
            .put("sha256", empreinte)
            .put("corps", corps)
            .toString(2)
    }

    /** sec-024 — restauration vérifiée : l'empreinte doit correspondre au corps. */
    fun importerReglagesVerifie(json: String): Result<Int> {
        return runCatching {
            val o = org.json.JSONObject(json)
            val corpsStr = o.optString("corps")
            if (corpsStr.isBlank()) error("Fichier sans corps de réglages")
            if (o.optString("type") == "zagent-reglages" && o.has("sha256")) {
                val empreinte = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(corpsStr.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                if (o.getString("sha256") != empreinte) error("Empreinte SHA-256 divergente : fichier altéré")
            }
            importerReglages(corpsStr).getOrThrow()
        }
    }

    // ── feat-091 : export / import des réglages (SANS les clés API) ─────────

    /**
     * feat-091 — Exporte les réglages non secrets en JSON lisible (modèle,
     * apparence, agent, sécurité…). Les clés API et le jeton GitHub ne partent
     * JAMAIS : seule la configuration transite.
     */
    fun exporterReglages(): String {
        val map = linkedMapOf<String, Any?>(
            "version" to 1,
            "model" to model,
            "baseUrl" to baseUrl,
            "autoMode" to autoMode,
            "smartMode" to smartMode,
            "themeMode" to themeMode.name,
            "oledBlack" to oledBlack,
            "cornerRadius" to cornerRadius,
            "dynamicColors" to dynamicSystemColors,
            "fontFamily" to prefs.getString(KEY_FONT_FAMILY, ""),
            "fontScale" to prefs.getFloat(KEY_FONT_SCALE, 1f),
            "densityScale" to prefs.getFloat(KEY_DENSITY_SCALE, 1f),
            "langueUi" to prefs.getString(KEY_LANGUE, ""),
            "hapticLevel" to prefs.getString(KEY_HAPTIC_LEVEL, "NORMAL"),
            "reduceAnimations" to prefs.getBoolean(KEY_REDUCE_ANIMATIONS, false),
            "maxSteps" to maxSteps,
            "budgetTokensJour" to budgetTokensJour,
            "allowRead" to prefs.getBoolean(KEY_ALLOW_READ, false),
            "allowWrite" to prefs.getBoolean(KEY_ALLOW_WRITE, false),
            "allowDelete" to prefs.getBoolean(KEY_ALLOW_DELETE, false),
            "allowExecute" to prefs.getBoolean(KEY_ALLOW_EXECUTE, false)
        )
        return org.json.JSONObject(map).toString(2)
    }

    /**
     * feat-091 — Importe un JSON d'export. Champs inconnus ignorés ; les clés
     * API / jeton éventuellement présents dans le fichier sont volontairement
     * refusés (on n'importe jamais de secret).
     */
    fun importerReglages(json: String): Result<Int> {
        return try {
            val o = org.json.JSONObject(json)
            var n = 0
            if (o.has("model")) { model = o.getString("model"); n++ }
            if (o.has("autoMode")) { autoMode = o.getBoolean("autoMode"); n++ }
            if (o.has("smartMode")) { smartMode = o.getBoolean("smartMode"); n++ }
            if (o.has("themeMode")) {
                val mode = runCatching { ThemeMode.valueOf(o.getString("themeMode")) }.getOrNull()
                if (mode != null) { themeMode = mode; n++ }
            }
            if (o.has("maxSteps")) { maxSteps = o.getInt("maxSteps"); n++ }
            if (o.has("budgetTokensJour")) { budgetTokensJour = o.getInt("budgetTokensJour"); n++ }
            if (o.has("fontScale")) { prefs.edit().putFloat(KEY_FONT_SCALE, o.getDouble("fontScale").toFloat()).apply(); n++ }
            if (o.has("reduceAnimations")) { prefs.edit().putBoolean(KEY_REDUCE_ANIMATIONS, o.getBoolean("reduceAnimations")).apply(); n++ }
            if (o.has("allowRead")) { prefs.edit().putBoolean(KEY_ALLOW_READ, o.getBoolean("allowRead")).apply(); n++ }
            if (o.has("allowWrite")) { prefs.edit().putBoolean(KEY_ALLOW_WRITE, o.getBoolean("allowWrite")).apply(); n++ }
            if (o.has("allowDelete")) { prefs.edit().putBoolean(KEY_ALLOW_DELETE, o.getBoolean("allowDelete")).apply(); n++ }
            if (o.has("allowExecute")) { prefs.edit().putBoolean(KEY_ALLOW_EXECUTE, o.getBoolean("allowExecute")).apply(); n++ }
            Result.success(n)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── feat-092 : profils de réglages ──────────────────────────────────────

    /**
     * feat-092 — Profils nommés (travail, économie de données…) : un instantané
     * d'un sous-ensemble de réglages, appliquable en un tap.
     */
    var profilsReglages: Map<String, String>
        get() {
            val brut = prefs.getString(KEY_PROFILS_REGLAGES, null) ?: return emptyMap()
            return runCatching {
                val o = org.json.JSONObject(brut)
                o.keys().asSequence().associateWith { o.getString(it) }
            }.getOrDefault(emptyMap())
        }
        set(value) = prefs.edit()
            .putString(KEY_PROFILS_REGLAGES, org.json.JSONObject(value).toString())
            .apply()

    /** feat-092 — Instantané des réglages du profil courant (modèle + agent + budget). */
    fun instantaneProfil(): String {
        val map = linkedMapOf<String, Any?>(
            "model" to model,
            "autoMode" to autoMode,
            "smartMode" to smartMode,
            "maxSteps" to maxSteps,
            "budgetTokensJour" to budgetTokensJour
        )
        return org.json.JSONObject(map).toString()
    }

    companion object {
        // sec-014 — formes de jetons reconnues : GitHub PAT (classic/fine),
        // OpenAI/Anthropic sk-, Bearer JWT, Google AIza. Testable JVM (pur).       
        private val FORMES_JETONS = listOf(
            Regex("github_pat_[A-Za-z0-9_]{30,}"),
            Regex("ghp_[A-Za-z0-9]{30,}"),
            Regex("gho_[A-Za-z0-9]{30,}"),
            Regex("sk-[A-Za-z0-9_-]{20,}"),
            Regex("sk-ant-[A-Za-z0-9_-]{20,}"),
            Regex("Bearer [A-Za-z0-9._-]{20,}"),
            Regex("AIza[A-Za-z0-9_-]{30,}")
        )
        private const val CHAINE_MASQUE = "«[masqué]»"

        /**
         * sec-014 — masque toute valeur ressemblant à un jeton dans un texte
         * destiné au modèle (contexte, résultats d'outils, fichiers piqués).
         */
        fun masquerSecrets(texte: String): String {
            var t = texte
            FORMES_JETONS.forEach { t = t.replace(it, CHAINE_MASQUE) }
            return t
        }

        private const val KEY_BUDGET_TOKENS = "budget_tokens_jour"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
        private const val KEY_PROFILS_REGLAGES = "profils_reglages"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_FAVORIS_FICHIERS = "favoris_fichiers"
        private const val KEY_RECENTS_FICHIERS = "recents_fichiers"
        private const val KEY_MAX_STEPS = "max_steps"
        private const val KEY_PRICE_IN = "price_in_per_m"
        private const val KEY_PRICE_OUT = "price_out_per_m"
        private const val KEY_CUSTOM_INSTR = "custom_instructions"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_FOURNISSEURS = "fournisseurs_json"
        private const val KEY_MODELES_FAVORIS = "modeles_favoris"
        private const val KEY_MODELES_DETECTES = "modeles_detectes"
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
