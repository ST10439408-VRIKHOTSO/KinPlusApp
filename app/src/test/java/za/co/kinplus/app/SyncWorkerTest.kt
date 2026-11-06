package za.co.kinplus.app

import androidx.work.ListenableWorker
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import za.co.kinplus.app.data.local.PendingActionDao
import za.co.kinplus.app.data.local.PendingActionEntity
import za.co.kinplus.app.data.local.SosHistoryDao
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.AlertRequest
import za.co.kinplus.app.data.remote.dto.SosRequest
import za.co.kinplus.app.data.repository.ReportPhotoRepository
import za.co.kinplus.app.data.sync.QueuedAlert
import za.co.kinplus.app.data.sync.SyncWorker
import java.io.File

/**
 * Verifies the offline queue replay in SyncWorker (FR-29): an SOS queued
 * offline is updated in Incident History once it is delivered, and a
 * community report made offline gets its photo uploaded to blob storage
 * before it is sent.
 */
class SyncWorkerTest {

    private val api = mockk<KinPlusApi>(relaxed = true)
    private val pendingDao = mockk<PendingActionDao>(relaxed = true)
    private val sosHistoryDao = mockk<SosHistoryDao>(relaxed = true)
    private val photoRepository = mockk<ReportPhotoRepository>(relaxed = true)
    private val gson = Gson()

    private fun worker() =
        SyncWorker(mockk(relaxed = true), mockk(relaxed = true), api, pendingDao, sosHistoryDao, gson, photoRepository)

    private fun queuedSos(id: String) =
        PendingActionEntity(id, "SOS", gson.toJson(SosRequest(-25.75, 28.23, "Help", listOf("c1"))))

    // Right case: the queued SOS reaches the API, so it is marked sent and removed from the queue.
    @Test
    fun `synced SOS is marked sent in history`() = runTest {
        coEvery { pendingDao.getAll() } returns listOf(queuedSos("a1"))

        val result = worker().doWork()

        coVerify(exactly = 1) { api.raiseSos(match { it.clientActionId == "a1" }) }
        coVerify(exactly = 1) { sosHistoryDao.markSent("a1") }
        coVerify(exactly = 1) { pendingDao.remove("a1") }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    // Wrong case: the API cannot be reached, so the SOS stays queued and WorkManager retries.
    @Test
    fun `SOS that fails to sync stays queued in history`() = runTest {
        coEvery { pendingDao.getAll() } returns listOf(queuedSos("a1"))
        coEvery { api.raiseSos(any()) } throws RuntimeException("offline")

        val result = worker().doWork()

        coVerify(exactly = 0) { sosHistoryDao.markSent(any()) }
        coVerify(exactly = 0) { pendingDao.remove(any()) }
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    // Right case: a report made offline uploads its photo first, then is sent with the photo URL.
    @Test
    fun `report queued offline uploads its photo before it is sent`() = runTest {
        val photo = File.createTempFile("report", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val payload = gson.toJson(QueuedAlert("HAZARD", "Pothole", -25.75, 28.23, localPhotoPath = photo.absolutePath))
        coEvery { pendingDao.getAll() } returns listOf(PendingActionEntity("r1", "ALERT", payload))
        coEvery { photoRepository.upload(any()) } returns "https://kinplus.blob.core.windows.net/community-reports/p.jpg"

        val result = worker().doWork()

        coVerify(exactly = 1) { photoRepository.upload(match { it.path == photo.path }) }
        coVerify(exactly = 1) {
            api.createAlert(AlertRequest("HAZARD", "Pothole", -25.75, 28.23, "https://kinplus.blob.core.windows.net/community-reports/p.jpg"))
        }
        verify(exactly = 1) { photoRepository.discard(match { it?.path == photo.path }) }
        coVerify(exactly = 1) { pendingDao.remove("r1") }
        assertEquals(ListenableWorker.Result.success(), result)
        photo.delete()
    }

    // Edge case: a report saved by version 1.2 (with no photo field) is still read and sent.
    @Test
    fun `report queued by an older version without a photo still syncs`() = runTest {
        // Version 1.2 queued the plain API body; it must still be read after the update.
        val oldPayload = gson.toJson(AlertRequest("CRIME", "Suspicious car", -25.75, 28.23, null))
        coEvery { pendingDao.getAll() } returns listOf(PendingActionEntity("r2", "ALERT", oldPayload))

        val result = worker().doWork()

        coVerify(exactly = 0) { photoRepository.upload(any()) }
        coVerify(exactly = 1) { api.createAlert(AlertRequest("CRIME", "Suspicious car", -25.75, 28.23, null)) }
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
