# ProGuard / R8 rules for AR Call Blocker

# Keep CallScreeningService (bound by Android Telecom framework)
-keep class com.asrou.arcallblocker.telecom.ARCallScreeningService { *; }

# Keep TileService (bound by Android Quick Settings)
-keep class com.asrou.arcallblocker.tile.BlockingTileService { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
