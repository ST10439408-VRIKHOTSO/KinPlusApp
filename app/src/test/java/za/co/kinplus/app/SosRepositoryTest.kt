package za.co.kinplus.app

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.data.local.SosHistoryDao
import za.co.kinplus.app.data.local.SosHistoryEntity
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.data.repository.SosRepository
import za.co.kinplus.app.util.ConnectivityObserver
import za.co.kinplus.app.util.Resource

/**
 * Verifies the offline-with-sync behaviour of the SOS feature (FR-29): when the
 * device is offline, an SOS must be queued for later delivery rather than lost.
 * This is the single most safety-critical path in the app.
 */
class SosRepositoryTest {

    @Test
    fun `SOS raised while offline is queued`() = runTest {
        val api = mockk<KinPlusApi>(relaxed = true)
        val offlineQueue = mockk<OfflineQueue>(relaxed = true)
        val connectivity = mockk<ConnectivityObserver>()
        val sosHistoryDao = mockk<SosHistoryDao>(relaxed = true)
        val authManager = mockk<AuthManager>(relaxed = true)

        // Simulate no connectivity.
        coEvery { connectivity.isOnlineNow() } returns false
        every { authManager.currentUid } returns "u1"

        val repository = SosRepository(api, offlineQueue, connectivity, sosHistoryDao, authManager)
        val result = repository.raiseSos(lat = -25.75, lng = 28.23, message = "Help", circleIds = listOf("c1"))

        // The alert must be queued, and the UI is told it is pending (data == null).
        coVerify(exactly = 1) { offlineQueue.enqueue(ActionType.SOS, any()) }
        assertTrue(result is Resource.Success && result.data == null)
    }

    @Test
    fun `queued SOS is logged in history under its queued action id`() = runTest {
        val api = mockk<KinPlusApi>(relaxed = true)
        val offlineQueue = mockk<OfflineQueue>(relaxed = true)
        val connectivity = mockk<ConnectivityObserver>()
        val sosHistoryDao = mockk<SosHistoryDao>(relaxed = true)
        val authManager = mockk<AuthManager>(relaxed = true)
        val entry = slot<SosHistoryEntity>()

        coEvery { connectivity.isOnlineNow() } returns false
        every { authManager.currentUid } returns "u1"
        coEvery { offlineQueue.enqueue(ActionType.SOS, any()) } returns "action-1"
        coEvery { sosHistoryDao.insert(capture(entry)) } just Runs

        val repository = SosRepository(api, offlineQueue, connectivity, sosHistoryDao, authManager)
        repository.raiseSos(lat = -25.75, lng = 28.23, message = "Help", circleIds = listOf("c1"))

        // The shared id is what lets SyncWorker mark this entry "SENT" later.
        assertEquals("action-1", entry.captured.id)
        assertEquals("QUEUED", entry.captured.status)
    }

    @Test
    fun `SOS raised while online is sent to the API`() = runTest {
        val api = mockk<KinPlusApi>(relaxed = true)
        val offlineQueue = mockk<OfflineQueue>(relaxed = true)
        val connectivity = mockk<ConnectivityObserver>()
        val sosHistoryDao = mockk<SosHistoryDao>(relaxed = true)
        val authManager = mockk<AuthManager>(relaxed = true)

        coEvery { connectivity.isOnlineNow() } returns true
        every { authManager.currentUid } returns "u1"
        coEvery { api.raiseSos(any()) } returns
            za.co.kinplus.app.data.remote.dto.SosDto("s1", "u1", "Vukosi", "ACTIVE", -25.75, 28.23, "Help", "2026-09-20T20:00:00Z")

        val repository = SosRepository(api, offlineQueue, connectivity, sosHistoryDao, authManager)
        val result = repository.raiseSos(lat = -25.75, lng = 28.23, message = "Help", circleIds = listOf("c1"))

        coVerify(exactly = 1) { api.raiseSos(any()) }
        coVerify(exactly = 0) { offlineQueue.enqueue(any(), any()) }
        assertTrue(result is Resource.Success && result.data != null)
    }
}
