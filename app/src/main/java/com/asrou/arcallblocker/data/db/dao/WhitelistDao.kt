package com.asrou.arcallblocker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.asrou.arcallblocker.data.db.entity.WhitelistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Query("SELECT * FROM whitelist ORDER BY date_added DESC")
    fun observeAll(): Flow<List<WhitelistEntry>>

    @Query("SELECT normalized_number FROM whitelist")
    suspend fun getAllNormalizedNumbers(): List<String>

    @Query("SELECT * FROM whitelist WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun findByNumber(normalizedNumber: String): WhitelistEntry?

    @Query("SELECT * FROM whitelist WHERE display_number LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' ORDER BY date_added DESC")
    fun search(query: String): Flow<List<WhitelistEntry>>

    @Query("SELECT COUNT(*) FROM whitelist")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WhitelistEntry): Long

    @Update
    suspend fun update(entry: WhitelistEntry)

    @Delete
    suspend fun delete(entry: WhitelistEntry)

    @Query("DELETE FROM whitelist WHERE normalized_number = :normalizedNumber")
    suspend fun deleteByNumber(normalizedNumber: String)

    @Query("DELETE FROM whitelist")
    suspend fun deleteAll()
}
