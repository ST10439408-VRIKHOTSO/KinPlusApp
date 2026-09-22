package za.co.kinplus.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================================================
// Room entities. These provide the offline cache (FR-28) so the app still
// shows circles, members, zones and last-known locations without a network
// connection, and the queue that drives offline sync (FR-29).
// ==========================================================================

@Entity(tableName = "circles")
data class CircleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val ownerUid: String,
    val joinCode: String,
    val memberCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "member_locations")
data class MemberLocationEntity(
    @PrimaryKey val uid: String,
    val circleId: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val batteryPct: Int,
    val capturedAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val circleId: String?,
    val notifyOnEnter: Boolean,
    val notifyOnExit: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * A queued action performed while offline. WorkManager replays these against
 * the API when connectivity returns. The [clientActionId] is sent to the server
 * so a replay is de-duplicated (idempotency, FR-29).
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val clientActionId: String,
    val type: String,          // e.g. "LOCATION", "SOS", "ZONE_EVENT", "ALERT"
    val payloadJson: String,    // serialized request body
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)

/**
 * Emergency Information (Profile > Emergency Information). One row per signed-in
 * user, stored locally only — never sent to the API. Grouped into the five
 * categories shown on the Emergency Information screen; each field is optional,
 * and "percent complete" per category is simply filled / total fields.
 */
@Entity(tableName = "emergency_info")
data class EmergencyInfoEntity(
    @PrimaryKey val uid: String,

    // About Me
    val idNumber: String? = null,
    val dateOfBirth: String? = null,
    val bloodType: String? = null,
    val aboutMeNotes: String? = null,

    // Medical
    val medicalConditions: String? = null,
    val medicalAllergies: String? = null,
    val medicalMedications: String? = null,
    val medicalDoctorContact: String? = null,

    // Primary Vehicle
    val vehicleMake: String? = null,
    val vehicleModel: String? = null,
    val vehicleColor: String? = null,
    val vehiclePlate: String? = null,

    // Addresses
    val homeAddress: String? = null,
    val workAddress: String? = null,

    // Home Security
    val alarmCompany: String? = null,
    val gateCode: String? = null,
    val alarmCode: String? = null,
    val homeSecurityNotes: String? = null,

    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A locally-logged record of an SOS the user raised, so Safety > Recent Activity
 * can show real Incident History without depending on the API to list it back.
 */
@Entity(tableName = "sos_history")
data class SosHistoryEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val status: String,     // "SENT", "QUEUED", "FAILED"
    val message: String?,
    val lat: Double?,
    val lng: Double?,
    val raisedAt: Long = System.currentTimeMillis()
)

/**
 * An emergency contact (Safety > Emergency Contacts) — a named person with a
 * phone number the user can reach with one tap. No matching API endpoint
 * exists, so this is on-device only, same as Emergency Information.
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val relationship: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A picked profile photo, stored purely on-device (no upload endpoint exists
 * for either user or circle photos). [id] is "user:<uid>" for the signed-in
 * user's own photo, or "circle:<circleId>" for a circle's group photo; [path]
 * is the absolute path of the copy this app keeps in its own files directory,
 * so it survives even if the original picked image is later deleted.
 */
@Entity(tableName = "local_photos")
data class LocalPhotoEntity(
    @PrimaryKey val id: String,
    val path: String
)
