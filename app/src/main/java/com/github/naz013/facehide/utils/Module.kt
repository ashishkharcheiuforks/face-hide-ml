package com.github.naz013.facehide.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object Module {

    val isPie: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    val isOreoMr1: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    val isOreo: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val isMarshmallow: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isNougat: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    val isNougat1: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    fun isChromeOs(context: Context): Boolean {
        return context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
    }

    fun hasCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
}
