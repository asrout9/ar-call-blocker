package com.asrou.arcallblocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.core.model.NotificationMode
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import com.asrou.arcallblocker.core.model.UnknownCallAction
import com.asrou.arcallblocker.data.datastore.UserPreferences
import com.asrou.arcallblocker.data.repository.BlacklistRepository
import com.asrou.arcallblocker.data.repository.BlockedCallRepository
import com.asrou.arcallblocker.data.repository.WhitelistRepository
import com.asrou.arcallblocker.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isBlockingEnabled: Boolean = false,
    val privateNumberPolicy: PrivateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY,
    val unknownNumberPolicy: PrivateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY,
    val unknownCallAction: UnknownCallAction = UnknownCallAction.BLOCK,
    val notificationMode: NotificationMode = NotificationMode.NONE,
    val historyRetentionDays: Int = 30,
    val isAppLockEnabled: Boolean = false,
    val lockTimeoutSeconds: Int = 0,
    val authCapability: BiometricAuthManager.AuthCapability = BiometricAuthManager.AuthCapability.AVAILABLE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val blacklistRepository: BlacklistRepository,
    private val whitelistRepository: WhitelistRepository,
    private val blockedCallRepository: BlockedCallRepository,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val securityFlow = combine(
        userPreferences.appLockEnabled,
        userPreferences.lockTimeoutSeconds
    ) { lockEnabled, timeout ->
        lockEnabled to timeout
    }

    private val policiesFlow = combine(
        userPreferences.blockingEnabled,
        userPreferences.privateNumberPolicy,
        userPreferences.unknownCallAction,
        userPreferences.notificationMode,
        userPreferences.historyRetentionDays
    ) { blocking, privatePol, callAction, notifMode, retention ->
        SettingsUiState(
            isBlockingEnabled = blocking,
            privateNumberPolicy = privatePol,
            unknownCallAction = callAction,
            notificationMode = notifMode,
            historyRetentionDays = retention
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        policiesFlow,
        securityFlow
    ) { policies, (lockEnabled, timeout) ->
        policies.copy(
            isAppLockEnabled = lockEnabled,
            lockTimeoutSeconds = timeout,
            authCapability = biometricAuthManager.checkCapability()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBlockingEnabled(enabled) }
    }

    fun setPrivateNumberPolicy(policy: PrivateNumberPolicy) {
        viewModelScope.launch { userPreferences.setPrivateNumberPolicy(policy) }
    }

    fun setUnknownNumberPolicy(policy: PrivateNumberPolicy) {
        viewModelScope.launch { userPreferences.setUnknownNumberPolicy(policy) }
    }

    fun setUnknownCallAction(action: UnknownCallAction) {
        viewModelScope.launch { userPreferences.setUnknownCallAction(action) }
    }

    fun setNotificationMode(mode: NotificationMode) {
        viewModelScope.launch { userPreferences.setNotificationMode(mode) }
    }

    fun setHistoryRetention(days: Int) {
        viewModelScope.launch { userPreferences.setHistoryRetentionDays(days) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAppLockEnabled(enabled) }
    }

    fun setLockTimeout(seconds: Int) {
        viewModelScope.launch { userPreferences.setLockTimeoutSeconds(seconds) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            blacklistRepository.clearAll()
            whitelistRepository.clearAll()
            blockedCallRepository.clearAll()
        }
    }
}
