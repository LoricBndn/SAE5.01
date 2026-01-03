package com.ltb.sae501.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestionnaire de préférences pour les paramètres de l'application
 */
object AppPreferences {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_PERCENTAGE_SHOWN = "is_percentage_shown"
    private const val KEY_AUTO_SAVE_ENABLED = "is_auto_save_enabled"
    private const val KEY_DARK_MODE_ENABLED = "is_dark_mode_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setPercentageShown(value: Boolean) {
        prefs.edit().putBoolean(KEY_PERCENTAGE_SHOWN, value).apply()
    }

    fun isPercentageShown(): Boolean {
        return prefs.getBoolean(KEY_PERCENTAGE_SHOWN, true)
    }

    fun setAutoSaveEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE_ENABLED, value).apply()
    }

    fun isAutoSaveEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SAVE_ENABLED, true)
    }

    fun setDarkModeEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, value).apply()
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE_ENABLED, false)
    }
}
