package za.co.kinplus.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ==========================================================================
// Retrofit DTOs. These mirror the ASP.NET Core API's DTOs (KinPlusApi/Dtos)
// so the JSON contract stays identical on both ends. Gson maps the camelCase
// JSON returned by the API to these properties.
// ==========================================================================

// ----- Users -----
data class UserSyncRequest(
    val displayName: String,
    val phone: String? = null,
    val fcmToken: String? = null,
    val locale: String? = null
)

data class UserPatchRequest(
    val displayName: String? = null,
    val phone: String? = null,
    val locale: String? = null,
    val sharingEnabled: Boolean? = null
)

data class UserDto(
    val uid: String,
    val displayName: String,
    val email: String,
    val phone: String?,
    val photoUrl: String?,
    val locale: String,
    val sharingEnabled: Boolean
)

// ----- Circles -----
data class CreateCircleRequest(val name: String, val description: String? = null)
data class JoinCircleRequest(val joinCode: String)
data class SharingRequest(val sharingEnabled: Boolean)

data class CircleMemberDto(
    val uid: String,
    val displayName: String,
    val role: String,
    val sharingEnabled: Boolean
)

data class CircleDto(
    val id: String,
    val name: String,
    val description: String?,
    val ownerUid: String,
    val joinCode: String,
    val memberCount: Int
)

data class CircleDetailDto(
    val id: String,
    val name: String,
    val description: String?,
    val ownerUid: String,
    val joinCode: String,
    val members: List<CircleMemberDto>
)

// ----- Locations -----
data class LocationUploadRequest(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
    val batteryPct: Int,
    val capturedAt: String,
    val clientActionId: String? = null
)

data class LocationBatchRequest(val items: List<LocationUploadRequest>)

data class MemberLocationDto(
    val uid: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val batteryPct: Int,
    val capturedAt: String
)

// ----- SOS -----
data class SosRequest(
    val lat: Double?,
    val lng: Double?,
    val message: String?,
    val circleIds: List<String>,
    val clientActionId: String? = null
)

data class SosDto(
    val id: String,
    val uid: String,
    val raisedByName: String,
    val status: String,
    val lat: Double?,
    val lng: Double?,
    val message: String?,
    val createdAt: String
)

// ----- Safe Zones -----
data class ZoneRequest(
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val circleId: String? = null,
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true
)

data class ZonePatchRequest(
    val name: String? = null,
    val radiusM: Int? = null,
    val notifyOnEnter: Boolean? = null,
    val notifyOnExit: Boolean? = null
)

data class ZoneEventRequest(val type: String) // "ENTER" or "EXIT"

data class ZoneDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val circleId: String?,
    val notifyOnEnter: Boolean,
    val notifyOnExit: Boolean
)

// ----- Journeys -----
data class JourneyRequest(
    val destinationLabel: String,
    val destLat: Double?,
    val destLng: Double?,
    val etaAt: String,
    val circleId: String?,
    val watcherUids: List<String>
)

data class JourneyDto(
    val id: String,
    val uid: String,
    val travellerName: String,
    val destinationLabel: String,
    val etaAt: String,
    val status: String,
    val createdAt: String
)

// ----- Community Alerts -----
data class UploadUrlRequest(val contentType: String)
data class UploadUrlDto(val uploadUrl: String, val blobUrl: String)

data class AlertRequest(
    val category: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val photoBlobUrl: String? = null
)

data class AlertDto(
    val id: String,
    val category: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val photoBlobUrl: String?,
    val status: String,
    val confirmCount: Int,
    val createdAt: String
)

data class ApiError(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)
