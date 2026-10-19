package com.asrou.arcallblocker.core.model

/**
 * User's configured policy for handling private/hidden/unknown callers.
 */
enum class PrivateNumberPolicy {
    /** Block private/hidden numbers */
    BLOCK,

    /** Allow private/hidden numbers */
    ALLOW,

    /** Follow the contacts-only policy (block since they can't be matched to a contact) */
    FOLLOW_CONTACTS_ONLY
}
