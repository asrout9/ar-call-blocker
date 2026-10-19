package com.asrou.arcallblocker.core.model

/**
 * The result of evaluating an incoming call against the blocking rules.
 * Immutable and allocation-free on the hot path via pre-defined constants.
 */
data class BlockDecision(
    val action: Action,
    val reason: BlockReason
) {
    enum class Action {
        ALLOW,
        BLOCK,
        SILENCE,
        REJECT
    }

    companion object {
        // Pre-allocated decisions to avoid object creation on the hot path
        val ALLOW_BLOCKING_OFF = BlockDecision(Action.ALLOW, BlockReason.BLOCKING_DISABLED)
        val ALLOW_EMERGENCY = BlockDecision(Action.ALLOW, BlockReason.EMERGENCY)
        val ALLOW_WHITELIST = BlockDecision(Action.ALLOW, BlockReason.WHITELIST)
        val ALLOW_CONTACT = BlockDecision(Action.ALLOW, BlockReason.CONTACT)
        val ALLOW_SCHEDULE_INACTIVE = BlockDecision(Action.ALLOW, BlockReason.SCHEDULE_INACTIVE)
        val ALLOW_PRIVATE_ALLOWED = BlockDecision(Action.ALLOW, BlockReason.PRIVATE_NUMBER_ALLOWED)

        val BLOCK_BLACKLIST = BlockDecision(Action.BLOCK, BlockReason.BLACKLIST)
        val SILENCE_BLACKLIST = BlockDecision(Action.SILENCE, BlockReason.BLACKLIST)
        val REJECT_BLACKLIST = BlockDecision(Action.REJECT, BlockReason.BLACKLIST)

        val BLOCK_NOT_IN_CONTACTS = BlockDecision(Action.BLOCK, BlockReason.NOT_IN_CONTACTS)
        val SILENCE_NOT_IN_CONTACTS = BlockDecision(Action.SILENCE, BlockReason.NOT_IN_CONTACTS)
        val REJECT_NOT_IN_CONTACTS = BlockDecision(Action.REJECT, BlockReason.NOT_IN_CONTACTS)

        val BLOCK_PRIVATE_NUMBER = BlockDecision(Action.BLOCK, BlockReason.PRIVATE_NUMBER)
        val SILENCE_PRIVATE_NUMBER = BlockDecision(Action.SILENCE, BlockReason.PRIVATE_NUMBER)
        val REJECT_PRIVATE_NUMBER = BlockDecision(Action.REJECT, BlockReason.PRIVATE_NUMBER)

        val BLOCK_UNKNOWN_CALLER = BlockDecision(Action.BLOCK, BlockReason.UNKNOWN_CALLER)
        val SILENCE_UNKNOWN_CALLER = BlockDecision(Action.SILENCE, BlockReason.UNKNOWN_CALLER)
        val REJECT_UNKNOWN_CALLER = BlockDecision(Action.REJECT, BlockReason.UNKNOWN_CALLER)

        val BLOCK_SCHEDULE = BlockDecision(Action.BLOCK, BlockReason.SCHEDULE)
    }
}
