package com.ltb.sae501.preferences

import android.content.Context
import android.content.SharedPreferences

object CameraPreferences {
    private const val PREF_NAME = "camera_preferences"
    private const val KEY_IS_FRONT_CAMERA = "is_front_camera"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setFrontCamera(isFront: Boolean) {
        prefs.edit().putBoolean(KEY_IS_FRONT_CAMERA, isFront).apply()
    }

    fun isFrontCamera(): Boolean {
        return prefs.getBoolean(KEY_IS_FRONT_CAMERA, true) // true par défaut (caméra frontale)
    }
}