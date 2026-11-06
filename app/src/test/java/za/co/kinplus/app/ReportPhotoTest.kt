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

    // Right input: a photo already under 1600 px is not shrunk.
    @Test
    fun smallImagesAreNotSampled() {
        assertEquals(1, ReportPhotoRepository.sampleSizeFor(1200, 900, 1600))
    }

    // Right input: large camera photos are sampled down but never below 1600 px.
    @Test
    fun largeCameraPhotoIsSampledButStaysAboveTheLimit() {
        // A 12 MP photo (4000 x 3000) decodes at half size, 2000 px, then is scaled to 1600 px.
        assertEquals(2, ReportPhotoRepository.sampleSizeFor(4000, 3000, 1600))
        assertEquals(4, ReportPhotoRepository.sampleSizeFor(8000, 6000, 1600))
    }

    // Wrong input: an unreadable image size (-1) does not break the calculation.
    @Test
    fun unknownImageSizeIsNotSampled() {
        assertEquals(1, ReportPhotoRepository.sampleSizeFor(-1, -1, 1600))
    }

    // Right input: an HTTPS Azure Blob Storage URL is allowed.
    @Test
    fun azureBlobUrlsAreAccepted() {
        assertTrue(ReportPhotoRepository.isBlobStorageUrl("https://kinplusstore.blob.core.windows.net/community-reports/a.jpg"))
    }

    // Wrong input: plain HTTP, other websites, look alike hosts and text that is not a URL are refused.
    @Test
    fun otherHostsAndPlainHttpAreRejected() {
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("http://kinplusstore.blob.core.windows.net/community-reports/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("https://example.com/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("https://blob.core.windows.net.example.com/a.jpg"))
        assertFalse(ReportPhotoRepository.isBlobStorageUrl("not a url"))
    }
}
