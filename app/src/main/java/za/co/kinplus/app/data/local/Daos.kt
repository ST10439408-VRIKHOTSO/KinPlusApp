package za.co.kinplus.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CircleDao {
    @Query("SELECT * FROM circles ORDER BY name")
    fun observeAll(): Flow<List<CircleEntity>>

    @Query("SELECT * FROM circles ORDER BY name")
    suspend fun getAll(): List<CircleEntity>

    @Upsert
    suspend fun upsertAll(circles: List<CircleEntity>)

    @Query("DELETE FROM circles")
    suspend fun clear()

    @Query("DELETE FROM circles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun replaceAll(circles: List<CircleEntity>) {
        clear()
        upsertAll(circles)
    }
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM member_locations WHERE circleId = :circleId")
    fun observeForCircle(circleId: String): Flow<List<MemberLocationEntity>>

    @Upsert
    suspend fun upsertAll(items: List<MemberLocationEntity>)

    @Query("DELETE FROM member_locations WHERE circleId = :circleId")
    suspend fun clearCircle(circleId: String)

    @Transaction
    suspend fun replaceForCircle(circleId: String, items: List<MemberLocationEntity>) {
        clearCircle(circleId)
        upsertAll(items)
    }
}

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones ORDER BY name")
    fun observeAll(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones")
    suspend fun getAll(): List<ZoneEntity>

    @Upsert
    suspend fun upsertAll(zones: List<ZoneEntity>)

    @Query("DELETE FROM zones WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM zones")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(zones: List<ZoneEntity>) {
        clear()
        upsertAll(zones)
    }
}

@Dao
interface EmergencyInfoDao {
    @Query("SELECT * FROM emergency_info WHERE uid = :uid")
    fun observe(uid: String): Flow<EmergencyInfoEntity?>

    @Query("SELECT * FROM emergency_info WHERE uid = :uid")
    suspend fun getOnce(uid: String): EmergencyInfoEntity?

    @Upsert
    suspend fun upsert(info: EmergencyInfoEntity)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE uid = :uid ORDER BY createdAt")
    fun observeForUser(uid: String): Flow<List<EmergencyContactEntity>>

    @Upsert
    suspend fun upsert(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SosHistoryDao {
    @Query("SELECT * FROM sos_history WHERE uid = :uid ORDER BY raisedAt DESC")
    fun observeForUser(uid: String): Flow<List<SosHistoryEntity>>

    @Insert
    suspend fun insert(entry: SosHistoryEntity)
}

@Dao
interface LocalPhotoDao {
    @Query("SELECT * FROM local_photos WHERE id = :id")
    fun observe(id: String): Flow<LocalPhotoEntity?>

    @Upsert
    suspend fun upsert(entity: LocalPhotoEntity)
}

@Dao
interface PendingActionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(action: PendingActionEntity)

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>

    @Query("UPDATE pending_actions SET attempts = attempts + 1 WHERE clientActionId = :id")
    suspend fun incrementAttempts(id: String)

    @Query("DELETE FROM pending_actions WHERE clientActionId = :id")
    suspend fun remove(id: String)
}
