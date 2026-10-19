package com.asrou.arcallblocker.core.model

/**
 * User's configured action for handling unknown calls (numbers not in contacts).
 */
enum class UnknownCallAction {
    /**
     * Drop the call silently without ringing or notifying the user,
     * and without actively sending a reject signal.
     */
    BLOCK,

    /**
     * Auto-silence the ringtone and vibration. System notification is suppressed.
     */
    SILENCE,

    /**
     * Actively reject the call immediately (sends busy tone or routes straight to voicemail).
     * Prevents the screen from turning on.
     */
    REJECT
}
