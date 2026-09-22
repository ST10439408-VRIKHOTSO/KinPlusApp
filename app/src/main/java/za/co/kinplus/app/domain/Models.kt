package za.co.kinplus.app.domain

// ==========================================================================
// UI-facing domain models. Repositories map remote DTOs and Room entities into
// these, so Composables and ViewModels never depend on transport details.
// ==========================================================================

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val phone: String?,
    val locale: String,
    val sharingEnabled: Boolean
)

data class Circle(
    val id: String,
    val name: String,
    val description: String?,
    val ownerUid: String,
    val joinCode: String,
    val memberCount: Int
)

data class CircleMember(
    val uid: String,
    val displayName: String,
    val role: String,
    val sharingEnabled: Boolean
) {
    val isOwner: Boolean get() = role.equals("OWNER", ignoreCase = true)
}

data class CircleDetail(
    val id: String,
    val name: String,
    val description: String?,
    val ownerUid: String,
    val joinCode: String,
    val members: List<CircleMember>
)

data class MemberLocation(
    val uid: String,
    val displayName: String,
    val lat: Double,
    val lng: Double,
    val batteryPct: Int,
    val capturedAt: String
)

data class SafeZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val circleId: String?,
    val notifyOnEnter: Boolean,
    val notifyOnExit: Boolean
)

data class Journey(
    val id: String,
    val uid: String,
    val travellerName: String,
    val destinationLabel: String,
    val etaAt: String,
    val status: String
)

data class CommunityAlert(
    val id: String,
    val category: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val photoUrl: String?,
    val status: String,
    val confirmCount: Int
)

data class SosEvent(
    val id: String,
    val raisedByName: String,
    val status: String,
    val message: String?
)

/** A locally-logged past SOS, shown on Safety > Recent Activity > Incident History. */
data class SosHistoryItem(
    val id: String,
    val status: String,
    val message: String?,
    val raisedAt: Long
)

/**
 * Emergency Information (Profile > Emergency Information), stored on-device only.
 * Split into the five categories shown on that screen; each exposes its own
 * [percentComplete] (filled fields / total fields) so the list and the Safety
 * Setup card can show real progress.
 */
data class EmergencyInfo(
    val aboutMe: AboutMe = AboutMe(),
    val medical: Medical = Medical(),
    val vehicle: PrimaryVehicle = PrimaryVehicle(),
    val addresses: Addresses = Addresses(),
    val homeSecurity: HomeSecurity = HomeSecurity()
) {
    data class AboutMe(
        val idNumber: String = "",
        val dateOfBirth: String = "",
        val bloodType: String = "",
        val notes: String = ""
    ) { val percentComplete: Int get() = fieldCompletion(idNumber, dateOfBirth, bloodType, notes) }

    data class Medical(
        val conditions: String = "",
        val allergies: String = "",
        val medications: String = "",
        val doctorContact: String = ""
    ) { val percentComplete: Int get() = fieldCompletion(conditions, allergies, medications, doctorContact) }

    data class PrimaryVehicle(
        val make: String = "",
        val model: String = "",
        val color: String = "",
        val plate: String = ""
    ) { val percentComplete: Int get() = fieldCompletion(make, model, color, plate) }

    data class Addresses(
        val home: String = "",
        val work: String = ""
    ) { val percentComplete: Int get() = fieldCompletion(home, work) }

    data class HomeSecurity(
        val alarmCompany: String = "",
        val gateCode: String = "",
        val alarmCode: String = "",
        val notes: String = ""
    ) { val percentComplete: Int get() = fieldCompletion(alarmCompany, gateCode, alarmCode, notes) }

    /** Average completion across all five categories, used by the Safety Setup card. */
    val overallPercentComplete: Int
        get() = listOf(
            aboutMe.percentComplete, medical.percentComplete, vehicle.percentComplete,
            addresses.percentComplete, homeSecurity.percentComplete
        ).average().toInt()
}

private fun fieldCompletion(vararg fields: String): Int {
    if (fields.isEmpty()) return 0
    return (fields.count { it.isNotBlank() } * 100) / fields.size
}
