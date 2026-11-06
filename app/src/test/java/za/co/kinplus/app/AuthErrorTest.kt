package za.co.kinplus.app

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import za.co.kinplus.app.auth.AuthManager
import za.co.kinplus.app.ui.screens.auth.LoginViewModel

/**
 * Sign in errors (FR-02, FR-31): empty fields are caught before Firebase, and
 * Firebase's English-only exceptions are turned into codes that the screen
 * shows in the user's language.
 */
class AuthErrorTest {

    // Wrong input: a blank email is stopped before Firebase is called.
    @Test
    fun emptyEmailIsCaughtBeforeSignIn() {
        assertEquals("EMAIL_REQUIRED", LoginViewModel.validateLogin("  ", "secret123"))
    }

    // Wrong input: an email with no domain is rejected.
    @Test
    fun badlyFormedEmailIsCaughtBeforeSignIn() {
        assertEquals("INVALID_EMAIL", LoginViewModel.validateLogin("thandi@", "secret123"))
    }

    // Wrong input: an empty password is rejected.
    @Test
    fun emptyPasswordIsCaughtBeforeSignIn() {
        assertEquals("PASSWORD_REQUIRED", LoginViewModel.validateLogin("thandi@example.com", ""))
    }

    // Right input: a valid email and a password pass the checks.
    @Test
    fun completeFormIsSent() {
        assertNull(LoginViewModel.validateLogin("thandi@example.com", "secret123"))
    }

    // Wrong input: Firebase's weak password error must give the weak password message, not wrong credentials.
    @Test
    fun weakPasswordIsCheckedBeforeInvalidCredentials() {
        // FirebaseAuthWeakPasswordException is a kind of invalid credentials exception.
        assertEquals(AuthManager.WEAK_PASSWORD, AuthManager.mapError(mockk<FirebaseAuthWeakPasswordException>(relaxed = true)))
    }

    // Wrong input: each Firebase failure (wrong password, unknown user, email in use, no network, too many attempts) maps to its own translated message.
    @Test
    fun firebaseExceptionsMapToTranslatableCodes() {
        assertEquals(AuthManager.WRONG_CREDENTIALS, AuthManager.mapError(mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)))
        assertEquals(AuthManager.WRONG_CREDENTIALS, AuthManager.mapError(mockk<FirebaseAuthInvalidUserException>(relaxed = true)))
        assertEquals(AuthManager.EMAIL_IN_USE, AuthManager.mapError(mockk<FirebaseAuthUserCollisionException>(relaxed = true)))
        assertEquals(AuthManager.NETWORK, AuthManager.mapError(mockk<FirebaseNetworkException>(relaxed = true)))
        assertEquals(AuthManager.TOO_MANY_ATTEMPTS, AuthManager.mapError(mockk<FirebaseTooManyRequestsException>(relaxed = true)))
    }

    // Wrong input: an unexpected error falls back to the general sign in failed message.
    @Test
    fun unknownFailureGetsTheGeneralCode() {
        assertEquals(AuthManager.AUTH_FAILED, AuthManager.mapError(IllegalStateException("boom")))
    }
}
