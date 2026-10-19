package com.asrou.arcallblocker.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates whether blocking should be active based on configured schedules.
 *
 * Supports:
 * - Day-of-week filtering (e.g., Monday-Friday)
 * - Time ranges within a day (e.g., 22:00 to 07:00)
 * - Overnight schedules that cross midnight
 * - All-day schedules
 * - Temporary blocking with an expiry timestamp
 * - Multiple concurrent schedules (any match activates blocking)
 *
 * This class is pure Kotlin with no Android dependencies for testability.
 */
@Singleton
class ScheduleEvaluator @Inject constructor() {

    /**
     * Represents a recurring blocking schedule.
     *
     * @param days The days of the week when this schedule is active. Empty means every day.
     * @param startTime The start time (inclusive). Null means start of day (00:00).
     * @param endTime The end time (exclusive). Null means end of day (23:59:59).
     * @param isAllDay If true, the schedule is active all day on the specified days.
     * @param isEnabled Whether this schedule is currently enabled by the user.
     */
    data class Schedule(
        val id: Long = 0,
        val days: Set<DayOfWeek> = emptySet(),
        val startTime: LocalTime? = null,
        val endTime: LocalTime? = null,
        val isAllDay: Boolean = false,
        val isEnabled: Boolean = true
    )

    /**
     * Check if blocking should be active right now based on the given schedules
     * and temporary blocking state.
     *
     * @param schedules The list of configured schedules.
     * @param temporaryBlockUntil If non-null, blocking is active until this time
     *                            regardless of schedules.
     * @param now The current date/time (injectable for testing).
     * @return true if blocking should be active.
     */
    fun isBlockingActive(
        schedules: List<Schedule>,
        temporaryBlockUntil: LocalDateTime?,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        // Temporary blocking takes priority
        if (temporaryBlockUntil != null && now.isBefore(temporaryBlockUntil)) {
            return true
        }

        // If no schedules are configured, blocking is always active
        // (the user controls it via the main toggle, not via schedule)
        if (schedules.isEmpty()) {
            return true
        }

        // Check if any enabled schedule matches the current time
        return schedules.any { schedule ->
            schedule.isEnabled && matchesSchedule(schedule, now)
        }
    }

    /**
     * Calculate remaining time for temporary blocking.
     *
     * @param temporaryBlockUntil The expiry timestamp.
     * @param now The current time.
     * @return Remaining duration in seconds, or 0 if expired/null.
     */
    fun remainingTemporarySeconds(
        temporaryBlockUntil: LocalDateTime?,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        if (temporaryBlockUntil == null) return 0
        val remaining = java.time.Duration.between(now, temporaryBlockUntil).seconds
        return maxOf(0, remaining)
    }

    private fun matchesSchedule(schedule: Schedule, now: LocalDateTime): Boolean {
        val today = now.dayOfWeek
        val currentTime = now.toLocalTime()

        // Check if we need to consider "yesterday's" overnight schedule
        if (schedule.isAllDay) {
            return matchesDay(schedule.days, today)
        }

        val startTime = schedule.startTime ?: LocalTime.MIN
        val endTime = schedule.endTime ?: LocalTime.MAX

        return if (startTime <= endTime) {
            // Same-day schedule (e.g., 09:00 to 17:00)
            matchesDay(schedule.days, today) && currentTime >= startTime && currentTime < endTime
        } else {
            // Overnight schedule (e.g., 22:00 to 07:00)
            // This is active during two periods:
            // 1. On the start day, from startTime to midnight
            // 2. On the next day, from midnight to endTime
            val inEveningPortion = matchesDay(schedule.days, today) && currentTime >= startTime
            val yesterday = today.minus(1)
            val inMorningPortion = matchesDay(schedule.days, yesterday) && currentTime < endTime
            inEveningPortion || inMorningPortion
        }
    }

    private fun matchesDay(scheduleDays: Set<DayOfWeek>, day: DayOfWeek): Boolean {
        // Empty days set means every day
        return scheduleDays.isEmpty() || day in scheduleDays
    }
}
