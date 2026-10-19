package com.asrou.arcallblocker.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.asrou.arcallblocker.MainActivity
import com.asrou.arcallblocker.R
import com.asrou.arcallblocker.core.model.BlockReason
import com.asrou.arcallblocker.core.model.NotificationMode
import com.asrou.arcallblocker.data.datastore.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles blocked-call notifications.
 *
 * Respects the user's notification preference and Android notification permission.
 * Never exposes full phone numbers on the lock screen.
 */
@Singleton
class BlockedCallNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val CHANNEL_ID = "blocked_calls"
        private const val NOTIFICATION_ID_BASE = 1000
        private var notificationCounter = 0
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Called when a call is blocked. Sends a notification if configured to do so.
     * This is called asynchronously from the screening service.
     */
    suspend fun onCallBlocked(displayNumber: String?, reason: BlockReason) {
        onCallScreened(displayNumber, reason, "block")
    }

    /**
     * Called when a call is screened (blocked, silenced, or rejected).
     * Sends a low-priority, silent notification if configured to do so.
     */
    suspend fun onCallScreened(displayNumber: String?, reason: BlockReason, action: String) {
        val mode = userPreferences.notificationMode.first()

        when (mode) {
            NotificationMode.EVERY_CALL -> sendScreenedCallNotification(displayNumber, reason, action)
            NotificationMode.DAILY_SUMMARY -> { /* Handled separately via scheduled work */ }
            NotificationMode.NONE -> { /* No notification */ }
        }
    }

    private fun sendScreenedCallNotification(displayNumber: String?, reason: BlockReason, action: String) {
        // Check notification permission (API 33+)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val actionTitle = when (action) {
            "silence" -> "Call silenced"
            "reject" -> "Call rejected"
            else -> "Call blocked"
        }

        val reasonText = when (reason) {
            BlockReason.NOT_IN_CONTACTS -> context.getString(R.string.reason_not_in_contacts)
            BlockReason.BLACKLIST -> context.getString(R.string.reason_blacklist)
            BlockReason.PRIVATE_NUMBER -> context.getString(R.string.reason_private_number)
            BlockReason.UNKNOWN_CALLER -> context.getString(R.string.reason_unknown_caller)
            BlockReason.SCHEDULE -> context.getString(R.string.reason_schedule)
            else -> actionTitle
        }

        // Mask number for notification — show partial for recognition but not full
        val maskedNumber = if (!displayNumber.isNullOrBlank()) {
            val digits = displayNumber.filter { it.isDigit() }
            if (digits.length > 4) "•••${digits.takeLast(4)}" else "Hidden"
        } else {
            "Hidden number"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(actionTitle)
            .setContentText("$maskedNumber — $reasonText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_shield)
                    .setContentTitle(actionTitle)
                    .setContentText("An incoming call was $action")
                    .build()
            )
            .build()

        val notificationId = NOTIFICATION_ID_BASE + (notificationCounter++ % 50)
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /** Check if notification permission is granted */
    fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
