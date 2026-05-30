package com.example.speak2easy.di

import android.content.Context
import com.example.speak2easy.data.auth.AuthRepository
import com.example.speak2easy.data.auth.TokenStore
import com.example.speak2easy.data.lessons.LessonRepository
import com.example.speak2easy.data.practice.PracticeRepository
import com.example.speak2easy.data.practice.ProgressRepository
import com.example.speak2easy.data.prefs.SonariPreferences
import com.example.speak2easy.data.remote.AuthApi
import com.example.speak2easy.data.remote.AuthInterceptor
import com.example.speak2easy.data.remote.ContentApi
import com.example.speak2easy.data.remote.PracticeApi
import com.example.speak2easy.data.remote.SonariJson
import com.example.speak2easy.data.remote.UserApi
import com.example.speak2easy.data.remote.WritingApi
import com.example.speak2easy.data.writing.StrokeRepository
import com.example.speak2easy.service.HapticsManager
import com.example.speak2easy.service.NetworkMonitor
import com.example.speak2easy.service.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual dependency-injection container, created once in [com.example.speak2easy.Speak2EasyApp].
 * Holds preferences, the networking stack, and repositories. Triggers session restore at startup.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json: Json = SonariJson

    val preferences = SonariPreferences(appContext)

    private val tokenStore = TokenStore(appContext, json)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    private val userApi: UserApi = retrofit.create(UserApi::class.java)
    private val contentApi: ContentApi = retrofit.create(ContentApi::class.java)
    private val practiceApi: PracticeApi = retrofit.create(PracticeApi::class.java)
    private val writingApi: WritingApi = retrofit.create(WritingApi::class.java)

    val applicationContext: Context = appContext

    val hapticsManager = HapticsManager(appContext)
    val networkMonitor = NetworkMonitor(appContext)
    val reminderManager = ReminderManager(appContext)

    val authRepository = AuthRepository(authApi, userApi, tokenStore, json)
    val lessonRepository = LessonRepository(contentApi, json)
    val progressRepository = ProgressRepository(userApi, practiceApi, json)
    val practiceRepository = PracticeRepository(practiceApi, json, progressRepository)
    val strokeRepository = StrokeRepository(writingApi, json)

    init {
        appScope.launch { authRepository.restoreSession() }
        appScope.launch { preferences.hapticsEnabled.collect { hapticsManager.enabled = it } }
        // Keep the daily reminder alarm in sync with the toggle + chosen time.
        appScope.launch {
            combine(preferences.notificationsEnabled, preferences.reminderMinuteOfDay) { on, min -> on to min }
                .collect { (on, min) ->
                    if (on && reminderManager.isOsPermissionGranted()) {
                        reminderManager.scheduleDailyReminder(min / 60, min % 60)
                    } else {
                        reminderManager.cancelReminder()
                    }
                }
        }
    }

    private companion object {
        const val BASE_URL = "https://api.sonarilabs.com/api/v1/"
    }
}
