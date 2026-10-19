package com.asrou.arcallblocker.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.data.datastore.UserPreferences
import com.asrou.arcallblocker.data.db.entity.ScheduleEntry
import com.asrou.arcallblocker.data.repository.ScheduleRepository
import com.asrou.arcallblocker.domain.ScheduleEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ScheduleUiState(
    val schedules: List<ScheduleEntry> = emptyList(),
    val temporaryBlockUntil: Long = 0L,
    val isTemporaryBlockActive: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.observeAll(),
        userPreferences.temporaryBlockUntil
    ) { schedules, tempUntil ->
        ScheduleUiState(
            schedules = schedules,
            temporaryBlockUntil = tempUntil,
            isTemporaryBlockActive = tempUntil > System.currentTimeMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    fun addSchedule(
        days: Set<DayOfWeek>,
        startTime: LocalTime?,
        endTime: LocalTime?,
        isAllDay: Boolean,
        label: String? = null
    ) {
        viewModelScope.launch {
            val entry = ScheduleEntry(
                days = scheduleRepository.daysToString(days),
                startTime = startTime?.let { scheduleRepository.timeToString(it) },
                endTime = endTime?.let { scheduleRepository.timeToString(it) },
                isAllDay = isAllDay,
                label = label
            )
            scheduleRepository.insert(entry)
        }
    }

    fun toggleSchedule(entry: ScheduleEntry) {
        viewModelScope.launch {
            scheduleRepository.update(entry.copy(isEnabled = !entry.isEnabled))
        }
    }

    fun deleteSchedule(entry: ScheduleEntry) {
        viewModelScope.launch {
            scheduleRepository.delete(entry)
        }
    }

    fun setTemporaryBlock(durationMinutes: Long) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            userPreferences.setTemporaryBlockUntil(until)
            // Also enable blocking if not already on
            userPreferences.setBlockingEnabled(true)
        }
    }

    fun setBlockUntilMorning() {
        viewModelScope.launch {
            val tomorrow8am = LocalDateTime.now()
                .plusDays(1)
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
            val millis = tomorrow8am.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            userPreferences.setTemporaryBlockUntil(millis)
            userPreferences.setBlockingEnabled(true)
        }
    }

    fun clearTemporaryBlock() {
        viewModelScope.launch {
            userPreferences.clearTemporaryBlock()
        }
    }
}
