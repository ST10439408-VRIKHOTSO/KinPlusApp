package za.co.kinplus.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.EmergencyInfoDao
import za.co.kinplus.app.data.local.EmergencyInfoEntity
import za.co.kinplus.app.domain.EmergencyInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emergency Information (Profile > Emergency Information). This is sensitive,
 * safety-critical personal data (medical conditions, home security codes, ID
 * number) with no matching API endpoint, so it is kept on-device only in Room,
 * never synced or sent over the network.
 */
@Singleton
class EmergencyInfoRepository @Inject constructor(
    private val dao: EmergencyInfoDao,
    private val authManager: AuthManager
) {
    fun observe(): Flow<EmergencyInfo> =
        dao.observe(authManager.currentUid).map { it?.toDomain() ?: EmergencyInfo() }

    suspend fun saveAboutMe(idNumber: String, dateOfBirth: String, bloodType: String, notes: String) =
        upsert { it.copy(idNumber = idNumber, dateOfBirth = dateOfBirth, bloodType = bloodType, aboutMeNotes = notes) }

    suspend fun saveMedical(conditions: String, allergies: String, medications: String, doctorContact: String) =
        upsert {
            it.copy(
                medicalConditions = conditions, medicalAllergies = allergies,
                medicalMedications = medications, medicalDoctorContact = doctorContact
            )
        }

    suspend fun saveVehicle(make: String, model: String, color: String, plate: String) =
        upsert { it.copy(vehicleMake = make, vehicleModel = model, vehicleColor = color, vehiclePlate = plate) }

    suspend fun saveAddresses(home: String, work: String) =
        upsert { it.copy(homeAddress = home, workAddress = work) }

    suspend fun saveHomeSecurity(alarmCompany: String, gateCode: String, alarmCode: String, notes: String) =
        upsert {
            it.copy(
                alarmCompany = alarmCompany, gateCode = gateCode,
                alarmCode = alarmCode, homeSecurityNotes = notes
            )
        }

    private suspend fun upsert(transform: (EmergencyInfoEntity) -> EmergencyInfoEntity) {
        val existing = dao.getOnce(authManager.currentUid) ?: EmergencyInfoEntity(uid = authManager.currentUid)
        dao.upsert(transform(existing).copy(updatedAt = System.currentTimeMillis()))
    }
}

private fun EmergencyInfoEntity.toDomain() = EmergencyInfo(
    aboutMe = EmergencyInfo.AboutMe(
        idNumber = idNumber.orEmpty(), dateOfBirth = dateOfBirth.orEmpty(),
        bloodType = bloodType.orEmpty(), notes = aboutMeNotes.orEmpty()
    ),
    medical = EmergencyInfo.Medical(
        conditions = medicalConditions.orEmpty(), allergies = medicalAllergies.orEmpty(),
        medications = medicalMedications.orEmpty(), doctorContact = medicalDoctorContact.orEmpty()
    ),
    vehicle = EmergencyInfo.PrimaryVehicle(
        make = vehicleMake.orEmpty(), model = vehicleModel.orEmpty(),
        color = vehicleColor.orEmpty(), plate = vehiclePlate.orEmpty()
    ),
    addresses = EmergencyInfo.Addresses(home = homeAddress.orEmpty(), work = workAddress.orEmpty()),
    homeSecurity = EmergencyInfo.HomeSecurity(
        alarmCompany = alarmCompany.orEmpty(), gateCode = gateCode.orEmpty(),
        alarmCode = alarmCode.orEmpty(), notes = homeSecurityNotes.orEmpty()
    )
)
