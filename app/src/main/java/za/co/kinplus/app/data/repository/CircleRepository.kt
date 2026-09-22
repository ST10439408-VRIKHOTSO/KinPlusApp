package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.local.CircleDao
import za.co.kinplus.app.data.local.CircleEntity
import za.co.kinplus.app.data.local.LocationDao
import za.co.kinplus.app.data.local.MemberLocationEntity
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.*
import za.co.kinplus.app.domain.*
import za.co.kinplus.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Circles, membership and member locations (FR-09 to FR-16).
 *
 * Reads follow an offline-first pattern: the network result is written to Room
 * and the UI observes Room, so cached circles remain visible without a
 * connection (FR-28). Writes go straight to the API because they need a server
 * round-trip (creating a circle, joining, changing sharing).
 */
@Singleton
class CircleRepository @Inject constructor(
    private val api: KinPlusApi,
    private val circleDao: CircleDao,
    private val locationDao: LocationDao
) {
    /** Live list of cached circles for the Circles screen. */
    val circles: Flow<List<Circle>> = circleDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Refreshes circles from the API into the cache; returns a Resource for the UI. */
    suspend fun refreshCircles(): Resource<List<Circle>> = try {
        val remote = api.getCircles()
        circleDao.replaceAll(remote.map { it.toEntity() })
        Log.d(TAG, "Refreshed ${remote.size} circle(s) from API")
        Resource.Success(remote.map { it.toDomain() })
    } catch (e: Exception) {
        Log.w(TAG, "Circle refresh failed, serving cache", e)
        val cached = circleDao.getAll().map { it.toDomain() }
        if (cached.isNotEmpty()) Resource.Success(cached, fromCache = true)
        else Resource.Error("Could not load circles.", e)
    }

    suspend fun createCircle(name: String, description: String?): Resource<Circle> = try {
        val dto = api.createCircle(CreateCircleRequest(name, description))
        circleDao.upsertAll(listOf(dto.toEntity()))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Create circle failed", e)
        Resource.Error("Could not create the circle. You may be offline.", e)
    }

    suspend fun joinCircle(code: String): Resource<Circle> = try {
        val dto = api.joinCircle(JoinCircleRequest(code.trim().uppercase()))
        circleDao.upsertAll(listOf(dto.toEntity()))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Join circle failed", e)
        Resource.Error("Could not join. Check the code and your connection.", e)
    }

    suspend fun getCircleDetail(id: String): Resource<CircleDetail> = try {
        val dto = api.getCircle(id)
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Resource.Error("Could not load circle details.", e)
    }

    suspend fun setSharing(circleId: String, enabled: Boolean): Resource<Unit> = try {
        api.setCircleSharing(circleId, SharingRequest(enabled))
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Could not update sharing.", e)
    }

    /**
     * Leaves a circle (or, called by an owner, removes their own membership —
     * there is no separate "delete circle" endpoint). The local cache is
     * cleared immediately on success so the circle disappears from the list
     * without waiting on a follow-up [refreshCircles] round-trip.
     */
    suspend fun leaveOrRemove(circleId: String, memberUid: String): Resource<Unit> = try {
        api.removeMember(circleId, memberUid)
        circleDao.deleteById(circleId)
        locationDao.clearCircle(circleId)
        Log.i(TAG, "Left/removed from circle $circleId")
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Leave/remove failed for circle $circleId", e)
        Resource.Error("Could not leave this circle. Check your connection and try again.", e)
    }

    /** Live member locations for the map, cached per circle (FR-14, FR-28). */
    fun observeCircleLocations(circleId: String): Flow<List<MemberLocation>> =
        locationDao.observeForCircle(circleId).map { list -> list.map { it.toDomain() } }

    suspend fun refreshCircleLocations(circleId: String): Resource<List<MemberLocation>> = try {
        val remote = api.getCircleLocations(circleId)
        locationDao.replaceForCircle(circleId, remote.map { it.toEntity(circleId) })
        Resource.Success(remote.map { it.toDomain() })
    } catch (e: Exception) {
        Log.w(TAG, "Location refresh failed, serving cache", e)
        Resource.Error("Showing last known locations.", e)
    }

    private companion object {
        const val TAG = "CircleRepository"
    }
}

// ----- mappers -----
private fun CircleDto.toEntity() = CircleEntity(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleDto.toDomain() = Circle(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleEntity.toDomain() = Circle(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleDetailDto.toDomain() = CircleDetail(
    id, name, description, ownerUid, joinCode,
    members.map { CircleMember(it.uid, it.displayName, it.role, it.sharingEnabled) }
)
private fun MemberLocationDto.toEntity(circleId: String) =
    MemberLocationEntity(uid, circleId, displayName, lat, lng, batteryPct, capturedAt)
private fun MemberLocationDto.toDomain() = MemberLocation(uid, displayName, lat, lng, batteryPct, capturedAt)
private fun MemberLocationEntity.toDomain() = MemberLocation(uid, displayName, lat, lng, batteryPct, capturedAt)
