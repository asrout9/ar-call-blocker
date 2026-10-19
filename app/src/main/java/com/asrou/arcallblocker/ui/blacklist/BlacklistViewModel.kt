package com.asrou.arcallblocker.ui.blacklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.data.db.entity.BlacklistEntry
import com.asrou.arcallblocker.data.repository.BlacklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries: StateFlow<List<BlacklistEntry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) blacklistRepository.observeAll()
            else blacklistRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNumber(number: String, label: String? = null) {
        viewModelScope.launch {
            blacklistRepository.add(number, label = label, reason = "Manual")
        }
    }

    fun removeEntry(entry: BlacklistEntry) {
        viewModelScope.launch {
            blacklistRepository.remove(entry)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            blacklistRepository.clearAll()
        }
    }
}
