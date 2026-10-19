package com.asrou.arcallblocker.ui.diagnostics

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.asrou.arcallblocker.data.cache.ContactCache
import com.asrou.arcallblocker.data.contacts.ContactRepository
import com.asrou.arcallblocker.notification.BlockedCallNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DiagnosticResult(
    val hasCallScreeningRole: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val contactCacheState: ContactCache.State = ContactCache.State.UNINITIALIZED,
    val contactCacheSize: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val deviceManufacturer: String = "",
    val androidVersion: String = "",
    val sdkVersion: Int = 0
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactCache: ContactCache,
    private val contactRepository: ContactRepository,
    private val blockedCallNotifier: BlockedCallNotifier
) : ViewModel() {

    private val _diagnosticResult = MutableStateFlow(DiagnosticResult())
    val diagnosticResult: StateFlow<DiagnosticResult> = _diagnosticResult.asStateFlow()

    init {
        runDiagnostic()
    }

    fun runDiagnostic() {
        val hasRole = try {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } catch (e: Throwable) {
            false
        }

        _diagnosticResult.value = DiagnosticResult(
            hasCallScreeningRole = hasRole,
            hasContactsPermission = contactRepository.hasContactsPermission(),
            hasNotificationPermission = blockedCallNotifier.hasNotificationPermission(),
            contactCacheState = contactCache.state,
            contactCacheSize = contactCache.size,
            lastSyncTimestamp = contactCache.lastSyncTimestamp,
            deviceManufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }
}
