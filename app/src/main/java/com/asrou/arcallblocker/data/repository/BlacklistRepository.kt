package com.asrou.arcallblocker.data.repository

import com.asrou.arcallblocker.core.util.PhoneNumberNormalizer
import com.asrou.arcallblocker.data.db.dao.BlacklistDao
import com.asrou.arcallblocker.data.db.entity.BlacklistEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the user's block list.
 *
 * Maintains an in-memory cache of normalized blacklisted numbers for O(1) lookup
 * during the call screening hot path. The cache is loaded from Room at initialization
 * and kept in sync on mutations.
 */
@Singleton
class BlacklistRepository @Inject constructor(
    private val blacklistDao: BlacklistDao,
    private val normalizer: PhoneNumberNormalizer
) {
    /** Thread-safe in-memory cache of normalized blacklisted numbers */
    private val cache = ConcurrentHashMap.newKeySet<String>()
    private val mutex = Mutex()
    private var cacheLoaded = false

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        com.asrou.arcallblocker.core.util.AppLogger.e("Error in BlacklistRepository scope", throwable)
    }
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO + exceptionHandler)

    init {
        scope.launch {
            try {
                ensureCacheLoaded()
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Failed to preload blacklist cache", e)
            }
        }
    }

    /** Observe all blacklist entries (for UI) */
    fun observeAll(): Flow<List<BlacklistEntry>> = blacklistDao.observeAll()

    /** Search blacklist entries */
    fun search(query: String): Flow<List<BlacklistEntry>> = blacklistDao.search(query)

    /** Observe blacklist count */
    fun observeCount(): Flow<Int> = blacklistDao.observeCount()

    /**
     * Check if a normalized number is in the blacklist.
     * Non-blocking O(1) in-memory lookup.
     */
    fun isBlacklistedFast(normalizedNumber: String): Boolean {
        return cache.contains(normalizedNumber)
    }

    /**
     * Check if a normalized number is in the blacklist.
     * This is the suspend method for backward compatibility.
     * O(1) lookup, ensures cache loaded.
     */
    suspend fun isBlacklisted(normalizedNumber: String): Boolean {
        ensureCacheLoaded()
        return cache.contains(normalizedNumber)
    }

    /** Add a number to the blacklist */
    suspend fun add(
        rawNumber: String,
        label: String? = null,
        reason: String? = null,
        notes: String? = null
    ) {
        val normalized = normalizer.normalize(rawNumber) ?: return
        val entry = BlacklistEntry(
            normalizedNumber = normalized,
            displayNumber = rawNumber,
            label = label,
            reason = reason,
            notes = notes
        )
        mutex.withLock {
            blacklistDao.insert(entry)
            cache.add(normalized)
        }
    }

    /** Remove a blacklist entry */
    suspend fun remove(entry: BlacklistEntry) {
        mutex.withLock {
            blacklistDao.delete(entry)
            cache.remove(entry.normalizedNumber)
        }
    }

    /** Remove by normalized number */
    suspend fun removeByNumber(normalizedNumber: String) {
        mutex.withLock {
            blacklistDao.deleteByNumber(normalizedNumber)
            cache.remove(normalizedNumber)
        }
    }

    /** Clear the entire blacklist */
    suspend fun clearAll() {
        mutex.withLock {
            blacklistDao.deleteAll()
            cache.clear()
        }
    }

    /** Ensure the in-memory cache is populated from the database */
    private suspend fun ensureCacheLoaded() {
        if (cacheLoaded) return
        mutex.withLock {
            if (cacheLoaded) return
            val numbers = blacklistDao.getAllNormalizedNumbers()
            cache.addAll(numbers)
            cacheLoaded = true
        }
    }

    /** Force reload the cache (e.g., after data restore) */
    suspend fun reloadCache() {
        mutex.withLock {
            cache.clear()
            val numbers = blacklistDao.getAllNormalizedNumbers()
            cache.addAll(numbers)
            cacheLoaded = true
        }
    }
}
