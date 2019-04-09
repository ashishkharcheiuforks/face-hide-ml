package com.github.naz013.facehide.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object Permissions {

    const val WRITE_EXTERNAL = Manifest.permission.WRITE_EXTERNAL_STORAGE
    const val READ_EXTERNAL = Manifest.permission.READ_EXTERNAL_STORAGE
    const val CAMERA = Manifest.permission.CAMERA

    fun isAllGranted(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) {
            return false
        } else {
            for (p in grantResults) {
                if (p != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }
    }

    fun isAnyGranted(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) {
            return false
        } else {
            for (p in grantResults) {
                if (p == PackageManager.PERMISSION_GRANTED) {
                    return true
                }
            }
            return false
        }
    }

    fun ensurePermissions(activity: Activity, requestCode: Int, vararg permissions: String): Boolean {
        return if (checkPermission(activity, *permissions)) {
            true
        } else {
            requestPermission(activity, requestCode, *permissions)
            false
        }
    }

    fun checkPermission(a: Context, vararg permissions: String): Boolean {
        if (!Module.isMarshmallow) {
            return true
        }
        var res = true
        for (string in permissions) {
            if (ContextCompat.checkSelfPermission(a, string) != PackageManager.PERMISSION_GRANTED) {
                res = false
            }
        }
        return res
    }

    fun checkPermission(a: Context, permission: String): Boolean {
        return !Module.isMarshmallow || ContextCompat.checkSelfPermission(a, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(a: Activity, requestCode: Int, vararg permission: String) {
        if (Module.isMarshmallow) {
            val size = permission.size
            if (size == 1) {
                a.requestPermissions(permission, requestCode)
            } else {
                val array = arrayOfNulls<String>(size)
                System.arraycopy(permission, 0, array, 0, size)
                a.requestPermissions(array, requestCode)
            }
        }
    }
}
