package za.co.kinplus.app

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.AlertRequest
import za.co.kinplus.app.data.repository.AlertRepository
import za.co.kinplus.app.data.repository.ReportOutcome
import za.co.kinplus.app.data.repository.ReportPhotoRepository
import za.co.kinplus.app.data.sync.ActionType
import za.co.kinplus.app.data.sync.OfflineQueue
import za.co.kinplus.app.data.sync.QueuedAlert
import za.co.kinplus.app.util.ConnectivityObserver
import java.io.File

/**
 * Community report submission (FR-26) with an optional photo in Azure Blob
 * Storage: the report must never be lost because of the photo, and a report
 * made offline must keep its photo for the sync worker.
 */
class AlertRepositoryTest {

    private val api = mockk<KinPlusApi>(relaxed = true)
    private val offlineQueue = mockk<OfflineQueue>(relaxed = true)
    private val connectivity = mockk<ConnectivityObserver>()
    private val photoRepository = mockk<ReportPhotoRepository>(relaxed = true)
    private val photo = File("report.jpg")
    private val blobUrl = "https://kinplus.blob.core.windows.net/community-reports/p.jpg"

    private fun repository() = AlertRepository(api, offlineQueue, connectivity, photoRepository)

    // Right input: online with a photo that uploads, so the report carries the blob URL and the local copy is deleted.
    @Test
    fun `online report uploads the photo and sends its blob URL`() = runTest {
        every { connectivity.isOnlineNow() } returns true
        coEvery { photoRepository.upload(photo) } returns blobUrl

        val outcome = repository().submit("HAZARD", "Open manhole", -25.75, 28.23, photo)

        assertEquals(ReportOutcome.SENT, outcome)
        coVerify(exactly = 1) { api.createAlert(AlertRequest("HAZARD", "Open manhole", -25.75, 28.23, blobUrl)) }
        verify(exactly = 1) { photoRepository.discard(photo) }
    }

    // Wrong input: the photo cannot be uploaded, so the report must still be sent without it.
    @Test
    fun `report is still sent when the photo upload fails`() = runTest {
        every { connectivity.isOnlineNow() } returns true
        coEvery { photoRepository.upload(photo) } returns null

        val outcome = repository().submit("CRIME", "", -25.75, 28.23, photo)

        assertEquals(ReportOutcome.SENT_WITHOUT_PHOTO, outcome)
        coVerify(exactly = 1) { api.createAlert(AlertRequest("CRIME", "", -25.75, 28.23, null)) }
    }

    // Wrong condition: no connection, so nothing is uploaded and the report is queued with its photo.
    @Test
    fun `offline report is queued with the local photo path`() = runTest {
        every { connectivity.isOnlineNow() } returns false

        val outcome = repository().submit("FIRE", "Veld fire", -25.75, 28.23, photo)

        assertEquals(ReportOutcome.QUEUED, outcome)
        coVerify(exactly = 0) { photoRepository.upload(any()) }
        coVerify(exactly = 1) {
            offlineQueue.enqueue(ActionType.ALERT, QueuedAlert("FIRE", "Veld fire", -25.75, 28.23, localPhotoPath = photo.absolutePath))
        }
        verify(exactly = 0) { photoRepository.discard(any()) }
    }

    // Right input: a report with no photo must not touch blob storage at all.
    @Test
    fun `report without a photo never calls blob storage`() = runTest {
        every { connectivity.isOnlineNow() } returns true

        val outcome = repository().submit("MEDICAL", "Person collapsed", -25.75, 28.23, null)

        assertEquals(ReportOutcome.SENT, outcome)
        coVerify(exactly = 0) { photoRepository.upload(any()) }
    }
}
