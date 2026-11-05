package za.co.kinplus.app.data.local

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.co.kinplus.app.data.checkin.CheckInScheduler
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.location.GeofenceManager
import za.co.kinplus.app.messaging.PushRegistration
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes an account's data from the phone (POPIA, and the privacy policy's
 * promise that local data is removed on sign out).
 *
 * The cached circles, locations and Safe Zones and the offline queue are not
 * stored per user, so they are cleared on every sign out: the next account on
 * this phone must never see them, and a queued SOS must never be sent with
 * another account's sign in. Geofences, the check in timer and the push token
 * are removed for the same reason. Deleting the account clears everything.
 */
@Singleton
class LocalDataCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: KinPlusDatabase,
    private val circleDao: CircleDao,
    private val locationDao: LocationDao,
    private val zoneDao: ZoneDao,
    private val pendingActionDao: PendingActionDao,
    private val geofenceManager: GeofenceManager,
    private val pushRegistration: PushRegistration
) {
    /** Called before signing out. */
    suspend fun clearSession() {
        circleDao.clear()
        locationDao.clearAll()
        zoneDao.clear()
        pendingActionDao.clearAll()
        WorkManager.getInstance(context).cancelUniqueWork(OfflineQueue.WORK_NAME)
        CheckInScheduler.stop(context)
        runCatching { geofenceManager.syncZones(emptyList()) }
            .onFailure { Log.w(TAG, "Could not remove geofences", it) }
        pushRegistration.unregisterDevice()
        Log.i(TAG, "Session data cleared from this phone")
    }

    /** Called after the API has deleted the account: nothing of it stays on the phone. */
    suspend fun clearEverything() {
        clearSession()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            listOf("photos", "report_photos").forEach { File(context.filesDir, it).deleteRecursively() }
        }
        Log.i(TAG, "All Kin+ data deleted from this phone")
    }

    private companion object {
        const val TAG = "LocalDataCleaner"
    }
}
