package za.co.kinplus.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import kotlinx.coroutines.tasks.await
import za.co.kinplus.app.util.Resource
import java.security.MessageDigest
import java.util.UUID
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
 * Single sign on (FR-05, PoE): "Continue with Google" uses the Android
 * Credential Manager to get a Google ID token, which is exchanged for a
 * Firebase credential, so a Google account signs in to the same Firebase
 * project as email and password users.
 *
 * Reference: Android Developers (2024) Authenticate users with Sign in with
 * Google. Available at: https://developer.android.com/identity/sign-in/credential-manager-siwg
 * [Accessed 14 October 2026].
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

    // ----- Google single sign on -----

    /**
     * Shows the Google account picker and signs the chosen account in to
     * Firebase. [activityContext] must be an Activity so the picker can be
     * shown. The OAuth web client id is read from the default_web_client_id
     * string that the Google Services plugin generates from
     * google-services.json; builds without a real Firebase config return
     * GOOGLE_NOT_CONFIGURED instead of crashing.
     */
    suspend fun signInWithGoogle(activityContext: Context): Resource<Unit> {
        val webClientId = webClientId(activityContext)
            ?: return Resource.Error(GOOGLE_NOT_CONFIGURED).also { Log.w(TAG, "No default_web_client_id in this build") }

        return try {
            // A random nonce in the request mitigates ID token replay.
            val nonce = MessageDigest.getInstance("SHA-256")
                .digest(UUID.randomUUID().toString().toByteArray())
                .joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(nonce)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            val response = CredentialManager.create(activityContext).getCredential(activityContext, request)

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                Log.i(TAG, "Google sign-in succeeded for ${firebaseAuth.currentUser?.uid}")
                Resource.Success(Unit)
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
                Resource.Error(GOOGLE_FAILED)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google sign-in cancelled by the user")
            Resource.Error(GOOGLE_CANCELLED, e)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available on this device", e)
            Resource.Error(GOOGLE_NO_ACCOUNT, e)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            Resource.Error(GOOGLE_FAILED, e)
        }
    }

    /** The OAuth web client id generated from google-services.json, or null when absent. */
    private fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id).takeIf { it.isNotBlank() } else null
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

        // Google sign in result codes, mapped to friendly text in the UI.
        const val GOOGLE_NOT_CONFIGURED = "GOOGLE_NOT_CONFIGURED"
        const val GOOGLE_CANCELLED = "GOOGLE_CANCELLED"
        const val GOOGLE_NO_ACCOUNT = "GOOGLE_NO_ACCOUNT"
        const val GOOGLE_FAILED = "GOOGLE_FAILED"

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
