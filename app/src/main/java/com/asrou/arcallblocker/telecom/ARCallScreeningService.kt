package com.asrou.arcallblocker.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.asrou.arcallblocker.core.model.BlockDecision
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import com.asrou.arcallblocker.core.util.AppLogger
import com.asrou.arcallblocker.core.util.PhoneNumberNormalizer
import com.asrou.arcallblocker.data.cache.ContactCache
import com.asrou.arcallblocker.data.contacts.ContactRepository
import com.asrou.arcallblocker.data.datastore.UserPreferences
import com.asrou.arcallblocker.data.repository.BlacklistRepository
import com.asrou.arcallblocker.data.repository.BlockedCallRepository
import com.asrou.arcallblocker.data.repository.ScheduleRepository
import com.asrou.arcallblocker.data.repository.WhitelistRepository
import com.asrou.arcallblocker.domain.BlockingEngine
import com.asrou.arcallblocker.domain.ScheduleEvaluator
import com.asrou.arcallblocker.notification.BlockedCallNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Android CallScreeningService implementation for AR Call Blocker.
 *
 * This service is bound by the Android Telecom framework when an incoming call arrives.
 * It must respond quickly (within ~5 seconds) or the system will unbind it.
 *
 * ## Hot Path Design
 *
 * The [onScreenCall] method is the performance-critical hot path.
 * It performs ONLY in-memory operations:
 * 1. Read the incoming number from the call details
 * 2. Normalize the number
 * 3. Check blocking state (cached boolean)
 * 4. Check schedule state (pure computation)
 * 5. Check blacklist (HashSet lookup)
 * 6. Check whitelist (HashSet lookup)
 * 7. Check contact cache (HashSet lookup)
 * 8. Respond to the call
 *
 * Any I/O (logging blocked calls, sending notifications) is done asynchronously
 * AFTER the call response is sent.
 *
 * ## Safety
 *
 * - Never blocks outgoing calls
 * - Never interferes with emergency calls
 * - Fails open (allows the call) if any error occurs
 * - Never logs full phone numbers
 */
@AndroidEntryPoint
class ARCallScreeningService : CallScreeningService() {

    @Inject lateinit var blockingEngine: BlockingEngine
    @Inject lateinit var scheduleEvaluator: ScheduleEvaluator
    @Inject lateinit var normalizer: PhoneNumberNormalizer
    @Inject lateinit var contactCache: ContactCache
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var blacklistRepository: BlacklistRepository
    @Inject lateinit var whitelistRepository: WhitelistRepository
    @Inject lateinit var scheduleRepository: ScheduleRepository
    @Inject lateinit var blockedCallRepository: BlockedCallRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var blockedCallNotifier: BlockedCallNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Ensure contact cache is initialized if the service starts fresh
        if (contactCache.state == ContactCache.State.UNINITIALIZED) {
            contactRepository.initialize()
        }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        AppLogger.d("onScreenCall: direction=${callDetails.callDirection} handle=${AppLogger.maskNumber(callDetails.handle?.schemeSpecificPart)}")

