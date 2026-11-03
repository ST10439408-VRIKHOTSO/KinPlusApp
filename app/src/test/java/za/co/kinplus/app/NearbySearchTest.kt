package za.co.kinplus.app

import org.junit.Assert.assertEquals
import org.junit.Test
import za.co.kinplus.app.ui.screens.safety.nearbyGeoUri
import za.co.kinplus.app.ui.screens.safety.nearbyWebUrl

/**
 * Unit tests for the Safety hub's nearby help links. The search must be
 * centred on the user, not left to the maps app (which showed Johannesburg
 * results for a user in Pretoria).
 */
class NearbySearchTest {

    @Test
    fun geoUriIsCentredOnTheLastKnownFix() {
        assertEquals("geo:-25.747700,28.185800?q=police+station", nearbyGeoUri("police station", -25.7477, 28.1858))
    }

    @Test
    fun geoUriWithoutAFixLetsTheMapsAppChoose() {
        assertEquals("geo:0,0?q=hospital", nearbyGeoUri("hospital", null, null))
    }

    @Test
    fun queryIsEncoded() {
        assertEquals("geo:0,0?q=clinic+%26+pharmacy", nearbyGeoUri("clinic & pharmacy", null, null))
    }

    @Test
    fun webFallbackIsCentredOnTheLastKnownFix() {
        assertEquals(
            "https://www.google.com/maps/search/pharmacy/@-25.747700,28.185800,14z",
            nearbyWebUrl("pharmacy", -25.7477, 28.1858)
        )
        assertEquals("https://www.google.com/maps/search/pharmacy", nearbyWebUrl("pharmacy", null, null))
    }
}
