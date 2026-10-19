package com.asrou.arcallblocker.data.repository

import com.asrou.arcallblocker.core.util.PhoneNumberNormalizer
import com.asrou.arcallblocker.data.db.dao.WhitelistDao
import com.asrou.arcallblocker.data.db.entity.WhitelistEntry
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
 * Repository for the user's allow list.
 *
 * Same caching strategy as [BlacklistRepository]: O(1) in-memory lookup
 * for the hot path, backed by Room for persistence.
 */
@Singleton
class WhitelistRepository @Inject constructor(
    private val whitelistDao: WhitelistDao,
    private val normalizer: PhoneNumberNormalizer
) {
    private val cache = ConcurrentHashMap.newKeySet<String>()
    private val mutex = Mutex()
    private var cacheLoaded = false

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        com.asrou.arcallblocker.core.util.AppLogger.e("Error in WhitelistRepository scope", throwable)
    }
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO + exceptionHandler)

    init {
        scope.launch {
            try {
                ensureCacheLoaded()
            } catch (e: Throwable) {
                com.asrou.arcallblocker.core.util.AppLogger.e("Failed to preload whitelist cache", e)
            }
        }
    }

    fun observeAll(): Flow<List<WhitelistEntry>> = whitelistDao.observeAll()

    fun search(query: String): Flow<List<WhitelistEntry>> = whitelistDao.search(query)

    fun observeCount(): Flow<Int> = whitelistDao.observeCount()

    /**
     * Hot-path check: is this number whitelisted?
     * Non-blocking O(1) in-memory lookup.
     */
    fun isWhitelistedFast(normalizedNumber: String): Boolean {
        return cache.contains(normalizedNumber)
    }

    /**
     * Hot-path check: is this number whitelisted?
     * O(1), ensures cache loaded.
     */
    suspend fun isWhitelisted(normalizedNumber: String): Boolean {
        ensureCacheLoaded()
        return cache.contains(normalizedNumber)
    }

    suspend fun add(
        rawNumber: String,
        label: String? = null,
        notes: String? = null
    ) {
        val normalized = normalizer.normalize(rawNumber) ?: return
        val entry = WhitelistEntry(
            normalizedNumber = normalized,
            displayNumber = rawNumber,
            label = label,
            notes = notes
        )
        mutex.withLock {
            whitelistDao.insert(entry)
            cache.add(normalized)
        }
    }

    suspend fun update(entry: WhitelistEntry) {
        mutex.withLock {
            whitelistDao.update(entry)
        }
    }

    suspend fun remove(entry: WhitelistEntry) {
        mutex.withLock {
            whitelistDao.delete(entry)
            cache.remove(entry.normalizedNumber)
        }
    }

    suspend fun removeByNumber(normalizedNumber: String) {
        mutex.withLock {
            whitelistDao.deleteByNumber(normalizedNumber)
            cache.remove(normalizedNumber)
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            whitelistDao.deleteAll()
            cache.clear()
        }
    }

    private suspend fun ensureCacheLoaded() {
        if (cacheLoaded) return
        mutex.withLock {
            if (cacheLoaded) return
            val numbers = whitelistDao.getAllNormalizedNumbers()
            cache.addAll(numbers)
            cacheLoaded = true
        }
    }

    suspend fun reloadCache() {
        mutex.withLock {
            cache.clear()
            val numbers = whitelistDao.getAllNormalizedNumbers()
            cache.addAll(numbers)
            cacheLoaded = true
        }
    }
}
