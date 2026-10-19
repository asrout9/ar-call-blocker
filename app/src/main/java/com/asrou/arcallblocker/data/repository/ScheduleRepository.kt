package com.asrou.arcallblocker.data.repository

import com.asrou.arcallblocker.data.db.dao.ScheduleDao
import com.asrou.arcallblocker.data.db.entity.ScheduleEntry
import com.asrou.arcallblocker.domain.ScheduleEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for blocking schedules.
 *
 * Converts between database [ScheduleEntry] and domain [ScheduleEvaluator.Schedule].
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        com.asrou.arcallblocker.core.util.AppLogger.e("Error in ScheduleRepository scope", throwable)
    }
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO + exceptionHandler)

    @Volatile
    private var cachedEnabledSchedules: List<ScheduleEvaluator.Schedule> = emptyList()

    init {
        scope.launch {
            try {
                scheduleDao.observeAll().collect { entries ->
                    cachedEnabledSchedules = entries.filter { it.isEnabled }.map { it.toDomain() }
                }
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Failed to observe schedules", e)
            }
        }
    }

    fun observeAll(): Flow<List<ScheduleEntry>> = scheduleDao.observeAll()

    fun observeEnabledCount(): Flow<Int> = scheduleDao.observeEnabledCount()

    /** Fast in-memory getter for the screening service hot path */
    fun getEnabledSchedulesFast(): List<ScheduleEvaluator.Schedule> = cachedEnabledSchedules

    /** Get enabled schedules as domain objects for the evaluator */
    suspend fun getEnabledSchedules(): List<ScheduleEvaluator.Schedule> {
        return scheduleDao.getEnabledSchedules().map { it.toDomain() }
    }

    suspend fun insert(entry: ScheduleEntry): Long = scheduleDao.insert(entry)

    suspend fun update(entry: ScheduleEntry) = scheduleDao.update(entry)

    suspend fun delete(entry: ScheduleEntry) = scheduleDao.delete(entry)

    suspend fun clearAll() = scheduleDao.deleteAll()

    /** Convert a database entry to a domain schedule */
    fun ScheduleEntry.toDomain(): ScheduleEvaluator.Schedule {
        val daySet = if (days.isBlank()) {
            emptySet()
        } else {
            days.split(",").mapNotNull { dayStr ->
                dayStr.trim().toIntOrNull()?.let { DayOfWeek.of(it) }
            }.toSet()
        }

        return ScheduleEvaluator.Schedule(
            id = id,
            days = daySet,
            startTime = startTime?.let { LocalTime.parse(it, timeFormatter) },
            endTime = endTime?.let { LocalTime.parse(it, timeFormatter) },
            isAllDay = isAllDay,
            isEnabled = isEnabled
        )
    }

    /** Convert days set to storage format */
    fun daysToString(days: Set<DayOfWeek>): String {
        return days.map { it.value }.sorted().joinToString(",")
    }

    /** Convert time to storage format */
    fun timeToString(time: LocalTime): String {
        return time.format(timeFormatter)
    }
}
