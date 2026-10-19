package com.asrou.arcallblocker.core.model

/**
 * User's configured notification mode for blocked calls.
 */
enum class NotificationMode {
    /** Send a notification for every blocked call */
    EVERY_CALL,

    /** Send a daily summary notification */
    DAILY_SUMMARY,

    /** No notifications */
    NONE
}
