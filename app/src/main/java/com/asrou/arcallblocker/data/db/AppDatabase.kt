package com.asrou.arcallblocker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.asrou.arcallblocker.data.db.dao.BlacklistDao
import com.asrou.arcallblocker.data.db.dao.BlockedCallDao
import com.asrou.arcallblocker.data.db.dao.ScheduleDao
import com.asrou.arcallblocker.data.db.dao.WhitelistDao
import com.asrou.arcallblocker.data.db.entity.BlacklistEntry
import com.asrou.arcallblocker.data.db.entity.BlockedCallEntry
import com.asrou.arcallblocker.data.db.entity.ScheduleEntry
import com.asrou.arcallblocker.data.db.entity.WhitelistEntry

@Database(
    entities = [
        BlacklistEntry::class,
        WhitelistEntry::class,
        BlockedCallEntry::class,
        ScheduleEntry::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blacklistDao(): BlacklistDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE blocked_calls ADD COLUMN action TEXT NOT NULL DEFAULT 'block'")
                } catch (e: Throwable) {
                    // Column may already exist if table was recreated or migrated
                }
            }
        }
    }
}
