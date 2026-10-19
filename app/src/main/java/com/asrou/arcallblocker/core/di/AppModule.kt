package com.asrou.arcallblocker.core.di

import android.content.Context
import com.asrou.arcallblocker.core.util.PhoneNumberNormalizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePhoneNumberNormalizer(
        @ApplicationContext context: Context
    ): PhoneNumberNormalizer {
        return PhoneNumberNormalizer(context)
    }
}
