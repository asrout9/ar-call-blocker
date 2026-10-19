package com.asrou.arcallblocker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.core.model.UnknownCallAction
import com.asrou.arcallblocker.data.cache.ContactCache
import com.asrou.arcallblocker.data.contacts.ContactRepository
import com.asrou.arcallblocker.data.datastore.UserPreferences
import com.asrou.arcallblocker.data.repository.BlockedCallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

import android.app.role.RoleManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class HomeUiState(
    val isBlockingEnabled: Boolean = false,
    val unknownCallAction: UnknownCallAction = UnknownCallAction.BLOCK,
    val blockedTodayCount: Int = 0,
    val isScreeningRoleHeld: Boolean = true,
    val contactCacheState: ContactCache.State = ContactCache.State.UNINITIALIZED,
    val contactCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val blockedCallRepository: BlockedCallRepository,
    private val contactCache: ContactCache,
    private val contactRepository: ContactRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isScreeningRoleHeld = MutableStateFlow(true)
    val isScreeningRoleHeld: StateFlow<Boolean> = _isScreeningRoleHeld.asStateFlow()

    private val todayStartMillis: Long
        get() = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferences.blockingEnabled,
        userPreferences.unknownCallAction,
        blockedCallRepository.observeCountSince(todayStartMillis),
        _isScreeningRoleHeld
    ) { blockingEnabled, callAction, blockedToday, roleHeld ->
        HomeUiState(
            isBlockingEnabled = blockingEnabled,
            unknownCallAction = callAction,
            blockedTodayCount = blockedToday,
            isScreeningRoleHeld = roleHeld,
            contactCacheState = contactCache.state,
            contactCount = contactCache.size,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    init {
        // Safely check screening role on startup
        refreshScreeningRole()

        // Ensure contacts are loaded
        if (contactRepository.hasContactsPermission() &&
            contactCache.state == ContactCache.State.UNINITIALIZED
        ) {
            contactRepository.initialize()
        }
    }

    fun checkScreeningRole(): Boolean {
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } else {
                false
            }
        } catch (e: Throwable) {
            com.asrou.arcallblocker.core.util.AppLogger.e("Failed to check screening role", e)
            false
        }
    }

    fun refreshScreeningRole() {
        _isScreeningRoleHeld.value = checkScreeningRole()
    }

    fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBlockingEnabled(enabled)
        }
    }

    fun setUnknownCallAction(action: UnknownCallAction) {
        viewModelScope.launch {
            userPreferences.setUnknownCallAction(action)
        }
    }
}
