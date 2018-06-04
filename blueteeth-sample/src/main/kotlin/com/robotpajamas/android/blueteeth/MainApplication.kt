package com.robotpajamas.android.blueteeth

import android.app.Application
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.logger
import com.robotpajamas.blueteeth.models.Logger
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Blueteeth.logger = object : Logger {
            override fun verbose(message: String) {
                Timber.v(message)
            }

            override fun debug(message: String) {
                Timber.d(message)
            }

            override fun info(message: String) {
                Timber.i(message)
            }

            override fun warning(message: String) {
                Timber.w(message)
            }

            override fun error(message: String) {
                Timber.e(message)
            }
        }
    }
}
