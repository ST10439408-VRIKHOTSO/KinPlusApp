package za.co.kinplus.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "kinplus_settings")

/**
 * User preferences (FR-32): units, notification opt-in and the mirrored
 * language tag. Backed by Jetpack DataStore so reads are reactive Flows the
 * Settings screen can observe.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val METRIC = booleanPreferencesKey("units_metric")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val LANGUAGE = stringPreferencesKey("language_tag")
        val MAILING_LIST = booleanPreferencesKey("mailing_list_consent")
    }

    val useMetric: Flow<Boolean> = context.dataStore.data.map { it[Keys.METRIC] ?: true }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }
    val languageTag: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val mailingListConsent: Flow<Boolean> = context.dataStore.data.map { it[Keys.MAILING_LIST] ?: false }

    suspend fun setUseMetric(value: Boolean) =
        context.dataStore.edit { it[Keys.METRIC] = value }.let { }

    suspend fun setNotificationsEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }.let { }

    suspend fun setLanguageTag(tag: String) =
        context.dataStore.edit { it[Keys.LANGUAGE] = tag }.let { }

    suspend fun setMailingListConsent(value: Boolean) =
        context.dataStore.edit { it[Keys.MAILING_LIST] = value }.let { }
}
