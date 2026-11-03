package za.co.kinplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import za.co.kinplus.app.ui.screens.journey.formatEta
import za.co.kinplus.app.ui.screens.journey.parseEta
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Unit tests for the journey card's ETA. The API sends a UTC ISO 8601 string;
 * the card shows it as a short local time. South Africa is UTC+2 all year.
 */
class EtaFormatTest {

    private val sast = ZoneId.of("Africa/Johannesburg")
    private val locale = Locale.UK

    private fun format(iso: String, now: String) = formatEta(iso, sast, locale, Instant.parse(now))

    @Test
    fun apiEtaIsShownAsLocalTime() {
        // The exact value seen on the journey card while recording the demo.
        assertEquals("17:15", format("2026-11-03T15:15:17.6280000Z", now = "2026-11-03T14:45:00Z"))
    }

    @Test
    fun etaOnAnotherDayIncludesTheDate() {
        // 23:30 UTC is 01:30 the next morning in South Africa.
        assertEquals("4 Nov, 01:30", format("2026-11-03T23:30:00Z", now = "2026-11-03T21:00:00Z"))
    }

    @Test
    fun etaWithAnOffsetIsConvertedToLocalTime() {
        assertEquals("17:15", format("2026-11-03T13:15:00-02:00", now = "2026-11-03T14:45:00Z"))
    }

    @Test
    fun etaWithNoOffsetIsTreatedAsUtc() {
        assertEquals("17:15", format("2026-11-03T15:15:17", now = "2026-11-03T14:45:00Z"))
    }

    @Test
    fun unreadableEtaIsShownAsItIs() {
        assertNull(parseEta("soon"))
        assertEquals("soon", format("soon", now = "2026-11-03T14:45:00Z"))
    }
}
