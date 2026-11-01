package com.zagent.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zagent.app.ui.theme.ThemeMode

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

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

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
        private const val KEY_ALWAYS_ALLOW = "always_allow_paths"
    }
}
