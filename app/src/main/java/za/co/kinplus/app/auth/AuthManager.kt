package za.co.kinplus.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import kotlinx.coroutines.tasks.await
import za.co.kinplus.app.BuildConfig
import za.co.kinplus.app.util.Resource
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Authentication (FR-01 to FR-06).
 *
 * Password handling: the app never stores or transmits the password itself.
 * Firebase Authentication hashes and salts credentials on Google's servers
 * (scrypt), and all traffic is TLS. This is what satisfies the PoE requirement
 * to "encrypt the password" (FR-03).
 *
 * Single sign-on (FR-05, PoE): Google sign-in uses the Android Credential
 * Manager to obtain a Google ID token, which is exchanged for a Firebase
 * credential.
 */
@Singleton
class AuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val isSignedIn: Boolean get() = firebaseAuth.currentUser != null
    val currentUid: String get() = firebaseAuth.currentUser?.uid ?: "local"
    val currentDisplayName: String get() = firebaseAuth.currentUser?.displayName ?: ""
    val currentEmail: String get() = firebaseAuth.currentUser?.email ?: ""
    val currentPhone: String? get() = firebaseAuth.currentUser?.phoneNumber

    // ----- Email / password -----

    suspend fun register(displayName: String, email: String, password: String): Resource<Unit> {
        validateCredentials(email, password)?.let { return it }
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            // Store the chosen display name on the Firebase user record.
            result.user?.updateProfile(
                userProfileChangeRequest { this.displayName = displayName.trim() }
            )?.await()
            Log.i(TAG, "Registered new user ${result.user?.uid}")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Resource.Error(mapError(e), e)
        }
    }

    suspend fun login(email: String, password: String): Resource<Unit> = try {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        Log.i(TAG, "Email sign-in succeeded")
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Email sign-in failed", e)
        Resource.Error(mapError(e), e)
    }

    suspend fun sendPasswordReset(email: String): Resource<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(mapError(e), e)
    }

    // ----- Google single sign-on -----

    /**
     * Launches the Credential Manager Google sign-in flow. Requires the web
     * client id from local.properties (KINPLUS_GOOGLE_WEB_CLIENT_ID).
     */
    suspend fun signInWithGoogle(activityContext: Context): Resource<Unit> {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return Resource.Error("Google sign-in is not configured (missing web client id).")
        }
        return try {
            val credentialManager = CredentialManager.create(activityContext)

            // A random nonce mitigates token replay.
            val nonce = MessageDigest.getInstance("SHA-256")
                .digest(java.util.UUID.randomUUID().toString().toByteArray())
                .joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            val response = credentialManager.getCredential(activityContext, request)

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                Log.i(TAG, "Google SSO succeeded")
                Resource.Success(Unit)
            } else {
                Resource.Error("Unexpected credential type from Google.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google SSO failed", e)
            Resource.Error("Google sign-in was cancelled or failed.", e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        Log.i(TAG, "User signed out")
    }

    /** Updates the display name on the Firebase user record (Profile > Edit Profile). */
    suspend fun updateDisplayName(name: String): Resource<Unit> = try {
        firebaseAuth.currentUser?.updateProfile(
            userProfileChangeRequest { displayName = name.trim() }
        )?.await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Display name update failed", e)
        Resource.Error("Could not update your name.", e)
    }

    // ----- validation helpers -----

    /** Returns an error Resource if the credentials are invalid, else null. */
    private fun validateCredentials(email: String, password: String): Resource.Error? {
        if (!isValidEmail(email)) return Resource.Error("INVALID_EMAIL")
        if (!isStrongPassword(password)) return Resource.Error("WEAK_PASSWORD")
        return null
    }

    private fun mapError(e: Exception): String = e.message ?: "AUTH_FAILED"

    companion object {
        private const val TAG = "AuthManager"

        /** FR-02: a syntactically valid email. */
        fun isValidEmail(email: String): Boolean =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email.trim())

        /** FR-01: at least 8 characters including a letter and a number. */
        fun isStrongPassword(password: String): Boolean =
            password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

        /** A cellphone number: digits, spaces, and an optional leading "+", 7-15 digits. */
        fun isValidPhone(phone: String): Boolean =
            Regex("^\\+?[0-9\\s-]{7,15}$").matches(phone.trim()) && phone.count { it.isDigit() } >= 7
    }
}
