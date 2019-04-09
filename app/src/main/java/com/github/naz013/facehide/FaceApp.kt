package com.github.naz013.facehide

import android.app.Application
import timber.log.Timber

class FaceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}