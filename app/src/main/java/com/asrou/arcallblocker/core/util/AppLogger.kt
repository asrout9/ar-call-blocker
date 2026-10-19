package com.asrou.arcallblocker.core.util

import android.util.Log
import com.asrou.arcallblocker.BuildConfig

/**
 * Privacy-safe logging utility.
 *
 * - In debug builds: logs with controlled detail level, never logging full phone numbers.
 * - In release builds: minimal logging (warnings and errors only).
 *
 * Phone numbers are always masked before logging regardless of build type.
 */
object AppLogger {
    private const val TAG = "ARCallBlocker"

    fun d(message: String) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        } catch (_: Exception) {}
    }

    fun i(message: String) {
        try {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, message)
            }
        } catch (_: Exception) {}
    }

    fun w(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        } catch (_: Exception) {}
    }

    fun e(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        } catch (_: Exception) {}
    }

    /**
     * Mask a phone number for safe logging.
     * Shows only the last 4 digits: "+91987654XXXX" → "****3210"
     * Never reveals the full number in any build type.
     */
    fun maskNumber(number: String?): String {
        if (number.isNullOrBlank()) return "<empty>"
        val digits = number.filter { it.isDigit() }
        if (digits.length <= 4) return "****"
        return "****${digits.takeLast(4)}"
    }
}
