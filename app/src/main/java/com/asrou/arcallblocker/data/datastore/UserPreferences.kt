package com.asrou.arcallblocker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.asrou.arcallblocker.core.model.NotificationMode
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import com.asrou.arcallblocker.core.model.UnknownCallAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Wrapper around Preferences DataStore for all user settings.
 *
 * Provides type-safe access to preferences with Flow-based observation
 * and suspend functions for writes.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.dataStore
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        com.asrou.arcallblocker.core.util.AppLogger.e("Error in UserPreferences background scope", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    // ── In-Memory Volatile Cache for Hot Path (CallScreeningService) ──────
    @Volatile var cachedBlockingEnabled: Boolean = false
        private set
    @Volatile var cachedUnknownCallAction: UnknownCallAction = UnknownCallAction.BLOCK
        private set
    @Volatile var cachedPrivateNumberPolicy: PrivateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY
        private set
    @Volatile var cachedTemporaryBlockUntil: Long = 0L
        private set

    init {
        scope.launch {
            try {
                blockingEnabled.collect { cachedBlockingEnabled = it }
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Error caching blockingEnabled", e)
            }
        }
        scope.launch {
            try {
                unknownCallAction.collect { cachedUnknownCallAction = it }
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Error caching unknownCallAction", e)
            }
        }
        scope.launch {
            try {
                privateNumberPolicy.collect { cachedPrivateNumberPolicy = it }
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Error caching privateNumberPolicy", e)
            }
        }
        scope.launch {
            try {
                temporaryBlockUntil.collect { cachedTemporaryBlockUntil = it }
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Error caching temporaryBlockUntil", e)
            }
        }
    }

    // ── Keys ──────────────────────────────────────────────────────────────

    private object Keys {
        val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
        val PRIVATE_NUMBER_POLICY = stringPreferencesKey("private_number_policy")
        val UNKNOWN_NUMBER_POLICY = stringPreferencesKey("unknown_number_policy")
        val UNKNOWN_CALL_ACTION = stringPreferencesKey("unknown_call_action")
        val NOTIFICATION_MODE = stringPreferencesKey("notification_mode")
        val TEMPORARY_BLOCK_UNTIL = longPreferencesKey("temporary_block_until")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val PREVIOUS_BLOCKING_STATE = booleanPreferencesKey("previous_blocking_state")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")
    }

    // ── Blocking Mode ─────────────────────────────────────────────────────

    val blockingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        val enabled = prefs[Keys.BLOCKING_ENABLED] ?: false
        cachedBlockingEnabled = enabled
        enabled
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        cachedBlockingEnabled = enabled
        dataStore.edit { prefs ->
            prefs[Keys.BLOCKING_ENABLED] = enabled
        }
    }

    // ── First Launch ──────────────────────────────────────────────────────

    val firstLaunchCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.FIRST_LAUNCH_COMPLETED] ?: false
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.FIRST_LAUNCH_COMPLETED] = completed
        }
    }

    // ── Private Number Policy ─────────────────────────────────────────────

    val privateNumberPolicy: Flow<PrivateNumberPolicy> = dataStore.data.map { prefs ->
        val value = prefs[Keys.PRIVATE_NUMBER_POLICY]
        value?.let {
            try { PrivateNumberPolicy.valueOf(it) } catch (_: Exception) { null }
        } ?: PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY
    }

    suspend fun setPrivateNumberPolicy(policy: PrivateNumberPolicy) {
        cachedPrivateNumberPolicy = policy
        dataStore.edit { prefs ->
            prefs[Keys.PRIVATE_NUMBER_POLICY] = policy.name
        }
    }

    // ── Unknown Number Policy ─────────────────────────────────────────────

    val unknownNumberPolicy: Flow<PrivateNumberPolicy> = dataStore.data.map { prefs ->
        val value = prefs[Keys.UNKNOWN_NUMBER_POLICY]
        value?.let {
            try { PrivateNumberPolicy.valueOf(it) } catch (_: Exception) { null }
        } ?: PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY
    }

    suspend fun setUnknownNumberPolicy(policy: PrivateNumberPolicy) {
        dataStore.edit { prefs ->
            prefs[Keys.UNKNOWN_NUMBER_POLICY] = policy.name
        }
    }

    // ── Unknown Call Action (Block, Silence, Reject) ───────────────────────

    val unknownCallAction: Flow<UnknownCallAction> = dataStore.data.map { prefs ->
        val value = prefs[Keys.UNKNOWN_CALL_ACTION]
        val action = value?.let {
            try { UnknownCallAction.valueOf(it) } catch (_: Exception) { null }
        } ?: UnknownCallAction.BLOCK
        cachedUnknownCallAction = action
        action
    }

    suspend fun setUnknownCallAction(action: UnknownCallAction) {
        cachedUnknownCallAction = action
        dataStore.edit { prefs ->
            prefs[Keys.UNKNOWN_CALL_ACTION] = action.name
        }
    }

    // ── Notification Mode ─────────────────────────────────────────────────

    val notificationMode: Flow<NotificationMode> = dataStore.data.map { prefs ->
        val value = prefs[Keys.NOTIFICATION_MODE]
        value?.let {
            try { NotificationMode.valueOf(it) } catch (_: Exception) { null }
        } ?: NotificationMode.NONE
    }

    suspend fun setNotificationMode(mode: NotificationMode) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_MODE] = mode.name
        }
    }

    // ── Temporary Blocking ────────────────────────────────────────────────

    /** Epoch millis when temporary blocking expires, or 0 if not active */
    val temporaryBlockUntil: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.TEMPORARY_BLOCK_UNTIL] ?: 0L
    }

    suspend fun setTemporaryBlockUntil(epochMillis: Long) {
        cachedTemporaryBlockUntil = epochMillis
        dataStore.edit { prefs ->
            prefs[Keys.TEMPORARY_BLOCK_UNTIL] = epochMillis
        }
    }

    suspend fun clearTemporaryBlock() {
        cachedTemporaryBlockUntil = 0L
        dataStore.edit { prefs ->
            prefs[Keys.TEMPORARY_BLOCK_UNTIL] = 0L
        }
    }

    // ── Previous Blocking State (for restoring after temporary block) ─────

    val previousBlockingState: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.PREVIOUS_BLOCKING_STATE] ?: false
    }

    suspend fun setPreviousBlockingState(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.PREVIOUS_BLOCKING_STATE] = enabled
        }
    }

    // ── History Retention ─────────────────────────────────────────────────

    /** Number of days to retain blocked call history. Default: 30. 0 = keep forever */
    val historyRetentionDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.HISTORY_RETENTION_DAYS] ?: 30
    }

    suspend fun setHistoryRetentionDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.HISTORY_RETENTION_DAYS] = days
        }
    }

    // ── App Lock ──────────────────────────────────────────────────────────

    /** Whether biometric/device credential app lock is enabled */
    val appLockEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    /** Seconds before app locks after backgrounding. Default: 0 (immediately) */
    val lockTimeoutSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.LOCK_TIMEOUT_SECONDS] ?: 0
    }

    suspend fun setLockTimeoutSeconds(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    // ── Clear All ─────────────────────────────────────────────────────────

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
