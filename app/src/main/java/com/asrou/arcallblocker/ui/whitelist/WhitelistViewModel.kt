package com.asrou.arcallblocker.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.data.db.entity.WhitelistEntry
import com.asrou.arcallblocker.data.repository.WhitelistRepository
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
class WhitelistViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries: StateFlow<List<WhitelistEntry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) whitelistRepository.observeAll()
            else whitelistRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNumber(number: String, label: String? = null) {
        viewModelScope.launch {
            whitelistRepository.add(number, label = label)
        }
    }

    fun removeEntry(entry: WhitelistEntry) {
        viewModelScope.launch {
            whitelistRepository.remove(entry)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            whitelistRepository.clearAll()
        }
    }
}
