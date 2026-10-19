package com.asrou.arcallblocker.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.data.contacts.ContactRepository
import com.asrou.arcallblocker.data.datastore.UserPreferences
import com.asrou.arcallblocker.notification.BlockedCallNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val contactRepository: ContactRepository,
    private val blockedCallNotifier: BlockedCallNotifier,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean> = userPreferences.firstLaunchCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    val totalSteps = 6

    fun hasContactsPermission(): Boolean = contactRepository.hasContactsPermission()

    fun hasCallScreeningRole(): Boolean {
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } catch (e: Throwable) {
            false
        }
    }

    fun hasNotificationPermission(): Boolean = blockedCallNotifier.hasNotificationPermission()

    fun hasCallLogPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(android.Manifest.permission.WRITE_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun onContactsPermissionGranted() {
        contactRepository.initialize()
    }

    fun nextStep() {
        if (_currentStep.value < totalSteps - 1) {
            _currentStep.value++
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setFirstLaunchCompleted(true)
        }
    }
}
