package com.jax.pcscdemo

import android.app.Application
import android.os.Build
import timber.log.Timber

class App : Application() {

    companion object {
        const val TAG = "PcscReader"
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(debugTree)
        }
    }

    private val debugTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, "${TAG}_${tag}", message, t)
        }
    }
}