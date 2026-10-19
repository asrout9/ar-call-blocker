package com.asrou.arcallblocker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring blocking schedule.
 *
 * Days are stored as a comma-separated string of ISO day numbers (1=Monday, 7=Sunday).
 * Times are stored as "HH:mm" strings.
 */
@Entity(tableName = "schedules")
data class ScheduleEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Comma-separated ISO day-of-week numbers (1=Monday through 7=Sunday).
     * Empty string means every day.
     */
    @ColumnInfo(name = "days")
    val days: String = "",

    /** Start time in "HH:mm" format, or null for start of day */
    @ColumnInfo(name = "start_time")
    val startTime: String? = null,

    /** End time in "HH:mm" format, or null for end of day */
    @ColumnInfo(name = "end_time")
    val endTime: String? = null,

    /** Whether this is an all-day schedule */
    @ColumnInfo(name = "is_all_day")
    val isAllDay: Boolean = false,

    /** Whether this schedule is enabled */
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /** Optional user label for this schedule (e.g., "Work Hours", "Night") */
    @ColumnInfo(name = "label")
    val label: String? = null
)
