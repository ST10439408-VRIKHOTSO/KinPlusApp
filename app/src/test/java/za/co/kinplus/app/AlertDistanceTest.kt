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

    // Right input: the same point twice must give a distance of 0 km.
    @Test
    fun samePointIsZeroKilometres() {
        assertEquals(0.0, distanceKm(-25.7545, 28.2314, -25.7545, 28.2314), 0.0001)
    }

    // Right input: two real cities must come out close to the known 55 km.
    @Test
    fun pretoriaToJohannesburgIsAboutFiftyFiveKilometres() {
        // Pretoria CBD to Johannesburg CBD is roughly 54 to 56 km in a straight line.
        val km = distanceKm(-25.7479, 28.2293, -26.2041, 28.0473)
        assertTrue("Expected about 55 km but was $km", km in 52.0..58.0)
    }

    // Right input: swapping the start and end must not change the distance.
    @Test
    fun distanceIsTheSameInBothDirections() {
        val there = distanceKm(-25.7545, 28.2314, -25.8601, 28.1891)
        val back = distanceKm(-25.8601, 28.1891, -25.7545, 28.2314)
        assertEquals(there, back, 0.0001)
    }

    // Wrong value guarded: no check in timer may be zero or negative, and the choices must be in order.
    @Test
    fun checkInIntervalsArePositiveAndSorted() {
        val options = CheckInScheduler.intervalOptions
        assertTrue(options.isNotEmpty())
        assertTrue(options.all { it > 0 })
        assertEquals(options.sorted(), options)
    }

    // Wrong value guarded: a grace period longer than the shortest timer would let two prompts overlap.
    @Test
    fun graceWindowIsShorterThanTheSmallestInterval() {
        assertTrue(CheckInScheduler.GRACE_MINUTES < CheckInScheduler.intervalOptions.first())
    }
}
