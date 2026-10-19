package com.asrou.arcallblocker.telecom

import android.content.ContentUris
import android.content.Context
import android.provider.CallLog
import android.telephony.PhoneNumberUtils
import com.asrou.arcallblocker.core.util.AppLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Deletes call log entries for screened calls.
 *
 * Android's CallScreeningService.CallResponse.setSkipCallLog(true) is only
 * honoured for system and carrier call screening apps. For third-party apps
 * holding ROLE_CALL_SCREENING, the system still writes entries to the call log.
 *
 * This class works around that limitation by querying the call log after a
 * call is screened and deleting ALL matching entries (by ID) regardless of how
 * the system formatted the phone number.
 *
 * IMPORTANT: Uses its own process-level CoroutineScope because the
 * CallScreeningService is destroyed immediately after respondToCall(),
 * which would cancel any service-scoped coroutines before the system
 * has even written the call log entry.
 *
 * Requires READ_CALL_LOG and WRITE_CALL_LOG permissions.
 */
object CallLogCleaner {

    private const val TAG = "CallLogCleaner"

    /** Process-level scope that survives CallScreeningService destruction */
    private val cleanerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            AppLogger.e("$TAG: Uncaught error in cleaner scope", throwable)
        }
    )

    /**
     * Schedules deletion of call log entries for the given phone number.
     *
     * Launches work on a process-level scope (NOT the service scope) so it
     * continues even after the CallScreeningService is destroyed.
     *
     * @param appContext The APPLICATION context (not the service context)
     * @param phoneNumber The raw phone number (as received from Call.Details.handle)
     * @param screenedAtMillis Approximate timestamp when the call was screened
     */
    fun scheduleCleanup(appContext: Context, phoneNumber: String?, screenedAtMillis: Long) {
        cleanerScope.launch {
            try {
                performCleanup(appContext, phoneNumber, screenedAtMillis)
            } catch (e: Exception) {
                AppLogger.e("$TAG: Cleanup failed", e)
            }
        }
    }

    /**
     * Runs multiple cleanup passes with increasing delays.
     *
     * We run ALL passes because:
     * 1. The system call log write is asynchronous — entries may not appear
     *    immediately when respondToCall() returns.
     * 2. Some OEMs write TWO entries (e.g. MISSED_TYPE + BLOCKED_TYPE),
     *    sometimes at slightly different times.
     */
    private suspend fun performCleanup(
        context: Context,
        phoneNumber: String?,
        screenedAtMillis: Long
    ) {
        val delays = longArrayOf(500, 1500, 3000, 6000, 10000)
        var totalDeleted = 0

        for (delayMs in delays) {
            delay(delayMs)
            val deleted = deleteMatchingEntries(context, phoneNumber, screenedAtMillis)
            totalDeleted += deleted
            if (deleted > 0) {
                AppLogger.d("$TAG: Pass @${delayMs}ms — deleted $deleted entry/entries for ${AppLogger.maskNumber(phoneNumber)}")
            }
        }

        if (totalDeleted == 0) {
            AppLogger.d("$TAG: No call log entries found to delete for ${AppLogger.maskNumber(phoneNumber)}")
        } else {
            AppLogger.d("$TAG: Total deleted $totalDeleted entry/entries for ${AppLogger.maskNumber(phoneNumber)}")
        }
    }

    /**
     * Queries the call log for entries in the time window, then deletes those
     * whose number matches [phoneNumber] (using [PhoneNumberUtils.compare] for
     * fuzzy matching) or whose call type is MISSED/REJECTED/BLOCKED.
     *
     * Deletes by individual _ID to avoid accidentally removing unrelated entries.
     */
    private fun deleteMatchingEntries(
        context: Context,
        phoneNumber: String?,
        screenedAtMillis: Long
    ): Int {
        return try {
            val windowStart = screenedAtMillis - 5_000
            val windowEnd = screenedAtMillis + 60_000  // 1-minute window

            // Query ALL entries in the time window that are non-outgoing
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE
            )
            val where = "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ? AND " +
                "${CallLog.Calls.TYPE} != ${CallLog.Calls.OUTGOING_TYPE}"
            val selectionArgs = arrayOf(windowStart.toString(), windowEnd.toString())

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                where,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return 0

            val idsToDelete = mutableListOf<Long>()

            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                while (c.moveToNext()) {
                    val entryId = c.getLong(idIdx)
                    val entryNumber = c.getString(numberIdx)
                    val entryType = c.getInt(typeIdx)

                    val isTargetType = entryType == CallLog.Calls.MISSED_TYPE ||
                        entryType == CallLog.Calls.REJECTED_TYPE ||
                        entryType == CallLog.Calls.BLOCKED_TYPE ||
                        entryType == CallLog.Calls.INCOMING_TYPE

                    if (!isTargetType) continue

                    if (phoneNumber.isNullOrBlank()) {
                        // Private number: match on empty/null entry number + target type
                        if (entryNumber.isNullOrBlank()) {
                            idsToDelete.add(entryId)
                        }
                    } else {
                        // Use PhoneNumberUtils.compare for fuzzy matching
                        // (handles +91xxx vs 0xxx vs xxx differences)
                        @Suppress("DEPRECATION")
                        if (PhoneNumberUtils.compare(phoneNumber, entryNumber)) {
                            idsToDelete.add(entryId)
                        }
                    }
                }
            }

            // Delete each matched entry by _ID
            var deleted = 0
            for (id in idsToDelete) {
                val uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id)
                deleted += context.contentResolver.delete(uri, null, null)
            }
            deleted

        } catch (e: SecurityException) {
            AppLogger.e("$TAG: Missing CALL_LOG permission — grant in Settings > Apps > AR Call Blocker > Permissions", e)
            0
        } catch (e: Throwable) {
            AppLogger.e("$TAG: Error deleting call log entries", e)
            0
        }
    }
}
