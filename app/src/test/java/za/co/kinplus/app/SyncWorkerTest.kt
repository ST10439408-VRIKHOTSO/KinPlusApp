package za.co.kinplus.app

import androidx.work.ListenableWorker
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import za.co.kinplus.app.data.local.PendingActionDao
import za.co.kinplus.app.data.local.PendingActionEntity
import za.co.kinplus.app.data.local.SosHistoryDao
import za.co.kinplus.app.data.remote.KinPlusApi
import za.co.kinplus.app.data.remote.dto.SosRequest
import za.co.kinplus.app.data.sync.SyncWorker

/**
 * Verifies that an SOS queued offline is updated in Incident History once
 * SyncWorker delivers it, instead of staying "queued" for good (FR-29).
 */
class SyncWorkerTest {

    private val api = mockk<KinPlusApi>(relaxed = true)
    private val pendingDao = mockk<PendingActionDao>(relaxed = true)
    private val sosHistoryDao = mockk<SosHistoryDao>(relaxed = true)
    private val gson = Gson()

    private fun worker() = SyncWorker(mockk(relaxed = true), mockk(relaxed = true), api, pendingDao, sosHistoryDao, gson)

    private fun queuedSos(id: String) =
        PendingActionEntity(id, "SOS", gson.toJson(SosRequest(-25.75, 28.23, "Help", listOf("c1"))))

    @Test
    fun `synced SOS is marked sent in history`() = runTest {
        coEvery { pendingDao.getAll() } returns listOf(queuedSos("a1"))

        val result = worker().doWork()

        coVerify(exactly = 1) { api.raiseSos(match { it.clientActionId == "a1" }) }
        coVerify(exactly = 1) { sosHistoryDao.markSent("a1") }
        coVerify(exactly = 1) { pendingDao.remove("a1") }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `SOS that fails to sync stays queued in history`() = runTest {
        coEvery { pendingDao.getAll() } returns listOf(queuedSos("a1"))
        coEvery { api.raiseSos(any()) } throws RuntimeException("offline")

        val result = worker().doWork()

        coVerify(exactly = 0) { sosHistoryDao.markSent(any()) }
        coVerify(exactly = 0) { pendingDao.remove(any()) }
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
