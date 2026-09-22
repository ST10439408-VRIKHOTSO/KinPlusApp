package za.co.kinplus.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import za.co.kinplus.app.auth.AuthManager

/**
 * Unit tests for the credential-validation rules (FR-01, FR-02). These are the
 * checks that stop invalid input reaching Firebase and are pure functions, so
 * they run on the JVM without a device (NFR-08).
 */
class AuthValidationTest {

    @Test
    fun `valid emails are accepted`() {
        assertTrue(AuthManager.isValidEmail("vukosi@example.com"))
        assertTrue(AuthManager.isValidEmail("theo.golele@rc.iie.ac.za"))
    }

    @Test
    fun `invalid emails are rejected`() {
        assertFalse(AuthManager.isValidEmail("no-at-sign"))
        assertFalse(AuthManager.isValidEmail("missing@domain"))
        assertFalse(AuthManager.isValidEmail("@nope.com"))
        assertFalse(AuthManager.isValidEmail(""))
    }

    @Test
    fun `strong passwords need 8 chars a letter and a number`() {
        assertTrue(AuthManager.isStrongPassword("kinplus1"))
        assertTrue(AuthManager.isStrongPassword("SafeR2024"))
    }

    @Test
    fun `weak passwords are rejected`() {
        assertFalse(AuthManager.isStrongPassword("short1"))     // < 8
        assertFalse(AuthManager.isStrongPassword("allletters")) // no digit
        assertFalse(AuthManager.isStrongPassword("12345678"))   // no letter
    }
}
