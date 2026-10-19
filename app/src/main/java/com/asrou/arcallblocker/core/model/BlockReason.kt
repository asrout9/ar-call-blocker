package com.asrou.arcallblocker.core.model

/**
 * Reasons why a call was allowed or blocked.
 * Used for blocked-call history display and diagnostics.
 */
enum class BlockReason(val displayKey: String) {
    BLOCKING_DISABLED("blocking_disabled"),
    EMERGENCY("emergency"),
    BLACKLIST("blacklist"),
    WHITELIST("whitelist"),
    CONTACT("contact"),
    NOT_IN_CONTACTS("not_in_contacts"),
    PRIVATE_NUMBER("private_number"),
    PRIVATE_NUMBER_ALLOWED("private_number_allowed"),
    UNKNOWN_CALLER("unknown_caller"),
    SCHEDULE("schedule"),
    SCHEDULE_INACTIVE("schedule_inactive")
}
