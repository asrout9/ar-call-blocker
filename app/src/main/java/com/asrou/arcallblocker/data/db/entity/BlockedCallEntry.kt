package com.asrou.arcallblocker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A record of a blocked incoming call.
 *
 * Indexed by timestamp for efficient date-range queries and cleanup.
 */
@Entity(
    tableName = "blocked_calls",
    indices = [Index(value = ["timestamp"])]
)
data class BlockedCallEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The normalized phone number, or null if private/hidden */
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String? = null,

    /** The display number (as received from the carrier) */
    @ColumnInfo(name = "display_number")
    val displayNumber: String? = null,

    /** Timestamp when the call was blocked (epoch millis) */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /** The reason the call was blocked (maps to BlockReason.displayKey) */
    @ColumnInfo(name = "reason")
    val reason: String,

    /** The action taken on the call: "block", "silence", or "reject" */
    @ColumnInfo(name = "action", defaultValue = "'block'")
    val action: String = "block"
)
