package za.co.kinplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import za.co.kinplus.app.ui.common.DistanceFormat

/**
 * Settings > Units (FR-32): distances are worked out in km and metres and
 * only converted for display, so the filters must convert correctly.
 */
class DistanceFormatTest {

    @Test
    fun metricFiltersAreTheOriginalKilometreChoices() {
        assertEquals(listOf(1, 5, 20, null), DistanceFormat.filterOptions(metric = true))
    }

    @Test
    fun imperialFiltersAreMilesAndEndWithAnyDistance() {
        val options = DistanceFormat.filterOptions(metric = false)
        assertEquals(listOf(1, 3, 12), options.filterNotNull())
        assertNull(options.last())
    }

    @Test
    fun mileFilterIsConvertedToKilometres() {
        assertEquals(1.609344, DistanceFormat.filterToKm(1, metric = false), 1e-9)
        assertEquals(5.0, DistanceFormat.filterToKm(5, metric = true), 1e-9)
    }

    @Test
    fun kilometresConvertToMiles() {
        assertEquals(10.0, DistanceFormat.kmToMiles(16.09344), 1e-9)
    }

    @Test
    fun zoneRadiusConvertsToWholeFeet() {
        assertEquals(328, DistanceFormat.metresToFeet(100))
        assertEquals(16404, DistanceFormat.metresToFeet(5000))
    }
}
