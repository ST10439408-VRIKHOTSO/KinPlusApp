package za.co.kinplus.app.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import za.co.kinplus.app.R
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.CircleDao
import za.co.kinplus.app.data.local.CircleEntity
import za.co.kinplus.app.data.local.LocationDao
import za.co.kinplus.app.data.local.MemberLocationEntity
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.*
import za.co.kinplus.app.domain.*
import za.co.kinplus.app.util.LocaleManager
import za.co.kinplus.app.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 *
 * Error messages are shown to the user, so they come from string resources in
 * the in-app language (FR-31) rather than being written in English here.
 */
@Singleton
class CircleRepository @Inject constructor(
    private val api: KinPlusApi,
    private val circleDao: CircleDao,
    private val locationDao: LocationDao,
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) {
    /** User facing text in the language chosen in Settings. */
    private fun text(id: Int): String = LocaleManager.localized(context).getString(id)

    /**
     * The API stores each member's name inside the circle when they create or
     * join it. Accounts that had no name at that moment come back blank, which
     * showed as a "?" avatar and an empty row. This fills the gap: the signed
     * in user gets their own Firebase name (or email prefix), anyone else gets
     * a neutral "Kin+ member" label.
     */
    private fun nameFor(uid: String, name: String?): String {
        if (!name.isNullOrBlank()) return name
        if (uid == authManager.currentUid) {
            myName()?.let { return it }
        }
        return text(R.string.member_name_fallback)
    }

    /** The signed in user's best available name, or null when Firebase has neither name nor email. */
    private fun myName(): String? =
        authManager.currentDisplayName.trim().ifBlank { authManager.currentEmail.substringBefore('@').trim() }.ifBlank { null }

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
        else Resource.Error(text(R.string.error_load_circles), e)
    }

    suspend fun createCircle(name: String, description: String?): Resource<Circle> = try {
        val dto = api.createCircle(CreateCircleRequest(name, description))
        circleDao.upsertAll(listOf(dto.toEntity()))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Create circle failed", e)
        Resource.Error(text(R.string.error_create_circle), e)
    }

    suspend fun joinCircle(code: String): Resource<Circle> = try {
        val dto = api.joinCircle(JoinCircleRequest(code.trim().uppercase()))
        circleDao.upsertAll(listOf(dto.toEntity()))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Join circle failed", e)
        Resource.Error(text(R.string.error_join_circle), e)
    }

    suspend fun getCircleDetail(id: String): Resource<CircleDetail> = try {
        val dto = api.getCircle(id)
        repairMyStoredName(dto.members.firstOrNull { it.uid == authManager.currentUid }?.displayName)
        Resource.Success(dto.toDomain(::nameFor))
    } catch (e: Exception) {
        Resource.Error(text(R.string.error_load_circle), e)
    }

    suspend fun setSharing(circleId: String, enabled: Boolean): Resource<Unit> = try {
        api.setCircleSharing(circleId, SharingRequest(enabled))
        Log.i(TAG, "Location sharing for circle $circleId set to $enabled")
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Could not change sharing for circle $circleId", e)
        Resource.Error(text(R.string.error_update_sharing), e)
    }

    /**
     * Whether the signed in user shares their location with each of [circleIds]
     * (the per circle switches in Settings, FR-15). The circle list does not carry
     * this, so each circle's member list is read in parallel. A circle that cannot
     * be read is left out, and the screen then shows sharing as on, the default.
     */
    suspend fun mySharingStates(circleIds: List<String>): Map<String, Boolean> = coroutineScope {
        val me = authManager.currentUid
        circleIds.map { id ->
            async {
                runCatching { api.getCircle(id).members.firstOrNull { it.uid == me }?.sharingEnabled }
                    .onFailure { Log.w(TAG, "Could not read sharing for circle $id", it) }
                    .getOrNull()
                    ?.let { id to it }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    /**
     * Leaves a circle (or, for the owner, removes another member). Retrofit does
     * not throw for HTTP errors when the call returns Response, so the status is
     * checked before anything is removed locally. Otherwise a refused request
     * would delete the circle from the cache and the next refresh would bring
     * it straight back.
     */
    suspend fun leaveOrRemove(circleId: String, memberUid: String): Resource<Unit> {
        return try {
            val res = api.removeMember(circleId, memberUid)
            if (!res.isSuccessful) {
                Log.w(TAG, "Leave/remove refused for circle $circleId: HTTP ${res.code()}")
                val message = if (res.code() == 400) text(R.string.error_owner_cannot_leave)
                else text(R.string.error_leave_circle)
                return Resource.Error(message)
            }
            circleDao.deleteById(circleId)
            locationDao.clearCircle(circleId)
            Log.i(TAG, "Left/removed from circle $circleId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Leave/remove failed for circle $circleId", e)
            Resource.Error(text(R.string.error_leave_circle), e)
        }
    }

    /** Deletes a circle the signed in user owns, for every member. */
    suspend fun deleteCircle(circleId: String): Resource<Unit> {
        return try {
            val res = api.deleteCircle(circleId)
            if (!res.isSuccessful) {
                Log.w(TAG, "Delete refused for circle $circleId: HTTP ${res.code()}")
                return Resource.Error(text(R.string.error_delete_circle))
            }
            circleDao.deleteById(circleId)
            locationDao.clearCircle(circleId)
            Log.i(TAG, "Deleted circle $circleId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed for circle $circleId", e)
            Resource.Error(text(R.string.error_delete_circle), e)
        }
    }

    /**
     * If the server holds a blank name for the signed in user, send their real
     * name once so the profile (and, with the API fix, every circle they are
     * in) is corrected for the other members too. Failures are only logged.
     */
    private suspend fun repairMyStoredName(storedName: String?) {
        if (!storedName.isNullOrBlank() || nameRepairSent) return
        val name = myName() ?: return
        nameRepairSent = true
        try {
            api.patchMe(UserPatchRequest(displayName = name))
            Log.i(TAG, "Sent missing display name to the API")
        } catch (e: Exception) {
            Log.w(TAG, "Could not repair display name", e)
        }
    }

    @Volatile private var nameRepairSent = false

    /** Live member locations for the map, cached per circle (FR-14, FR-28). */
    fun observeCircleLocations(circleId: String): Flow<List<MemberLocation>> =
        locationDao.observeForCircle(circleId).map { list -> list.map { it.toDomain(::nameFor) } }

    suspend fun refreshCircleLocations(circleId: String): Resource<List<MemberLocation>> = try {
        val remote = api.getCircleLocations(circleId)
        locationDao.replaceForCircle(circleId, remote.map { it.toEntity(circleId) })
        Resource.Success(remote.map { it.toDomain(::nameFor) })
    } catch (e: Exception) {
        Log.w(TAG, "Location refresh failed, serving cache", e)
        Resource.Error(text(R.string.map_status_offline), e)
    }

    private companion object {
        const val TAG = "CircleRepository"
    }
}

// ----- mappers -----
private fun CircleDto.toEntity() = CircleEntity(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleDto.toDomain() = Circle(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleEntity.toDomain() = Circle(id, name, description, ownerUid, joinCode, memberCount)
private fun CircleDetailDto.toDomain(nameFor: (String, String?) -> String) = CircleDetail(
    id, name, description, ownerUid, joinCode,
    members.map { CircleMember(it.uid, nameFor(it.uid, it.displayName), it.role, it.sharingEnabled) }
)
private fun MemberLocationDto.toEntity(circleId: String) =
    MemberLocationEntity(uid, circleId, displayName, lat, lng, batteryPct, capturedAt)
private fun MemberLocationDto.toDomain(nameFor: (String, String?) -> String) =
    MemberLocation(uid, nameFor(uid, displayName), lat, lng, batteryPct, capturedAt)
private fun MemberLocationEntity.toDomain(nameFor: (String, String?) -> String) =
    MemberLocation(uid, nameFor(uid, displayName), lat, lng, batteryPct, capturedAt)
