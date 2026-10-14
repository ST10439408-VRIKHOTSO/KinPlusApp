package za.co.kinplus.app.ui.common

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Small helpers shared by every Fragment so each screen stays focused on its
 * own layout. Collecting a ViewModel's StateFlow only while the screen is
 * visible (repeatOnLifecycle) is the recommended way to observe state from
 * Views.
 *
 * Reference: Android Developers (2024) A safer way to collect flows from
 * Android UIs. Available at: https://developer.android.com/topic/libraries/architecture/coroutines
 */

/** Collects [flow] while the Fragment's view is at least STARTED, calling [block] with each value. */
fun <T> Fragment.collectWhileStarted(flow: Flow<T>, block: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { block(it) }
        }
    }
}

/**
 * Navigates only while this Fragment is still the visible screen. ViewModels
 * finish network calls asynchronously, so a late callback must not crash by
 * navigating from a screen the user already left.
 */
fun Fragment.navigateSafely(destinationId: Int, args: android.os.Bundle? = null, options: androidx.navigation.NavOptions? = null) {
    if (!isAdded || view == null) return
    runCatching { findNavController().navigate(destinationId, args, options) }
}

/** Pops back to the previous screen if this Fragment is still showing. */
fun Fragment.popBackSafely() {
    if (!isAdded || view == null) return
    runCatching { findNavController().popBackStack() }
}

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

/**
 * Sets text on an EditText only when it differs from what is already typed.
 * Pushing every state update straight back into the field would move the
 * cursor to the start while the user is typing.
 */
fun EditText.setTextIfDifferent(value: String) {
    if (text?.toString() != value) {
        setText(value)
        setSelection(value.length)
    }
}

/** Shows [text] in the view, or hides it when [text] is null or blank. */
fun TextView.setTextOrGone(text: CharSequence?) {
    isVisible = !text.isNullOrBlank()
    this.text = text
}

fun View.show(visible: Boolean) { isVisible = visible }

/**
 * A profile or circle avatar: the picked photo when [photoPath] is set, or
 * the first letter of [fallbackText] on the evergreen circle, the same
 * default avatar pattern most apps use.
 */
fun bindAvatar(image: ImageView, initial: TextView, photoPath: String?, fallbackText: String) {
    val bitmap = photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
    if (bitmap != null) {
        image.setImageBitmap(bitmap)
        image.isVisible = true
        initial.isVisible = false
    } else {
        image.setImageDrawable(null)
        image.isVisible = false
        initial.isVisible = true
        initial.text = fallbackText.take(1).ifBlank { "?" }.uppercase()
    }
}

/** Up to two letters ("Sarah Miller" becomes "SM") so a person is identifiable at a glance. */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Converts dp to pixels for sizes set from code. */
fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
