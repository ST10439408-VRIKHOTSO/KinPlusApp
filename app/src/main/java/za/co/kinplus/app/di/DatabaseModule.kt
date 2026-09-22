package za.co.kinplus.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import za.co.kinplus.app.data.local.*
import javax.inject.Singleton

/** Provides the Room database and its DAOs for the offline cache and sync queue. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KinPlusDatabase =
        Room.databaseBuilder(context, KinPlusDatabase::class.java, KinPlusDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideCircleDao(db: KinPlusDatabase): CircleDao = db.circleDao()
    @Provides fun provideLocationDao(db: KinPlusDatabase): LocationDao = db.locationDao()
    @Provides fun provideZoneDao(db: KinPlusDatabase): ZoneDao = db.zoneDao()
    @Provides fun providePendingActionDao(db: KinPlusDatabase): PendingActionDao = db.pendingActionDao()
    @Provides fun provideEmergencyInfoDao(db: KinPlusDatabase): EmergencyInfoDao = db.emergencyInfoDao()
    @Provides fun provideSosHistoryDao(db: KinPlusDatabase): SosHistoryDao = db.sosHistoryDao()
    @Provides fun provideLocalPhotoDao(db: KinPlusDatabase): LocalPhotoDao = db.localPhotoDao()
    @Provides fun provideEmergencyContactDao(db: KinPlusDatabase): EmergencyContactDao = db.emergencyContactDao()
}
