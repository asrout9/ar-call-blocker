package com.asrou.arcallblocker.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.asrou.arcallblocker.core.util.AppLogger
import com.asrou.arcallblocker.core.util.PhoneNumberNormalizer
import com.asrou.arcallblocker.data.cache.ContactCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for reading contacts, normalizing phone numbers,
 * and maintaining the in-memory contact cache.
 *
 * Uses the Contacts Provider to read all phone numbers, normalizes them
 * via [PhoneNumberNormalizer], and stores them in [ContactCache] for
 * O(1) lookup during call screening.
 */
@Singleton
class ContactRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactCache: ContactCache,
    private val contactObserver: ContactObserver,
    private val normalizer: PhoneNumberNormalizer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Initialize the contact system: load contacts and start observing changes.
     * Call this after contacts permission is granted.
     */
    fun initialize() {
        if (!hasContactsPermission()) {
            AppLogger.w("ContactRepository: contacts permission not granted")
            contactCache.markError()
            return
        }

        // Start observing for changes
        contactObserver.startObserving {
            scope.launch { syncContacts() }
        }

        // Initial sync
        scope.launch { syncContacts() }
    }

    /**
     * Synchronize the contact cache with the Contacts Provider.
     * Reads all phone numbers, normalizes them, and atomically replaces the cache.
     */
    suspend fun syncContacts() {
        if (!hasContactsPermission()) {
            AppLogger.w("ContactRepository: cannot sync - no permission")
            contactCache.markError()
            return
        }

        contactCache.markSyncing()

        try {
            val numbers = readAllContactNumbers()
            contactCache.rebuild(numbers)
            AppLogger.d("Contact sync complete: ${numbers.size} unique normalized numbers")
        } catch (e: Exception) {
            AppLogger.e("Contact sync failed", e)
            contactCache.markError()
        }
    }

    /**
     * Read all phone numbers from the Contacts Provider and normalize them.
     *
     * Queries ContactsContract.CommonDataKinds.Phone which efficiently provides
     * all phone numbers across all contacts in a single query.
     */
    private fun readAllContactNumbers(): Set<String> {
        val normalizedNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                if (numberIndex < 0) return@use

                while (cursor.moveToNext()) {
                    val rawNumber = cursor.getString(numberIndex)
                    if (!rawNumber.isNullOrBlank()) {
                        // Add all normalized forms of this number to the cache
                        val forms = normalizer.normalizeToSet(rawNumber)
                        normalizedNumbers.addAll(forms)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to read contacts", e)
            throw e
        }

        return normalizedNumbers
    }

    /** Check if READ_CONTACTS permission is granted */
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Stop observing and clear cache */
    fun shutdown() {
        contactObserver.stopObserving()
        contactCache.clear()
    }
}
