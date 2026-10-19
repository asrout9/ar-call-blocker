package com.asrou.arcallblocker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEvaluatorTest {

    private lateinit var evaluator: ScheduleEvaluator

    @Before
    fun setUp() {
        evaluator = ScheduleEvaluator()
    }

    @Test
    fun `empty schedules means blocking is active`() {
        val isActive = evaluator.isBlockingActive(
            schedules = emptyList(),
            temporaryBlockUntil = null,
            now = LocalDateTime.of(2026, 9, 1, 14, 0)
        )
        assertTrue(isActive)
    }

    @Test
    fun `temporary blocking takes priority when active`() {
        val now = LocalDateTime.of(2026, 9, 1, 14, 0)
        val tempUntil = now.plusHours(2)

        val isActive = evaluator.isBlockingActive(
            schedules = emptyList(),
            temporaryBlockUntil = tempUntil,
            now = now
        )
        assertTrue(isActive)
    }

    @Test
    fun `same day schedule matches when within time range`() {
        // Tuesday at 14:00 (2026-09-01 is a Tuesday)
        val now = LocalDateTime.of(2026, 9, 1, 14, 0)

        val schedule = ScheduleEvaluator.Schedule(
            days = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            isEnabled = true
        )

        val isActive = evaluator.isBlockingActive(
            schedules = listOf(schedule),
            temporaryBlockUntil = null,
            now = now
        )
        assertTrue(isActive)
    }

    @Test
    fun `same day schedule does not match when outside time range`() {
        // Tuesday at 18:00
        val now = LocalDateTime.of(2026, 9, 1, 18, 0)

        val schedule = ScheduleEvaluator.Schedule(
            days = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            isEnabled = true
        )

        val isActive = evaluator.isBlockingActive(
            schedules = listOf(schedule),
            temporaryBlockUntil = null,
            now = now
        )
        assertFalse(isActive)
    }

    @Test
    fun `overnight schedule matches evening portion on scheduled day`() {
        // Tuesday at 23:00 (schedule is Tue 22:00 -> 07:00)
        val now = LocalDateTime.of(2026, 9, 1, 23, 0)

        val schedule = ScheduleEvaluator.Schedule(
            days = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(7, 0),
            isEnabled = true
        )

        val isActive = evaluator.isBlockingActive(
            schedules = listOf(schedule),
            temporaryBlockUntil = null,
            now = now
        )
        assertTrue(isActive)
    }

    @Test
    fun `overnight schedule matches morning portion on following day`() {
        // Wednesday at 05:00 (schedule was set for Tuesday 22:00 -> 07:00)
        val now = LocalDateTime.of(2026, 9, 2, 5, 0) // Wednesday

        val schedule = ScheduleEvaluator.Schedule(
            days = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(7, 0),
            isEnabled = true
        )

        val isActive = evaluator.isBlockingActive(
            schedules = listOf(schedule),
            temporaryBlockUntil = null,
            now = now
        )
        assertTrue(isActive)
    }

    @Test
    fun `disabled schedule is ignored`() {
        val now = LocalDateTime.of(2026, 9, 1, 14, 0)

        val schedule = ScheduleEvaluator.Schedule(
            days = setOf(DayOfWeek.TUESDAY),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            isEnabled = false
        )

        val isActive = evaluator.isBlockingActive(
            schedules = listOf(schedule),
            temporaryBlockUntil = null,
            now = now
        )
        assertFalse(isActive)
    }
}
