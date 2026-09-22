package za.co.kinplus.app.data.repository

import android.util.Log
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.UserPatchRequest
import za.co.kinplus.app.data.remote.dto.UserSyncRequest
import za.co.kinplus.app.domain.UserProfile
import za.co.kinplus.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User profile (FR-07, FR-08). syncProfile is called immediately after a
 * successful Firebase sign-in to create or update the Kin+ user document and
 * register the device's FCM token for push delivery.
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: KinPlusApi
) {
    suspend fun syncProfile(displayName: String, phone: String?, fcmToken: String?, locale: String?): Resource<UserProfile> = try {
        val dto = api.syncUser(UserSyncRequest(displayName, phone, fcmToken, locale))
        Log.i(TAG, "Profile synced for ${dto.uid}")
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Profile sync failed", e)
        Resource.Error("Could not sync your profile.", e)
    }

    suspend fun getProfile(): Resource<UserProfile> = try {
        Resource.Success(api.getMe().toDomain())
    } catch (e: Exception) {
        Resource.Error("Could not load your profile.", e)
    }

    suspend fun updateProfile(
        displayName: String? = null, phone: String? = null,
        locale: String? = null, sharingEnabled: Boolean? = null
    ): Resource<UserProfile> = try {
        val dto = api.patchMe(UserPatchRequest(displayName, phone, locale, sharingEnabled))
        Resource.Success(dto.toDomain())
    } catch (e: Exception) {
        Resource.Error("Could not update your profile.", e)
    }

    suspend fun deleteAccount(): Resource<Unit> = try {
        api.deleteMe()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error("Could not delete your account.", e)
    }

    private companion object {
        const val TAG = "UserRepository"
    }
}

private fun za.co.kinplus.app.data.remote.dto.UserDto.toDomain() =
    UserProfile(uid, displayName, email, phone, locale, sharingEnabled)
