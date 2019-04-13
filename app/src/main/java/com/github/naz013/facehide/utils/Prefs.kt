package com.github.naz013.facehide.utils

import android.content.Context

class Prefs(context: Context) {

    private val shared = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isAutoFace(): Boolean {
        return shared.getBoolean(AUTO_FACE, true)
    }

    fun setAutoFace(value: Boolean) {
        shared.edit().putBoolean(AUTO_FACE, value).apply()
    }

    companion object {
        private const val NAME = "photo_prefs"
        private const val AUTO_FACE = "photo_prefs"

        private var INSTANCE: Prefs? = null

        fun getInstance(context: Context): Prefs {
            val inst: Prefs = INSTANCE ?: Prefs(context)
            INSTANCE = inst
            return inst
        }
    }
}