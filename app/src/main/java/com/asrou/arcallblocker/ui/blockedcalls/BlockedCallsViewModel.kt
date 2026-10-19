package com.asrou.arcallblocker.ui.blockedcalls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asrou.arcallblocker.data.db.entity.BlockedCallEntry
import com.asrou.arcallblocker.data.repository.BlacklistRepository
import com.asrou.arcallblocker.data.repository.BlockedCallRepository
import com.asrou.arcallblocker.data.repository.WhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedCallsViewModel @Inject constructor(
    private val blockedCallRepository: BlockedCallRepository,
    private val blacklistRepository: BlacklistRepository,
    private val whitelistRepository: WhitelistRepository
) : ViewModel() {

    val blockedCalls: StateFlow<List<BlockedCallEntry>> =
        blockedCallRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEntry(entry: BlockedCallEntry) {
        viewModelScope.launch {
            blockedCallRepository.delete(entry)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            blockedCallRepository.clearAll()
        }
    }

    fun addToBlacklist(number: String) {
        viewModelScope.launch {
            blacklistRepository.add(number, reason = "From blocked history")
        }
    }

    fun addToWhitelist(number: String) {
        viewModelScope.launch {
            whitelistRepository.add(number)
        }
    }
}
