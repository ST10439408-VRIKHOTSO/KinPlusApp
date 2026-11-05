package za.co.kinplus.app.util

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

/**
 * Persists and applies the in-app language choice (FR-31: English, isiZulu,
 * Sepedi). The tag is stored in SharedPreferences because it must be read
 * synchronously inside [android.app.Activity.attachBaseContext], before any
 * coroutine-based store is available.
 *
 * Language tags map to the resource qualifiers as follows:
 *   "en"  -> res/values           (English, default)
 *   "zu"  -> res/values-zu         (isiZulu)
 *   "nso" -> res/values-nso        (Sepedi)
 *
 * Only the Activity is wrapped in attachBaseContext, so text built outside it
 * (repositories, background workers, notifications) uses [localized] to get a
 * context in the same language instead of the phone's system language.
 *
 * Reference: Android Developers (2026) Support different languages and
 * cultures. Available at: https://developer.android.com/training/basics/supporting-devices/languages
 * [Accessed 5 November 2026].
 */
object LocaleManager {

    private const val TAG = "LocaleManager"
    private const val PREFS = "kinplus_locale"
    private const val KEY_LANG = "language_tag"

    val supportedTags = listOf("en", "zu", "nso")

    fun getLanguageTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "en") ?: "en"

    fun setLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, tag).apply()
        Log.i(TAG, "App language changed to '$tag'")
    }

    /** Returns a context whose resources resolve to the saved language. */
    fun wrap(base: Context): Context {
        val tag = getLanguageTag(base)
        Locale.setDefault(Locale.forLanguageTag(tag))
        return localized(base)
    }

    /**
     * A context for [base] whose strings come from the saved in-app language,
     * without changing the process default locale. Used wherever user facing
     * text is built from the application context.
     */
    fun localized(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(getLanguageTag(base)))
        return base.createConfigurationContext(config)
    }
}
