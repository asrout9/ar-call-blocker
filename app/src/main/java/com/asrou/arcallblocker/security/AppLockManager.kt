package com.asrou.arcallblocker.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.asrou.arcallblocker.core.util.AppLogger
import com.asrou.arcallblocker.data.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the application-level lock state based on user preferences,
 * cold-start lifecycle, and background-to-foreground transitions.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val userPreferences: UserPreferences
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L
    private var isInitialized = false

    /**
     * Start observing process lifecycle for auto-locking.
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            val enabled = userPreferences.appLockEnabled.first()
            if (enabled) {
                _isLocked.value = true
                AppLogger.d("AppLockManager: Cold start lock applied")
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTimestamp = System.currentTimeMillis()
        AppLogger.d("AppLockManager: App backgrounded at $backgroundTimestamp")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        scope.launch {
            val isEnabled = userPreferences.appLockEnabled.first()
            if (!isEnabled) {
                _isLocked.value = false
                return@launch
            }

            if (backgroundTimestamp > 0L) {
                val timeoutSeconds = userPreferences.lockTimeoutSeconds.first()
                val elapsedSeconds = (System.currentTimeMillis() - backgroundTimestamp) / 1000
                if (elapsedSeconds >= timeoutSeconds) {
                    _isLocked.value = true
                    AppLogger.d("AppLockManager: App locked after $elapsedSeconds s in background (timeout: $timeoutSeconds s)")
                }
            }
        }
    }

    /** Call upon successful biometric/PIN authentication */
    fun unlock() {
        _isLocked.value = false
        backgroundTimestamp = 0L
        AppLogger.d("AppLockManager: App unlocked")
    }

    /** Manually lock the app immediately */
    fun lock() {
        _isLocked.value = true
        AppLogger.d("AppLockManager: Manually locked")
    }
}
