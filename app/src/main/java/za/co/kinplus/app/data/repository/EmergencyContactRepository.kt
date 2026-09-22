package za.co.kinplus.app.data.repository

import kotlinx.coroutines.flow.Flow
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.EmergencyContactDao
import za.co.kinplus.app.data.local.EmergencyContactEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emergency Contacts (Safety > Emergency Contacts) — named people with a phone
 * number to call in an emergency. No matching API endpoint exists, so this is
 * kept on-device only, same as Emergency Information.
 */
@Singleton
class EmergencyContactRepository @Inject constructor(
    private val dao: EmergencyContactDao,
    private val authManager: AuthManager
) {
    fun observe(): Flow<List<EmergencyContactEntity>> = dao.observeForUser(authManager.currentUid)

    suspend fun add(name: String, relationship: String, phone: String) {
        dao.upsert(
            EmergencyContactEntity(
                id = UUID.randomUUID().toString(),
                uid = authManager.currentUid,
                name = name,
                relationship = relationship,
                phone = phone
            )
        )
    }

    suspend fun delete(id: String) = dao.delete(id)
}
