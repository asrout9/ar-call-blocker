package com.asrou.arcallblocker.data.cache

import com.asrou.arcallblocker.core.util.AppLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe in-memory cache of normalized contact phone numbers.
 *
 * This is the primary data structure queried on the call screening hot path.
 * Uses a [ConcurrentHashMap]-backed set for O(1) thread-safe lookups
 * without requiring synchronization on reads.
 *
 * The cache is treated as derived data — it can be rebuilt from the
 * Contacts Provider at any time.
 */
@Singleton
class ContactCache @Inject constructor() {

    private val numbers = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    var state: State = State.UNINITIALIZED
        private set

    @Volatile
    var lastSyncTimestamp: Long = 0L
        private set

    /** The number of unique normalized numbers in the cache */
    val size: Int get() = numbers.size

    /**
     * Check if a normalized number is in the contact cache.
     * This is the hot-path method — O(1), no I/O, no blocking.
     */
    fun contains(normalizedNumber: String): Boolean {
        return numbers.contains(normalizedNumber)
    }

    /**
     * Atomically replace the entire cache with a new set of numbers.
     * Called after a full contact sync.
     */
    fun rebuild(newNumbers: Set<String>) {
        numbers.clear()
        numbers.addAll(newNumbers)
        lastSyncTimestamp = System.currentTimeMillis()
        state = State.READY
        AppLogger.d("ContactCache rebuilt with ${newNumbers.size} numbers")
    }

    /** Mark the cache as loading/syncing */
    fun markSyncing() {
        state = State.SYNCING
    }

    /** Mark the cache as errored */
    fun markError() {
        state = State.ERROR
    }

    /** Clear the cache (e.g., when contacts permission is revoked) */
    fun clear() {
        numbers.clear()
        state = State.UNINITIALIZED
        lastSyncTimestamp = 0L
    }

    enum class State {
        UNINITIALIZED,
        SYNCING,
        READY,
        ERROR
    }
}
