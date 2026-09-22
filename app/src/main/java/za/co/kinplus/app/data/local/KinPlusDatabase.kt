package za.co.kinplus.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database backing the Kin+ offline cache and sync queue (FR-28,
 * FR-29). Bumped to version 1 for the Part 2/PoE build.
 */
@Database(
    entities = [
        CircleEntity::class,
        MemberLocationEntity::class,
        ZoneEntity::class,
        PendingActionEntity::class,
        EmergencyInfoEntity::class,
        SosHistoryEntity::class,
        LocalPhotoEntity::class,
        EmergencyContactEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KinPlusDatabase : RoomDatabase() {
    abstract fun circleDao(): CircleDao
    abstract fun locationDao(): LocationDao
    abstract fun zoneDao(): ZoneDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun emergencyInfoDao(): EmergencyInfoDao
    abstract fun sosHistoryDao(): SosHistoryDao
    abstract fun localPhotoDao(): LocalPhotoDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val NAME = "kinplus.db"
    }
}
