package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.local.ZoneDao
import za.co.kinplus.app.data.local.ZoneEntity
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.*
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.data.sync.QueuedZoneEvent
import za.co.kinplus.app.domain.SafeZone
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safe Zones (FR-20 to FR-22). Zones are cached in Room so the on-device
 * geofencing still works offline; ENTER/EXIT crossings detected while offline
 * are queued and reported when connectivity returns.
 */
@Singleton
class ZoneRepository @Inject constructor(
    private val api: KinPlusApi,
    private val zoneDao: ZoneDao,
    private val offlineQueue: OfflineQueue,
    private val connectivity: ConnectivityObserver
) {
    val zones: Flow<List<SafeZone>> = zoneDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun refreshZones(): Resource<List<SafeZone>> = try {
        val remote = api.getZones()
        zoneDao.replaceAll(remote.map { it.toEntity() })
        Resource.Success(remote.map { it.toDomain() })
    } catch (e: Exception) {
        val cached = zoneDao.getAll().map { it.toDomain() }
        if (cached.isNotEmpty()) Resource.Success(cached, fromCache = true)
        else Resource.Error("Could not load Safe Zones.", e)
    }

    suspend fun createZone(
        name: String, lat: Double, lng: Double, radiusM: Int,
        circleId: String?, notifyEnter: Boolean, notifyExit: Boolean
    ): Resource<SafeZone> = try {
        val dto = api.createZone(ZoneRequest(name, lat, lng, radiusM, circleId, notifyEnter, notifyExit))
        zoneDao.upsertAll(listOf(dto.toEntity()))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Create zone failed", e)
        Resource.Error("Could not save the Safe Zone. You may be offline.", e)
    }

    suspend fun deleteZone(id: String): Resource<Unit> = try {
        api.deleteZone(id)
        zoneDao.deleteById(id)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Could not delete the Safe Zone.", e)
    }

    /** Called by the geofence receiver on ENTER/EXIT; queues when offline. */
    suspend fun reportEvent(zoneId: String, type: String): Resource<Unit> {
        if (!connectivity.isOnlineNow()) {
            offlineQueue.enqueue(ActionType.ZONE_EVENT, QueuedZoneEvent(zoneId, type))
            return Resource.Success(Unit, fromCache = true)
        }
        return try {
            api.reportZoneEvent(zoneId, ZoneEventRequest(type))
            Resource.Success(Unit)
        } catch (e: Exception) {
            offlineQueue.enqueue(ActionType.ZONE_EVENT, QueuedZoneEvent(zoneId, type))
            Resource.Success(Unit, fromCache = true)
        }
    }

    suspend fun cachedZones(): List<SafeZone> = zoneDao.getAll().map { it.toDomain() }

    private companion object {
        const val TAG = "ZoneRepository"
    }
}

private fun ZoneDto.toEntity() = ZoneEntity(id, name, lat, lng, radiusM, circleId, notifyOnEnter, notifyOnExit)
private fun ZoneDto.toDomain() = SafeZone(id, name, lat, lng, radiusM, circleId, notifyOnEnter, notifyOnExit)
private fun ZoneEntity.toDomain() = SafeZone(id, name, lat, lng, radiusM, circleId, notifyOnEnter, notifyOnExit)
