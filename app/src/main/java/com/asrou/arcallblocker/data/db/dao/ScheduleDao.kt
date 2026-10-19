package com.asrou.arcallblocker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.asrou.arcallblocker.data.db.entity.ScheduleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun observeAll(): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedules WHERE is_enabled = 1")
    suspend fun getEnabledSchedules(): List<ScheduleEntry>

    @Query("SELECT COUNT(*) FROM schedules WHERE is_enabled = 1")
    fun observeEnabledCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScheduleEntry): Long

    @Update
    suspend fun update(entry: ScheduleEntry)

    @Delete
    suspend fun delete(entry: ScheduleEntry)

    @Query("DELETE FROM schedules")
    suspend fun deleteAll()
}
