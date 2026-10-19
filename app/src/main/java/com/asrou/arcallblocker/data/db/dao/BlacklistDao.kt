package com.asrou.arcallblocker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asrou.arcallblocker.data.db.entity.BlacklistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist ORDER BY date_added DESC")
    fun observeAll(): Flow<List<BlacklistEntry>>

    @Query("SELECT normalized_number FROM blacklist")
    suspend fun getAllNormalizedNumbers(): List<String>

    @Query("SELECT * FROM blacklist WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun findByNumber(normalizedNumber: String): BlacklistEntry?

    @Query("SELECT * FROM blacklist WHERE display_number LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' ORDER BY date_added DESC")
    fun search(query: String): Flow<List<BlacklistEntry>>

    @Query("SELECT COUNT(*) FROM blacklist")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlacklistEntry): Long

    @Delete
    suspend fun delete(entry: BlacklistEntry)

    @Query("DELETE FROM blacklist WHERE normalized_number = :normalizedNumber")
    suspend fun deleteByNumber(normalizedNumber: String)

    @Query("DELETE FROM blacklist")
    suspend fun deleteAll()
}
