package com.zagent.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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

    var workingFolderUri: String?
        get() = prefs.getString(KEY_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_URI, value).apply()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTO_MODE = "auto_mode"
        private const val KEY_FOLDER_URI = "folder_uri"
    }
}
