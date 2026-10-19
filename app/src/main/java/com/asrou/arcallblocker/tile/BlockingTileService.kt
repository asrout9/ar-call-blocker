package com.asrou.arcallblocker.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.asrou.arcallblocker.data.datastore.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Quick Settings tile for toggling call blocking on/off.
 *
 * Allows the user to toggle blocking from the notification shade without
 * opening the app. The tile state always reflects the actual blocking state.
 */
@AndroidEntryPoint
class BlockingTileService : TileService() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        tileScope.launch {
            val currentState = userPreferences.blockingEnabled.first()
            userPreferences.setBlockingEnabled(!currentState)
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        try {
            val isEnabled = runBlocking { userPreferences.blockingEnabled.first() }
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (isEnabled) "ON" else "OFF"
            tile.updateTile()
        } catch (e: Exception) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tileScope.cancel()
    }
}
