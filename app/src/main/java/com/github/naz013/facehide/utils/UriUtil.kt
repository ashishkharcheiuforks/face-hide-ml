package com.github.naz013.facehide.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.github.naz013.facehide.BuildConfig
import com.github.naz013.facehide.utils.Module
import timber.log.Timber
import java.io.File

object UriUtil {

    const val URI_MIME = "application/x-arc-uri-list"
    const val ANY_MIME = "any"
    const val IMAGE_MIME = "image/*"

    fun getUri(context: Context, filePath: String): Uri? {
        Timber.d("getUri: %s", BuildConfig.APPLICATION_ID)
        return if (Module.isNougat) {
            try {
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", File(filePath))
            } catch (e: java.lang.Exception) {
                null
            }
        } else {
            Uri.fromFile(File(filePath))
        }
    }

    fun getUri(context: Context, file: File): Uri? {
        Timber.d("getUri: %s", BuildConfig.APPLICATION_ID)
        return if (Module.isNougat) {
            try {
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
            } catch (e: java.lang.Exception) {
                null
            }
        } else {
            Uri.fromFile(file)
        }
    }
}
