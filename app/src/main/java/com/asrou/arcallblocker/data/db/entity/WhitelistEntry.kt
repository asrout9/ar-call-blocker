package com.asrou.arcallblocker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A phone number explicitly allowed by the user, even if not in Contacts.
 */
@Entity(
    tableName = "whitelist",
    indices = [Index(value = ["normalized_number"], unique = true)]
)
data class WhitelistEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The normalized (E.164) phone number */
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String,

    /** The original number as entered by the user (for display) */
    @ColumnInfo(name = "display_number")
    val displayNumber: String,

    /** Optional user-provided label */
    @ColumnInfo(name = "label")
    val label: String? = null,

    /** Timestamp when this entry was added (epoch millis) */
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis(),

    /** Optional user notes */
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
