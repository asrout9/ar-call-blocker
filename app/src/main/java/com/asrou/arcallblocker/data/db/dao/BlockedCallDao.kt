package com.asrou.arcallblocker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.asrou.arcallblocker.data.db.entity.BlockedCallEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BlockedCallEntry>>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BlockedCallEntry>>

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :sinceTimestamp")
    fun observeCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun observeTotalCount(): Flow<Int>

    @Insert
    suspend fun insert(entry: BlockedCallEntry): Long

    @Delete
    suspend fun delete(entry: BlockedCallEntry)

    @Query("DELETE FROM blocked_calls WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()
}
