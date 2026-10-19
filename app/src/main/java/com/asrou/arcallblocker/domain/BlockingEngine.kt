package com.asrou.arcallblocker.domain

import com.asrou.arcallblocker.core.model.BlockDecision
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import com.asrou.arcallblocker.core.model.UnknownCallAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The core call blocking decision engine.
 *
 * This class is the single source of truth for whether an incoming call should be
 * allowed, blocked, silenced, or rejected. It is a pure logic class with NO Android framework dependencies,
 * making it fully unit-testable.
 *
 * ## Rule Evaluation Priority (deterministic, top-to-bottom)
 *
 * 1. **Blocking Mode OFF** → ALLOW (no interference whatsoever)
 * 2. **Schedule check** → If no schedule is active → ALLOW
 * 3. **Hidden/Private number** → Apply user's private number policy
 * 4. **Explicit Blacklist** → REJECT (user explicitly blocked this number)
 * 5. **Explicit Whitelist** → ALLOW (user explicitly allowed this number)
 * 6. **Contact match** → ALLOW (number is in the user's contacts)
 * 7. **Non-contact** → Apply [unknownCallAction] (BLOCK, SILENCE, or REJECT)
 *
 * The engine operates entirely on in-memory data structures.
 * No database queries, disk I/O, or network requests occur during evaluation.
 */
@Singleton
class BlockingEngine @Inject constructor() {

    /**
     * Evaluate whether an incoming call should be allowed, blocked, silenced, or rejected.
     *
     * This method is designed to be called from [CallScreeningService.onScreenCall]
     * and must return as fast as possible. All parameters are pre-resolved
     * before this call to avoid any I/O on the hot path.
     *
     * @param normalizedNumber The E.164-normalized incoming phone number, or null
     *                         if the number is hidden/private/unavailable.
     * @param isBlockingEnabled Whether the user has blocking mode turned on.
     * @param isScheduleActive Whether a blocking schedule is currently active.
     *                         If false and no temporary block is active, calls are allowed.
     *                         If the user has blocking enabled without schedules, pass true.
     * @param isInBlacklist Whether the normalized number is in the user's block list.
     * @param isInWhitelist Whether the normalized number is in the user's allow list.
     * @param isContact Whether the normalized number matches a contact in the cache.
     * @param privateNumberPolicy The user's configured policy for private/hidden numbers.
     * @param unknownCallAction The user's configured action for unknown/non-contact calls (BLOCK, SILENCE, REJECT).
     * @return A [BlockDecision] with the action (ALLOW/BLOCK/SILENCE/REJECT) and the reason.
     */
    fun evaluate(
        normalizedNumber: String?,
        isBlockingEnabled: Boolean,
        isScheduleActive: Boolean,
        isInBlacklist: Boolean,
        isInWhitelist: Boolean,
        isContact: Boolean,
        privateNumberPolicy: PrivateNumberPolicy,
        unknownCallAction: UnknownCallAction = UnknownCallAction.BLOCK
    ): BlockDecision {
        // Priority 1: Blocking disabled → never interfere
        if (!isBlockingEnabled) {
            return BlockDecision.ALLOW_BLOCKING_OFF
        }

        // Priority 2: Schedule check → if no schedule active, don't block
        if (!isScheduleActive) {
            return BlockDecision.ALLOW_SCHEDULE_INACTIVE
        }

        // Priority 3: Handle hidden/private/unknown numbers
        // A null or blank normalized number means the caller ID is hidden/unavailable
        if (normalizedNumber.isNullOrBlank()) {
            return when (privateNumberPolicy) {
                PrivateNumberPolicy.BLOCK -> BlockDecision.BLOCK_PRIVATE_NUMBER
                PrivateNumberPolicy.ALLOW -> BlockDecision.ALLOW_PRIVATE_ALLOWED
                PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY -> when (unknownCallAction) {
                    UnknownCallAction.BLOCK -> BlockDecision.BLOCK_UNKNOWN_CALLER
                    UnknownCallAction.SILENCE -> BlockDecision.SILENCE_UNKNOWN_CALLER
                    UnknownCallAction.REJECT -> BlockDecision.REJECT_UNKNOWN_CALLER
                }
            }
        }

        // Priority 4: Explicit blacklist → apply user's configured action
        if (isInBlacklist) {
            return when (unknownCallAction) {
                UnknownCallAction.BLOCK -> BlockDecision.BLOCK_BLACKLIST
                UnknownCallAction.SILENCE -> BlockDecision.SILENCE_BLACKLIST
                UnknownCallAction.REJECT -> BlockDecision.REJECT_BLACKLIST
            }
        }

        // Priority 5: Explicit whitelist → always allow
        if (isInWhitelist) {
            return BlockDecision.ALLOW_WHITELIST
        }

        // Priority 6: Contact match → allow
        if (isContact) {
            return BlockDecision.ALLOW_CONTACT
        }

        // Priority 7: Non-contact → apply user's configured unknown call action
        return when (unknownCallAction) {
            UnknownCallAction.BLOCK -> BlockDecision.BLOCK_NOT_IN_CONTACTS
            UnknownCallAction.SILENCE -> BlockDecision.SILENCE_NOT_IN_CONTACTS
            UnknownCallAction.REJECT -> BlockDecision.REJECT_NOT_IN_CONTACTS
        }
    }
}
