package za.co.kinplus.app.util

/**
 * A tiny result wrapper used between the data and UI layers so screens can
 * render loading, success and error states without throwing exceptions across
 * layers. This keeps the app resilient to network failures (NFR-05, NFR-07).
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T, val fromCache: Boolean = false) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}
