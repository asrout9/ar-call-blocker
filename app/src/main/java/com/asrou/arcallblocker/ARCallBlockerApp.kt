package com.asrou.arcallblocker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for AR Call Blocker.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class ARCallBlockerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                android.util.Log.e("ARCallBlockerApp", "FATAL CRASH on thread ${thread.name}", throwable)
                val crashFile = java.io.File(filesDir, "crash.log")
                crashFile.writeText(
                    "Timestamp: ${System.currentTimeMillis()}\n" +
                    "Thread: ${thread.name}\n" +
                    "Exception: ${throwable::class.java.name}: ${throwable.message}\n" +
                    "Stacktrace:\n${throwable.stackTraceToString()}"
                )
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
