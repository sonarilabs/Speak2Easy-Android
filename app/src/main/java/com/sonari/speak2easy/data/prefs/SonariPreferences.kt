package com.sonari.speak2easy.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sonari.speak2easy.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reactive UI preferences backed by DataStore. Keys mirror the iOS
 * UserDefaults keys so persisted behavior stays familiar across platforms.
 */
class SonariPreferences(context: Context) {

    private val store = context.applicationContext.sonariDataStore

    private object Keys {
        val THEME = stringPreferencesKey("sonari_app_theme")
        val HAPTICS = booleanPreferencesKey("sonari_haptics_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("sonari_notifications_enabled")
        // Minute-of-day (0..1439) — DST-safe wall-clock storage.
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("sonari_reminder_time")
        // Onboarding mirror — set on submit. Drives initial lesson unlock state without a re-fetch.
        val JAPANESE_LEVEL = stringPreferencesKey("sonari_japanese_level")
        // Auto-progression flags — flipped on once the learner finishes the preceding charset.
        val KATAKANA_UNLOCKED_BY_COMPLETION = booleanPreferencesKey("sonari_katakana_unlocked_by_completion")
        val WORDS_UNLOCKED_BY_COMPLETION = booleanPreferencesKey("sonari_words_unlocked_by_completion")
    }

    val theme: Flow<AppTheme> = store.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            AppTheme.LIGHT.rawValue -> AppTheme.LIGHT
            else -> AppTheme.DARK // default dark, mirroring iOS
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        store.edit { it[Keys.THEME] = theme.rawValue }
    }

    val hapticsEnabled: Flow<Boolean> = store.data.map { it[Keys.HAPTICS] ?: true }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[Keys.HAPTICS] = enabled }
    }

    val notificationsEnabled: Flow<Boolean> = store.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Default 20:00 (8 PM) — matches iOS's default reminder time. */
    val reminderMinuteOfDay: Flow<Int> = store.data.map { it[Keys.REMINDER_MINUTE_OF_DAY] ?: (20 * 60) }

    suspend fun setReminderMinuteOfDay(minute: Int) {
        store.edit { it[Keys.REMINDER_MINUTE_OF_DAY] = minute.coerceIn(0, 1439) }
    }

    /** Local mirror of the user's onboarding choice; null until onboarding completes. */
    val japaneseLevel: Flow<String?> = store.data.map { it[Keys.JAPANESE_LEVEL] }

    suspend fun setJapaneseLevel(level: String) {
        store.edit { it[Keys.JAPANESE_LEVEL] = level }
    }

    val katakanaUnlockedByCompletion: Flow<Boolean> =
        store.data.map { it[Keys.KATAKANA_UNLOCKED_BY_COMPLETION] ?: false }

    suspend fun setKatakanaUnlockedByCompletion(on: Boolean) {
        store.edit { it[Keys.KATAKANA_UNLOCKED_BY_COMPLETION] = on }
    }

    val wordsUnlockedByCompletion: Flow<Boolean> =
        store.data.map { it[Keys.WORDS_UNLOCKED_BY_COMPLETION] ?: false }

    suspend fun setWordsUnlockedByCompletion(on: Boolean) {
        store.edit { it[Keys.WORDS_UNLOCKED_BY_COMPLETION] = on }
    }
}