        // SAFETY: Only screen incoming calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            allowCall(callDetails)
            return
        }

        try {
            val decision = evaluateCall(callDetails)

            when (decision.action) {
                BlockDecision.Action.ALLOW -> allowCall(callDetails)
                BlockDecision.Action.BLOCK -> blockCall(callDetails, decision)
                BlockDecision.Action.SILENCE -> silenceCall(callDetails, decision)
                BlockDecision.Action.REJECT -> rejectCall(callDetails, decision)
            }
        } catch (e: Exception) {
            // SAFETY: If anything goes wrong, allow the call through
            AppLogger.e("Error screening call, failing open", e)
            allowCall(callDetails)
        }
    }

    /**
     * Evaluate whether to allow, block, silence, or reject this call.
     * ALL operations here are 100% in-memory and non-blocking.
     */
    private fun evaluateCall(callDetails: Call.Details): BlockDecision {
        // 1. Read the incoming number
        val rawNumber = callDetails.handle?.schemeSpecificPart
        val normalizedNumber = normalizer.normalize(rawNumber)

        AppLogger.d("evaluateCall: raw=${AppLogger.maskNumber(rawNumber)} normalized=${AppLogger.maskNumber(normalizedNumber)}")

        // 2. Read blocking state directly from volatile cache
        val isBlockingEnabled = userPreferences.cachedBlockingEnabled
        if (!isBlockingEnabled) {
            AppLogger.d("evaluateCall: ALLOW — blocking is disabled")
            return BlockDecision.ALLOW_BLOCKING_OFF
        }

        // 3. Read schedule state from in-memory cache
        val isScheduleActive = evaluateSchedule()
        if (!isScheduleActive) {
            AppLogger.d("evaluateCall: ALLOW — schedule is inactive")
            return BlockDecision.ALLOW_SCHEDULE_INACTIVE
        }

        // 4. Check blacklist (non-blocking in-memory ConcurrentHashMap set)
        val isBlacklisted = if (normalizedNumber != null) {
            blacklistRepository.isBlacklistedFast(normalizedNumber)
        } else false

        // 5. Check whitelist (non-blocking in-memory ConcurrentHashMap set)
        val isWhitelisted = if (normalizedNumber != null) {
            whitelistRepository.isWhitelistedFast(normalizedNumber)
        } else false

        // 6. Check contact cache (non-blocking in-memory ConcurrentHashMap set)
        val isContact = if (normalizedNumber != null) {
            contactCache.contains(normalizedNumber)
        } else false

        // 7. Read private number policy & unknown call action from volatile cache
        val privatePolicy = userPreferences.cachedPrivateNumberPolicy
        val unknownCallAction = userPreferences.cachedUnknownCallAction

        AppLogger.d(
            "evaluateCall: blacklisted=$isBlacklisted whitelisted=$isWhitelisted " +
            "contact=$isContact privatePolicy=$privatePolicy unknownAction=$unknownCallAction " +
            "contactCacheState=${contactCache.state} contactCacheSize=${contactCache.size}"
        )

        // 8. Evaluate
        val decision = blockingEngine.evaluate(
            normalizedNumber = normalizedNumber,
            isBlockingEnabled = true, // Already checked above
            isScheduleActive = isScheduleActive,
            isInBlacklist = isBlacklisted,
            isInWhitelist = isWhitelisted,
            isContact = isContact,
            privateNumberPolicy = privatePolicy,
            unknownCallAction = unknownCallAction
        )

        AppLogger.d("evaluateCall: DECISION=${decision.action} reason=${decision.reason}")
        return decision
    }

    /**
     * Evaluate schedule state. Pure computation on in-memory data.
     */
    private fun evaluateSchedule(): Boolean {
        val schedules = scheduleRepository.getEnabledSchedulesFast()
        val tempBlockUntil = userPreferences.cachedTemporaryBlockUntil

        val tempBlockDateTime = if (tempBlockUntil > 0) {
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(tempBlockUntil),
                ZoneId.systemDefault()
            )
        } else null

        return scheduleEvaluator.isBlockingActive(schedules, tempBlockDateTime)
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun blockCall(callDetails: Call.Details, decision: BlockDecision) {
        // Block: Disallows call silently without sending explicit carrier reject tone.
        // setSkipCallLog(true) ensures NO missed call entry or notification is created in the dialer app.
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(false)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)

        recordAndNotifyCall(callDetails, decision, action = "block")
    }

    private fun rejectCall(callDetails: Call.Details, decision: BlockDecision) {
        // Reject: Actively declines/drops the call immediately (carrier busy tone / straight to voicemail).
        // setSkipCallLog(true) ensures NO missed call entry or notification is created in the dialer app.
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)

        recordAndNotifyCall(callDetails, decision, action = "reject")
    }

    private fun silenceCall(callDetails: Call.Details, decision: BlockDecision) {
        // Silence: Must use setDisallowCall(true) to prevent the screen from waking.
        // Android's setSilenceCall(true) only mutes the ringtone but still shows InCallUI
        // and turns on the screen — which defeats the purpose.
        // With setDisallowCall(true) + setRejectCall(false), the caller hears ringing until
        // they hang up (same network behavior as Block), but the user is not disturbed.
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(false)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
        respondToCall(callDetails, response)

        recordAndNotifyCall(callDetails, decision, action = "silence")
    }

    private fun recordAndNotifyCall(
        callDetails: Call.Details,
        decision: BlockDecision,
        action: String
    ) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        val normalizedNumber = normalizer.normalize(rawNumber)
        val screenedAtMillis = System.currentTimeMillis()

        serviceScope.launch {
            try {
                // Record in history
                blockedCallRepository.recordBlockedCall(
                    normalizedNumber = normalizedNumber,
                    displayNumber = rawNumber,
                    reason = decision.reason.displayKey,
                    action = action
                )

                // Send silent notification if configured
                blockedCallNotifier.onCallScreened(rawNumber, decision.reason, action)

                AppLogger.d("Screened call ($action): ${AppLogger.maskNumber(rawNumber)} reason=${decision.reason}")
            } catch (e: Exception) {
                AppLogger.e("Failed to record/notify screened call", e)
            }
        }

        // Delete the call log entry the system creates (setSkipCallLog is ignored for non-system apps).
        // IMPORTANT: Uses applicationContext and CallLogCleaner's own process-level scope
        // because this service is destroyed immediately after respondToCall().
        CallLogCleaner.scheduleCleanup(applicationContext, rawNumber, screenedAtMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
