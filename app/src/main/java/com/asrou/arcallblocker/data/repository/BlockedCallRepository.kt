package com.asrou.arcallblocker.data.repository

import com.asrou.arcallblocker.data.db.dao.BlockedCallDao
import com.asrou.arcallblocker.data.db.entity.BlockedCallEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for blocked call history.
 * No in-memory cache needed — this is only accessed for UI display and logging.
 */
@Singleton
class BlockedCallRepository @Inject constructor(
    private val blockedCallDao: BlockedCallDao
) {
    fun observeAll(): Flow<List<BlockedCallEntry>> = blockedCallDao.observeAll()

    fun observeRecent(limit: Int = 50): Flow<List<BlockedCallEntry>> =
        blockedCallDao.observeRecent(limit)

    fun observeCountSince(sinceTimestamp: Long): Flow<Int> =
        blockedCallDao.observeCountSince(sinceTimestamp)

    fun observeTotalCount(): Flow<Int> = blockedCallDao.observeTotalCount()

    /** Record a screened call. Called asynchronously from the screening service. */
    suspend fun recordBlockedCall(
        normalizedNumber: String?,
        displayNumber: String?,
        reason: String,
        action: String = "block"
    ) {
        val entry = BlockedCallEntry(
            normalizedNumber = normalizedNumber,
            displayNumber = displayNumber,
            reason = reason,
            action = action
        )
        blockedCallDao.insert(entry)
    }

    suspend fun delete(entry: BlockedCallEntry) {
        blockedCallDao.delete(entry)
    }

    /** Delete records older than the given timestamp */
    suspend fun cleanupOldRecords(beforeTimestamp: Long): Int {
        return blockedCallDao.deleteOlderThan(beforeTimestamp)
    }

    suspend fun clearAll() {
        blockedCallDao.deleteAll()
    }
}
