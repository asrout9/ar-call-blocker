package com.asrou.arcallblocker.data.contacts

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.asrou.arcallblocker.core.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentObserver that watches for changes in the Contacts Provider.
 *
 * When contacts change (add, delete, modify), it debounces rapid changes
 * and triggers a cache rebuild via the provided callback.
 */
@Singleton
class ContactObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var observer: InnerObserver? = null
    private var onContactsChanged: (() -> Unit)? = null

    /**
     * Start observing contact changes.
     *
     * @param onChange Callback invoked when contacts change (debounced).
     */
    fun startObserving(onChange: () -> Unit) {
        stopObserving()
        onContactsChanged = onChange

        val innerObserver = InnerObserver(Handler(Looper.getMainLooper()), onChange)
        observer = innerObserver

        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true, // notifyForDescendants
                innerObserver
            )
            AppLogger.d("ContactObserver registered")
        } catch (e: Exception) {
            AppLogger.e("Failed to register ContactObserver", e)
        }
    }

    /** Stop observing contact changes */
    fun stopObserving() {
        observer?.let { obs ->
            try {
                context.contentResolver.unregisterContentObserver(obs)
                AppLogger.d("ContactObserver unregistered")
            } catch (e: Exception) {
                AppLogger.e("Failed to unregister ContactObserver", e)
            }
        }
        observer = null
        onContactsChanged = null
    }

    /**
     * Inner observer that debounces rapid changes.
     * Contact operations can trigger multiple onChange calls in quick succession
     * (e.g., syncing). We debounce to avoid unnecessary cache rebuilds.
     */
    private class InnerObserver(
        private val handler: Handler,
        private val onChange: () -> Unit
    ) : ContentObserver(handler) {

        private val debounceRunnable = Runnable {
            AppLogger.d("ContactObserver: debounced onChange triggered")
            onChange()
        }

        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Remove any pending debounce and schedule a new one
            handler.removeCallbacks(debounceRunnable)
            handler.postDelayed(debounceRunnable, DEBOUNCE_MS)
        }

        companion object {
            /** Wait 500ms after the last change before triggering a rebuild */
            private const val DEBOUNCE_MS = 500L
        }
    }
}
