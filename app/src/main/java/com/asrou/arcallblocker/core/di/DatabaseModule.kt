package com.asrou.arcallblocker.core.di

import android.content.Context
import androidx.room.Room
import com.asrou.arcallblocker.data.db.AppDatabase
import com.asrou.arcallblocker.data.db.dao.BlacklistDao
import com.asrou.arcallblocker.data.db.dao.BlockedCallDao
import com.asrou.arcallblocker.data.db.dao.ScheduleDao
import com.asrou.arcallblocker.data.db.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ar_call_blocker.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideBlacklistDao(database: AppDatabase): BlacklistDao = database.blacklistDao()

    @Provides
    fun provideWhitelistDao(database: AppDatabase): WhitelistDao = database.whitelistDao()

    @Provides
    fun provideBlockedCallDao(database: AppDatabase): BlockedCallDao = database.blockedCallDao()

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao = database.scheduleDao()
}
