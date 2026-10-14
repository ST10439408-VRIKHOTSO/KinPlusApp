package za.co.kinplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import za.co.kinplus.app.data.checkin.CheckInScheduler
import za.co.kinplus.app.ui.screens.alerts.distanceKm

/**
 * Unit tests for the Batch 1 Community Alerts distance filter and the check in
 * interval options. The distance maths runs on the JVM, so no device is needed.
 */
class AlertDistanceTest {

    @Test
    fun samePointIsZeroKilometres() {
        assertEquals(0.0, distanceKm(-25.7545, 28.2314, -25.7545, 28.2314), 0.0001)
    }

    @Test
    fun pretoriaToJohannesburgIsAboutFiftyFiveKilometres() {
        // Pretoria CBD to Johannesburg CBD is roughly 54 to 56 km in a straight line.
        val km = distanceKm(-25.7479, 28.2293, -26.2041, 28.0473)
        assertTrue("Expected about 55 km but was $km", km in 52.0..58.0)
    }

    @Test
    fun distanceIsTheSameInBothDirections() {
        val there = distanceKm(-25.7545, 28.2314, -25.8601, 28.1891)
        val back = distanceKm(-25.8601, 28.1891, -25.7545, 28.2314)
        assertEquals(there, back, 0.0001)
    }

    @Test
    fun checkInIntervalsArePositiveAndSorted() {
        val options = CheckInScheduler.intervalOptions
        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it > 0 })
        assertEquals(options.sorted(), options)
    }

    @Test
    fun graceWindowIsShorterThanTheSmallestInterval() {
        assertTrue(CheckInScheduler.GRACE_MINUTES < CheckInScheduler.intervalOptions.first())
    }
}
