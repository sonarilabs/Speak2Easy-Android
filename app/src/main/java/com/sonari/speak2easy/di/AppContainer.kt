package com.sonari.speak2easy.di

import android.content.Context
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.auth.AuthState
import com.sonari.speak2easy.data.auth.TokenStore
import com.sonari.speak2easy.data.feedback.FeedbackRepository
import com.sonari.speak2easy.data.lessons.LessonRepository
import com.sonari.speak2easy.data.practice.PracticeRepository
import com.sonari.speak2easy.data.practice.ProgressRepository
import com.sonari.speak2easy.data.prefs.SonariPreferences
import com.sonari.speak2easy.data.remote.AuthApi
import com.sonari.speak2easy.data.remote.AuthInterceptor
import com.sonari.speak2easy.data.remote.TokenAuthenticator
import com.sonari.speak2easy.data.remote.ContentApi
import com.sonari.speak2easy.data.remote.FeedbackApi
import com.sonari.speak2easy.data.remote.PracticeApi
import com.sonari.speak2easy.data.remote.SonariJson
import com.sonari.speak2easy.data.remote.SubscriptionApi
import com.sonari.speak2easy.data.remote.UserApi
import com.sonari.speak2easy.data.remote.WritingApi
import com.sonari.speak2easy.BuildConfig
import com.sonari.speak2easy.data.subscription.SubscriptionRepository
import com.sonari.speak2easy.data.writing.StrokeRepository
import com.sonari.speak2easy.service.HapticsManager
import com.sonari.speak2easy.service.NetworkMonitor
import com.sonari.speak2easy.service.ReminderManager
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
 * Manual dependency-injection container, created once in [com.sonari.speak2easy.Speak2EasyApp].
 * Holds preferences, the networking stack, and repositories. Triggers session restore at startup.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json: Json = SonariJson

    val preferences = SonariPreferences(appContext)

    private val tokenStore = TokenStore(appContext, json)

    // Late-bound AuthApi reference — TokenAuthenticator needs to call it on 401s, but the
    // authenticator must be installed on OkHttp before Retrofit can synthesize the api proxy.
    // We resolve the circular dep by passing a `() -> AuthApi` supplier and assigning the
    // proxy as soon as Retrofit finishes building.
    private var authApiRef: AuthApi? = null

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        // Body logging is debug-only — release builds would otherwise dump every
        // request/response (including `Authorization: Bearer …`) to Logcat, where
        // any tool with adb access could harvest live tokens.
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            }
        }
        .authenticator(
            TokenAuthenticator(
                tokenStore = tokenStore,
                authApi = { authApiRef ?: error("AuthApi not yet initialized") },
                onRefreshFailed = { tokenStore.clear() },
            ),
        )
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java).also { authApiRef = it }
    private val userApi: UserApi = retrofit.create(UserApi::class.java)
    private val contentApi: ContentApi = retrofit.create(ContentApi::class.java)
    private val practiceApi: PracticeApi = retrofit.create(PracticeApi::class.java)
    private val writingApi: WritingApi = retrofit.create(WritingApi::class.java)
    private val feedbackApi: FeedbackApi = retrofit.create(FeedbackApi::class.java)
    private val subscriptionApi: SubscriptionApi = retrofit.create(SubscriptionApi::class.java)

    val applicationContext: Context = appContext

    val hapticsManager = HapticsManager(appContext)
    val networkMonitor = NetworkMonitor(appContext)
    val reminderManager = ReminderManager(appContext)

    val authRepository = AuthRepository(authApi, userApi, tokenStore, json)
    val lessonRepository = LessonRepository(contentApi, json)
    val progressRepository = ProgressRepository(userApi, practiceApi, json)
    val practiceRepository = PracticeRepository(practiceApi, json, progressRepository, lessonRepository)
    val strokeRepository = StrokeRepository(writingApi, json)
    val feedbackRepository = FeedbackRepository(feedbackApi, json, appContext)
    val subscriptionRepository = SubscriptionRepository(subscriptionApi, authRepository, json)

    private var activeUserId: String? = null

    init {
        appScope.launch { authRepository.restoreSession() }
        appScope.launch {
            authRepository.authState.collect { state ->
                val nextUserId = when (state) {
                    is AuthState.Authenticated -> state.user.userId
                    is AuthState.PendingVerification -> state.user.userId
                    else -> null
                }
                val previousUserId = activeUserId
                if (previousUserId != null && previousUserId != nextUserId) {
                    clearUserScopedCaches()
                }
                activeUserId = nextUserId
            }
        }
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

    private suspend fun clearUserScopedCaches() {
        lessonRepository.invalidateAll()
        progressRepository.invalidateAll()
        strokeRepository.invalidateAll()
    }

    private companion object {
        const val BASE_URL = "https://api.sonarilabs.com/api/v1/"
    }
}
