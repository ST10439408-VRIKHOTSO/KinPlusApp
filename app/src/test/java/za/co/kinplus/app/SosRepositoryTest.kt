package za.co.kinplus.app

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
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

        // Simulate no connectivity.
        coEvery { connectivity.isOnlineNow() } returns false

        val repository = SosRepository(api, offlineQueue, connectivity)
        val result = repository.raiseSos(lat = -25.75, lng = 28.23, message = "Help", circleIds = listOf("c1"))

        // The alert must be queued, and the UI is told it is pending (data == null).
        coVerify(exactly = 1) { offlineQueue.enqueue(ActionType.SOS, any()) }
        assertTrue(result is Resource.Success && result.data == null)
    }

    @Test
    fun `SOS raised while online is sent to the API`() = runTest {
        val api = mockk<KinPlusApi>(relaxed = true)
        val offlineQueue = mockk<OfflineQueue>(relaxed = true)
        val connectivity = mockk<ConnectivityObserver>()

        coEvery { connectivity.isOnlineNow() } returns true
        coEvery { api.raiseSos(any()) } returns
            za.co.kinplus.app.data.remote.dto.SosDto("s1", "u1", "Vukosi", "ACTIVE", -25.75, 28.23, "Help", "2026-09-20T20:00:00Z")

        val repository = SosRepository(api, offlineQueue, connectivity)
        val result = repository.raiseSos(lat = -25.75, lng = 28.23, message = "Help", circleIds = listOf("c1"))

        coVerify(exactly = 1) { api.raiseSos(any()) }
        coVerify(exactly = 0) { offlineQueue.enqueue(any(), any()) }
        assertTrue(result is Resource.Success && result.data != null)
    }
}
