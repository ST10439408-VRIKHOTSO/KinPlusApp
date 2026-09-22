package za.co.kinplus.app.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the current Firebase ID token as a bearer credential to every API
 * request (Part 1B, Table 5.2, step 3). The token is retrieved synchronously
 * here because OkHttp interceptors run on a background thread.
 *
 * If the server replies 401 the token may have expired; we force-refresh it
 * once and retry, matching the error-handling contract (Table 5.4).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = currentIdToken(forceRefresh = false)
        val request = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }

        var response = chain.proceed(request)

        // One retry with a fresh token on 401 Unauthorized.
        if (response.code == 401) {
            Log.w(TAG, "401 received; refreshing token and retrying once")
            response.close()
            val refreshed = currentIdToken(forceRefresh = true)
            val retry = original.newBuilder()
                .apply { if (refreshed != null) header("Authorization", "Bearer $refreshed") }
                .build()
            response = chain.proceed(retry)
        }
        return response
    }

    private fun currentIdToken(forceRefresh: Boolean): String? = try {
        val user = firebaseAuth.currentUser ?: return null
        Tasks.await(user.getIdToken(forceRefresh)).token
    } catch (e: Exception) {
        Log.e(TAG, "Failed to obtain Firebase ID token", e)
        null
    }

    private companion object {
        const val TAG = "AuthInterceptor"
    }
}
