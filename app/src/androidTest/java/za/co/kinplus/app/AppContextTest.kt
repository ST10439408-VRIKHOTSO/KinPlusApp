package za.co.kinplus.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumented test that runs on a device/emulator and confirms the
 * application id is correct. Extend with Compose UI tests as needed.
 */
@RunWith(AndroidJUnit4::class)
class AppContextTest {
    @Test
    fun usesKinPlusPackageName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("za.co.kinplus.app", context.packageName)
    }
}
