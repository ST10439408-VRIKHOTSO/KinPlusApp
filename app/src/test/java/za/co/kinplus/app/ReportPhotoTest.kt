package za.co.kinplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import za.co.kinplus.app.data.repository.ReportPhotoRepository

/**
 * Community report photos (blob storage): images are sampled down before
 * upload, and photos are only ever loaded from Azure Blob Storage over HTTPS.
 */
class ReportPhotoTest {

    @Test
    fun smallImagesAreNotSampled() {
        assertEquals(1, ReportPhotoRepository.sampleSizeFor(1200, 900, 1600))
    }

    @Test
    fun largeCameraPhotoIsSampledButStaysAboveTheLimit() {
        // A 12 MP photo (4000 x 3000) decodes at half size, 2000 px, then is scaled to 1600 px.
        assertEquals(2, ReportPhotoRepository.sampleSizeFor(4000, 3000, 1600))
        assertEquals(4, ReportPhotoRepository.sampleSizeFor(8000, 6000, 1600))
    }

    @Test
    fun unknownImageSizeIsNotSampled() {
        assertEquals(1, ReportPhotoRepository.sampleSizeFor(-1, -1, 1600))
    }

    @Test
    fun azureBlobUrlsAreAccepted() {
        assertTrue(ReportPhotoRepository.isBlobStorageUrl("https://kinplusstore.blob.core.windows.net/community-reports/a.jpg"))
    }

    @Test
    fun otherHostsAndPlainHttpAreRejected() {
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("http://kinplusstore.blob.core.windows.net/community-reports/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("https://example.com/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("https://blob.core.windows.net.example.com/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("not a url"))
    }
}
