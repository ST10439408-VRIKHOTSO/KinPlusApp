package za.co.kinplus.app.util

import android.content.Context
import android.content.res.Configuration
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
 */
object LocaleManager {

    private const val PREFS = "kinplus_locale"
    private const val KEY_LANG = "language_tag"

    val supportedTags = listOf("en", "zu", "nso")

    fun getLanguageTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "en") ?: "en"

    fun setLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, tag).apply()
    }

    /** Returns a context whose resources resolve to the saved language. */
    fun wrap(base: Context): Context {
        val tag = getLanguageTag(base)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
